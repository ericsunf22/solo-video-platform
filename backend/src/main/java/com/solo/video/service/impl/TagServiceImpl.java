package com.solo.video.service.impl;

import com.solo.video.dto.request.TagCreateRequest;
import com.solo.video.dto.response.TagResponse;
import com.solo.video.entity.Tag;
import com.solo.video.exception.BusinessException;
import com.solo.video.mapper.TagMapper;
import com.solo.video.repository.TagRepository;
import com.solo.video.repository.VideoRepository;
import com.solo.video.repository.VideoTagRepository;
import com.solo.video.service.TagService;
import com.solo.video.util.StringUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    
    private final TagRepository tagRepository;
    private final VideoRepository videoRepository;
    private final VideoTagRepository videoTagRepository;
    private final TagMapper tagMapper;
    
    @SuppressWarnings("null")
    @Override
    public List<TagResponse> getAllTags(String keyword, String sortBy, String sortOrder) {
        List<Tag> tags;
        String sortByLower = sortBy != null ? sortBy.toLowerCase() : "";
        
        if (StringUtil.isNotBlank(keyword)) {
            tags = tagRepository.findByNameContainingIgnoreCase(keyword.trim());
        } else if ("videocount".equals(sortByLower)) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            tags = isAsc ? tagRepository.findAllOrderByVideoCountAsc() : tagRepository.findAllOrderByVideoCountDesc();
        } else {
            Sort sort = buildSort(sortBy, sortOrder);
            tags = tagRepository.findAll(sort);
        }
        
        return tagMapper.toResponseList(tags);
    }
    
    @SuppressWarnings("null")
    @Override
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException("标签不存在: " + id));
        return tagMapper.toResponse(tag);
    }
    
    @Override
    @Transactional
    public TagResponse createTag(TagCreateRequest request) {
        if (tagRepository.existsByName(request.getName().trim())) {
            throw new BusinessException("标签已存在: " + request.getName());
        }
        
        Tag tag = new Tag();
        tag.setName(request.getName().trim());
        tag.setColor(StringUtil.isNotBlank(request.getColor()) ? request.getColor() : "#3b82f6");
        tag.setDescription(request.getDescription());
        
        Tag savedTag = tagRepository.save(tag);
        log.info("标签创建成功: id={}, name={}", savedTag.getId(), savedTag.getName());
        
        return tagMapper.toResponse(savedTag);
    }
    
    @SuppressWarnings("null")
    @Override
    @Transactional
    public TagResponse updateTag(Long id, TagCreateRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException("标签不存在: " + id));
        
        if (StringUtil.isNotBlank(request.getName())) {
            String newName = request.getName().trim();
            if (!newName.equals(tag.getName()) && tagRepository.existsByName(newName)) {
                throw new BusinessException("标签已存在: " + newName);
            }
            tag.setName(newName);
        }
        
        if (StringUtil.isNotBlank(request.getColor())) {
            tag.setColor(request.getColor());
        }
        
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }
        
        Tag savedTag = tagRepository.save(tag);
        return tagMapper.toResponse(savedTag);
    }
    
    @SuppressWarnings("null")
    @Override
    @Transactional
    public void deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new BusinessException("标签不存在: " + id);
        }
        
        videoTagRepository.deleteByTagId(id);
        tagRepository.deleteById(id);
        log.info("标签已删除: id={}", id);
    }
    
    @SuppressWarnings("null")
    @Override
    @Transactional
    public void addTagsToVideo(Long videoId, List<Long> tagIds) {
        if (!videoRepository.existsById(videoId)) {
            throw new BusinessException("视频不存在: " + videoId);
        }
        
        List<Long> existingTagIds = tagRepository.findAllById(tagIds).stream()
                .map(Tag::getId)
                .toList();
        
        for (Long tagId : tagIds) {
            if (!existingTagIds.contains(tagId)) {
                throw new BusinessException("标签不存在: " + tagId);
            }
        }
        
        List<VideoTag> existingVideoTags = videoTagRepository.findByVideoId(videoId);
        List<Long> existingTagIdsForVideo = existingVideoTags.stream()
                .map(VideoTag::getTagId)
                .toList();
        
        List<VideoTag> newVideoTags = new ArrayList<>();
        for (Long tagId : tagIds) {
            if (!existingTagIdsForVideo.contains(tagId)) {
                VideoTag videoTag = new VideoTag();
                videoTag.setVideoId(videoId);
                videoTag.setTagId(tagId);
                newVideoTags.add(videoTag);
            }
        }
        
        if (!newVideoTags.isEmpty()) {
            videoTagRepository.saveAll(newVideoTags);
        }
        
        log.info("标签已添加到视频: videoId={}, tagIds={}", videoId, tagIds);
    }
    
    @Override
    @Transactional
    public void removeTagFromVideo(Long videoId, Long tagId) {
        videoTagRepository.deleteByVideoIdAndTagId(videoId, tagId);
        log.info("标签已从视频移除: videoId={}, tagId={}", videoId, tagId);
    }
    
    @Override
    @Transactional
    public void addTagsToVideos(List<Long> videoIds, List<Long> tagIds) {
        for (Long videoId : videoIds) {
            addTagsToVideo(videoId, tagIds);
        }
    }
    
    private Sort buildSort(String sortBy, String sortOrder) {
        String property = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "name" -> "name";
            default -> "createdAt";
        };
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
