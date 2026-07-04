package com.navbi.download;

import com.navbi.ApiTestBase;
import com.navbi.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        String token = jwtService.generate("user@example.com", "USER");

        mvc.perform(get("/api/downloads/client/windows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("Cloudflare_WARP_2026.6.822.0.msi")))
                .andExpect(content().bytes("windows".getBytes()));
    }

    @Test
    void loggedInUserCanDownloadMacClient() throws Exception {
        String token = jwtService.generate("user@example.com", "USER");

        mvc.perform(get("/api/downloads/client/macos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("Cloudflare_WARP_2026.6.822.0.pkg")))
                .andExpect(content().bytes("macos".getBytes()));
    }
}
