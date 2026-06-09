package com.yxshop.Module.Content.Service;

import com.yxshop.Module.Content.Dto.ContentPostDto;
import com.yxshop.Module.Content.Dto.ContentReviewDto;
import com.yxshop.Module.Content.Dto.ContentCommentDto;

public interface ContentPostService {
    Object feed(Integer page, Integer size, Long topicId, String type, String sortBy, Long currentUserId);

    Object reels(Integer page, Integer size, Long currentUserId);

    Object discover(Integer topicLimit, Integer activityLimit, Integer brandLimit);

    Object detail(Long id);

    Object publish(Long userId, ContentPostDto dto);

    Object like(Long userId, Long postId);

    Object comments(Long postId, Integer page, Integer size);

    Object addComment(Long userId, ContentCommentDto dto);

    Object myPosts(Long userId, Integer page, Integer size);

    Object review(Long reviewerId, ContentReviewDto dto);

    // Admin management
    Object adminListPosts(Integer page, Integer size, String keyword, Integer status,
                          Long topicId, Boolean hasImages, String startDate, String endDate);

    Object approvePost(Long postId);

    Object rejectPost(Long postId);

    Object hidePost(Long postId);

    Object restorePost(Long postId);

    void deletePost(Long postId);
}
