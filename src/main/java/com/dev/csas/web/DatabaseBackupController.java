package com.dev.csas.web;

import com.dev.csas.backup.DatabaseBackupException;
import com.dev.csas.backup.DatabaseBackupService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Controller
public class DatabaseBackupController {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private final DatabaseBackupService backupService;

    public DatabaseBackupController(DatabaseBackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping("/admin/backups")
    public String page(@RequestParam(name = "error", required = false) String error, Model model) {
        if ("creation".equals(error)) {
            model.addAttribute("backupError",
                    "Не удалось создать резервную копию. Проверьте, что Docker и PostgreSQL запущены.");
        }
        return "admin/backups";
    }

    @PostMapping("/admin/backups")
    public ResponseEntity<StreamingResponseBody> createBackup() throws IOException {
        Path backupFile;
        try {
            backupFile = backupService.createBackup();
        } catch (DatabaseBackupException exception) {
            return ResponseEntity.status(HttpStatus.SEE_OTHER)
                    .location(URI.create("/admin/backups?error=creation"))
                    .build();
        }

        String filename = "csas-backup-" + FILE_TIMESTAMP.format(Instant.now()) + ".dump";
        long contentLength = Files.size(backupFile);
        StreamingResponseBody responseBody = outputStream -> {
            try (var inputStream = Files.newInputStream(backupFile)) {
                inputStream.transferTo(outputStream);
            } finally {
                Files.deleteIfExists(backupFile);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(responseBody);
    }
}
