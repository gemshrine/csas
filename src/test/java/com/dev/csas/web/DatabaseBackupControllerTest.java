package com.dev.csas.web;

import com.dev.csas.backup.DatabaseBackupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseBackupControllerTest {

    @TempDir
    Path tempDirectory;

    @Test
    @SuppressWarnings("unchecked")
    void streamsBackupAsDownloadAndDeletesTemporaryFile() throws Exception {
        byte[] archive = {4, 3, 2, 1};
        Path backupFile = tempDirectory.resolve("backup.dump");
        Files.write(backupFile, archive);
        DatabaseBackupService backupService = mock(DatabaseBackupService.class);
        when(backupService.createBackup()).thenReturn(backupFile);
        DatabaseBackupController controller = new DatabaseBackupController(backupService);

        ResponseEntity<StreamingResponseBody> response = controller.createBackup();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);

        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment;"));
        assertArrayEquals(archive, output.toByteArray());
        assertFalse(Files.exists(backupFile));
    }
}
