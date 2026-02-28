package com.grizzly;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;
import com.grizzly.core.logging.GrizzlyLogger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * MainApplicationTest class for Grizzly Engine - Transform JSON using Python-like templates
 * <p>
 * NO COMMAND LINE ARGUMENTS NEEDED!
 * Just put your files in the right place and run:
 * java -jar grizzly.jar
 * <p>
 * Required folder structure:
 * examples/
 * ├── input.json      (your input data)
 * └── transform.py    (your transformation template)
 * <p>
 * Output:
 * - output.json       (transformed result, in current directory)
 */
public class MainApplicationTest {

    private static final String INPUT_FILE = "examples/input.json";
    private static final String TEMPLATE_FILE = "examples/transform.py";
    private static final String OUTPUT_FILE = "examples/output.json";

    @Test
    public void testEmptyList() throws Exception {
        try {
            GrizzlyLogger.info("MainApplicationTest", "Grizzly Engine - Data Transformation");
            GrizzlyLogger.info("MainApplicationTest", "=====================================");

            String result = transform();
            GrizzlyLogger.info("MainApplicationTest", result);

//          Files.writeString(Path.of(OUTPUT_FILE), result);

            GrizzlyLogger.info("MainApplicationTest", "Transformation complete!");
            GrizzlyLogger.info("MainApplicationTest", "Output written to: " + OUTPUT_FILE);

        } catch (Exception e) {
            System.err.println();
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Transform JSON using template
     *
     * @return Transformed JSON as string
     */
    public static String transform() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        GrizzlyLogger.info("MainApplicationTest", "Reading input data...");
        Map<String, Object> inputData = mapper.readValue(
                new File(INPUT_FILE),
                new TypeReference<Map<String, Object>>() {
                }
        );

        GrizzlyLogger.info("MainApplicationTest", "Reading template...");
        String template = Files.readString(Path.of(TEMPLATE_FILE));

        GrizzlyLogger.info("MainApplicationTest", "Compiling template...");
        GrizzlyEngine engine = new GrizzlyEngine();
        long startCompile = System.currentTimeMillis();
        GrizzlyTemplate compiledTemplate = engine.compile(template);
        long compileTime = System.currentTimeMillis() - startCompile;
        GrizzlyLogger.info("MainApplicationTest", "   Compiled in " + compileTime + "ms");

        GrizzlyLogger.info("MainApplicationTest", "Executing transformation...");
        long startExec = System.currentTimeMillis();
        Map<String, Object> outputData = compiledTemplate.executeRaw(inputData);
        long execTime = System.currentTimeMillis() - startExec;
        GrizzlyLogger.info("MainApplicationTest", "   Executed in " + execTime + "ms");
        GrizzlyLogger.info("MainApplicationTest", "   Generated " + outputData.size() + " fields");

        String outputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(outputData);
        return outputJson;
    }
}
