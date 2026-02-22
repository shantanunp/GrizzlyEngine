package com.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzly.GrizzlyEngine;
import com.grizzly.GrizzlyTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Main class for Grizzly Engine - Transform JSON using Python-like templates
 *
 * NO COMMAND LINE ARGUMENTS NEEDED!
 * Just put your files in the right place and run:
 *   java -jar grizzly.jar
 *
 * Required files (in same directory as JAR):
 *   - input.json      (your input data)
 *   - transform.py    (your transformation template)
 *
 * Output:
 *   - output.json     (transformed result)
 */
public class Main {

    // File names (in current directory)
    private static final String INPUT_FILE = "examples/customer_input.json";
    private static final String TEMPLATE_FILE = "examples/customer_transform.py";
    private static final String OUTPUT_FILE = "examples/output.json";             // Same (root)

    public static void main(String[] args) {
        try {
            System.out.println("🐻 Grizzly Engine - Data Transformation");
            System.out.println("═══════════════════════════════════════");
            System.out.println();

            // Check if files exist
            checkFilesExist();

            // Transform
            String result = transform();

            // Success!
            System.out.println();
            System.out.println("✅ Transformation complete!");
            Files.writeString(Path.of(OUTPUT_FILE), result);
            System.out.println(result);
            System.out.println();

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Error: " + e.getMessage());
            System.err.println();
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Check if required files exist
     */
    private static void checkFilesExist() {
        System.out.println("🔍 Checking for required files...");

        File inputFile = new File(INPUT_FILE);
        File templateFile = new File(TEMPLATE_FILE);

        if (!inputFile.exists()) {
            System.err.println("❌ Missing file: " + INPUT_FILE);
            System.err.println();
            printUsage();
            System.exit(1);
        }

        if (!templateFile.exists()) {
            System.err.println("❌ Missing file: " + TEMPLATE_FILE);
            System.err.println();
            printUsage();
            System.exit(1);
        }

        System.out.println("   ✓ Found " + INPUT_FILE);
        System.out.println("   ✓ Found " + TEMPLATE_FILE);
        System.out.println();
    }

    /**
     * Transform JSON using template
     *
     * @return Transformed JSON as string
     */
    public static String transform() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Read input JSON
        System.out.println("📖 Reading input data...");
        Map<String, Object> inputData = mapper.readValue(new File(INPUT_FILE), Map.class);
        System.out.println("   ✓ Loaded " + inputData.size() + " fields");

        // Show preview
        System.out.println("   Preview: " + preview(inputData));

        // 2. Read template
        System.out.println();
        System.out.println("📖 Reading template...");
        String template = Files.readString(Path.of(TEMPLATE_FILE));
        long lineCount = template.lines().count();
        System.out.println("   ✓ Loaded " + lineCount + " lines");

        // 3. Compile template
        System.out.println();
        System.out.println("🔨 Compiling template...");
        GrizzlyEngine engine = new GrizzlyEngine();
        long startCompile = System.currentTimeMillis();
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        long compileTime = System.currentTimeMillis() - startCompile;
        System.out.println("   ✓ Compiled in " + compileTime + "ms");

        // 4. Execute transformation
        System.out.println();
        System.out.println("⚡ Executing transformation...");
        long startExec = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        Map<String, Object> outputData = compiledTemplate.execute(inputData, Map.class);
        long execTime = System.currentTimeMillis() - startExec;
        System.out.println("   ✓ Executed in " + execTime + "ms");
        System.out.println("   ✓ Generated " + outputData.size() + " fields");

        // Show preview
        System.out.println("   Preview: " + preview(outputData));

        // 5. Convert to JSON
        System.out.println();
        System.out.println("📝 Formatting output...");
        String outputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(outputData);
        long jsonSize = outputJson.length();
        System.out.println("   ✓ Output size: " + jsonSize + " characters");

        return outputJson;
    }

    /**
     * Create a preview of Map data
     */
    private static String preview(Map<String, Object> data) {
        if (data.isEmpty()) {
            return "{}";
        }

        // Show first 3 keys
        StringBuilder sb = new StringBuilder("{");
        int count = 0;
        for (String key : data.keySet()) {
            if (count > 0) sb.append(", ");
            sb.append(key).append(": ...");
            if (++count >= 3) break;
        }
        if (data.size() > 3) {
            sb.append(", ... +" + (data.size() - 3) + " more");
        }
        sb.append("}");
        return sb.toString();
    }

    private static void printUsage() {
        System.out.println("═══════════════════════════════════════");
        System.out.println("🐻 Grizzly Engine - Simple Usage");
        System.out.println("═══════════════════════════════════════");
        System.out.println();
        System.out.println("Required files (in same directory):");
        System.out.println();
        System.out.println("  1. input.json      - Your input data");
        System.out.println("  2. transform.py    - Your transformation template");
        System.out.println();
        System.out.println("Output:");
        System.out.println("  - output.json      - Transformed result");
        System.out.println();
        System.out.println("Example Setup:");
        System.out.println("  my-project/");
        System.out.println("  ├── grizzly.jar");
        System.out.println("  ├── input.json");
        System.out.println("  └── transform.py");
        System.out.println();
        System.out.println("Run:");
        System.out.println("  java -jar grizzly.jar");
        System.out.println();
        System.out.println("═══════════════════════════════════════");
        System.out.println();
        System.out.println("Example input.json:");
        System.out.println("{");
        System.out.println("  \"customerId\": \"C123\",");
        System.out.println("  \"firstName\": \"John\",");
        System.out.println("  \"lastName\": \"Doe\"");
        System.out.println("}");
        System.out.println();
        System.out.println("Example transform.py:");
        System.out.println("def transform(INPUT):");
        System.out.println("    OUTPUT = {}");
        System.out.println("    OUTPUT[\"id\"] = INPUT.customerId");
        System.out.println("    OUTPUT[\"fullName\"] = INPUT.firstName + \" \" + INPUT.lastName");
        System.out.println("    return OUTPUT");
        System.out.println();
        System.out.println("═══════════════════════════════════════");
    }
}