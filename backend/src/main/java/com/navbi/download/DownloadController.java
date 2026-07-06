package com.navbi.download;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.navbi.auth.AppUser;
import com.navbi.auth.AppUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/downloads")
public class DownloadController {

    private record ClientFile(String filename, String contentType) {
    }

    private static final Map<String, ClientFile> CLIENT_FILES = Map.of(
            "windows", new ClientFile("Cloudflare_WARP_2026.6.822.0.msi", "application/octet-stream"),
            "macos", new ClientFile("Cloudflare_WARP_2026.6.822.0.pkg", "application/octet-stream"));

    private final Path downloadDir;
    private final AppUserMapper userMapper;

    public DownloadController(@Value("${navbi.download.dir:/opt/navbi/downloads}") String downloadDir,
                              AppUserMapper userMapper) {
        this.downloadDir = Path.of(downloadDir).normalize();
        this.userMapper = userMapper;
    }

    @GetMapping("/client/{platform}")
    public ResponseEntity<Resource> client(@PathVariable String platform,
                                           Authentication authentication) throws IOException {
        ClientFile clientFile = CLIENT_FILES.get(platform);
        if (clientFile == null) {
            return ResponseEntity.notFound().build();
        }

        Path file = downloadDir.resolve(clientFile.filename()).normalize();
        if (!file.startsWith(downloadDir) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            return ResponseEntity.notFound().build();
        }

        if (!isAdmin(authentication)) {
            consumeDailyDownload(authentication.getName());
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(clientFile.contentType()))
                .contentLength(Files.size(file))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(clientFile.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }

    private void consumeDailyDownload(String email) {
        int updated = userMapper.consumeClientDownload(email);
        if (updated == 1) {
            return;
        }

        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getEmail, email));
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new DownloadLimitExceededException("账号不可下载客户端，请联系管理员");
        }
        throw new DownloadLimitExceededException("今日客户端下载次数已用完，请联系管理员");
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
