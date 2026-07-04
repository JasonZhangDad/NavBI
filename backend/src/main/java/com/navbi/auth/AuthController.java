package com.navbi.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.navbi.common.ApiResponse;
import com.navbi.common.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.util.Map;

@Controller
@ResponseBody
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record CodeRequest(@NotBlank @Email String email) {
    }

    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank String code,
                                  @NotBlank @Size(min = 8, max = 64) String password) {
    }

    private final String adminUsername;
    private final String adminPasswordHash;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppUserMapper userMapper;
    private final RegisterService registerService;
    private final RateLimiter rateLimiter;

    public AuthController(@Value("${navbi.admin.username}") String adminUsername,
                          @Value("${navbi.admin.password}") String adminPassword,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          AppUserMapper userMapper,
                          RegisterService registerService,
                          RateLimiter rateLimiter) {
        this.adminUsername = adminUsername;
        this.adminPasswordHash = passwordEncoder.encode(adminPassword);
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.registerService = registerService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Validated @RequestBody LoginRequest request,
                                                                  HttpServletRequest http) {
        // 同一 IP 每分钟最多 10 次登录尝试，防口令爆破
        if (!rateLimiter.tryAcquire("login-ip:" + ClientIpUtil.resolve(http), 10, Duration.ofMinutes(1))) {
            throw new RateLimitExceededException("尝试过于频繁，请稍后再试");
        }
        String role = authenticate(request.username(), request.password());
        if (role == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "用户名或密码错误"));
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "token", jwtService.generate(request.username(), role),
                "role", role)));
    }

    /** 认证成功返回角色，失败返回 null。 */
    private String authenticate(String username, String password) {
        if (adminUsername.equals(username) && passwordEncoder.matches(password, adminPasswordHash)) {
            return "ADMIN";
        }
        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getEmail, username));
        if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
            if (Boolean.FALSE.equals(user.getEnabled())) {
                throw new IllegalArgumentException("账号已被封禁，请联系管理员");
            }
            return user.getRole();
        }
        return null;
    }

    @PostMapping("/register/code")
    public ApiResponse<Void> sendCode(@Validated @RequestBody CodeRequest request, HttpServletRequest http) {
        registerService.sendCode(request.email(), ClientIpUtil.resolve(http));
        return ApiResponse.ok();
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Validated @RequestBody RegisterRequest request, HttpServletRequest http) {
        registerService.register(request.email(), request.code(), request.password(),
                ClientIpUtil.resolve(http), http.getHeader("CF-IPCountry"));
        return ApiResponse.ok();
    }

    @PostMapping("/password/code")
    public ApiResponse<Void> sendPasswordCode(@Validated @RequestBody CodeRequest request,
                                              HttpServletRequest http) {
        registerService.sendPasswordCode(request.email(), ClientIpUtil.resolve(http));
        return ApiResponse.ok();
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Validated @RequestBody RegisterRequest request,
                                           HttpServletRequest http) {
        registerService.resetPassword(request.email(), request.code(), request.password(),
                ClientIpUtil.resolve(http));
        return ApiResponse.ok();
    }
}
