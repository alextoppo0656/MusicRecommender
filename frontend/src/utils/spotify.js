const SPOTIFY_CLIENT_ID = import.meta.env.VITE_SPOTIFY_CLIENT_ID
const REDIRECT_URI = import.meta.env.VITE_SPOTIFY_REDIRECT_URI || 'http://localhost:3000/callback'
const SCOPES = ['user-library-read']

export const getSpotifyAuthUrl = () => {
  const params = new URLSearchParams({
    client_id: SPOTIFY_CLIENT_ID,
    response_type: 'code',
    redirect_uri: REDIRECT_URI,
    scope: SCOPES.join(' '),
    show_dialog: 'true' // Force account picker for multi-user support
  })
  
  return `https://accounts.spotify.com/authorize?${params.toString()}`
}

export const getSpotifySearchUrl = (trackName, artist) => {
  const query = encodeURIComponent(`${trackName} ${artist}`)
  return `https://open.spotify.com/search/${query}`
}