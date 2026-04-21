package com.solo.video.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "play_history", indexes = {
        @Index(name = "idx_video_id", columnList = "video_id", unique = true),
        @Index(name = "idx_last_played", columnList = "last_played_at")
})
public class PlayHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
    
    @Column(name = "progress")
    private Long progress = 0L;
    
    @Column(name = "play_count")
    private Integer playCount = 0;
    
    @Column(name = "total_play_time")
    private Long totalPlayTime = 0L;
    
    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastPlayedAt = LocalDateTime.now();
        if (progress == null) {
            progress = 0L;
        }
        if (playCount == null) {
            playCount = 0;
        }
        if (totalPlayTime == null) {
            totalPlayTime = 0L;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
