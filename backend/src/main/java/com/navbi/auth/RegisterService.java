package com.navbi.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.navbi.track.GeoInfo;
import com.navbi.track.GeoResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
public class RegisterService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);

    private final AppUserMapper userMapper;
    private final EmailCodeMapper codeMapper;
    private final MailSender mailSender;
    private final RateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final GeoResolver geoResolver;
    private final SecureRandom random = new SecureRandom();

    public RegisterService(AppUserMapper userMapper, EmailCodeMapper codeMapper, MailSender mailSender,
                           RateLimiter rateLimiter, PasswordEncoder passwordEncoder, GeoResolver geoResolver) {
        this.userMapper = userMapper;
        this.codeMapper = codeMapper;
        this.mailSender = mailSender;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
        this.geoResolver = geoResolver;
    }

    public void sendCode(String email, String ip) {
        checkCodeRateLimit(email, ip);
        if (emailRegistered(email)) {
            throw new IllegalArgumentException("邮箱已注册");
        }
        issueCode(email, "NavBI 注册验证码");
    }

    public void sendPasswordCode(String email, String ip) {
        checkCodeRateLimit(email, ip);
        if (!emailRegistered(email)) {
            throw new IllegalArgumentException("邮箱未注册");
        }
        issueCode(email, "NavBI 修改密码验证码");
    }

    public void register(String email, String code, String password, String ip, String countryCode) {
        if (!rateLimiter.tryAcquire("register-ip:" + ip, 5, Duration.ofHours(1))) {
            throw new RateLimitExceededException("注册请求过于频繁，请稍后再试");
        }
        if (emailRegistered(email)) {
            throw new IllegalArgumentException("邮箱已注册");
        }
        consumeCode(email, code);

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setRegisterIp(ip);
        GeoInfo geo = geoResolver.resolve(ip, countryCode);
        user.setCountry(geo.country());
        user.setProvince(geo.province());
        user.setCity(geo.city());
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        log.info("新用户注册: {}", email);
    }

    public void resetPassword(String email, String code, String password, String ip) {
        if (!rateLimiter.tryAcquire("pwd-reset-ip:" + ip, 5, Duration.ofHours(1))) {
            throw new RateLimitExceededException("请求过于频繁，请稍后再试");
        }
        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getEmail, email));
        if (user == null) {
            throw new IllegalArgumentException("邮箱未注册");
        }
        consumeCode(email, code);
        user.setPasswordHash(passwordEncoder.encode(password));
        userMapper.updateById(user);
        log.info("用户修改密码: {}", email);
    }

    private void checkCodeRateLimit(String email, String ip) {
        if (!rateLimiter.tryAcquire("code-ip-min:" + ip, 1, Duration.ofMinutes(1))
                || !rateLimiter.tryAcquire("code-ip-day:" + ip, 10, Duration.ofDays(1))
                || !rateLimiter.tryAcquire("code-email-min:" + email, 1, Duration.ofMinutes(1))) {
            throw new RateLimitExceededException("验证码请求过于频繁，请稍后再试");
        }
    }

    private void issueCode(String email, String subject) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        EmailCode record = new EmailCode();
        record.setEmail(email);
        record.setCode(code);
        record.setUsed(false);
        record.setExpiresAt(LocalDateTime.now().plus(CODE_TTL));
        record.setCreatedAt(LocalDateTime.now());
        codeMapper.insert(record);
        mailSender.send(email, subject,
                "您的验证码是 " + code + "，" + CODE_TTL.toMinutes() + " 分钟内有效。如非本人操作请忽略。");
    }

    /** 校验并标记验证码：一次性、限时。 */
    private void consumeCode(String email, String code) {
        EmailCode latest = codeMapper.selectOne(new LambdaQueryWrapper<EmailCode>()
                .eq(EmailCode::getEmail, email)
                .eq(EmailCode::getUsed, false)
                .gt(EmailCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(EmailCode::getId)
                .last("LIMIT 1"));
        if (latest == null || !latest.getCode().equals(code)) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        latest.setUsed(true);
        codeMapper.updateById(latest);
    }

    private boolean emailRegistered(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<AppUser>().eq(AppUser::getEmail, email)) > 0;
    }
}
