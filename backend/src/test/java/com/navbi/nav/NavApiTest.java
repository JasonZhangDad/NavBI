package com.navbi.nav;

import com.navbi.ApiTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NavApiTest extends ApiTestBase {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = login();
    }

    private long addItem(String title, String url, String category, boolean enabled) throws Exception {
        ensureCategory(category);
        String body = mvc.perform(post("/api/nav/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"category\":\""
                                + category + "\",\"enabled\":" + enabled + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private void ensureCategory(String category) throws Exception {
        if ("默认".equals(category)) {
            return;
        }
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM nav_category WHERE name = ?", Integer.class, category);
        if (count != null && count > 0) {
            return;
        }
        mvc.perform(post("/api/nav/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + category + "\",\"sort\":0}"))
                .andExpect(status().isOk());
    }

    @Test
    void addWithoutTokenIsRejected() throws Exception {
        mvc.perform(post("/api/nav/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"url\":\"https://x.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicListGroupsByCategoryAndHidesDisabled() throws Exception {
        addItem("GitHub", "https://github.com", "开发", true);
        addItem("Claude", "https://claude.ai", "AI", true);
        addItem("Hidden", "https://hidden.com", "AI", false);

        mvc.perform(get("/api/nav/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.category=='AI')].items[0].title").value("Claude"));
    }

    @Test
    void keywordFiltersList() throws Exception {
        addItem("GitHub", "https://github.com", "开发", true);
        addItem("Claude", "https://claude.ai", "AI", true);

        mvc.perform(get("/api/nav/list").param("keyword", "GitHub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].items[0].title").value("GitHub"));
    }

    @Test
    void keywordDoesNotMatchCategory() throws Exception {
        addItem("少数派", "https://sspai.com", "资讯", true);

        mvc.perform(get("/api/nav/list").param("keyword", "资讯"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void clickTwiceIncrementsPvByTwo() throws Exception {
        long id = addItem("GitHub", "https://github.com", "开发", true);

        mvc.perform(post("/api/nav/click/" + id)).andExpect(status().isOk());
        mvc.perform(post("/api/nav/click/" + id).header("X-Session-Id", "s1")).andExpect(status().isOk());

        mvc.perform(get("/api/nav/all").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].clickCount").value(2));
    }

    @Test
    void ticketEndpointIsNotExposed() throws Exception {
        long id = addItem("GitHub", "https://github.com", "开发", true);

        mvc.perform(post("/api/nav/ticket/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateChangesTitle() throws Exception {
        long id = addItem("Old", "https://old.com", "默认", true);

        mvc.perform(put("/api/nav/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"title\":\"New\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/nav/all").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[0].title").value("New"));
    }

    @Test
    void updateMissingIdReturns400() throws Exception {
        mvc.perform(put("/api/nav/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRemovesItem() throws Exception {
        long id = addItem("Temp", "https://temp.com", "默认", true);

        mvc.perform(delete("/api/nav/delete/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/nav/all").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
