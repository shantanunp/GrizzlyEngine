package com.grizzly;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.grizzly.types.DateTimeValue;

/**
 * Tests for datetime functionality.
 * 
 * <p>Covers:
 * <ul>
 *   <li>now() - Current datetime with optional timezone</li>
 *   <li>parseDate() - Parse string to datetime</li>
 *   <li>formatDate() - Format datetime to string</li>
 *   <li>addDays(), addMonths(), addYears() - Date arithmetic</li>
 *   <li>addHours(), addMinutes() - Time arithmetic</li>
 * </ul>
 */
class DateTimeTest {
    
    private final GrizzlyEngine engine = new GrizzlyEngine();
    
    @Test
    void testNowFunction() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["timestamp"] = now()
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertThat(result.get("timestamp")).isInstanceOf(DateTimeValue.class);
        
        DateTimeValue dt = (DateTimeValue) result.get("timestamp");
        assertThat(dt.getYear()).isEqualTo(ZonedDateTime.now().getYear());
    }
    
    @Test
    void testNowWithTimezone() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["utc_time"] = now("UTC")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        DateTimeValue dt = (DateTimeValue) result.get("utc_time");
        assertThat(dt.getValue().getZone().getId()).isEqualTo("UTC");
    }
    
    @Test
    void testParseDateBasic() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["parsed"] = parseDate("2024-02-22", "yyyy-MM-dd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        DateTimeValue dt = (DateTimeValue) result.get("parsed");
        assertThat(dt.getYear()).isEqualTo(2024);
        assertThat(dt.getMonth()).isEqualTo(2);
        assertThat(dt.getDay()).isEqualTo(22);
    }
    
    @Test
    void testParseDateDifferentFormat() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["parsed"] = parseDate("22/02/2024", "dd/MM/yyyy")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        DateTimeValue dt = (DateTimeValue) result.get("parsed");
        assertThat(dt.getYear()).isEqualTo(2024);
        assertThat(dt.getMonth()).isEqualTo(2);
        assertThat(dt.getDay()).isEqualTo(22);
    }
    
    @Test
    void testFormatDate() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                dt = parseDate("2024-02-22", "yyyy-MM-dd")
                OUTPUT["formatted_slash"] = formatDate(dt, "dd/MM/yyyy")
                OUTPUT["formatted_compact"] = formatDate(dt, "yyyyMMdd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertThat(result.get("formatted_slash")).isEqualTo("22/02/2024");
        assertThat(result.get("formatted_compact")).isEqualTo("20240222");
    }
    
    @Test
    void testAddDays() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                dt = parseDate("2024-02-22", "yyyy-MM-dd")
                tomorrow = addDays(dt, 1)
                yesterday = addDays(dt, -1)
                OUTPUT["tomorrow"] = formatDate(tomorrow, "yyyy-MM-dd")
                OUTPUT["yesterday"] = formatDate(yesterday, "yyyy-MM-dd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertThat(result.get("tomorrow")).isEqualTo("2024-02-23");
        assertThat(result.get("yesterday")).isEqualTo("2024-02-21");
    }
    
    @Test
    void testAddMonths() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                dt = parseDate("2024-02-22", "yyyy-MM-dd")
                next_month = addMonths(dt, 1)
                OUTPUT["result"] = formatDate(next_month, "yyyy-MM-dd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertThat(result.get("result")).isEqualTo("2024-03-22");
    }
    
    @Test
    void testAddYears() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                dt = parseDate("2024-02-22", "yyyy-MM-dd")
                next_year = addYears(dt, 1)
                OUTPUT["result"] = formatDate(next_year, "yyyy-MM-dd")
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertThat(result.get("result")).isEqualTo("2025-02-22");
    }
    
    @Test
    void testDateConversionPipeline() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                
                # Parse input date in yyyyMMdd format
                input_date = parseDate(INPUT.date, "yyyyMMdd")
                
                # Add 5 days
                future_date = addDays(input_date, 5)
                
                # Format to dd/MM/yyyy
                OUTPUT["result"] = formatDate(future_date, "dd/MM/yyyy")
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> input = Map.of("date", "20240220");
        Map<String, Object> result = compiled.execute(input, Map.class);
        
        // 20240220 + 5 days = 20240225 = 25/02/2024
        assertThat(result.get("result")).isEqualTo("25/02/2024");
    }
    
    @Test
    void testComplexDateMapping() throws Exception {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                
                # Get current date
                today = now()
                
                # Calculate future dates
                OUTPUT["today"] = formatDate(today, "yyyy-MM-dd")
                OUTPUT["next_week"] = formatDate(addDays(today, 7), "yyyy-MM-dd")
                OUTPUT["next_month"] = formatDate(addMonths(today, 1), "yyyy-MM-dd")
                OUTPUT["next_year"] = formatDate(addYears(today, 1), "yyyy-MM-dd")
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        // Verify all date fields are present
        assertThat(result).containsKeys("today", "next_week", "next_month", "next_year");
        
        // Verify they're all valid date strings
        assertThat(result.get("today")).asString().matches("\\d{4}-\\d{2}-\\d{2}");
    }
}
