package com.atomic.focus.modules.review.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.R;
import com.atomic.focus.modules.review.dto.CreateReviewDTO;
import com.atomic.focus.modules.review.dto.FavoriteDTO;
import com.atomic.focus.modules.review.dto.GenerateReviewDTO;
import com.atomic.focus.modules.review.dto.UpdateReviewDTO;
import com.atomic.focus.modules.review.service.ReviewService;
import com.atomic.focus.modules.review.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public R<PageResult<ReviewVO>> list(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "goal_id", required = false) String goalId,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "is_favorite", required = false) Boolean isFavorite,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize) {
        return R.ok(reviewService.list(UserContext.requireUserId(), type, goalId,
                startDate, endDate, keyword, isFavorite, page, pageSize));
    }

    @GetMapping("/{reviewId}")
    public R<ReviewVO> detail(@PathVariable("reviewId") String reviewId) {
        return R.ok(reviewService.detail(UserContext.requireUserId(), reviewId));
    }

    @PostMapping
    public R<ReviewVO> create(@RequestBody @Valid CreateReviewDTO dto) {
        return R.ok(reviewService.create(UserContext.requireUserId(), dto));
    }

    @PatchMapping("/{reviewId}")
    public R<ReviewVO> update(@PathVariable("reviewId") String reviewId,
                              @RequestBody UpdateReviewDTO dto) {
        return R.ok(reviewService.update(UserContext.requireUserId(), reviewId, dto));
    }

    @DeleteMapping("/{reviewId}")
    public R<Void> delete(@PathVariable("reviewId") String reviewId) {
        reviewService.delete(UserContext.requireUserId(), reviewId);
        return R.ok();
    }

    @PostMapping("/{reviewId}/favorite")
    public R<ReviewVO> favorite(@PathVariable("reviewId") String reviewId,
                                @RequestBody FavoriteDTO dto) {
        return R.ok(reviewService.favorite(UserContext.requireUserId(), reviewId, dto));
    }

    @PostMapping("/generate")
    public R<ReviewVO> generate(@RequestBody @Valid GenerateReviewDTO dto) {
        return R.ok(reviewService.generate(UserContext.requireUserId(), dto));
    }

    @GetMapping("/trend")
    public R<List<ReviewVO>> trend(@RequestParam(value = "limit", defaultValue = "8") int limit) {
        return R.ok(reviewService.trend(UserContext.requireUserId(), limit));
    }

    @GetMapping("/guides")
    public R<Map<String, List<String>>> guides() {
        return R.ok(Map.of("items", reviewService.guides()));
    }
}
