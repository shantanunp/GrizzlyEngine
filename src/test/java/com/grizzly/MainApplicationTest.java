package com.grizzly;

import com.grizzly.format.json.JsonTemplate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.grizzly.core.logging.GrizzlyLogger.info;

public class MainApplicationTest {

    private static final String INPUT_FILE = "examples/input.json";
    private static final String TEMPLATE_FILE = "examples/transform.py";

    @Test
    public void compile_transform() throws IOException {
        String templateString = Files.readString(Path.of(TEMPLATE_FILE));
        String input = Files.readString(Path.of(INPUT_FILE));
        // Compile a template
        long startCompile = System.currentTimeMillis();
        JsonTemplate template = JsonTemplate.compile(templateString);
        long compileTime = System.currentTimeMillis() - startCompile;

        // Transform JSON
        long startExec = System.currentTimeMillis();
        String output = template.transform(input);
        long execTime = System.currentTimeMillis() - startExec;

        info("MainApplicationTest", "  Compiled in " + compileTime + " ms");
        info("MainApplicationTest", "   Executed in " + execTime + " ms");
        info("MainApplicationTest", output);
    }
}
