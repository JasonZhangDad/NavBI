package com.navbi.auth;

import com.navbi.ApiTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetApiTest extends ApiTestBase {

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
    void setUpUser() {
        SENT.clear();
        // 已注册用户 a@b.com，密码 password8
        jdbc.update("INSERT INTO app_user (email, password_hash, role) VALUES (?, ?, 'USER')", "a@b.com",
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password8"));
    }

    private int requestCode(String email) throws Exception {
        return mvc.perform(post("/api/auth/password/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    private int reset(String email, String code, String password) throws Exception {
        return mvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code
                                + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    private int login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    private void insertCode(String email, String code) {
        jdbc.update("INSERT INTO email_code (email, code, expires_at) VALUES (?, ?, "
                + "DATEADD('MINUTE', 10, CURRENT_TIMESTAMP))", email, code);
    }

    @Test
    void codeForRegisteredEmailIsSentByMail() throws Exception {
        assertThat(requestCode("a@b.com")).isEqualTo(200);
        String code = jdbc.queryForObject("SELECT code FROM email_code WHERE email = 'a@b.com'", String.class);
        assertThat(SENT).hasSize(1);
        assertThat(SENT.get(0).text()).contains(code);
    }

    @Test
    void codeForUnregisteredEmailRejected() throws Exception {
        assertThat(requestCode("nobody@b.com")).isEqualTo(400);
        assertThat(SENT).isEmpty();
    }

    @Test
    void codeRequestsAreRateLimitedPerIp() throws Exception {
        assertThat(requestCode("a@b.com")).isEqualTo(200);
        assertThat(requestCode("a@b.com")).isEqualTo(429);
    }

    @Test
    void resetWithCorrectCodeChangesPassword() throws Exception {
        insertCode("a@b.com", "654321");

        assertThat(reset("a@b.com", "654321", "new-password9")).isEqualTo(200);

        assertThat(login("a@b.com", "new-password9")).isEqualTo(200);
        assertThat(login("a@b.com", "password8")).isEqualTo(401);
    }

    @Test
    void resetWithWrongCodeReturns400AndKeepsOldPassword() throws Exception {
        insertCode("a@b.com", "654321");

        assertThat(reset("a@b.com", "000000", "new-password9")).isEqualTo(400);
        assertThat(login("a@b.com", "password8")).isEqualTo(200);
    }

    @Test
    void resetCodeIsSingleUse() throws Exception {
        insertCode("a@b.com", "654321");
        assertThat(reset("a@b.com", "654321", "new-password9")).isEqualTo(200);
        assertThat(reset("a@b.com", "654321", "other-password1")).isEqualTo(400);
    }

    @Test
    void resetShortPasswordReturns400() throws Exception {
        insertCode("a@b.com", "654321");
        assertThat(reset("a@b.com", "654321", "short")).isEqualTo(400);
    }
}
