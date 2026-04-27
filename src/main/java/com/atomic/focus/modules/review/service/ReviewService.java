package com.atomic.focus.modules.review.service;

import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.modules.review.dto.CreateReviewDTO;
import com.atomic.focus.modules.review.dto.FavoriteDTO;
import com.atomic.focus.modules.review.dto.GenerateReviewDTO;
import com.atomic.focus.modules.review.dto.UpdateReviewDTO;
import com.atomic.focus.modules.review.vo.ReviewVO;

import java.time.LocalDate;
import java.util.List;

public interface ReviewService {

    PageResult<ReviewVO> list(String userId, String type, String goalId, LocalDate startDate,
                              LocalDate endDate, String keyword, Boolean isFavorite,
                              long page, long pageSize);

    ReviewVO detail(String userId, String reviewId);

    ReviewVO create(String userId, CreateReviewDTO dto);

    ReviewVO update(String userId, String reviewId, UpdateReviewDTO dto);

    void delete(String userId, String reviewId);

    ReviewVO favorite(String userId, String reviewId, FavoriteDTO dto);

    ReviewVO generate(String userId, GenerateReviewDTO dto);

    List<ReviewVO> trend(String userId, int limit);

    List<String> guides();
}
