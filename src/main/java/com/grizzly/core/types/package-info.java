/**
 * <h1>Types Package - Type-Safe Value Hierarchy</h1>
 * 
 * <p>This package defines the type-safe value classes used throughout the interpreter.
 * Instead of using raw {@code Object} types, all runtime values are wrapped in
 * specific {@link Value} implementations.
 * 
 * <h2>Why Type-Safe Values?</h2>
 * 
 * <ul>
 *   <li><b>Compile-time safety</b>: Catch type errors early</li>
 *   <li><b>Better IDE support</b>: Autocomplete and documentation</li>
 *   <li><b>Clear semantics</b>: Each type has defined behavior</li>
 *   <li><b>Pattern matching</b>: Use Java 21 switch expressions</li>
 * </ul>
 * 
 * <h2>Value Hierarchy</h2>
 * 
 * <pre>{@code
 * Value (sealed interface)
 *   ├── StringValue    "hello world"
 *   ├── NumberValue    42, 3.14, 1000000L
 *   ├── BoolValue      True, False
 *   ├── NullValue      None
 *   ├── ListValue      [1, 2, 3]
 *   ├── DictValue      {"key": "value"}
 *   ├── DecimalValue   3.14159265358979...
 *   └── DateTimeValue  2024-01-15T10:30:00
 * }</pre>
 * 
 * <h2>Pattern Matching Example</h2>
 * 
 * <pre>{@code
 * Value result = evaluate(expression);
 * 
 * String display = switch (result) {
 *     case StringValue s -> "String: " + s.value();
 *     case NumberValue n -> "Number: " + n.asDouble();
 *     case BoolValue b   -> "Bool: " + b.value();
 *     case ListValue l   -> "List with " + l.size() + " items";
 *     case DictValue d   -> "Dict with " + d.size() + " keys";
 *     case NullValue _   -> "None";
 *     default            -> "Unknown: " + result.typeName();
 * };
 * }</pre>
 * 
 * <h2>Conversion to Java Types</h2>
 * 
 * <p>Use {@link com.grizzly.interpreter.ValueConverter} to convert between
 * Value types and standard Java types:
 * 
 * <pre>{@code
 * // Java Map → DictValue
 * Map<String, Object> javaMap = Map.of("name", "John");
 * DictValue dict = ValueConverter.fromJavaMap(javaMap);
 * 
 * // DictValue → Java Map
 * Map<String, Object> result = ValueConverter.toJavaMap(dict);
 * }</pre>
 * 
 * @see com.grizzly.core.types.Value Base interface for all values
 * @see com.grizzly.interpreter.ValueConverter Java ↔ Value conversion
 * @see com.grizzly.interpreter.ValueUtils Utility methods
 */
package com.grizzly.core.types;
