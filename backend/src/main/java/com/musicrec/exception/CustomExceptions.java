package com.musicrec.exception;

public class CustomExceptions {
    
    public static class SpotifyApiException extends RuntimeException {
        public SpotifyApiException(String message) {
            super(message);
        }
        public SpotifyApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class LastFmApiException extends RuntimeException {
        public LastFmApiException(String message) {
            super(message);
        }
        public LastFmApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
    
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
    
    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}