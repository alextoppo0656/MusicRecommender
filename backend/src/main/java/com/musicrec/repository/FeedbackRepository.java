package com.musicrec.repository;

import com.musicrec.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    /**
     * Find feedback by user and track
     */
    Optional<Feedback> findByUserIdAndTrackId(String userId, Long trackId);
    
    /**
     * Find all feedback for a user
     */
    List<Feedback> findByUserId(String userId);
    
    /**
     * Count feedback by type
     */
    long countByUserIdAndFeedbackType(String userId, String feedbackType);
    
    /**
     * Delete all feedback for a user
     */
    void deleteByUserId(String userId);
}