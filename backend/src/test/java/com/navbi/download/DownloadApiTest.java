package com.navbi.download;

import com.navbi.ApiTestBase;
import com.navbi.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "navbi.download.dir=target/test-downloads")
class DownloadApiTest extends ApiTestBase {

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seedFiles() throws Exception {
        Path dir = Path.of("target/test-downloads");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("Cloudflare_WARP_2026.6.822.0.msi"), "windows");
        Files.writeString(dir.resolve("Cloudflare_WARP_2026.6.822.0.pkg"), "macos");
    }

    @Test
    void anonymousCannotDownloadClient() throws Exception {
        mvc.perform(get("/api/downloads/client/windows"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loggedInUserCanDownloadWindowsClient() throws Exception {
        insertUser("user@example.com", 1, 0, LocalDate.now());
        String token = jwtService.generate("user@example.com", "USER");

        mvc.perform(get("/api/downloads/client/windows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("Cloudflare_WARP_2026.6.822.0.msi")))
                .andExpect(content().bytes("windows".getBytes()));
        Integer usedToday = jdbc.queryForObject(
                "SELECT downloads_used_today FROM app_user WHERE email = ?",
                Integer.class, "user@example.com");
        org.assertj.core.api.Assertions.assertThat(usedToday).isEqualTo(1);
    }

    @Test
    void loggedInUserCanDownloadMacClient() throws Exception {
        insertUser("user@example.com", 1, 0, LocalDate.now());
        String token = jwtService.generate("user@example.com", "USER");

        mvc.perform(get("/api/downloads/client/macos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("Cloudflare_WARP_2026.6.822.0.pkg")))
                .andExpect(content().bytes("macos".getBytes()));
        Integer usedToday = jdbc.queryForObject(
                "SELECT downloads_used_today FROM app_user WHERE email = ?",
                Integer.class, "user@example.com");
        org.assertj.core.api.Assertions.assertThat(usedToday).isEqualTo(1);
    }

    @Test
    void loggedInUserCannotDownloadWhenDailyLimitIsExhausted() throws Exception {
        insertUser("user@example.com", 1, 1, LocalDate.now());
        String token = jwtService.generate("user@example.com", "USER");

        mvc.perform(get("/api/downloads/client/windows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("今日客户端下载次数已用完，请联系管理员"));
    }

    @Test
    void downloadLimitResetsOnNextDay() throws Exception {
        insertUser("user@example.com", 1, 1, LocalDate.now().minusDays(1));
        String token = jwtService.generate("user@example.com", "USER");

        mvc.perform(get("/api/downloads/client/windows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("windows".getBytes()));

        Integer usedToday = jdbc.queryForObject(
                "SELECT downloads_used_today FROM app_user WHERE email = ?",
                Integer.class, "user@example.com");
        LocalDate resetDate = jdbc.queryForObject(
                "SELECT download_limit_reset_date FROM app_user WHERE email = ?",
                LocalDate.class, "user@example.com");
        org.assertj.core.api.Assertions.assertThat(usedToday).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(resetDate).isEqualTo(LocalDate.now());
    }

    @Test
    void defaultDailyLimitAllowsOneDownloadPerAccount() throws Exception {
        insertUserWithDefaultLimit("user@example.com");
        String token = jwtService.generate("user@example.com", "USER");

        mvc.perform(get("/api/downloads/client/windows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes("windows".getBytes()));

        mvc.perform(get("/api/downloads/client/macos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("今日客户端下载次数已用完，请联系管理员"));
    }

    private void insertUser(String email, int dailyDownloadLimit, int downloadsUsedToday, LocalDate resetDate) {
        jdbc.update("""
                INSERT INTO app_user
                    (email, password_hash, role, enabled,
                     daily_download_limit, downloads_used_today, download_limit_reset_date)
                VALUES (?, ?, 'USER', TRUE, ?, ?, ?)
                """, email, "unused", dailyDownloadLimit, downloadsUsedToday, resetDate);
    }

    private void insertUserWithDefaultLimit(String email) {
        jdbc.update("""
                INSERT INTO app_user (email, password_hash, role, enabled)
                VALUES (?, ?, 'USER', TRUE)
                """, email, "unused");
    }
}
