package com.atomic.focus.modules.review.service.impl;

import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.common.util.IdGenerator;
import com.atomic.focus.common.util.JsonUtil;
import com.atomic.focus.modules.checkin.entity.Checkin;
import com.atomic.focus.modules.checkin.mapper.CheckinMapper;
import com.atomic.focus.modules.goal.entity.Goal;
import com.atomic.focus.modules.goal.mapper.GoalMapper;
import com.atomic.focus.modules.review.dto.CreateReviewDTO;
import com.atomic.focus.modules.review.dto.FavoriteDTO;
import com.atomic.focus.modules.review.dto.GenerateReviewDTO;
import com.atomic.focus.modules.review.dto.UpdateReviewDTO;
import com.atomic.focus.modules.review.entity.Review;
import com.atomic.focus.modules.review.mapper.ReviewMapper;
import com.atomic.focus.modules.review.service.ReviewService;
import com.atomic.focus.modules.review.vo.ReviewMetricsVO;
import com.atomic.focus.modules.review.vo.ReviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final Set<String> TYPES = Set.of("weekly", "monthly", "manual");
    private static final List<String> GUIDES = List.of(
            "本周/本月哪些时段最容易完成打卡？背后的原因是什么？",
            "中断或低谷出现在什么场景？下次如何用『习惯堆叠』化解？",
            "哪些细节让你坚持下来？哪些奖励/反馈最有效？"
    );

    private final ReviewMapper reviewMapper;
    private final CheckinMapper checkinMapper;
    private final GoalMapper goalMapper;

    @Override
    public PageResult<ReviewVO> list(String userId, String type, String goalId, LocalDate startDate,
                                     LocalDate endDate, String keyword, Boolean isFavorite,
                                     long page, long pageSize) {
        LambdaQueryWrapper<Review> w = new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId);
        if (type != null && !"all".equals(type) && !type.isBlank()) {
            if (!TYPES.contains(type)) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "type 取值非法");
            }
            w.eq(Review::getType, type);
        }
        if (goalId != null && !goalId.isBlank()) w.eq(Review::getGoalId, goalId);
        if (startDate != null) w.ge(Review::getReviewDate, startDate);
        if (endDate != null) w.le(Review::getReviewDate, endDate);
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(Review::getTitle, keyword).or().like(Review::getContent, keyword));
        }
        if (isFavorite != null) w.eq(Review::getIsFavorite, isFavorite);
        w.orderByDesc(Review::getReviewDate).orderByDesc(Review::getCreatedAt);

        Page<Review> p = Page.of(page, pageSize);
        Page<Review> r = reviewMapper.selectPage(p, w);
        Map<String, Goal> goalCache = new HashMap<>();
        List<ReviewVO> items = r.getRecords().stream()
                .map(rv -> toVO(rv, goalCache)).collect(Collectors.toList());
        return PageResult.of(items, r.getCurrent(), r.getSize(), r.getTotal());
    }

    @Override
    public ReviewVO detail(String userId, String reviewId) {
        Review r = require(userId, reviewId);
        return toVO(r, new HashMap<>());
    }

    @Override
    @Transactional
    public ReviewVO create(String userId, CreateReviewDTO dto) {
        if (!TYPES.contains(dto.getType())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "type 取值非法");
        }
        if (dto.getClientOpId() != null) {
            Review exist = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                    .eq(Review::getUserId, userId)
                    .eq(Review::getClientOpId, dto.getClientOpId())
                    .last("LIMIT 1"));
            if (exist != null) return toVO(exist, new HashMap<>());
        }
        Review r = new Review();
        r.setId(IdGenerator.review());
        r.setUserId(userId);
        r.setGoalId(dto.getGoalId());
        r.setType(dto.getType());
        r.setTitle(dto.getTitle());
        r.setReviewDate(dto.getDate() == null ? LocalDate.now() : dto.getDate());
        r.setContent(dto.getContent());
        r.setIsFavorite(false);
        r.setClientOpId(dto.getClientOpId());
        reviewMapper.insert(r);
        return toVO(r, new HashMap<>());
    }

    @Override
    public ReviewVO update(String userId, String reviewId, UpdateReviewDTO dto) {
        Review r = require(userId, reviewId);
        if (dto.getTitle() != null) r.setTitle(dto.getTitle());
        if (dto.getContent() != null) r.setContent(dto.getContent());
        if (dto.getDate() != null) r.setReviewDate(dto.getDate());
        if (dto.getGoalId() != null) r.setGoalId(dto.getGoalId());
        reviewMapper.updateById(r);
        return toVO(r, new HashMap<>());
    }

    @Override
    public void delete(String userId, String reviewId) {
        Review r = require(userId, reviewId);
        reviewMapper.deleteById(r.getId());
    }

    @Override
    public ReviewVO favorite(String userId, String reviewId, FavoriteDTO dto) {
        Review r = require(userId, reviewId);
        r.setIsFavorite(Boolean.TRUE.equals(dto.getIsFavorite()));
        reviewMapper.updateById(r);
        return toVO(r, new HashMap<>());
    }

    @Override
    @Transactional
    public ReviewVO generate(String userId, GenerateReviewDTO dto) {
        String scope = dto.getScope();
        if (!"weekly".equals(scope) && !"monthly".equals(scope)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "scope 仅支持 weekly|monthly");
        }
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        String title;
        if ("weekly".equals(scope)) {
            start = today.with(java.time.DayOfWeek.MONDAY);
            end = start.plusDays(6);
            title = "本周复盘报告";
        } else {
            YearMonth ym = YearMonth.from(today);
            start = ym.atDay(1);
            end = ym.atEndOfMonth();
            title = "本月复盘报告";
        }

        if (!Boolean.TRUE.equals(dto.getForce())) {
            Review exist = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                    .eq(Review::getUserId, userId)
                    .eq(Review::getType, scope)
                    .eq(Review::getReviewDate, today)
                    .last("LIMIT 1"));
            if (exist != null) return toVO(exist, new HashMap<>());
        }

        // 计算 metrics
        ReviewMetricsVO metrics = calcMetrics(userId, start, end);
        List<String> suggestions = buildSuggestions(metrics);

        Review r = new Review();
        r.setId(IdGenerator.review());
        r.setUserId(userId);
        r.setType(scope);
        r.setTitle(title);
        r.setReviewDate(today);
        r.setIsFavorite(false);
        r.setMetricsJson(JsonUtil.toJson(metrics));
        r.setSuggestionsJson(JsonUtil.toJson(suggestions));
        reviewMapper.insert(r);
        return toVO(r, new HashMap<>());
    }

    @Override
    public List<ReviewVO> trend(String userId, int limit) {
        if (limit <= 0) limit = 8;
        List<Review> list = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .in(Review::getType, "weekly", "monthly")
                .orderByDesc(Review::getReviewDate)
                .last("LIMIT " + limit));
        list.sort((a, b) -> a.getReviewDate().compareTo(b.getReviewDate()));
        Map<String, Goal> goalCache = new HashMap<>();
        return list.stream().map(r -> toVO(r, goalCache)).collect(Collectors.toList());
    }

    @Override
    public List<String> guides() {
        return GUIDES;
    }

    // ---------- private ----------

    private Review require(String userId, String reviewId) {
        Review r = reviewMapper.selectById(reviewId);
        if (r == null || !userId.equals(r.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "复盘不存在");
        }
        return r;
    }

    private ReviewMetricsVO calcMetrics(String userId, LocalDate start, LocalDate end) {
        List<Goal> goals = goalMapper.selectList(new LambdaQueryWrapper<Goal>()
                .eq(Goal::getUserId, userId)
                .eq(Goal::getArchived, false));
        long totalDays = end.toEpochDay() - start.toEpochDay() + 1;
        int planTotal = (int) totalDays * goals.size();
        int doneCount = 0;
        int totalDuration = 0;
        int missedDays = 0;
        for (Goal g : goals) {
            List<Checkin> list = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                    .eq(Checkin::getGoalId, g.getId())
                    .between(Checkin::getCheckinDate, start, end));
            for (Checkin c : list) {
                if ("done".equals(c.getStatus()) || "late".equals(c.getStatus())) {
                    doneCount++;
                    totalDuration += c.getDuration() == null ? 0 : c.getDuration();
                } else if ("missed".equals(c.getStatus())) {
                    missedDays++;
                }
            }
        }
        ReviewMetricsVO m = new ReviewMetricsVO();
        m.setCheckinRate(planTotal == 0 ? 0 : (int) Math.round(doneCount * 100.0 / planTotal));
        m.setAvgDuration(doneCount == 0 ? 0 : totalDuration / doneCount);
        m.setMissedDays(missedDays);
        m.setProgressDelta(0);
        m.setTotalMinutes(totalDuration);
        return m;
    }

    private List<String> buildSuggestions(ReviewMetricsVO m) {
        List<String> result = new ArrayList<>();
        if (m.getCheckinRate() < 60) {
            result.add("打卡率偏低，建议将打卡提醒与现有习惯（如『晚饭后』）绑定形成习惯堆叠");
        }
        if (m.getMissedDays() > 0) {
            result.add("出现 " + m.getMissedDays() + " 天未打卡，可在『特殊场景』中预先设置缩短时长应对忙碌期");
        }
        if (m.getAvgDuration() > 10) {
            result.add("平均时长 " + m.getAvgDuration() + " 分钟已超目标，可以开启『自动进阶』巩固习惯");
        }
        if (result.isEmpty()) {
            result.add("保持当前节奏即可，记得用奖励为下一阶段的努力提供正向反馈");
        }
        return result;
    }

    private ReviewVO toVO(Review r, Map<String, Goal> goalCache) {
        ReviewVO vo = new ReviewVO();
        vo.setId(r.getId());
        vo.setType(r.getType());
        vo.setTitle(r.getTitle());
        vo.setDate(r.getReviewDate());
        vo.setGoalId(r.getGoalId());
        vo.setContent(r.getContent());
        vo.setIsFavorite(Boolean.TRUE.equals(r.getIsFavorite()));
        vo.setCreatedAt(r.getCreatedAt());
        if (r.getMetricsJson() != null) {
            vo.setMetrics(JsonUtil.fromJson(r.getMetricsJson(), ReviewMetricsVO.class));
        }
        if (r.getSuggestionsJson() != null) {
            vo.setSuggestions(JsonUtil.stringList(r.getSuggestionsJson()));
        }
        if (r.getGoalId() != null) {
            Goal g = goalCache.computeIfAbsent(r.getGoalId(), goalMapper::selectById);
            if (g != null) vo.setGoalName(g.getName());
        }
        return vo;
    }
}
