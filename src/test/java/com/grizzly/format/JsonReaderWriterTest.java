package com.grizzly.format;

import com.grizzly.format.json.JsonReader;
import com.grizzly.format.json.JsonWriter;
import com.grizzly.core.types.DictValue;
import com.grizzly.core.types.ListValue;
import com.grizzly.core.types.NumberValue;
import com.grizzly.core.types.StringValue;
import com.grizzly.core.types.BoolValue;
import com.grizzly.core.types.NullValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JSON reader and writer.
 */
class JsonReaderWriterTest {
    
    private final JsonReader reader = new JsonReader();
    private final JsonWriter writer = new JsonWriter();
    
    // ==================== Reader Tests ====================
    
    @Test
    void readSimpleObject() {
        String json = "{\"name\": \"John\", \"age\": 30}";
        DictValue result = reader.read(json);
        
        assertEquals("John", ((StringValue) result.get("name")).value());
        assertEquals(30, ((NumberValue) result.get("age")).asInt());
    }
    
    @Test
    void readNestedObject() {
        String json = "{\"person\": {\"name\": \"John\", \"address\": {\"city\": \"NYC\"}}}";
        DictValue result = reader.read(json);
        
        DictValue person = (DictValue) result.get("person");
        assertEquals("John", ((StringValue) person.get("name")).value());
        
        DictValue address = (DictValue) person.get("address");
        assertEquals("NYC", ((StringValue) address.get("city")).value());
    }
    
    @Test
    void readArray() {
        String json = "{\"items\": [1, 2, 3]}";
        DictValue result = reader.read(json);
        
        ListValue items = (ListValue) result.get("items");
        assertEquals(3, items.size());
        assertEquals(1, ((NumberValue) items.get(0)).asInt());
        assertEquals(2, ((NumberValue) items.get(1)).asInt());
        assertEquals(3, ((NumberValue) items.get(2)).asInt());
    }
    
    @Test
    void readBooleanAndNull() {
        String json = "{\"active\": true, \"deleted\": false, \"data\": null}";
        DictValue result = reader.read(json);
        
        assertTrue(((BoolValue) result.get("active")).value());
        assertFalse(((BoolValue) result.get("deleted")).value());
        assertInstanceOf(NullValue.class, result.get("data"));
    }
    
    @Test
    void readDecimalNumbers() {
        String json = "{\"price\": 19.99, \"quantity\": 5}";
        DictValue result = reader.read(json);
        
        assertEquals(19.99, ((NumberValue) result.get("price")).asDouble(), 0.001);
        assertEquals(5, ((NumberValue) result.get("quantity")).asInt());
    }
    
    @Test
    void readEmptyObject() {
        String json = "{}";
        DictValue result = reader.read(json);
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void readInvalidJsonThrowsException() {
        assertThrows(FormatException.class, () -> reader.read("{invalid json}"));
    }
    
    // ==================== Writer Tests ====================
    
    @Test
    void writeSimpleObject() {
        DictValue dict = DictValue.empty();
        dict.put("name", new StringValue("John"));
        dict.put("age", new NumberValue(30));
        
        String json = writer.write(dict);
        
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"John\""));
        assertTrue(json.contains("\"age\""));
        assertTrue(json.contains("30"));
    }
    
    @Test
    void writeNestedObject() {
        DictValue address = DictValue.empty();
        address.put("city", new StringValue("NYC"));
        
        DictValue person = DictValue.empty();
        person.put("name", new StringValue("John"));
        person.put("address", address);
        
        DictValue root = DictValue.empty();
        root.put("person", person);
        
        String json = writer.write(root);
        
        assertTrue(json.contains("\"person\""));
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"address\""));
        assertTrue(json.contains("\"city\""));
        assertTrue(json.contains("\"NYC\""));
    }
    
    @Test
    void writeArray() {
        ListValue items = ListValue.of(
            new NumberValue(1),
            new NumberValue(2),
            new NumberValue(3)
        );
        
        DictValue dict = DictValue.empty();
        dict.put("items", items);
        
        String json = writer.write(dict);
        
        assertTrue(json.contains("["));
        assertTrue(json.contains("1"));
        assertTrue(json.contains("2"));
        assertTrue(json.contains("3"));
        assertTrue(json.contains("]"));
    }
    
    @Test
    void writeBooleanAndNull() {
        DictValue dict = DictValue.empty();
        dict.put("active", BoolValue.TRUE);
        dict.put("deleted", BoolValue.FALSE);
        dict.put("data", NullValue.INSTANCE);
        
        String json = writer.write(dict);
        
        assertTrue(json.contains("true"));
        assertTrue(json.contains("false"));
        assertTrue(json.contains("null"));
    }
    
    @Test
    void compactOutput() {
        DictValue dict = DictValue.empty();
        dict.put("name", new StringValue("John"));
        
        String compactJson = new JsonWriter().compact().write(dict);
        
        assertFalse(compactJson.contains("\n") && compactJson.contains("  "));
    }
    
    // ==================== Round-Trip Tests ====================
    
    @Test
    void roundTripPreservesData() {
        String originalJson = "{\"name\":\"John\",\"age\":30,\"active\":true}";
        
        DictValue dict = reader.read(originalJson);
        String outputJson = new JsonWriter().compact().write(dict);
        DictValue roundTrip = reader.read(outputJson);
        
        assertEquals(
            ((StringValue) dict.get("name")).value(),
            ((StringValue) roundTrip.get("name")).value()
        );
        assertEquals(
            ((NumberValue) dict.get("age")).asInt(),
            ((NumberValue) roundTrip.get("age")).asInt()
        );
        assertEquals(
            ((BoolValue) dict.get("active")).value(),
            ((BoolValue) roundTrip.get("active")).value()
        );
    }
}
