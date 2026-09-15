import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { CallRoom } from './CallRoom'
import { API_BASE_URL, clearToken, createSession, currentUser, getCatalog, getDashboard, getInvite, joinSession, login, register, saveToken } from './lib/api'
import type { User, Domain, InterviewType, Session, DashboardSession } from './lib/api'

function App() {
  const oauthToken = new URLSearchParams(window.location.search).get('token')
  if (oauthToken && !localStorage.getItem('peerview_token')) {
    saveToken(oauthToken)
    window.history.replaceState({}, '', window.location.pathname)
  }
  const joinToken = window.location.pathname.startsWith('/join/') ? window.location.pathname.split('/')[2] : null
  const isAbout = window.location.pathname === '/about'
  const [user, setUser] = useState<User | null>(null)
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!localStorage.getItem('peerview_token')) return
    currentUser().then(setUser).catch(clearToken)
  }, [])

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const response = mode === 'register'
        ? await register(name, email, password)
        : await login(email, password)
      saveToken(response.token)
      setUser(response.user)
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'Unable to sign in')
    } finally {
      setLoading(false)
    }
  }

  if (user) {
    return <Dashboard user={user} onSignOut={() => { clearToken(); setUser(null) }} />
  }

  if (isAbout) return <AboutPage />
  if (joinToken) return <JoinPage token={joinToken} />

  return (
    <main className="page-shell">
      <nav className="topbar" aria-label="Primary navigation">
        <a className="brand" href="/" aria-label="PeerView home">
          <span className="brand-mark" aria-hidden="true">PV</span>
          <span>PeerView</span>
        </a>
        <div className="nav-actions"><a className="about-link" href="/about">About</a><a className="phase-label" href={`${API_BASE_URL}/oauth2/authorization/google`}>Sign in with Google</a></div>
      </nav>

      <section className="hero" aria-labelledby="hero-title">
        <div className="hero-copy">
          <p className="eyebrow">Practice with a real person</p>
          <h1 id="hero-title">Interviews are conversations, not quizzes.</h1>
          <p className="hero-text">
            PeerView pairs people in the same field for a focused, realistic mock interview.
            Start with a quiet dashboard, then invite a peer when you are ready.
          </p>
          <form className="auth-form" onSubmit={submit}>
            <div className="auth-tabs" role="tablist" aria-label="Authentication mode">
              <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Sign in</button>
              <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Create account</button>
            </div>
            {mode === 'register' && (
              <label>
                Name
                <input value={name} onChange={(event) => setName(event.target.value)} autoComplete="name" required />
              </label>
            )}
            <label>
              Email
              <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required />
            </label>
            <label>
              Password
              <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} minLength={8} required />
            </label>
            {error && <p className="form-error" role="alert">{error}</p>}
            <button className="primary-action" type="submit" disabled={loading}>
              {loading ? 'Connecting...' : mode === 'login' ? 'Sign in' : 'Create account'} <span aria-hidden="true">&#8594;</span>
            </button>
          </form>
        </div>
        <div className="signal-panel" aria-label="PeerView foundation status">
          <div className="signal-line"><span className="signal-dot" /> Backend ready</div>
          <div className="signal-line"><span className="signal-dot" /> Frontend ready</div>
          <div className="signal-line muted"><span className="signal-dot" /> Your first session is next</div>
        </div>
      </section>

      <section className="principles" aria-label="Product principles">
        <article>
          <span className="number">01</span>
          <h2>Real-time practice</h2>
          <p>Build the calm, responsive thinking that only happens with another person.</p>
        </article>
        <article>
          <span className="number">02</span>
          <h2>Useful reflection</h2>
          <p>Leave the call with a clear report, grounded in what you actually said.</p>
        </article>
        <article>
          <span className="number">03</span>
          <h2>Peer perspective</h2>
          <p>Trade roles and learn how your answers land from the other side of the table.</p>
        </article>
      </section>
    </main>
  )
}

function Dashboard({ user, onSignOut }: { user: User; onSignOut: () => void }) {
  const [domains, setDomains] = useState<Domain[]>([])
  const [types, setTypes] = useState<InterviewType[]>([])
  const [domainId, setDomainId] = useState('')
  const [typeId, setTypeId] = useState('')
  const [role, setRole] = useState<Session['role']>('INTERVIEWER')
  const [peerEmail, setPeerEmail] = useState('')
  const [session, setSession] = useState<Session | null>(null)
  const [activeSession, setActiveSession] = useState<Session | null>(null)
  const [error, setError] = useState('')
  const [history, setHistory] = useState<DashboardSession[]>([])

  useEffect(() => { getCatalog().then(([loadedDomains, loadedTypes]) => { setDomains(loadedDomains); setTypes(loadedTypes); setDomainId(loadedDomains[0]?.id ?? ''); setTypeId(loadedTypes[0]?.id ?? '') }).catch((loadError) => setError(loadError.message)); getDashboard().then(setHistory).catch(() => undefined) }, [])

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    try { setSession(await createSession(domainId, typeId, role, peerEmail)) } catch (createError) { setError(createError instanceof Error ? createError.message : 'Unable to create session') }
  }

  if (activeSession) return <CallRoom session={activeSession} onEnd={() => setActiveSession(null)} />

  return (
    <main className="page-shell dashboard-page">
      <nav className="topbar" aria-label="Primary navigation">
        <a className="brand" href="/" aria-label="PeerView home"><span className="brand-mark" aria-hidden="true">PV</span><span>PeerView</span></a>
        <button className="text-button" type="button" onClick={onSignOut}>Sign out</button>
      </nav>
      <section className="dashboard-hero">
        <p className="eyebrow">Your practice room</p>
        <h1>Welcome, {user.name.split(' ')[0]}.</h1>
        <p className="hero-text">Your interview history and next practice session will live here.</p>
      </section>
      <section className="empty-dashboard" aria-label="Create an interview session">
        <span className="number">Start a session</span>
        <h2>Invite a peer into the room.</h2>
        <form className="session-form" onSubmit={create}>
          <label>Field<select value={domainId} onChange={(event) => setDomainId(event.target.value)} required>{domains.map((domain) => <option key={domain.id} value={domain.id}>{domain.name}</option>)}</select></label>
          <label>Interview type<select value={typeId} onChange={(event) => setTypeId(event.target.value)} required>{types.map((type) => <option key={type.id} value={type.id}>{type.name}</option>)}</select></label>
          <label>Your role<select value={role} onChange={(event) => setRole(event.target.value as Session['role'])}><option value="INTERVIEWER">Interviewer</option><option value="INTERVIEWEE">Interviewee</option></select></label>
          <label>Peer email<input type="email" value={peerEmail} onChange={(event) => setPeerEmail(event.target.value)} required /></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="primary-action" type="submit">Create invite <span aria-hidden="true">&#8594;</span></button>
        </form>
        {session && <div className="invite-result"><strong>Invite ready</strong><span>{session.inviteUrl}</span><button className="text-button" type="button" onClick={() => setActiveSession(session)}>Open waiting room</button></div>}
      </section>
      {history.length > 0 && <section className="history-list" aria-label="Interview history"><span className="number">Your history</span>{history.map((item) => <article key={item.id}><div><strong>{item.interviewType}</strong><span>{item.domain} / {item.role.toLowerCase()} / {item.status.toLowerCase()}</span></div>{item.report ? <p>{item.report}</p> : <span className="muted-copy">Report not available yet</span>}</article>)}</section>}
    </main>
  )
}

function JoinPage({ token }: { token: string }) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [session, setSession] = useState<Session | null>(null)
  const [active, setActive] = useState(false)
  const [error, setError] = useState('')
  const [invite, setInvite] = useState<{ domain: string; interviewType: string; status: string } | null>(null)
  useEffect(() => { getInvite(token).then(setInvite).catch((loadError) => setError(loadError instanceof Error ? loadError.message : 'This invite could not be loaded')) }, [token])
  async function join(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    try { setSession(await joinSession(token, name, email)) } catch (joinError) { setError(joinError instanceof Error ? joinError.message : 'Unable to join invite') }
  }
  if (session && active) return <CallRoom session={session} onEnd={() => setActive(false)} />
  return <main className="page-shell join-page"><nav className="topbar"><a className="brand" href="/"><span className="brand-mark" aria-hidden="true">PV</span><span>PeerView</span></a></nav><section className="join-card"><p className="eyebrow">You are invited</p><h1>Step into the practice room.</h1>{invite && <p className="hero-text">{invite.domain} / {invite.interviewType}. Tell us who is joining. Your role is assigned automatically.</p>}{error && <p className="form-error" role="alert">{error}</p>}{session ? <div className="invite-result"><strong>You are the {session.role === 'INTERVIEWER' ? 'interviewer' : 'interviewee'}.</strong><span>Session setup is ready.</span><button className="text-button" type="button" onClick={() => setActive(true)}>Enter waiting room</button></div> : invite && <form className="auth-form" onSubmit={join}><label>Name<input value={name} onChange={(event) => setName(event.target.value)} required /></label><label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label><button className="primary-action" type="submit">Join session <span aria-hidden="true">&#8594;</span></button></form>}</section></main>
}

function AboutPage() {
  return <main className="page-shell about-page"><nav className="topbar"><a className="brand" href="/"><span className="brand-mark" aria-hidden="true">PV</span><span>PeerView</span></a></nav><section className="about-content"><p className="eyebrow">About PeerView</p><h1>Practice is better with another person.</h1><p className="hero-text">PeerView is an internship project built to make mock interviews feel like the real thing: live, human, and useful afterward.</p><p className="credit">Built by <a href="https://github.com/kelvinmoyo1st">kelvinmoyo1st</a>.</p></section></main>
}

export default App
