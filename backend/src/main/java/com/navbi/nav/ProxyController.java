package com.navbi.nav;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.navbi.auth.AppUser;
import com.navbi.auth.AppUserMapper;
import com.navbi.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@RestController
public class ProxyController {

    private final StringRedisTemplate redisTemplate;
    private final NavItemMapper navItemMapper;
    private final AppUserMapper appUserMapper;
    private final String adminUsername;

    public ProxyController(StringRedisTemplate redisTemplate,
                           NavItemMapper navItemMapper,
                           AppUserMapper appUserMapper,
                           @Value("${navbi.admin.username}") String adminUsername) {
        this.redisTemplate = redisTemplate;
        this.navItemMapper = navItemMapper;
        this.appUserMapper = appUserMapper;
        this.adminUsername = adminUsername;
    }

    @PostMapping("/api/nav/ticket/{id}")
    public ApiResponse<String> createTicket(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ApiResponse.error(401, "请先登录");
        }

        String username = auth.getName();
        boolean hasProxyAccess = false;

        if (adminUsername.equals(username)) {
            hasProxyAccess = true;
        } else {
            AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getEmail, username));
            if (user != null && Boolean.TRUE.equals(user.getProxyEnabled())) {
                hasProxyAccess = true;
            }
        }

        if (!hasProxyAccess) {
            return ApiResponse.error(403, "您没有代理访问权限");
        }

        NavItem item = navItemMapper.selectById(id);
        if (item == null || Boolean.FALSE.equals(item.getEnabled())) {
            return ApiResponse.error(404, "导航不存在或已下线");
        }

        if (!Boolean.TRUE.equals(item.getProxyEnabled())) {
            return ApiResponse.error(403, "该导航未开启代理访问");
        }

        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("nav:ticket:" + ticket, item.getUrl(), Duration.ofSeconds(30));

        return ApiResponse.ok(ticket);
    }

    @GetMapping("/go/{ticket}")
    public ResponseEntity<Void> go(@PathVariable String ticket) {
        String key = "nav:ticket:" + ticket;
        String url = redisTemplate.opsForValue().get(key);
        if (url == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        // 使用后立即删除 ticket，确保一次性
        redisTemplate.delete(key);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }
}
