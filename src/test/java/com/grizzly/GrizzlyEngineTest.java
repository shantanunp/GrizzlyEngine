package com.grizzly;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;

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
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["clientReference"] = INPUT.customerId
                return OUTPUT
            """;
        
        Path templateFile = tempDir.resolve("transform.py");
        Files.writeString(templateFile, template);
        
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "C123");
        
        GrizzlyEngine engine = new GrizzlyEngine();
        GrizzlyTemplate compiled = engine.compileFromFile(templateFile.toString());
        Map<String, Object> result = compiled.executeRaw(input);
        
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
        
        Map<String, Object> personalInfo = new HashMap<>();
        personalInfo.put("email", "john@example.com");
        
        Map<String, Object> input = new HashMap<>();
        input.put("personalInfo", personalInfo);
        
        GrizzlyEngine engine = new GrizzlyEngine();
        GrizzlyTemplate compiled = engine.compileFromFile(templateFile.toString());
        Map<String, Object> result = compiled.executeRaw(input);
        
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
        GrizzlyTemplate compiled = engine.compileFromFile(templateFile.toString());
        
        Map<String, Object> premiumInput = new HashMap<>();
        premiumInput.put("type", "PREMIUM");
        Map<String, Object> premiumResult = compiled.executeRaw(premiumInput);
        assertThat(premiumResult).containsEntry("level", "GOLD");
        
        Map<String, Object> regularInput = new HashMap<>();
        regularInput.put("type", "REGULAR");
        Map<String, Object> regularResult = compiled.executeRaw(regularInput);
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
        
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "C123");
        input.put("firstName", "John");
        input.put("email", "john@example.com");
        
        GrizzlyEngine engine = new GrizzlyEngine();
        GrizzlyTemplate compiled = engine.compileFromFile(templateFile.toString());
        Map<String, Object> result = compiled.executeRaw(input);
        
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
        
        GrizzlyEngine engine = new GrizzlyEngine(true);
        
        Map<String, Object> input1 = Map.of("customerId", "C1");
        GrizzlyTemplate compiled1 = engine.compileFromFile(templateFile.toString());
        compiled1.executeRaw(input1);
        
        assertThat(engine.getCacheSize()).isEqualTo(1);
        
        Map<String, Object> input2 = Map.of("customerId", "C2");
        GrizzlyTemplate compiled2 = engine.compileFromFile(templateFile.toString());
        compiled2.executeRaw(input2);
        
        assertThat(engine.getCacheSize()).isEqualTo(1);
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
        GrizzlyTemplate compiled = engine.compile(template);
        
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> result = compiled.executeRaw(input);
        
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
        GrizzlyTemplate compiled = engine.compileFromFile(templateFile.toString());
        
        Map<String, Object> adultInput = Map.of("age", 25);
        Map<String, Object> adultResult = compiled.executeRaw(adultInput);
        assertThat(adultResult).containsEntry("status", "adult");
        
        Map<String, Object> minorInput = Map.of("age", 15);
        Map<String, Object> minorResult = compiled.executeRaw(minorInput);
        assertThat(minorResult).containsEntry("status", "minor");
    }
}
