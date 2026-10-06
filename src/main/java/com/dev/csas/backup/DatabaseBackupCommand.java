package com.dev.csas.backup;

import java.io.IOException;
import java.nio.file.Path;

public interface DatabaseBackupCommand {
    void execute(Path outputFile) throws IOException, InterruptedException;
}
