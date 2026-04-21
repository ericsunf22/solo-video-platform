package com.solo.video.service;

import com.solo.video.dto.request.TagCreateRequest;
import com.solo.video.dto.response.TagResponse;

import java.util.List;

public interface TagService {
    
    List<TagResponse> getAllTags(String keyword, String sortBy, String sortOrder);
    
    TagResponse getTagById(Long id);
    
    TagResponse createTag(TagCreateRequest request);
    
    TagResponse updateTag(Long id, TagCreateRequest request);
    
    void deleteTag(Long id);
    
    void addTagsToVideo(Long videoId, List<Long> tagIds);
    
    void removeTagFromVideo(Long videoId, Long tagId);
    
    void addTagsToVideos(List<Long> videoIds, List<Long> tagIds);
}
