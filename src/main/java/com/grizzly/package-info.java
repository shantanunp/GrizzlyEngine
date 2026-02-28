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
 * <h2>Quick Start</h2>
 * 
 * <pre>{@code
 * // Create engine
 * GrizzlyEngine engine = new GrizzlyEngine();
 * 
 * // Write template
 * String template = """
 *     def transform(INPUT):
 *         OUTPUT = {}
 *         OUTPUT["fullName"] = INPUT["firstName"] + " " + INPUT["lastName"]
 *         return OUTPUT
 *     """;
 * 
 * // Compile and execute
 * GrizzlyTemplate compiled = engine.compileFromString(template);
 * Map<String, Object> input = Map.of("firstName", "John", "lastName", "Doe");
 * Map<String, Object> result = compiled.executeRaw(input);
 * 
 * System.out.println(result.get("fullName")); // "John Doe"
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
 *   <li>{@link com.grizzly} - Main engine and template classes</li>
 *   <li>{@link com.grizzly.lexer} - Tokenization (source → tokens)</li>
 *   <li>{@link com.grizzly.parser} - Parsing (tokens → AST)</li>
 *   <li>{@link com.grizzly.parser.ast} - AST node definitions</li>
 *   <li>{@link com.grizzly.interpreter} - Execution (AST → result)</li>
 *   <li>{@link com.grizzly.types} - Type-safe value hierarchy</li>
 *   <li>{@link com.grizzly.logging} - Debug logging utilities</li>
 * </ul>
 * 
 * @see com.grizzly.GrizzlyEngine Main entry point
 * @see com.grizzly.GrizzlyTemplate Compiled template
 * @see com.grizzly.logging.GrizzlyLogger Debug logging
 */
package com.grizzly;
