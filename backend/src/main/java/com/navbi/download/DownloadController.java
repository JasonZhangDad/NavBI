package com.navbi.download;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public DownloadController(@Value("${navbi.download.dir:/opt/navbi/downloads}") String downloadDir) {
        this.downloadDir = Path.of(downloadDir).normalize();
    }

    @GetMapping("/client/{platform}")
    public ResponseEntity<Resource> client(@PathVariable String platform) throws IOException {
        ClientFile clientFile = CLIENT_FILES.get(platform);
        if (clientFile == null) {
            return ResponseEntity.notFound().build();
        }

        Path file = downloadDir.resolve(clientFile.filename()).normalize();
        if (!file.startsWith(downloadDir) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            return ResponseEntity.notFound().build();
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
}
