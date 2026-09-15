# PeerView

Peer-to-peer mock interview platform for realistic interview practice with another person.

## MVP status

The repository now contains the demoable MVP foundation and end-to-end interview workflow:

- `backend/`: Java 21-targeted Spring Boot 3.5 Maven application with JPA, PostgreSQL, and Actuator.
- `frontend/`: React and TypeScript application built with Vite.
- `render.yaml`, `backend/Dockerfile`, and Vercel rewrites for the free hosting layout.

The MVP uses a deterministic local question fallback when Gemini is disabled, browser-native Web Speech in Chromium browsers, Google's public STUN server, and Spring's in-memory STOMP broker. Redis relay, TURN, and container orchestration for scale are designed for later work.

### Run the backend

The application expects PostgreSQL by default:

```bash
cd backend
export DATABASE_URL=jdbc:postgresql://localhost:5432/peerview
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
mvn spring-boot:run
```

The health endpoint is available at `http://localhost:8080/actuator/health`.

### Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend is available at `http://localhost:5173`.

### Validate the phase

```bash
cd frontend && npm run build
cd ../backend && mvn test
```

Backend tests use an in-memory H2 database so they do not require a local PostgreSQL instance.

## Deployment

1. Create a Neon PostgreSQL database and copy its JDBC URL.
2. Create a Render web service from this repository using `render.yaml`.
3. Set the Render variables from `backend/.env.example`, including a long random `JWT_SECRET`, the Vercel URL in `FRONTEND_URL` and `CORS_ORIGINS`, and optional Gemini, Google OAuth, and SMTP credentials.
4. Import the repository into Vercel with the root directory set to `frontend` and `VITE_API_URL` set to the Render backend URL.
5. Warm the Render service before a live demo because the free tier sleeps when idle.

Google OAuth redirect URI: `https://<render-backend-host>/login/oauth2/code/google`.
