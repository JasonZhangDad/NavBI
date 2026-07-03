package com.navbi.auth;

/** 邮件发送抽象：生产走 HTTP 邮件 API，本地/测试可替换。 */
public interface MailSender {

    void send(String to, String subject, String text);
}
