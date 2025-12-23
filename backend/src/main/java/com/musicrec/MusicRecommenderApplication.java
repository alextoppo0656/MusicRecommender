package com.musicrec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class MusicRecommenderApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MusicRecommenderApplication.class, args);
        System.out.println("\n🎧 AI Music Recommender Backend Started Successfully!");
        System.out.println("📍 Server: http://localhost:8080");
        System.out.println("📊 H2 Console: http://localhost:8080/h2-console\n");
    }
}