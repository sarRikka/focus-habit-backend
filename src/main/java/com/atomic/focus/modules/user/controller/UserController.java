package com.atomic.focus.modules.user.controller;

import com.atomic.focus.common.context.UserContext;
import com.atomic.focus.common.exception.BusinessException;
import com.atomic.focus.common.result.PageResult;
import com.atomic.focus.common.result.R;
import com.atomic.focus.common.result.ResultCode;
import com.atomic.focus.modules.user.dto.UpdateProfileDTO;
import com.atomic.focus.modules.user.service.UserService;
import com.atomic.focus.modules.user.vo.AchievementVO;
import com.atomic.focus.modules.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public R<UserVO> me() {
        return R.ok(userService.getMe(UserContext.requireUserId()));
    }

    @PatchMapping
    public R<UserVO> updateMe(@RequestBody UpdateProfileDTO dto) {
        return R.ok(userService.updateProfile(UserContext.requireUserId(), dto));
    }

    @PostMapping("/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "file 不能为空");
        }
        try {
            return R.ok(userService.uploadAvatar(
                    UserContext.requireUserId(), file.getBytes(), file.getOriginalFilename()));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "读取文件失败");
        }
    }

    @GetMapping("/achievements")
    public R<PageResult<AchievementVO>> achievements() {
        List<AchievementVO> items = userService.achievements(UserContext.requireUserId());
        return R.ok(PageResult.of(items, 1, items.size(), items.size()));
    }
}
