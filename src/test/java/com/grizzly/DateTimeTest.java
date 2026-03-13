package com.grizzly;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Tests for datetime functionality. Python-compatible: now(), formatDate(), datetime module.
 * 
 * <p>Covers:
 * <ul>
 *   <li>now() - Current datetime with optional timezone</li>
 *   <li>formatDate() - Format datetime to string</li>
 *   <li>datetime.strptime() - Parse string to datetime</li>
 * </ul>
 */
class DateTimeTest {
    
    private final GrizzlyEngine engine = new GrizzlyEngine();
    
    @Test
    void testNowFunction() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["timestamp"] = formatDate(now(), "yyyy-MM-dd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        // DateTimeValue is serialized to ISO string in JSON output
        assertThat(result.get("timestamp")).isInstanceOf(String.class);
        
        String dateStr = (String) result.get("timestamp");
        assertThat(dateStr).startsWith(String.valueOf(ZonedDateTime.now().getYear()));
    }
    
    @Test
    void testNowWithTimezone() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["utc_time"] = formatDate(now("UTC"), "yyyy-MM-dd'T'HH:mm:ssZ")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        // DateTimeValue is serialized to string, check it contains UTC offset (+0000)
        String utcTime = (String) result.get("utc_time");
        assertThat(utcTime).endsWith("+0000");
    }
    
    @Test
    void testParseDateBasic() throws Exception {
        String template = """
            from datetime import datetime
            
            def transform(INPUT):
                OUTPUT = {}
                dt = datetime.strptime("2024-02-22", "%Y-%m-%d")
                OUTPUT["year"] = formatDate(dt, "yyyy")
                OUTPUT["month"] = formatDate(dt, "MM")
                OUTPUT["day"] = formatDate(dt, "dd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertThat(result.get("year")).isEqualTo("2024");
        assertThat(result.get("month")).isEqualTo("02");
        assertThat(result.get("day")).isEqualTo("22");
    }
    
    @Test
    void testParseDateDifferentFormat() throws Exception {
        String template = """
            from datetime import datetime
            
            def transform(INPUT):
                OUTPUT = {}
                dt = datetime.strptime("22/02/2024", "%d/%m/%Y")
                OUTPUT["formatted"] = formatDate(dt, "yyyy-MM-dd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertThat(result.get("formatted")).isEqualTo("2024-02-22");
    }
    
    @Test
    void testFormatDate() throws Exception {
        String template = """
            from datetime import datetime
            
            def transform(INPUT):
                OUTPUT = {}
                dt = datetime.strptime("2024-02-22", "%Y-%m-%d")
                OUTPUT["formatted_slash"] = formatDate(dt, "dd/MM/yyyy")
                OUTPUT["formatted_compact"] = formatDate(dt, "yyyyMMdd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compile(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertThat(result.get("formatted_slash")).isEqualTo("22/02/2024");
        assertThat(result.get("formatted_compact")).isEqualTo("20240222");
    }
    
}
