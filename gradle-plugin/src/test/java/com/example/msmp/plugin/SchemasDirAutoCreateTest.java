package com.example.msmp.plugin;

import com.example.msmp.plugin.generator.OpenRpcCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SchemasDirAutoCreateTest {

    @Test
    void testGenerateAllCreatesMissingSchemasDir(@TempDir Path tempDir) throws IOException {
        Path schemasDir = tempDir.resolve("non_existent_schemas");
        Path outputDir = tempDir.resolve("generated_src");

        assertFalse(Files.exists(schemasDir));

        OpenRpcCodeGenerator generator = new OpenRpcCodeGenerator("com.example.msmp.transport.MinecraftManagementClient");
        generator.generateAll(schemasDir, outputDir, "com.example.msmp.generated");

        assertTrue(Files.exists(schemasDir), "schemasDir should be automatically created if not present");
        assertTrue(Files.exists(outputDir), "outputDir should also be created");
    }
}
