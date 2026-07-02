package com.navbi.track;

import com.navbi.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrackApiTest extends ApiTestBase {

    private static final String CHROME_MAC_UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private String track(String sessionId) throws Exception {
        String sessionField = sessionId == null ? "" : ",\"sessionId\":\"" + sessionId + "\"";
        String body = mvc.perform(post("/api/track")
                        .header("User-Agent", CHROME_MAC_UA)
                        .header("X-Forwarded-For", "114.114.114.114")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"/\",\"referer\":\"https://google.com\"" + sessionField + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("sessionId").asText();
    }

    @Test
    void trackWithoutSessionIdGeneratesOne() throws Exception {
        String sessionId = track(null);
        assertThat(sessionId).isNotBlank();
    }

    @Test
    void trackWritesEnrichedVisitLog() throws Exception {
        String sessionId = track(null);
        awaitRowCount("visit_log", 1);

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM visit_log LIMIT 1");
        assertThat(row.get("browser")).isEqualTo("Chrome");
        assertThat(row.get("device")).isEqualTo("PC");
        assertThat((String) row.get("os")).contains("Mac");
        assertThat(row.get("ip")).isEqualTo("114.114.114.114");
        assertThat(row.get("country")).isEqualTo("中国");
        assertThat(row.get("session_id")).isEqualTo(sessionId);
        assertThat(row.get("referer")).isEqualTo("https://google.com");
    }

    @Test
    void sameSessionSecondVisitIncrementsPageCount() throws Exception {
        String sessionId = track(null);
        awaitRowCount("visit_log", 1);
        track(sessionId);
        awaitRowCount("visit_log", 2);

        long deadline = System.currentTimeMillis() + 3000;
        Integer pageCount = 0;
        while (System.currentTimeMillis() < deadline) {
            pageCount = jdbc.queryForObject(
                    "SELECT page_count FROM user_session WHERE session_id = ?", Integer.class, sessionId);
            if (pageCount != null && pageCount == 2) {
                break;
            }
            Thread.sleep(50);
        }
        assertThat(pageCount).isEqualTo(2);
        Integer sessions = jdbc.queryForObject("SELECT COUNT(*) FROM user_session", Integer.class);
        assertThat(sessions).isEqualTo(1);
    }

    @Test
    void trackWithoutUrlReturns400() throws Exception {
        mvc.perform(post("/api/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
