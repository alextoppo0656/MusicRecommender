package com.musicrec.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Value("${app.recommendation.cache-ttl-seconds:3600}")
    private long cacheTtl;
    
    @Value("${app.recommendation.cache-max-size:1000}")
    private long cacheMaxSize;
    
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
            .maximumSize(cacheMaxSize)
            .recordStats();
    }
    
    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("recommendations", "spotifyData");
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}