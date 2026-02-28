/**
 * <h1>Grizzly Engine - Python-like Data Transformation for Java</h1>
 * 
 * <p>Grizzly Engine allows you to write data transformations in a Python-like
 * syntax and execute them in Java. Perfect for JSON-to-JSON mapping, data
 * normalization, and ETL pipelines.
 * 
 * <h2>Why Grizzly?</h2>
 * 
 * <ul>
 *   <li><b>Readable Templates</b>: Write transformations in Python-like syntax</li>
 *   <li><b>Hot Reloading</b>: Change templates without recompiling Java code</li>
 *   <li><b>Type Safe</b>: Full Java type safety at compile time</li>
 *   <li><b>Production Ready</b>: Built-in safeguards for loops, recursion, timeouts</li>
 * </ul>
 * 
 * <h2>Quick Start (JSON)</h2>
 * 
 * <pre>{@code
 * // Use JsonTemplate for JSON transformations
 * JsonTemplate template = JsonTemplate.compile("""
 *     def transform(INPUT):
 *         OUTPUT = {}
 *         OUTPUT["fullName"] = INPUT["firstName"] + " " + INPUT["lastName"]
 *         return OUTPUT
 *     """);
 * 
 * String result = template.transform("{\"firstName\": \"John\", \"lastName\": \"Doe\"}");
 * }</pre>
 * 
 * <h2>How It Works</h2>
 * 
 * <pre>{@code
 * Source Code → LEXER → Tokens → PARSER → AST → INTERPRETER → Result
 *                  │              │             │
 *              Characters     Grammar        Execute
 *              to Tokens      to Tree         Tree
 * }</pre>
 * 
 * <h2>Package Structure</h2>
 * 
 * <ul>
 *   <li>{@code com.grizzly} - Entry point (Main class)</li>
 *   <li>{@code com.grizzly.core} - Core engine (GrizzlyEngine, GrizzlyTemplate)</li>
 *   <li>{@code com.grizzly.core.lexer} - Tokenization (source → tokens)</li>
 *   <li>{@code com.grizzly.core.parser} - Parsing (tokens → AST)</li>
 *   <li>{@code com.grizzly.core.interpreter} - Execution (AST → result)</li>
 *   <li>{@code com.grizzly.core.types} - Type-safe value hierarchy</li>
 *   <li>{@code com.grizzly.format} - Format handling (pluggable readers/writers)</li>
 *   <li>{@code com.grizzly.format.json} - JSON support (JsonTemplate)</li>
 * </ul>
 * 
 * @see com.grizzly.core.GrizzlyEngine Core compilation engine
 * @see com.grizzly.core.GrizzlyTemplate Core format-agnostic template
 * @see com.grizzly.format.json.JsonTemplate JSON convenience wrapper
 */
package com.grizzly;
