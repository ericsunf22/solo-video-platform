package com.solo.video.repository;

import com.solo.video.entity.PlayHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {
    
    Optional<PlayHistory> findByVideoId(Long videoId);
    
    boolean existsByVideoId(Long videoId);
    
    Page<PlayHistory> findAllByOrderByLastPlayedAtDesc(Pageable pageable);
    
    void deleteByVideoId(Long videoId);
}
