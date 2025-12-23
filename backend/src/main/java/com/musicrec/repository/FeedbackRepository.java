package com.musicrec.repository;

import com.musicrec.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    List<Feedback> findByUserId(String userId);
    
    List<Feedback> findByUserIdAndLiked(String userId, Boolean liked);
    
    Long countByUserId(String userId);
    
    Long countByUserIdAndLiked(String userId, Boolean liked);
    
    Optional<Feedback> findByUserIdAndTrackNameAndArtist(
        String userId, String trackName, String artist
    );
    
    void deleteByUserId(String userId);
}
