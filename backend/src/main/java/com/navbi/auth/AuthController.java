package com.navbi.auth;

import com.navbi.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@ResponseBody
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    private final String adminUsername;
    private final String adminPasswordHash;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(@Value("${navbi.admin.username}") String adminUsername,
                          @Value("${navbi.admin.password}") String adminPassword,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.adminUsername = adminUsername;
        this.adminPasswordHash = passwordEncoder.encode(adminPassword);
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Validated @RequestBody LoginRequest request) {
        if (adminUsername.equals(request.username())
                && passwordEncoder.matches(request.password(), adminPasswordHash)) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("token", jwtService.generate(request.username()))));
        }
        return ResponseEntity.status(401).body(ApiResponse.error(401, "用户名或密码错误"));
    }
}
