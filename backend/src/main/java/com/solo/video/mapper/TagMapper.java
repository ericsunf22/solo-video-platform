package com.solo.video.mapper;

import com.solo.video.dto.response.TagResponse;
import com.solo.video.entity.Tag;
import com.solo.video.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TagMapper {
    
    private final TagRepository tagRepository;
    
    public TagResponse toResponse(Tag tag) {
        if (tag == null) {
            return null;
        }
        
        Long videoCount = tagRepository.countVideosByTagId(tag.getId());
        
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .description(tag.getDescription())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .videoCount(videoCount)
                .build();
    }
    
    public List<TagResponse> toResponseList(List<Tag> tags) {
        return tags.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
