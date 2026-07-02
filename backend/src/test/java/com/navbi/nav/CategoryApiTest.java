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

class CategoryApiTest extends ApiTestBase {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = login();
    }

    private long addCategory(String name, int sort) throws Exception {
        String body = mvc.perform(post("/api/nav/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"sort\":" + sort + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    @Test
    void categoryCrudMaintainsSortedList() throws Exception {
        long ai = addCategory("AI", 2);
        addCategory("开发", 1);

        mvc.perform(get("/api/nav/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("默认"))
                .andExpect(jsonPath("$.data[1].name").value("开发"));

        mvc.perform(put("/api/nav/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + ai + ",\"name\":\"AI 工具\",\"sort\":0}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/nav/categories/" + ai).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/nav/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[1].name").value("开发"));
    }

    @Test
    void renamingCategoryUpdatesExistingNavItems() throws Exception {
        long id = addCategory("AI", 1);
        mvc.perform(post("/api/nav/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Claude\",\"url\":\"https://claude.ai\",\"category\":\"AI\"}"))
                .andExpect(status().isOk());

        mvc.perform(put("/api/nav/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"name\":\"AI 工具\",\"sort\":1}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/nav/all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("AI 工具"));
    }

    @Test
    void deletingCategoryInUseIsRejected() throws Exception {
        long id = addCategory("AI", 1);
        mvc.perform(post("/api/nav/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Claude\",\"url\":\"https://claude.ai\",\"category\":\"AI\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/nav/categories/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void navItemCategoryMustExist() throws Exception {
        mvc.perform(post("/api/nav/add")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Claude\",\"url\":\"https://claude.ai\",\"category\":\"AI\"}"))
                .andExpect(status().isBadRequest());
    }
}
