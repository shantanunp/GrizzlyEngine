package com.grizzly;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

public class RawStringDebugTest {
    
    private final GrizzlyEngine engine = new GrizzlyEngine();
    
    @Test
    public void testRawStringBasic() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                pattern = r"\\d+"
                OUTPUT["pattern"] = pattern
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        System.out.println("Pattern result: " + result.get("pattern"));
        assertEquals("\\d+", result.get("pattern"));
    }
    
    @Test
    public void testRegexMatchSimple() {
        String template = """
            import re
            
            def transform(INPUT):
                OUTPUT = {}
                match = re.match(r"\\d+", INPUT.text)
                if match:
                    OUTPUT["matched"] = true
                else:
                    OUTPUT["matched"] = false
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        Map<String, Object> result1 = compiled.executeRaw(Map.of("text", "123"));
        System.out.println("Result for '123': " + result1);
        
        Map<String, Object> result2 = compiled.executeRaw(Map.of("text", "abc"));
        System.out.println("Result for 'abc': " + result2);
        
        assertEquals(true, result1.get("matched"));
        assertEquals(false, result2.get("matched"));
    }
}
