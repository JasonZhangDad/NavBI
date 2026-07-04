package com.navbi.auth;

import com.navbi.ApiTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegisterApiTest extends ApiTestBase {

    record SentMail(String to, String subject, String text) {
    }

    static final List<SentMail> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class RecordingMailConfig {
        @Bean
        @Primary
        MailSender recordingMailSender() {
            return (to, subject, text) -> SENT.add(new SentMail(to, subject, text));
        }
    }

    @BeforeEach
    void clearSent() {
        SENT.clear();
    }

    private void requestCode(String email, int expectStatus) throws Exception {
        mvc.perform(post("/api/auth/register/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().is(expectStatus));
    }

    private void insertCode(String email, String code, String expiresAtSql) {
        jdbc.update("INSERT INTO email_code (email, code, expires_at) VALUES (?, ?, " + expiresAtSql + ")",
                email, code);
    }

    private int register(String email, String code, String password) throws Exception {
        return register(email, code, password, null, null);
    }

    private int register(String email, String code, String password, String ip, String countryCode) throws Exception {
        var builder = post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"code\":\"" + code
                        + "\",\"password\":\"" + password + "\"}");
        if (ip != null) {
            builder.header("CF-Connecting-IP", ip);
        }
        if (countryCode != null) {
            builder.header("CF-IPCountry", countryCode);
        }
        return mvc.perform(builder)
                .andReturn().getResponse().getStatus();
    }

    @Test
    void sendCodeStoresCodeAndSendsMail() throws Exception {
        requestCode("a@b.com", 200);

        String code = jdbc.queryForObject("SELECT code FROM email_code WHERE email = 'a@b.com'", String.class);
        assertThat(code).hasSize(6);
        assertThat(SENT).hasSize(1);
        assertThat(SENT.get(0).to()).isEqualTo("a@b.com");
        assertThat(SENT.get(0).text()).contains(code);
    }

    @Test
    void sendCodeRejectsInvalidEmail() throws Exception {
        requestCode("not-an-email", 400);
        assertThat(SENT).isEmpty();
    }

    @Test
    void sendCodeSecondRequestWithinMinuteIs429() throws Exception {
        requestCode("a@b.com", 200);
        requestCode("c@d.com", 429);
        assertThat(SENT).hasSize(1);
    }

    @Test
    void sendCodeForRegisteredEmailRejected() throws Exception {
        jdbc.update("INSERT INTO app_user (email, password_hash, role) VALUES ('a@b.com', 'x', 'USER')");
        requestCode("a@b.com", 400);
        assertThat(SENT).isEmpty();
    }

    @Test
    void registerWithCorrectCodeCreatesUserAndLoginReturnsUserRole() throws Exception {
        insertCode("a@b.com", "654321", "DATEADD('MINUTE', 10, CURRENT_TIMESTAMP)");

        assertThat(register("a@b.com", "654321", "password8")).isEqualTo(200);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a@b.com\",\"password\":\"password8\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void registerStoresIpAndGeo() throws Exception {
        insertCode("a@b.com", "654321", "DATEADD('MINUTE', 10, CURRENT_TIMESTAMP)");

        assertThat(register("a@b.com", "654321", "password8", "2606:4700:4700::1111", "CN")).isEqualTo(200);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT register_ip, country, province, city FROM app_user WHERE email = 'a@b.com'");
        assertThat(row.get("register_ip")).isEqualTo("2606:4700:4700::1111");
        assertThat(row.get("country")).isEqualTo("中国");
        assertThat(row.get("province")).isEqualTo("未知");
        assertThat(row.get("city")).isEqualTo("未知");
    }

    @Test
    void registerWithWrongCodeReturns400() throws Exception {
        insertCode("a@b.com", "654321", "DATEADD('MINUTE', 10, CURRENT_TIMESTAMP)");
        assertThat(register("a@b.com", "000000", "password8")).isEqualTo(400);
    }

    @Test
    void registerWithExpiredCodeReturns400() throws Exception {
        insertCode("a@b.com", "654321", "DATEADD('MINUTE', -1, CURRENT_TIMESTAMP)");
        assertThat(register("a@b.com", "654321", "password8")).isEqualTo(400);
    }

    @Test
    void registerWithoutCodeReturns400() throws Exception {
        assertThat(register("a@b.com", "123456", "password8")).isEqualTo(400);
    }

    @Test
    void codeIsSingleUse() throws Exception {
        insertCode("a@b.com", "654321", "DATEADD('MINUTE', 10, CURRENT_TIMESTAMP)");
        assertThat(register("a@b.com", "654321", "password8")).isEqualTo(200);
        // 同码同邮箱再次注册：无论因已注册还是码已用，必须拒绝
        assertThat(register("a@b.com", "654321", "password8")).isEqualTo(400);
    }

    @Test
    void shortPasswordReturns400() throws Exception {
        insertCode("a@b.com", "654321", "DATEADD('MINUTE', 10, CURRENT_TIMESTAMP)");
        assertThat(register("a@b.com", "654321", "short")).isEqualTo(400);
    }

    @Test
    void userTokenCannotAccessAdminApis() throws Exception {
        insertCode("a@b.com", "654321", "DATEADD('MINUTE', 10, CURRENT_TIMESTAMP)");
        register("a@b.com", "654321", "password8");
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a@b.com\",\"password\":\"password8\"}"))
                .andReturn().getResponse().getContentAsString();
        String userToken = objectMapper.readTree(body).path("data").path("token").asText();

        mvc.perform(get("/api/nav/all").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/bi/summary").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginFailuresAreRateLimitedPerIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().is(429));
    }
}
