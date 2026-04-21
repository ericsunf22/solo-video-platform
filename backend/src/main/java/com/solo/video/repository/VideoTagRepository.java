package com.solo.video.repository;

import com.solo.video.entity.VideoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoTagRepository extends JpaRepository<VideoTag, Long> {
    
    Optional<VideoTag> findByVideoIdAndTagId(Long videoId, Long tagId);
    
    boolean existsByVideoIdAndTagId(Long videoId, Long tagId);
    
    List<VideoTag> findByVideoId(Long videoId);
    
    List<VideoTag> findByTagId(Long tagId);
    
    @Modifying
    @Query("DELETE FROM VideoTag vt WHERE vt.videoId = :videoId")
    void deleteByVideoId(@Param("videoId") Long videoId);
    
    @Modifying
    @Query("DELETE FROM VideoTag vt WHERE vt.tagId = :tagId")
    void deleteByTagId(@Param("tagId") Long tagId);
    
    @Modifying
    @Query("DELETE FROM VideoTag vt WHERE vt.videoId = :videoId AND vt.tagId = :tagId")
    void deleteByVideoIdAndTagId(@Param("videoId") Long videoId, @Param("tagId") Long tagId);
}
