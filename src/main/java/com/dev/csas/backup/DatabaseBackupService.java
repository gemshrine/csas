package com.dev.csas.backup;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DatabaseBackupService {
    private final DatabaseBackupCommand backupCommand;

    public DatabaseBackupService(DatabaseBackupCommand backupCommand) {
        this.backupCommand = backupCommand;
    }

    public Path createBackup() {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("csas-backup-", ".dump");
            backupCommand.execute(outputFile);
            if (Files.size(outputFile) == 0) {
                throw new IOException("pg_dump создал пустой файл");
            }
            return outputFile;
        } catch (InterruptedException exception) {
            deleteFailedBackup(outputFile, exception);
            Thread.currentThread().interrupt();
            throw new DatabaseBackupException("Создание резервной копии прервано", exception);
        } catch (IOException exception) {
            deleteFailedBackup(outputFile, exception);
            throw new DatabaseBackupException("Не удалось создать резервную копию базы данных", exception);
        } catch (RuntimeException exception) {
            deleteFailedBackup(outputFile, exception);
            throw new DatabaseBackupException("Не удалось создать резервную копию базы данных", exception);
        }
    }

    private void deleteFailedBackup(Path outputFile, Exception exception) {
        if (outputFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(outputFile);
        } catch (IOException cleanupException) {
            exception.addSuppressed(cleanupException);
        }
    }
}
