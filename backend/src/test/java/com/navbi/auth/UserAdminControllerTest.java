package com.navbi.auth;

import com.navbi.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAdminControllerTest extends ApiTestBase {

    @Test
    void adminCanListAndUpdateDailyDownloadLimit() throws Exception {
        Long userId = insertUser("user@example.com", 2, 1, LocalDate.now());
        String token = login();

        mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].dailyDownloadLimit").value(2))
                .andExpect(jsonPath("$.data.list[0].downloadsUsedToday").value(1))
                .andExpect(jsonPath("$.data.list[0].downloadsRemainingToday").value(1));

        mvc.perform(patch("/api/admin/users/{id}/download-limit", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyDownloadLimit\":5}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].dailyDownloadLimit").value(5))
                .andExpect(jsonPath("$.data.list[0].downloadsUsedToday").value(1))
                .andExpect(jsonPath("$.data.list[0].downloadsRemainingToday").value(4));
    }

    @Test
    void adminCannotSetNegativeDailyDownloadLimit() throws Exception {
        Long userId = insertUser("user@example.com", 1, 0, LocalDate.now());
        String token = login();

        mvc.perform(patch("/api/admin/users/{id}/download-limit", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyDownloadLimit\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("下载次数不能为负数"));
    }

    @Test
    void listTreatsYesterdayUsageAsReset() throws Exception {
        insertUser("user@example.com", 2, 2, LocalDate.now().minusDays(1));
        String token = login();

        mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].dailyDownloadLimit").value(2))
                .andExpect(jsonPath("$.data.list[0].downloadsUsedToday").value(0))
                .andExpect(jsonPath("$.data.list[0].downloadsRemainingToday").value(2));
    }

    private Long insertUser(String email, int dailyDownloadLimit, int downloadsUsedToday, LocalDate resetDate) {
        jdbc.update("""
                INSERT INTO app_user
                    (email, password_hash, role, enabled,
                     daily_download_limit, downloads_used_today, download_limit_reset_date)
                VALUES (?, ?, 'USER', TRUE, ?, ?, ?)
                """, email, "unused", dailyDownloadLimit, downloadsUsedToday, resetDate);
        return jdbc.queryForObject("SELECT id FROM app_user WHERE email = ?", Long.class, email);
    }
}
