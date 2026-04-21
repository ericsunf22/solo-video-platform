package com.solo.video.repository;

import com.solo.video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long>, JpaSpecificationExecutor<Video> {
    
    Optional<Video> findByFilePath(String filePath);
    
    boolean existsByFilePath(String filePath);
    
    Page<Video> findByIsFavoriteTrue(Pageable pageable);
    
    Page<Video> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    @Query("SELECT v FROM Video v JOIN v.tags t WHERE t.id IN :tagIds")
    Page<Video> findByTagIds(@Param("tagIds") List<Long> tagIds, Pageable pageable);
    
    @Query("SELECT v FROM Video v WHERE v.title LIKE %:keyword% OR EXISTS (SELECT t FROM v.tags t WHERE t.name LIKE %:keyword%)")
    Page<Video> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT COUNT(v) FROM Video v WHERE v.isFavorite = true")
    long countFavorites();
}
