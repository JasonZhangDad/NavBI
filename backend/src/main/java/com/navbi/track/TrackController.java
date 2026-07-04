package com.navbi.track;

import com.navbi.common.ApiResponse;
import com.navbi.common.ClientIpUtil;
import com.navbi.counter.CounterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class TrackController {

    public record TrackRequest(@NotBlank String url, String referer, String sessionId) {
    }

    private final TrackingService trackingService;
    private final CounterService counterService;

    public TrackController(TrackingService trackingService, CounterService counterService) {
        this.trackingService = trackingService;
        this.counterService = counterService;
    }

    @PostMapping("/api/track")
    public ApiResponse<Map<String, String>> track(@Validated @RequestBody TrackRequest body,
                                                  HttpServletRequest request) {
        String sessionId = body.sessionId() == null || body.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : body.sessionId();
        String ip = ClientIpUtil.resolve(request);
        counterService.recordVisit(sessionId, ip);
        trackingService.recordAsync(ip, request.getHeader("User-Agent"), request.getHeader("CF-IPCountry"),
                body.url(), body.referer(), sessionId);
        return ApiResponse.ok(Map.of("sessionId", sessionId));
    }
}
