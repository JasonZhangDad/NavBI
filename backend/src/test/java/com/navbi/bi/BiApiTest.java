package com.navbi.bi;

import com.navbi.ApiTestBase;
import com.navbi.track.VisitLog;
import com.navbi.track.VisitLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BiApiTest extends ApiTestBase {

    @Autowired
    private VisitLogMapper visitLogMapper;

    private String token;

    @BeforeEach
    void seed() throws Exception {
        token = login();
        // 今日 3 条（2 个会话、2 个 IP），昨日 1 条
        insertLog("s1", "1.1.1.1", "/", "PC", "Chrome", "中国", "广东省", LocalDateTime.now());
        insertLog("s1", "1.1.1.1", "/tools", "PC", "Chrome", "中国", "广东省", LocalDateTime.now());
        insertLog("s2", "2.2.2.2", "/", "手机", "Safari", "美国", "未知", LocalDateTime.now());
        insertLog("s3", "3.3.3.3", "/", "PC", "Edge", "中国", "北京市", LocalDateTime.now().minusDays(1));
    }

    private void insertLog(String sessionId, String ip, String url, String device, String browser,
                           String country, String province, LocalDateTime createdAt) {
        VisitLog log = new VisitLog();
        log.setSessionId(sessionId);
        log.setIp(ip);
        log.setUrl(url);
        log.setDevice(device);
        log.setBrowser(browser);
        log.setOs("Windows");
        log.setCountry(country);
        log.setProvince(province);
        log.setCity("未知");
        log.setCreatedAt(createdAt);
        visitLogMapper.insert(log);
    }

    @Test
    void summaryCountsMatchSeed() throws Exception {
        mvc.perform(get("/api/bi/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayPv").value(3))
                .andExpect(jsonPath("$.data.todayUv").value(2))
                .andExpect(jsonPath("$.data.yesterdayPv").value(1))
                .andExpect(jsonPath("$.data.totalPv").value(4))
                .andExpect(jsonPath("$.data.totalUv").value(3))
                .andExpect(jsonPath("$.data.ipCount").value(3));
    }

    @Test
    void trendByHourHasBuckets() throws Exception {
        mvc.perform(get("/api/bi/trend").param("dimension", "hour")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].pv").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void invalidTrendDimensionReturns400() throws Exception {
        mvc.perform(get("/api/bi/trend").param("dimension", "week")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topPagesOrderedByPv() throws Exception {
        mvc.perform(get("/api/bi/top-pages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("/"))
                .andExpect(jsonPath("$.data[0].value").value(3));
    }

    @Test
    void deviceShareAggregates() throws Exception {
        mvc.perform(get("/api/bi/device").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("PC"))
                .andExpect(jsonPath("$.data[0].value").value(3));
    }

    @Test
    void geoByCountryAndProvince() throws Exception {
        mvc.perform(get("/api/bi/geo").param("level", "country")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].name").value("中国"))
                .andExpect(jsonPath("$.data[0].value").value(3));

        mvc.perform(get("/api/bi/geo").param("level", "province")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].name").value("广东省"))
                .andExpect(jsonPath("$.data[0].value").value(2));
    }

    @Test
    void logsPaginated() throws Exception {
        mvc.perform(get("/api/bi/logs").param("page", "1").param("size", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.records", hasSize(3)));

        mvc.perform(get("/api/bi/logs").param("page", "2").param("size", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.records", hasSize(1)));
    }
}
