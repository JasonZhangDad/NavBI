package com.navbi.apilog;

import com.navbi.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiLogApiTest extends ApiTestBase {

    /** 轮询等待指定路径的 api_log 出现。 */
    private Map<String, Object> awaitLogFor(String path) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            var rows = jdbc.queryForList("SELECT * FROM api_log WHERE path = ?", path);
            if (!rows.isEmpty()) {
                return rows.get(0);
            }
            Thread.sleep(50);
        }
        throw new AssertionError("等待 api_log 记录 " + path + " 超时");
    }

    @Test
    void apiCallIsLoggedWithMethodStatusAndIp() throws Exception {
        mvc.perform(get("/api/nav/list")).andExpect(status().isOk());

        Map<String, Object> row = awaitLogFor("/api/nav/list");
        assertThat(row.get("method")).isEqualTo("GET");
        assertThat((Integer) row.get("status")).isEqualTo(200);
        assertThat((String) row.get("ip")).isNotBlank();
    }

    @Test
    void unauthorizedCallIsLoggedWith401() throws Exception {
        mvc.perform(get("/api/bi/summary")).andExpect(status().isUnauthorized());

        Map<String, Object> row = awaitLogFor("/api/bi/summary");
        assertThat((Integer) row.get("status")).isEqualTo(401);
        assertThat(row.get("username")).isNull();
    }

    @Test
    void authenticatedCallRecordsUsername() throws Exception {
        String token = login();
        mvc.perform(get("/api/nav/all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Map<String, Object> row = awaitLogFor("/api/nav/all");
        assertThat(row.get("username")).isEqualTo("admin");
    }

    @Test
    void iconRequestsAreNotLogged() throws Exception {
        mvc.perform(get("/api/nav/icon/99999"));
        mvc.perform(get("/api/nav/list")).andExpect(status().isOk());

        awaitLogFor("/api/nav/list");
        Integer iconLogs = jdbc.queryForObject(
                "SELECT COUNT(*) FROM api_log WHERE path LIKE '/api/nav/icon%'", Integer.class);
        assertThat(iconLogs).isZero();
    }

    @Test
    void apiLogsEndpointReturnsPageForAdmin() throws Exception {
        mvc.perform(get("/api/nav/list")).andExpect(status().isOk());
        awaitLogFor("/api/nav/list");

        mvc.perform(get("/api/bi/api-logs")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.records[0].path").isNotEmpty());
    }
}
