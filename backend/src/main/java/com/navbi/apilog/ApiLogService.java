package com.navbi.apilog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ApiLogService {

    private final ApiLogMapper apiLogMapper;

    public ApiLogService(ApiLogMapper apiLogMapper) {
        this.apiLogMapper = apiLogMapper;
    }

    /** 异步落库：访问日志不能拖慢业务响应。 */
    @Async("trackExecutor")
    public void record(ApiLog apiLog) {
        try {
            apiLogMapper.insert(apiLog);
        } catch (Exception e) {
            log.warn("API 日志落库失败: {}", e.getMessage());
        }
    }

    public record Page(long total, List<ApiLog> records) {
    }

    public Page page(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        long offset = (long) Math.max(page - 1, 0) * safeSize;
        long total = apiLogMapper.selectCount(null);
        List<ApiLog> records = apiLogMapper.selectList(new LambdaQueryWrapper<ApiLog>()
                .orderByDesc(ApiLog::getId)
                .last("LIMIT " + safeSize + " OFFSET " + offset));
        return new Page(total, records);
    }
}
