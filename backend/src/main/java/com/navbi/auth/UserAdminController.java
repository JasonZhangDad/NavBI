package com.navbi.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.navbi.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 管理员用户管理接口（仅 ADMIN 可访问，由 SecurityConfig 保障）。 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final AppUserMapper userMapper;

    public UserAdminController(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public record PageResult(List<UserView> list, long total) {}

    /** 用户列表（按注册时间倒序）。 */
    @GetMapping
    public ApiResponse<PageResult> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = (page - 1) * size;
        List<AppUser> users = userMapper.selectList(
                new LambdaQueryWrapper<AppUser>()
                        .orderByDesc(AppUser::getCreatedAt)
                        .last("LIMIT " + size + " OFFSET " + offset));
        long total = userMapper.selectCount(null);
        List<UserView> views = users.stream().map(UserView::of).toList();
        return ApiResponse.ok(new PageResult(views, total));
    }

    /** 封禁 / 解封用户。 */
    @PatchMapping("/{id}/enabled")
    public ApiResponse<Void> setEnabled(@PathVariable Long id,
                                        @RequestBody EnabledRequest req) {
        AppUser user = userMapper.selectById(id);
        if (user == null) return ApiResponse.error(404, "用户不存在");
        user.setEnabled(req.enabled());
        userMapper.updateById(user);
        return ApiResponse.ok();
    }

    public record EnabledRequest(boolean enabled) {}

    /** 调整用户每日客户端下载总次数。 */
    @PatchMapping("/{id}/download-limit")
    public ApiResponse<Void> setDownloadLimit(@PathVariable Long id,
                                              @RequestBody DownloadLimitRequest req) {
        if (req.dailyDownloadLimit() == null) {
            throw new IllegalArgumentException("下载次数不能为空");
        }
        if (req.dailyDownloadLimit() < 0) {
            throw new IllegalArgumentException("下载次数不能为负数");
        }
        AppUser user = userMapper.selectById(id);
        if (user == null) return ApiResponse.error(404, "用户不存在");
        user.setDailyDownloadLimit(req.dailyDownloadLimit());
        userMapper.updateById(user);
        return ApiResponse.ok();
    }

    public record DownloadLimitRequest(Integer dailyDownloadLimit) {}

    /** 安全视图：不暴露密码哈希。 */
    public record UserView(Long id, String email, String role,
                           boolean enabled, int dailyDownloadLimit,
                           int downloadsUsedToday, int downloadsRemainingToday,
                           String createdAt) {
        static UserView of(AppUser u) {
            int dailyLimit = valueOrZero(u.getDailyDownloadLimit());
            int usedToday = isToday(u.getDownloadLimitResetDate())
                    ? valueOrZero(u.getDownloadsUsedToday())
                    : 0;
            return new UserView(
                    u.getId(), u.getEmail(), u.getRole(),
                    Boolean.TRUE.equals(u.getEnabled()),
                    dailyLimit,
                    usedToday,
                    Math.max(dailyLimit - usedToday, 0),
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().toString());
        }

        private static boolean isToday(LocalDate date) {
            return LocalDate.now().equals(date);
        }

        private static int valueOrZero(Integer value) {
            return value == null ? 0 : value;
        }
    }
}
