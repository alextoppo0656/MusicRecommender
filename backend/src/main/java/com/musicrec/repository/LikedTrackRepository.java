package com.musicrec.repository;

import com.musicrec.entity.LikedTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LikedTrackRepository extends JpaRepository<LikedTrack, Long> {
    
    List<LikedTrack> findByUserId(String userId);
    
    Long countByUserId(String userId);
    
    Optional<LikedTrack> findByUserIdAndTrackNameAndArtist(
        String userId, String trackName, String artist
    );
    
    boolean existsByUserIdAndTrackNameAndArtist(
        String userId, String trackName, String artist
    );
    
    void deleteByUserId(String userId);
}