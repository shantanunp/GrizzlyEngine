/**
 * Format handling for multi-format transformations.
 * 
 * <p>This package provides pluggable format support for the Grizzly engine,
 * enabling transformations between different data formats:
 * 
 * <ul>
 *   <li>JSON ↔ JSON</li>
 *   <li>JSON → XML</li>
 *   <li>XML → JSON</li>
 *   <li>XML ↔ XML</li>
 *   <li>Future: YAML, CSV, etc.</li>
 * </ul>
 * 
 * <h2>Architecture</h2>
 * <pre>{@code
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │                         FORMAT LAYER                                     │
 * ├──────────────────────────────────────────────────────────────────────────┤
 * │                                                                          │
 * │  ┌──────────┐  ┌──────────┐           ┌──────────┐  ┌──────────┐        │
 * │  │   JSON   │  │   XML    │           │   JSON   │  │   XML    │        │
 * │  │  Input   │  │  Input   │           │  Output  │  │  Output  │        │
 * │  └────┬─────┘  └────┬─────┘           └────▲─────┘  └────▲─────┘        │
 * │       │             │                      │             │              │
 * │       ▼             ▼                      │             │              │
 * │  ┌─────────────────────────┐    ┌──────────────────────────────┐        │
 * │  │     FormatReader        │    │      FormatWriter            │        │
 * │  │  ┌─────────┐ ┌────────┐ │    │  ┌─────────┐ ┌────────┐     │        │
 * │  │  │JsonReader│ │XmlReader│ │    │  │JsonWriter│ │XmlWriter│     │        │
 * │  │  └────┬────┘ └───┬────┘ │    │  └────▲────┘ └───▲────┘     │        │
 * │  └───────┼──────────┼──────┘    └───────┼──────────┼──────────┘        │
 * │          │          │                   │          │                    │
 * │          ▼          ▼                   │          │                    │
 * │       ┌──────────────────┐         ┌────┴──────────┴────┐               │
 * │       │    DictValue     │────────▶│     DictValue      │               │
 * │       │   (INPUT)        │  Engine │    (OUTPUT)        │               │
 * │       └──────────────────┘         └────────────────────┘               │
 * │                                                                          │
 * └──────────────────────────────────────────────────────────────────────────┘
 * }</pre>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get the default registry (JSON + XML)
 * FormatRegistry registry = FormatRegistry.defaultRegistry();
 * 
 * // Read XML input
 * DictValue input = registry.getReader("xml").read(xmlString);
 * 
 * // Execute transformation
 * DictValue output = interpreter.executeTyped(input);
 * 
 * // Write as JSON
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
 * @see com.grizzly.format.xml XML format handlers
 */
package com.grizzly.format;
