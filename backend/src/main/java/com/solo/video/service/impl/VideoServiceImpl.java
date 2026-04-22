package com.solo.video.service.impl;

import com.solo.video.dto.request.VideoUpdateRequest;
import com.solo.video.dto.response.BatchUploadResult;
import com.solo.video.dto.response.UploadFailure;
import com.solo.video.dto.response.VideoResponse;
import com.solo.video.entity.Video;
import com.solo.video.exception.VideoNotFoundException;
import com.solo.video.mapper.VideoMapper;
import com.solo.video.repository.VideoRepository;
import com.solo.video.service.FileStorageService;
import com.solo.video.service.VideoService;
import com.solo.video.util.FileUtil;
import com.solo.video.util.StringUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    
    private final VideoRepository videoRepository;
    private final FileStorageService fileStorageService;
    private final VideoMapper videoMapper;
    
    @Override
    public Page<VideoResponse> getAllVideos(String keyword, List<Long> tagIds, Boolean isFavorite,
                                             String sortBy, String sortOrder, Pageable pageable) {
        Specification<Video> spec = Specification.where(null);
        
        if (StringUtil.isNotBlank(keyword)) {
            String finalKeyword = keyword.trim();
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("title")), "%" + finalKeyword.toLowerCase() + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + finalKeyword.toLowerCase() + "%")
                    )
            );
        }
        
        if (isFavorite != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isFavorite"), isFavorite)
            );
        }
        
        Sort sort = buildSort(sortBy, sortOrder);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
        
        Page<Video> videoPage = videoRepository.findAll(spec, sortedPageable);
        return videoPage.map(videoMapper::toResponse);
    }
    
    @Override
    public VideoResponse getVideoById(Long id) {
        Video video = getVideoEntityById(id);
        return videoMapper.toResponse(video);
    }
    
    @Override
    public Video getVideoEntityById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException(id));
    }
    
    @Override
    public Optional<Video> getVideoByFilePath(String filePath) {
        return videoRepository.findByFilePath(filePath);
    }
    
    @Override
    @Transactional
    public VideoResponse uploadVideo(MultipartFile file, String title, String description) {
        if (!FileUtil.isVideoFile(file.getOriginalFilename())) {
            throw new IllegalArgumentException("不支持的文件格式: " + file.getOriginalFilename());
        }
        
        String relativePath = fileStorageService.storeFile(file);
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        Video video = new Video();
        video.setTitle(StringUtil.isNotBlank(title) ? title : FileUtil.getFileNameWithoutExtension(originalFileName));
        video.setDescription(description);
        video.setFilePath(relativePath);
        video.setFileName(originalFileName);
        video.setFileSize(file.getSize());
        video.setFormat(FileUtil.getFileExtension(originalFileName));
        video.setSourceType(Video.SourceType.UPLOADED);
        
        Video savedVideo = videoRepository.save(video);
        log.info("视频上传成功: id={}, filePath={}", savedVideo.getId(), savedVideo.getFilePath());
        
        return videoMapper.toResponse(savedVideo);
    }
    
    @Override
    @Transactional
    public BatchUploadResult uploadVideos(List<MultipartFile> files) {
        List<VideoResponse> successes = new ArrayList<>();
        List<UploadFailure> failures = new ArrayList<>();
        
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            try {
                VideoResponse response = uploadVideo(file, null, null);
                successes.add(response);
                log.info("批量上传成功: {}", fileName);
            } catch (Exception e) {
                log.error("批量上传失败: {}", fileName, e);
                failures.add(UploadFailure.builder()
                        .fileName(fileName)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }
        
        return BatchUploadResult.builder()
                .successes(successes)
                .failures(failures)
                .totalCount(files.size())
                .successCount(successes.size())
                .failureCount(failures.size())
                .build();
    }
    
    @Override
    @Transactional
    public VideoResponse updateVideo(Long id, VideoUpdateRequest request) {
        Video video = getVideoEntityById(id);
        
        if (StringUtil.isNotBlank(request.getTitle())) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        
        Video savedVideo = videoRepository.save(video);
        return videoMapper.toResponse(savedVideo);
    }
    
    @Override
    @Transactional
    public void deleteVideo(Long id) {
        Video video = getVideoEntityById(id);
        fileStorageService.deleteFile(video.getFilePath());
        videoRepository.delete(video);
        log.info("视频已删除: id={}", id);
    }
    
    @Override
    @Transactional
    public void deleteVideos(List<Long> ids) {
        for (Long id : ids) {
            deleteVideo(id);
        }
    }
    
    @Override
    public long countVideos() {
        return videoRepository.count();
    }
    
    @Override
    public long countFavorites() {
        return videoRepository.countFavorites();
    }
    
    @Override
    @Transactional
    public void toggleFavorite(Long videoId) {
        Video video = getVideoEntityById(videoId);
        video.setIsFavorite(!video.getIsFavorite());
        videoRepository.save(video);
        log.info("视频收藏状态已切换: id={}, isFavorite={}", videoId, video.getIsFavorite());
    }
    
    @Override
    @Transactional
    public void addToFavorites(List<Long> videoIds) {
        for (Long videoId : videoIds) {
            Video video = getVideoEntityById(videoId);
            if (!video.getIsFavorite()) {
                video.setIsFavorite(true);
                videoRepository.save(video);
            }
        }
    }
    
    @Override
    @Transactional
    public void removeFromFavorites(List<Long> videoIds) {
        for (Long videoId : videoIds) {
            Video video = getVideoEntityById(videoId);
            if (video.getIsFavorite()) {
                video.setIsFavorite(false);
                videoRepository.save(video);
            }
        }
    }
    
    @Override
    public boolean existsById(Long id) {
        return videoRepository.existsById(id);
    }
    
    private Sort buildSort(String sortBy, String sortOrder) {
        String property = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "title" -> "title";
            case "duration" -> "duration";
            case "filesize" -> "fileSize";
            default -> "createdAt";
        };
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
