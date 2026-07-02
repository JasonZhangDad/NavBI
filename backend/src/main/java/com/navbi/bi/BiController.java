package com.navbi.bi;

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

    public BiController(BiService biService) {
        this.biService = biService;
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
