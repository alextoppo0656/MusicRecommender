package com.musicrec.repository;

import com.musicrec.entity.ExpandedTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpandedTrackRepository extends JpaRepository<ExpandedTrack, Long> {
    
    List<ExpandedTrack> findByUserId(String userId);
    
    Long countByUserId(String userId);
    
    Optional<ExpandedTrack> findByUserIdAndTrackNameAndArtist(
        String userId, String trackName, String artist
    );
    
    boolean existsByUserIdAndTrackNameAndArtist(
        String userId, String trackName, String artist
    );
    
    @Query("SELECT DISTINCT e.artist FROM ExpandedTrack e WHERE e.userId = :userId AND e.artist IS NOT NULL AND e.artist != ''")
    List<String> findDistinctArtistsByUserId(@Param("userId") String userId);
    
    void deleteByUserId(String userId);
}