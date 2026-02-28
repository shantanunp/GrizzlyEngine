package com.grizzly.format;

import com.grizzly.format.json.JsonReader;
import com.grizzly.format.json.JsonWriter;
import com.grizzly.format.xml.XmlReader;
import com.grizzly.format.xml.XmlWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FormatRegistry.
 */
class FormatRegistryTest {
    
    @Test
    void defaultRegistryHasJsonAndXml() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        assertTrue(registry.hasReader("json"));
        assertTrue(registry.hasWriter("json"));
        assertTrue(registry.hasReader("xml"));
        assertTrue(registry.hasWriter("xml"));
    }
    
    @Test
    void getReaderReturnsCorrectType() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        assertInstanceOf(JsonReader.class, registry.getReader("json"));
        assertInstanceOf(XmlReader.class, registry.getReader("xml"));
    }
    
    @Test
    void getWriterReturnsCorrectType() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        assertInstanceOf(JsonWriter.class, registry.getWriter("json"));
        assertInstanceOf(XmlWriter.class, registry.getWriter("xml"));
    }
    
    @Test
    void formatNameIsCaseInsensitive() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        assertTrue(registry.hasReader("JSON"));
        assertTrue(registry.hasReader("Json"));
        assertTrue(registry.hasReader("json"));
        
        assertTrue(registry.hasWriter("XML"));
        assertTrue(registry.hasWriter("Xml"));
        assertTrue(registry.hasWriter("xml"));
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
        assertTrue(formats.contains("xml"));
        assertEquals(2, formats.size());
    }
    
    @Test
    void getWriterFormats() {
        FormatRegistry registry = FormatRegistry.createDefaultRegistry();
        
        var formats = registry.getWriterFormats();
        
        assertTrue(formats.contains("json"));
        assertTrue(formats.contains("xml"));
        assertEquals(2, formats.size());
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
