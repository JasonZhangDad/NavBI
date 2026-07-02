package com.navbi.nav;

import com.navbi.common.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nav")
public class NavController {

    private final NavService navService;

    public NavController(NavService navService) {
        this.navService = navService;
    }

    @GetMapping("/list")
    public ApiResponse<List<NavGroup>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(navService.listGrouped(keyword));
    }

    @GetMapping("/all")
    public ApiResponse<List<NavItem>> all() {
        return ApiResponse.ok(navService.listAll());
    }

    @PostMapping("/add")
    public ApiResponse<NavItem> add(@RequestBody NavItem item) {
        return ApiResponse.ok(navService.add(item));
    }

    @PutMapping("/update")
    public ApiResponse<Void> update(@RequestBody NavItem item) {
        navService.update(item);
        return ApiResponse.ok();
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        navService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/click/{id}")
    public ApiResponse<Void> click(@PathVariable Long id,
                                   @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        navService.click(id, sessionId);
        return ApiResponse.ok();
    }
}
