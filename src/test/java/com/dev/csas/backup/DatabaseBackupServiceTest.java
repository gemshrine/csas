package com.dev.csas.backup;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBackupServiceTest {

    @Test
    void createsBackupFileFromDumpCommand() throws Exception {
        byte[] archive = {1, 2, 3, 4};
        DatabaseBackupService service = new DatabaseBackupService(path -> Files.write(path, archive));

        Path backup = service.createBackup();
        try {
            assertArrayEquals(archive, Files.readAllBytes(backup));
        } finally {
            Files.deleteIfExists(backup);
        }
    }

    @Test
    void removesPartialFileWhenDumpCommandFails() throws Exception {
        Path[] partialFile = new Path[1];
        DatabaseBackupService service = new DatabaseBackupService(path -> {
            partialFile[0] = path;
            Files.write(path, new byte[]{1});
            throw new IOException("pg_dump failed");
        });

        assertThrows(DatabaseBackupException.class, service::createBackup);
        assertTrue(Files.notExists(partialFile[0]));
    }

    @Test
    void rejectsEmptyBackup() throws Exception {
        Path[] emptyFile = new Path[1];
        DatabaseBackupService service = new DatabaseBackupService(path -> emptyFile[0] = path);

        assertThrows(DatabaseBackupException.class, service::createBackup);
        assertTrue(Files.notExists(emptyFile[0]));
    }
}
