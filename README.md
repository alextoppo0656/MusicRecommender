# 🎧 AI Music Recommender

A full-stack music recommendation system powered by Spring Boot and React.

## Features

- 🎵 Spotify integration for liked songs
- 🎸 Last.fm integration for music discovery
- 🧠 Machine Learning recommendations
- 👥 Multi-user support with session isolation
- 🔐 Secure OAuth authentication
- 📊 Real-time feedback training

## Tech Stack

**Backend:**
- Spring Boot 3.2
- Spring Security + JWT
- JPA/Hibernate with H2
- Smile ML (Random Forest)
- WebFlux for async calls

**Frontend:**
- React 18
- Vite
- React Router
- Axios
- Lucide Icons

## Setup Instructions

1. **Environment Variables:**
   ```bash
   cd backend
   cp .env.example .env
   # Edit .env with your API keys
   ```

2. **Backend:**
   ```bash
   cd backend
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

3. **Frontend:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. **Access:**
   - Frontend: http://localhost:3000
   - Backend: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console

## API Credentials

### Spotify:
1. Go to https://developer.spotify.com/dashboard
2. Create an app
3. Add redirect URI: `http://localhost:3000/callback`
4. Copy Client ID and Secret

### Last.fm:
1. Go to https://www.last.fm/api/account/create
2. Create an API account
3. Copy API Key

## Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   React     │ ───► │ Spring Boot  │ ───► │  Database   │
│  Frontend   │      │   Backend    │      │  (H2/SQL)   │
└─────────────┘      └──────────────┘      └─────────────┘
                            │
                            ├─────► Spotify API
                            └─────► Last.fm API
```

## License

MIT
