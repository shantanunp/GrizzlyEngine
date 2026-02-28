package com.grizzly;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Required folder structure:
 *   examples/
 *   ├── input.json      (your input data)
 *   └── transform.py    (your transformation template)
 * 
 * Output:
 *   - output.json       (transformed result, in current directory)
 */
public class Main {
    
    // File paths (relative to JAR location)
    private static final String INPUT_FILE = "examples/input.json";
    private static final String TEMPLATE_FILE = "examples/transform.py";
    private static final String OUTPUT_FILE = "output.json";
    
    public static void main(String[] args) {
        try {
            System.out.println("🐻 Grizzly Engine - Data Transformation");
            System.out.println("═══════════════════════════════════════");
            System.out.println();

            // Transform
            String result = transform();
            
            // Write output
            Files.writeString(Path.of(OUTPUT_FILE), result);
            
            // Success!
            System.out.println();
            System.out.println("✅ Transformation complete!");
            System.out.println("📄 Output written to: " + OUTPUT_FILE);

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Error: " + e.getMessage());
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
        
        // 1. Read input JSON
        System.out.println("📖 Reading input data...");
        Map<String, Object> inputData = mapper.readValue(new File(INPUT_FILE), Map.class);
        // 2. Read template
        System.out.println("📖 Reading template...");
        String template = Files.readString(Path.of(TEMPLATE_FILE));
        // 3. Compile template
        System.out.println("🔨 Compiling template...");
        GrizzlyEngine engine = new GrizzlyEngine();
        long startCompile = System.currentTimeMillis();
        GrizzlyTemplate compiledTemplate = engine.compileFromString(template);
        long compileTime = System.currentTimeMillis() - startCompile;
        System.out.println("   ✓ Compiled in " + compileTime + "ms");
        // 4. Execute transformation
        System.out.println("⚡ Executing transformation...");
        long startExec = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        Map<String, Object> outputData = compiledTemplate.execute(inputData, Map.class);
        long execTime = System.currentTimeMillis() - startExec;
        System.out.println("   ✓ Executed in " + execTime + "ms");
        System.out.println("   ✓ Generated " + outputData.size() + " fields");
         // 5. Convert to JSON
        String outputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(outputData);
        return outputJson;
    }

}
