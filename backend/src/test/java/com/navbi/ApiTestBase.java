package com.navbi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navbi.auth.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected RateLimiter rateLimiter;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("DELETE FROM visit_log");
        jdbc.execute("DELETE FROM user_session");
        jdbc.execute("DELETE FROM nav_item");
        jdbc.execute("DELETE FROM nav_category");
        jdbc.execute("DELETE FROM app_user");
        jdbc.execute("DELETE FROM email_code");
        jdbc.execute("DELETE FROM api_log");
        jdbc.execute("INSERT INTO nav_category (name, sort) VALUES ('默认', 0)");
        rateLimiter.clear();
    }

    protected String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.path("data").path("token").asText();
    }

    /** 轮询等待异步落库完成。 */
    protected void awaitRowCount(String table, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
            if (count != null && count >= expected) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("等待 " + table + " 行数达到 " + expected + " 超时");
    }
}
