package com.musicrec.util;

import com.musicrec.exception.CustomExceptions;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class RateLimiter {
    
    private final Map<String, Queue<Long>> requestTimestamps = new ConcurrentHashMap<>();
    
    public synchronized boolean allowRequest(String key, int maxRequests, long windowSeconds) {
        Queue<Long> timestamps = requestTimestamps.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;
        
        // Remove old timestamps
        timestamps.removeIf(timestamp -> timestamp < windowStart);
        
        if (timestamps.size() < maxRequests) {
            timestamps.add(now);
            return true;
        }
        
        return false;
    }
    
    public void checkRateLimit(String key, int maxRequests, long windowSeconds) {
        if (!allowRequest(key, maxRequests, windowSeconds)) {
            throw new CustomExceptions.RateLimitException(
                "Rate limit exceeded. Please try again later."
            );
        }
    }
    
    public long getWaitTime(String key, int maxRequests, long windowSeconds) {
        Queue<Long> timestamps = requestTimestamps.get(key);
        if (timestamps == null || timestamps.size() < maxRequests) {
            return 0;
        }
        
        long now = Instant.now().getEpochSecond();
        long oldest = timestamps.peek();
        long waitTime = (oldest + windowSeconds) - now;
        
        return Math.max(0, waitTime);
    }
    
    // Cleanup old entries periodically
    public void cleanup() {
        long now = Instant.now().getEpochSecond();
        requestTimestamps.entrySet().removeIf(entry -> {
            Queue<Long> timestamps = entry.getValue();
            timestamps.removeIf(timestamp -> timestamp < now - 3600); // 1 hour
            return timestamps.isEmpty();
        });
    }
}