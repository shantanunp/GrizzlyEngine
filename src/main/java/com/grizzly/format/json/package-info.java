/**
 * JSON format support for Grizzly Engine.
 * 
 * <p>This package provides JSON reading and writing capabilities using Jackson.
 * 
 * <h2>Reading JSON</h2>
 * <pre>{@code
 * JsonReader reader = new JsonReader();
 * DictValue data = reader.read("{\"name\": \"John\", \"age\": 30}");
 * }</pre>
 * 
 * <h2>Writing JSON</h2>
 * <pre>{@code
 * JsonWriter writer = new JsonWriter();
 * String json = writer.write(dictValue);
 * 
 * // Compact output (no pretty-printing)
 * String compact = new JsonWriter().compact().write(dictValue);
 * }</pre>
 * 
 * @see com.grizzly.format.json.JsonReader For parsing JSON
 * @see com.grizzly.format.json.JsonWriter For generating JSON
 */
package com.grizzly.format.json;
