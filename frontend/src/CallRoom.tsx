import { Client } from '@stomp/stompjs'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { API_BASE_URL, appendTranscript, endSession, getEvaluations, getQuestions, markQuestionAsked, submitReview } from './lib/api'
import type { Evaluation, Session, SessionQuestion } from './lib/api'

type SignalPayload = { type: string; payload: Record<string, unknown> }
type SpeechResultEvent = Event & { resultIndex: number; results: { length: number; [index: number]: { isFinal: boolean; [index: number]: { transcript: string } } } }
type SpeechRecognitionLike = { continuous: boolean; interimResults: boolean; lang: string; onresult: ((event: SpeechResultEvent) => void) | null; start: () => void; stop: () => void }
type SpeechRecognitionConstructor = new () => SpeechRecognitionLike

export function CallRoom({ session, onEnd }: { session: Session; onEnd: () => void }) {
  const localVideo = useRef<HTMLVideoElement>(null)
  const remoteVideo = useRef<HTMLVideoElement>(null)
  const peerConnection = useRef<RTCPeerConnection | null>(null)
  const client = useRef<Client | null>(null)
  const subscription = useRef<StompSubscription | null>(null)
  const transcriptSubscription = useRef<StompSubscription | null>(null)
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState('')
  const [questions, setQuestions] = useState<SessionQuestion[]>([])
  const [transcript, setTranscript] = useState<string[]>([])
  const [timerStart, setTimerStart] = useState<number | null>(null)
  const [elapsed, setElapsed] = useState(0)
  const [ended, setEnded] = useState(false)
  const [ending, setEnding] = useState(false)
  const [evaluations, setEvaluations] = useState<Evaluation[]>([])
  const [report, setReport] = useState<string | null>(null)
  const sections = session.sections.length ? session.sections : [{ name: 'Intro', durationMinutes: 5 }]
  const currentSection = sections.reduce<{ name: string; durationMinutes: number; sectionStart: number }>((selected, section, index) => {
    const sectionStart = sections.slice(0, index).reduce((total, item) => total + item.durationMinutes * 60, 0)
    return elapsed >= sectionStart ? { ...section, sectionStart } : selected
  }, { ...sections[0], sectionStart: 0 })
  const remaining = Math.max(0, currentSection.durationMinutes * 60 - (elapsed - currentSection.sectionStart))
  const timerLabel = `${String(Math.floor(remaining / 60)).padStart(2, '0')}:${String(remaining % 60).padStart(2, '0')}`

  useEffect(() => {
    let active = true
    let mediaStream: MediaStream | null = null
    const connection = new RTCPeerConnection({
      iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
    })
    peerConnection.current = connection

    async function start() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
        mediaStream = stream
        if (!active) return
        if (localVideo.current) localVideo.current.srcObject = stream
        stream.getTracks().forEach((track) => connection.addTrack(track, stream))
        connection.ontrack = (event) => {
          if (remoteVideo.current) remoteVideo.current.srcObject = event.streams[0]
        }
        const signaling = new Client({
          webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
          reconnectDelay: 3000,
          onConnect: async () => {
            if (!active) return
            setConnected(true)
            subscription.current = signaling.subscribe(`/topic/session/${session.id}/signal`, async (message: IMessage) => {
              const signal = JSON.parse(message.body) as SignalPayload
              if (signal.type === 'offer') {
                await connection.setRemoteDescription(signal.payload as unknown as RTCSessionDescriptionInit)
                const answer = await connection.createAnswer()
                await connection.setLocalDescription(answer)
                signaling.publish({ destination: `/app/session/${session.id}/signal`, body: JSON.stringify({ type: 'answer', payload: answer }) })
              } else if (signal.type === 'answer') {
                await connection.setRemoteDescription(signal.payload as unknown as RTCSessionDescriptionInit)
              } else if (signal.type === 'candidate') {
                await connection.addIceCandidate(signal.payload as RTCIceCandidateInit)
              } else if (signal.type === 'timer') {
                setTimerStart(signal.payload.startedAt as number)
              }
            })
            if (session.role === 'INTERVIEWER') {
              const startedAt = Date.now()
              setTimerStart(startedAt)
              signaling.publish({ destination: `/app/session/${session.id}/signal`, body: JSON.stringify({ type: 'timer', payload: { startedAt } }) })
            } else {
              transcriptSubscription.current = signaling.subscribe(`/topic/session/${session.id}/transcript`, (message: IMessage) => {
                const item = JSON.parse(message.body) as { text: string }
                setTranscript((current) => [...current, item.text])
              })
            }
            if (session.role === 'INTERVIEWER') {
              const offer = await connection.createOffer()
              await connection.setLocalDescription(offer)
              signaling.publish({ destination: `/app/session/${session.id}/signal`, body: JSON.stringify({ type: 'offer', payload: offer }) })
            }
          },
          onStompError: () => setError('The signaling server rejected the connection.'),
          onWebSocketError: () => setError('The call server is unavailable. Check that the backend is running.'),
        })
        connection.onicecandidate = (event) => {
          if (event.candidate && signaling.connected) {
            signaling.publish({ destination: `/app/session/${session.id}/signal`, body: JSON.stringify({ type: 'candidate', payload: event.candidate }) })
          }
        }
        client.current = signaling
        signaling.activate()
      } catch (mediaError) {
        setError(mediaError instanceof Error ? mediaError.message : 'Camera and microphone access is required.')
      }
    }
    start()

    return () => {
      active = false
      subscription.current?.unsubscribe()
      transcriptSubscription.current?.unsubscribe()
      client.current?.deactivate()
      connection.close()
      mediaStream?.getTracks().forEach((track) => track.stop())
    }
  }, [session.id, session.role])

  useEffect(() => {
    if (timerStart === null) return
    const timer = window.setInterval(() => setElapsed(Math.max(0, Math.floor((Date.now() - timerStart) / 1000))), 1000)
    return () => window.clearInterval(timer)
  }, [timerStart])

  useEffect(() => {
    if (session.role !== 'INTERVIEWEE') return
    const Recognition = (window as unknown as { SpeechRecognition?: SpeechRecognitionConstructor }).SpeechRecognition
    if (!Recognition) {
      return
    }
    const recognition = new Recognition()
    recognition.continuous = true
    recognition.interimResults = false
    recognition.lang = 'en-US'
    recognition.onresult = (event) => {
      for (let index = event.resultIndex; index < event.results.length; index += 1) {
        if (event.results[index].isFinal) {
          const text = event.results[index][0].transcript.trim()
          if (text) appendTranscript(session.id, text, currentSection.name).catch(() => setError('Transcript could not be saved.'))
        }
      }
    }
    recognition.start()
    return () => recognition.stop()
  }, [session.id, session.role, currentSection.name])

  useEffect(() => {
    if (session.role !== 'INTERVIEWER') return
    getQuestions(session.id).then(setQuestions).catch((questionError) => setError(questionError.message))
  }, [session.id, session.role])

  async function markAsked(question: SessionQuestion) {
    try {
      const updated = await markQuestionAsked(session.id, question.id)
      setQuestions((current) => current.map((item) => item.id === updated.id ? updated : item))
    } catch (questionError) {
      setError(questionError instanceof Error ? questionError.message : 'Unable to mark question')
    }
  }

  async function finishCall() {
    if (ending) return
    setEnding(true)
    try {
      await endSession(session.id)
      setEnded(true)
      if (session.role === 'INTERVIEWER') {
        getEvaluations(session.id).then(setEvaluations).catch((evaluationError) => {
          setError(evaluationError instanceof Error ? evaluationError.message : 'Review data could not be loaded.')
        })
      }
    } catch (endError) {
      setError(endError instanceof Error ? endError.message : 'Unable to end the call')
    } finally {
      setEnding(false)
    }
  }

  if (ended && session.role === 'INTERVIEWER') return <ReviewPanel sessionId={session.id} evaluations={evaluations} report={report} onReport={setReport} onClose={onEnd} />
  if (ended) return <main className="call-page"><section className="ended-state"><p className="eyebrow">Interview complete</p><h1>Thanks for practicing together.</h1><button className="primary-action" type="button" onClick={onEnd}>Return to dashboard</button></section></main>

  return (
    <main className="call-page">
      <header className="call-header">
        <a className="brand" href="/"><span className="brand-mark" aria-hidden="true">PV</span><span>PeerView</span></a>
        <span className={connected ? 'call-status live' : 'call-status'}>{connected ? 'Connected' : 'Connecting'}</span>
        <button className="end-button" type="button" onClick={finishCall} disabled={ending}>{ending ? 'Ending...' : 'End call'}</button>
      </header>
      <section className="call-layout">
        <div className="video-stage">
          <video ref={remoteVideo} className="remote-video" autoPlay playsInline />
          <div className="remote-placeholder">Waiting for your peer to join the camera...</div>
          <video ref={localVideo} className="local-video" autoPlay muted playsInline />
        </div>
        <aside className="call-rail">
          <p className="eyebrow">{session.domain}</p>
          <h1>{session.interviewType}</h1>
          <div className="timer-block"><span>{currentSection.name}</span><strong>{timerLabel}</strong></div>
          <p className="call-note">Your session room is live. Questions, transcript, and synced sections will appear here as those phases land.</p>
          {session.role === 'INTERVIEWER' && <div className="question-panel"><h2>Question set</h2>{questions.map((question) => <button className={question.askedAt ? 'question asked' : 'question'} type="button" key={question.id} onClick={() => markAsked(question)}><span>{question.sectionName}</span>{question.text}{question.askedAt && <small>Asked</small>}</button>)}</div>}
          {session.role === 'INTERVIEWER' && <div className="transcript-panel"><h2>Live transcript</h2>{transcript.length ? transcript.map((line, index) => <p key={`${line}-${index}`}>{line}</p>) : <p className="muted-copy">Waiting for the interviewee to speak...</p>}</div>}
          {error && <p className="form-error" role="alert">{error}</p>}
          {session.role === 'INTERVIEWEE' && !('SpeechRecognition' in window) && <p className="form-error" role="alert">Live transcript requires Chrome or another browser with Web Speech support.</p>}
        </aside>
      </section>
    </main>
  )
}

function ReviewPanel({ sessionId, evaluations, report, onReport, onClose }: { sessionId: string; evaluations: Evaluation[]; report: string | null; onReport: (report: string) => void; onClose: () => void }) {
  const [items, setItems] = useState(() => evaluations.map((item) => ({ questionId: item.questionId, score: item.aiScore, feedback: '' })))
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')
  useEffect(() => {
    setItems((current) => evaluations.map((item) => current.find((existing) => existing.questionId === item.questionId) ?? ({ questionId: item.questionId, score: item.aiScore, feedback: '' })))
  }, [evaluations])
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try { const result = await submitReview(sessionId, items, notes); onReport(result.narrative) } catch (submitError) { setError(submitError instanceof Error ? submitError.message : 'Unable to submit review') }
  }
  return <main className="review-page"><header className="call-header"><a className="brand" href="/"><span className="brand-mark" aria-hidden="true">PV</span><span>PeerView</span></a><button className="end-button" type="button" onClick={onClose}>Dashboard</button></header><section className="review-content"><p className="eyebrow">Interviewer review</p><h1>Make the final call.</h1><p className="review-intro">The AI suggestions are a second opinion. Your scores below are the authoritative result.</p>{report ? <div className="report-box"><span className="number">Final report</span><p>{report}</p></div> : !evaluations.length || items.length !== evaluations.length ? <><p className="muted-copy">Review data is not available yet.</p>{error && <p className="form-error" role="alert">{error}</p>}<button className="text-button" type="button" onClick={onClose}>Return to dashboard</button></> : <form onSubmit={submit} className="review-form">{evaluations.map((evaluation, index) => { const item = items[index] ?? { questionId: evaluation.questionId, score: evaluation.aiScore, feedback: '' }; return <article className="review-item" key={evaluation.questionId}><h2>{evaluation.question}</h2><p className="ai-suggestion">AI suggestion: {evaluation.aiScore}/5. {evaluation.aiFeedback}</p><label>Your score (1-5)<input type="number" min="1" max="5" value={item.score} onChange={(event) => setItems((current) => current.map((currentItem) => currentItem.questionId === item.questionId ? { ...currentItem, score: Number(event.target.value) } : currentItem))} required /></label><label>Your feedback<textarea value={item.feedback} onChange={(event) => setItems((current) => current.map((currentItem) => currentItem.questionId === item.questionId ? { ...currentItem, feedback: event.target.value } : currentItem))} /></label></article> })}<label>Overall notes<textarea value={notes} onChange={(event) => setNotes(event.target.value)} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="primary-action" type="submit">Submit final review <span aria-hidden="true">&#8594;</span></button></form>}</section></main>
}