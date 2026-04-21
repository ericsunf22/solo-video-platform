package com.solo.video.repository;

import com.solo.video.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    Optional<Tag> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Tag> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT t FROM Tag t ORDER BY t.name ASC")
    List<Tag> findAllOrderByName();
    
    @Query("SELECT COUNT(DISTINCT v.id) FROM Tag t JOIN t.videos v WHERE t.id = :tagId")
    long countVideosByTagId(@Param("tagId") Long tagId);
}
