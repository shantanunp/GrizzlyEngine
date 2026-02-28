/**
 * JSON format handling for Grizzly Engine.
 * 
 * <p>This package provides JSON input/output support:
 * <ul>
 *   <li>{@link com.grizzly.format.json.JsonReader} - Parse JSON to DictValue</li>
 *   <li>{@link com.grizzly.format.json.JsonWriter} - Write DictValue to JSON</li>
 *   <li>{@link com.grizzly.format.json.JsonTemplate} - High-level JSON wrapper</li>
 * </ul>
 * 
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Compile and use directly
 * JsonTemplate template = JsonTemplate.compile(templateCode);
 * String output = template.transform(jsonInput);
 * }</pre>
 * 
 * @see com.grizzly.format.json.JsonTemplate Main entry point for JSON transformations
 * @see com.grizzly.core.GrizzlyTemplate Core format-agnostic template
 */
package com.grizzly.format.json;
