package com.grizzly;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;

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
    
    private static final String INPUT_FILE = "examples/input.json";
    private static final String TEMPLATE_FILE = "examples/transform.py";
    private static final String OUTPUT_FILE = "examples/output.json";
    
    public static void main(String[] args) {
        try {
            System.out.println("Grizzly Engine - Data Transformation");
            System.out.println("=====================================");
            System.out.println();

            String result = transform();
            
            Files.writeString(Path.of(OUTPUT_FILE), result);
            
            System.out.println();
            System.out.println("Transformation complete!");
            System.out.println("Output written to: " + OUTPUT_FILE);

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
        
        System.out.println("Reading input data...");
        Map<String, Object> inputData = mapper.readValue(
            new File(INPUT_FILE), 
            new TypeReference<Map<String, Object>>() {}
        );
        
        System.out.println("Reading template...");
        String template = Files.readString(Path.of(TEMPLATE_FILE));
        
        System.out.println("Compiling template...");
        GrizzlyEngine engine = new GrizzlyEngine();
        long startCompile = System.currentTimeMillis();
        GrizzlyTemplate compiledTemplate = engine.compile(template);
        long compileTime = System.currentTimeMillis() - startCompile;
        System.out.println("   Compiled in " + compileTime + "ms");
        
        System.out.println("Executing transformation...");
        long startExec = System.currentTimeMillis();
        Map<String, Object> outputData = compiledTemplate.executeRaw(inputData);
        long execTime = System.currentTimeMillis() - startExec;
        System.out.println("   Executed in " + execTime + "ms");
        System.out.println("   Generated " + outputData.size() + " fields");
        
        String outputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(outputData);
        return outputJson;
    }
}
