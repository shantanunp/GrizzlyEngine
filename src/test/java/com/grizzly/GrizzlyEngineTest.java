package com.grizzly;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for Grizzly Engine
 */
class GrizzlyEngineTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void shouldTransformSimpleTemplate() throws Exception {
        // Create template
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["clientReference"] = INPUT.customerId
                return OUTPUT
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        // Create input
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "C123");
        
        // Execute
        GrizzlyEngine engine = new GrizzlyEngine();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = engine.transform(
            input, 
            templateFile.toString(), 
            Map.class
        );
        
        // Verify
        assertThat(result).containsEntry("clientReference", "C123");
    }
    
    @Test
    void shouldTransformNestedObjects() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["profile"]["contact"] = INPUT.personalInfo.email
                return OUTPUT
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        // Create nested input
        Map<String, Object> personalInfo = new HashMap<>();
        personalInfo.put("email", "john@example.com");
        
        Map<String, Object> input = new HashMap<>();
        input.put("personalInfo", personalInfo);
        
        // Execute
        GrizzlyEngine engine = new GrizzlyEngine();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = engine.transform(
            input, 
            templateFile.toString(), 
            Map.class
        );
        
        // Verify nested structure
        assertThat(result).containsKey("profile");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) result.get("profile");
        assertThat(profile).containsEntry("contact", "john@example.com");
    }
    
    @Test
    void shouldExecuteIfElse() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                if INPUT.type == "PREMIUM":
                    OUTPUT["level"] = "GOLD"
                else:
                    OUTPUT["level"] = "SILVER"
                return OUTPUT
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        GrizzlyEngine engine = new GrizzlyEngine();
        
        // Test PREMIUM
        Map<String, Object> premiumInput = new HashMap<>();
        premiumInput.put("type", "PREMIUM");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> premiumResult = engine.transform(
            premiumInput, 
            templateFile.toString(), 
            Map.class
        );
        assertThat(premiumResult).containsEntry("level", "GOLD");
        
        // Test non-PREMIUM
        Map<String, Object> regularInput = new HashMap<>();
        regularInput.put("type", "REGULAR");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> regularResult = engine.transform(
            regularInput, 
            templateFile.toString(), 
            Map.class
        );
        assertThat(regularResult).containsEntry("level", "SILVER");
    }
    
    @Test
    void shouldCallModuleFunctions() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["id"] = INPUT.customerId
                map_customer(INPUT, OUTPUT)
                return OUTPUT
            
            def map_customer(INPUT, OUTPUT):
                OUTPUT["name"] = INPUT.firstName
                OUTPUT["email"] = INPUT.email
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        // Create input
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "C123");
        input.put("firstName", "John");
        input.put("email", "john@example.com");
        
        // Execute
        GrizzlyEngine engine = new GrizzlyEngine();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = engine.transform(
            input, 
            templateFile.toString(), 
            Map.class
        );
        
        // Verify
        assertThat(result)
            .containsEntry("id", "C123")
            .containsEntry("name", "John")
            .containsEntry("email", "john@example.com");
    }
    
    @Test
    void shouldCacheCompiledTemplates() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["id"] = INPUT.customerId
                return OUTPUT
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        GrizzlyEngine engine = new GrizzlyEngine(true); // Enable caching
        
        // First execution - should compile
        Map<String, Object> input1 = Map.of("customerId", "C1");
        engine.transform(input1, templateFile.toString(), Map.class);
        
        assertThat(engine.getCacheSize()).isEqualTo(1);
        
        // Second execution - should use cache
        Map<String, Object> input2 = Map.of("customerId", "C2");
        engine.transform(input2, templateFile.toString(), Map.class);
        
        assertThat(engine.getCacheSize()).isEqualTo(1); // Still 1 (same template)
    }
    
    @Test
    void shouldCompileFromString() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["result"] = "success"
                return OUTPUT
            """;
        
        GrizzlyEngine engine = new GrizzlyEngine();
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        Map<String, Object> input = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = compiled.execute(input, Map.class);
        
        assertThat(result).containsEntry("result", "success");
    }
    
    @Test
    void shouldHandleComparisons() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                if INPUT.age >= 18:
                    OUTPUT["status"] = "adult"
                else:
                    OUTPUT["status"] = "minor"
                return OUTPUT
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        GrizzlyEngine engine = new GrizzlyEngine();
        
        // Test adult
        Map<String, Object> adultInput = Map.of("age", 25);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> adultResult = engine.transform(
            adultInput, 
            templateFile.toString(), 
            Map.class
        );
        assertThat(adultResult).containsEntry("status", "adult");
        
        // Test minor
        Map<String, Object> minorInput = Map.of("age", 15);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> minorResult = engine.transform(
            minorInput, 
            templateFile.toString(), 
            Map.class
        );
        assertThat(minorResult).containsEntry("status", "minor");
    }
}
