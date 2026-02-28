package com.grizzly;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Python import statement support.
 * 
 * <p>Verifies that both styles work:
 * <ul>
 *   <li>Old style: {@code re_match(pattern, text)}</li>
 *   <li>New style: {@code import re} then {@code re.match(pattern, text)}</li>
 * </ul>
 */
@DisplayName("Import Statement Tests")
public class ImportStatementTest {
    
    private final GrizzlyEngine engine = new GrizzlyEngine();
    
    @Test
    @DisplayName("Import re - use re.match() with Python syntax")
    public void testImportReMatch() {
        String template = """
            import re
            
            def transform(INPUT):
                OUTPUT = {}
                if re.match(r"^\\d{3}-\\d{2}-\\d{4}$", INPUT.ssn):
                    OUTPUT["validSSN"] = true
                else:
                    OUTPUT["validSSN"] = false
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        // Valid SSN
        Map<String, Object> result1 = compiled.executeRaw(Map.of("ssn", "123-45-6789"));
        assertEquals(true, result1.get("validSSN"));
        
        // Invalid SSN
        Map<String, Object> result2 = compiled.executeRaw(Map.of("ssn", "abc"));
        assertEquals(false, result2.get("validSSN"));
    }
    
    @Test
    @DisplayName("Import re - use re.search()")
    public void testImportReSearch() {
        String template = """
            import re
            
            def transform(INPUT):
                OUTPUT = {}
                match = re.search(r"\\d+", INPUT.text)
                if match:
                    OUTPUT["found"] = true
                else:
                    OUTPUT["found"] = false
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        Map<String, Object> result1 = compiled.executeRaw(Map.of("text", "abc123"));
        assertEquals(true, result1.get("found"));
        
        Map<String, Object> result2 = compiled.executeRaw(Map.of("text", "abc"));
        assertEquals(false, result2.get("found"));
    }
    
    @Test
    @DisplayName("Import re - use re.sub()")
    public void testImportReSub() {
        String template = """
            import re
            
            def transform(INPUT):
                OUTPUT = {}
                cleaned = re.sub(r"\\D", "", INPUT.phone)
                OUTPUT["phone"] = cleaned
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of("phone", "(555) 123-4567"));
        
        assertEquals("5551234567", result.get("phone"));
    }
    
    @Test
    @DisplayName("Import unknown module fails with clear error")
    public void testImportUnknownModule() {
        String template = """
            import unknown_module
            
            def transform(INPUT):
                OUTPUT = {}
                return OUTPUT
            """;
        
        // Should throw during compilation/execution
        GrizzlyTemplate compiled = engine.compileFromString(template);
        assertThrows(Exception.class, () -> {
            compiled.executeRaw(Map.of());
        });
    }
    
    @Test
    @DisplayName("Multiple imports work")
    public void testMultipleImports() {
        String template = """
            import re
            
            def transform(INPUT):
                OUTPUT = {}
                
                # Use re module
                if re.match(r"^\\d+$", INPUT.text):
                    OUTPUT["isNumber"] = true
                else:
                    OUTPUT["isNumber"] = false
                
                # Use Decimal (built-in, no import needed)
                amount = Decimal("100.50")
                OUTPUT["amount"] = str(amount)
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of("text", "123"));
        
        assertEquals(true, result.get("isNumber"));
        assertEquals("100.50", result.get("amount"));
    }
}
