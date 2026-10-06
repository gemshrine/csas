package com.dev.csas.backup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class DockerPgDumpCommand implements DatabaseBackupCommand {
    private final String containerName;
    private final String databaseName;
    private final String username;
    private final String password;
    private final Duration timeout;

    public DockerPgDumpCommand(
            @Value("${app.backup.container:postgres}") String containerName,
            @Value("${POSTGRES_DB:csas}") String databaseName,
            @Value("${spring.datasource.username:csas}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${app.backup.timeout-seconds:300}") long timeoutSeconds
    ) {
        this.containerName = containerName;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public void execute(Path outputFile) throws IOException, InterruptedException {
        Path errorFile = Files.createTempFile("csas-backup-error-", ".log");
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "docker", "exec", "--env", "PGPASSWORD", containerName,
                    "pg_dump", "--host=127.0.0.1", "--format=custom", "--no-owner", "--no-acl",
                    "--username", username, "--dbname", databaseName
            );
            processBuilder.environment().put("PGPASSWORD", password);
            processBuilder.redirectOutput(outputFile.toFile());
            processBuilder.redirectError(errorFile.toFile());

            process = processBuilder.start();
            if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor();
                throw new IOException("Превышено время ожидания pg_dump");
            }
            if (process.exitValue() != 0) {
                String error = Files.readString(errorFile).strip();
                throw new IOException(error.isEmpty() ? "pg_dump завершился с ошибкой" : error);
            }
        } catch (InterruptedException exception) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw exception;
        } finally {
            Files.deleteIfExists(errorFile);
        }
    }
}
