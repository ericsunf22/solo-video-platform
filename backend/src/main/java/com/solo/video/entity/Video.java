package com.solo.video.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "video", indexes = {
        @Index(name = "idx_file_path", columnList = "file_path", unique = true),
        @Index(name = "idx_title", columnList = "title"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_is_favorite", columnList = "is_favorite")
})
public class Video {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;
    
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "duration")
    private Long duration;
    
    @Column(name = "format", length = 20)
    private String format;
    
    @Column(name = "resolution", length = 20)
    private String resolution;
    
    @Column(name = "cover_path", length = 1000)
    private String coverPath;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType = SourceType.UPLOADED;
    
    @Column(name = "is_favorite")
    private Boolean isFavorite = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "video_tag",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Tag> tags = new ArrayList<>();
    
    @OneToOne(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PlayHistory playHistory;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isFavorite == null) {
            isFavorite = false;
        }
        if (sourceType == null) {
            sourceType = SourceType.UPLOADED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum SourceType {
        UPLOADED, SCANNED
    }
}
