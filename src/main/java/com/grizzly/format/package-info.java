/**
 * Format handling for data transformations.
 * 
 * <p>This package provides pluggable format support for the Grizzly engine,
 * enabling transformations between different data formats.
 * 
 * <h2>Currently Supported</h2>
 * <ul>
 *   <li>JSON (via {@code com.grizzly.format.json})</li>
 * </ul>
 * 
 * <h2>Future Extensions</h2>
 * <p>The architecture supports adding new formats (XML, YAML, CSV, etc.)
 * by implementing {@link FormatReader} and {@link FormatWriter}.
 * 
 * <h2>Architecture</h2>
 * <pre>{@code
 * ┌──────────────────────────────────────────────────────────────────┐
 * │                         FORMAT LAYER                             │
 * ├──────────────────────────────────────────────────────────────────┤
 * │                                                                  │
 * │  ┌──────────────────┐                 ┌──────────────────┐       │
 * │  │   JsonReader     │                 │   JsonWriter     │       │
 * │  │  (String → Dict) │                 │  (Dict → String) │       │
 * │  └────────┬─────────┘                 └────────▲─────────┘       │
 * │           │                                    │                 │
 * │           ▼                                    │                 │
 * │  ┌──────────────────┐    Core Engine    ┌──────┴─────────┐       │
 * │  │    DictValue     │──────────────────▶│   DictValue    │       │
 * │  │    (INPUT)       │                   │   (OUTPUT)     │       │
 * │  └──────────────────┘                   └────────────────┘       │
 * │                                                                  │
 * └──────────────────────────────────────────────────────────────────┘
 * }</pre>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Option 1: Use JsonTemplate (recommended)
 * JsonTemplate template = JsonTemplate.compile(templateCode);
 * String output = template.transform(jsonInput);
 * 
 * // Option 2: Use FormatRegistry directly
 * FormatRegistry registry = FormatRegistry.defaultRegistry();
 * DictValue input = registry.getReader("json").read(jsonString);
 * DictValue output = coreTemplate.execute(input);
 * String json = registry.getWriter("json").write(output);
 * }</pre>
 * 
 * <h2>Extending with Custom Formats</h2>
 * <pre>{@code
 * // Implement FormatReader and FormatWriter
 * public class YamlReader implements FormatReader { ... }
 * public class YamlWriter implements FormatWriter { ... }
 * 
 * // Register with the registry
 * registry.registerReader("yaml", new YamlReader());
 * registry.registerWriter("yaml", new YamlWriter());
 * }</pre>
 * 
 * @see com.grizzly.format.FormatReader Interface for reading formats
 * @see com.grizzly.format.FormatWriter Interface for writing formats
 * @see com.grizzly.format.FormatRegistry Central registry for formats
 * @see com.grizzly.format.json JSON format handlers
 */
package com.grizzly.format;
