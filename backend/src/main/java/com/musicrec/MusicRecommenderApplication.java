package com.musicrec;

import io.github.cdimascio.dotenv.Dotenv;
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
        // Load .env file
        try {
            Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();
            
            // Set environment variables
            dotenv.entries().forEach(entry -> 
                System.setProperty(entry.getKey(), entry.getValue())
            );
            
            System.out.println("✅ Environment variables loaded from .env");
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not load .env file: " + e.getMessage());
        }
        
        SpringApplication.run(MusicRecommenderApplication.class, args);
        
        System.out.println("\n🎧 AI Music Recommender Backend Started Successfully!");
        System.out.println("📍 Server: http://127.0.0.1:8080");
        System.out.println("📊 H2 Console: http://127.0.0.1:8080/h2-console\n");
    }
}