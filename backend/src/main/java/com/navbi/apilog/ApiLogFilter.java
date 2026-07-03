package com.navbi.apilog;

import com.navbi.auth.JwtAuthFilter;
import com.navbi.common.ClientIpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 全量 API 访问日志：注册在 Security 过滤链之前，401/403/429 也会被记录。
 * 图标代理请求量大且无审计价值，不记录。
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 10)
public class ApiLogFilter extends OncePerRequestFilter {

    private final ApiLogService apiLogService;

    public ApiLogFilter(ApiLogService apiLogService) {
        this.apiLogService = apiLogService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/") || uri.startsWith("/api/nav/icon/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            ApiLog apiLog = new ApiLog();
            apiLog.setMethod(request.getMethod());
            apiLog.setPath(request.getRequestURI());
            apiLog.setIp(ClientIpUtil.resolve(request));
            apiLog.setUsername((String) request.getAttribute(JwtAuthFilter.ATTR_USERNAME));
            apiLog.setStatus(response.getStatus());
            apiLog.setCostMs((int) ((System.nanoTime() - start) / 1_000_000));
            apiLog.setUserAgent(truncate(request.getHeader("User-Agent")));
            apiLog.setCreatedAt(LocalDateTime.now());
            apiLogService.record(apiLog);
        }
    }

    private static String truncate(String value) {
        return value != null && value.length() > 512 ? value.substring(0, 512) : value;
    }
}
