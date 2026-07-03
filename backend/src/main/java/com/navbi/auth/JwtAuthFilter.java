package com.navbi.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** 请求属性：认证用户名，供访问日志过滤器读取。 */
    public static final String ATTR_USERNAME = "navbi.username";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                String username = claims.getSubject();
                // 无 role 声明的旧 token 按最低权限处理
                String role = claims.get("role", String.class);
                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + (role == null ? "USER" : role))));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(ATTR_USERNAME, username);
            } catch (JwtException | IllegalArgumentException ignored) {
                // token 非法：不设置认证，由 Security 的 EntryPoint 统一返回 401
            }
        }
        filterChain.doFilter(request, response);
    }
}
