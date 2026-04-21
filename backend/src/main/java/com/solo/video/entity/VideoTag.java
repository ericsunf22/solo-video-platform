package com.solo.video.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "video_tag", indexes = {
        @Index(name = "idx_video_tag", columnList = "video_id, tag_id", unique = true),
        @Index(name = "idx_tag_id", columnList = "tag_id")
})
public class VideoTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "video_id", nullable = false)
    private Long videoId;
    
    @Column(name = "tag_id", nullable = false)
    private Long tagId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
