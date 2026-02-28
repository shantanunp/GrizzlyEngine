package com.grizzly.format;

import com.grizzly.format.json.JsonReader;
import com.grizzly.format.json.JsonWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FormatRegistry.
 */
class FormatRegistryTest {
    
    @Test
    void defaultRegistryHasJson() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        assertTrue(registry.hasReader("json"));
        assertTrue(registry.hasWriter("json"));
    }
    
    @Test
    void getReaderReturnsCorrectType() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        assertInstanceOf(JsonReader.class, registry.getReader("json"));
    }
    
    @Test
    void getWriterReturnsCorrectType() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        assertInstanceOf(JsonWriter.class, registry.getWriter("json"));
    }
    
    @Test
    void formatNameIsCaseInsensitive() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        assertTrue(registry.hasReader("JSON"));
        assertTrue(registry.hasReader("Json"));
        assertTrue(registry.hasReader("json"));
        
        assertTrue(registry.hasWriter("JSON"));
        assertTrue(registry.hasWriter("Json"));
        assertTrue(registry.hasWriter("json"));
    }
    
    @Test
    void getUnknownReaderThrowsException() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        FormatException exception = assertThrows(FormatException.class, 
            () -> registry.getReader("yaml"));
        
        assertEquals("yaml", exception.getFormat());
        assertTrue(exception.getMessage().contains("No reader registered"));
    }
    
    @Test
    void getUnknownWriterThrowsException() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        FormatException exception = assertThrows(FormatException.class, 
            () -> registry.getWriter("csv"));
        
        assertEquals("csv", exception.getFormat());
        assertTrue(exception.getMessage().contains("No writer registered"));
    }
    
    @Test
    void registerCustomFormat() {
        FormatRegistry registry = new FormatRegistry();
        
        registry.registerReader("json", new JsonReader());
        registry.registerWriter("json", new JsonWriter());
        
        assertTrue(registry.hasReader("json"));
        assertTrue(registry.hasWriter("json"));
    }
    
    @Test
    void getReaderFormats() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        var formats = registry.getReaderFormats();
        
        assertTrue(formats.contains("json"));
        assertEquals(1, formats.size());
    }
    
    @Test
    void getWriterFormats() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        var formats = registry.getWriterFormats();
        
        assertTrue(formats.contains("json"));
        assertEquals(1, formats.size());
    }
    
    @Test
    void registerNullFormatThrows() {
        FormatRegistry registry = new FormatRegistry();
        
        assertThrows(NullPointerException.class, 
            () -> registry.registerReader(null, new JsonReader()));
        
        assertThrows(NullPointerException.class, 
            () -> registry.registerWriter(null, new JsonWriter()));
    }
    
    @Test
    void registerNullHandlerThrows() {
        FormatRegistry registry = new FormatRegistry();
        
        assertThrows(NullPointerException.class, 
            () -> registry.registerReader("json", null));
        
        assertThrows(NullPointerException.class, 
            () -> registry.registerWriter("json", null));
    }
}
