package com.navbi.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MailConfig {

    /** 未配置邮件 API 时仅打日志（本地开发），配置后走 HTTP 发送。 */
    @Bean
    public MailSender mailSender(@Value("${navbi.mail.api-url:}") String apiUrl,
                                 @Value("${navbi.mail.api-token:}") String apiToken) {
        if (apiUrl == null || apiUrl.isBlank()) {
            log.warn("navbi.mail.api-url 未配置，验证码邮件只打日志不真实发送");
            return (to, subject, text) -> log.info("[仅日志] 邮件 to={} subject={} text={}", to, subject, text);
        }
        return new HttpMailSender(apiUrl, apiToken);
    }
}
