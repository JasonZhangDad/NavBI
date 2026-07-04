package com.navbi.bi;

import com.navbi.apilog.ApiLogService;
import com.navbi.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bi")
public class BiController {

    private final BiService biService;
    private final ApiLogService apiLogService;

    public BiController(BiService biService, ApiLogService apiLogService) {
        this.biService = biService;
        this.apiLogService = apiLogService;
    }

    @GetMapping("/api-logs")
    public ApiResponse<ApiLogService.Page> apiLogs(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(apiLogService.page(page, size));
    }

    @GetMapping("/summary")
    public ApiResponse<SummaryVo> summary() {
        return ApiResponse.ok(biService.summary());
    }

    @GetMapping("/trend")
    public ApiResponse<List<TrendPoint>> trend(@RequestParam(defaultValue = "hour") String dimension) {
        return ApiResponse.ok(biService.trend(dimension));
    }

    @GetMapping("/top-pages")
    public ApiResponse<List<NameValue>> topPages() {
        return ApiResponse.ok(biService.topPages());
    }

    @GetMapping("/register-trend")
    public ApiResponse<List<RegisterTrendPoint>> registerTrend() {
        return ApiResponse.ok(biService.registerTrend());
    }

    @GetMapping("/register-geo")
    public ApiResponse<List<NameValue>> registerGeo() {
        return ApiResponse.ok(biService.registerGeo());
    }

    @GetMapping("/device")
    public ApiResponse<List<NameValue>> device() {
        return ApiResponse.ok(biService.deviceShare());
    }

    @GetMapping("/browser")
    public ApiResponse<List<NameValue>> browser() {
        return ApiResponse.ok(biService.browserShare());
    }

    @GetMapping("/geo")
    public ApiResponse<List<NameValue>> geo(@RequestParam(defaultValue = "country") String level) {
        return ApiResponse.ok(biService.geo(level));
    }

    @GetMapping("/logs")
    public ApiResponse<LogPage> logs(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(biService.logs(page, size));
    }
}
