package com.navbi.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.navbi.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

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

    /** 安全视图：不暴露密码哈希。 */
    public record UserView(Long id, String email, String role,
                           boolean enabled, String createdAt) {
        static UserView of(AppUser u) {
            return new UserView(
                    u.getId(), u.getEmail(), u.getRole(),
                    Boolean.TRUE.equals(u.getEnabled()),
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().toString());
        }
    }
}
