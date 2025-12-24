package com.musicrec.repository;

import com.musicrec.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {
    
    /**
     * Find all tracks for a user with a specific source
     * Used to get liked tracks or recommendations
     */
    List<Track> findByUserIdAndSource(String userId, String source);
    
    /**
     * Delete all tracks for a user with a specific source
     * Used to clear old data before refreshing
     */
    void deleteByUserIdAndSource(String userId, String source);
    
    /**
     * Count tracks by user and source
     */
    long countByUserIdAndSource(String userId, String source);
    
    /**
     * Count tracks by user, excluding a specific source
     */
    long countByUserIdAndSourceNot(String userId, String source);
    
    /**
     * Get unique artists for a user
     */
    @Query("SELECT DISTINCT t.artist FROM Track t WHERE t.userId = :userId")
    List<String> findDistinctArtistsByUserId(@Param("userId") String userId);
    
    /**
     * Find all tracks for a user
     */
    List<Track> findByUserId(String userId);
    
    /**
     * Delete all tracks for a user
     */
    void deleteByUserId(String userId);
}