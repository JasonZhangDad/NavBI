package com.navbi.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** 调用邮件 API（161.153.58.251 上的 mail-api）发送邮件。 */
@Slf4j
public class HttpMailSender implements MailSender {

    private final String apiUrl;
    private final String apiToken;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public HttpMailSender(String apiUrl, String apiToken) {
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
    }

    @Override
    public void send(String to, String subject, String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("to", to, "subject", subject, "text", text));
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("邮件 API 返回 {}: {}", response.statusCode(), response.body());
                throw new IllegalStateException("验证码邮件发送失败，请稍后再试");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("验证码邮件发送失败，请稍后再试", e);
        } catch (Exception e) {
            log.error("邮件 API 调用异常", e);
            throw new IllegalStateException("验证码邮件发送失败，请稍后再试", e);
        }
    }
}
