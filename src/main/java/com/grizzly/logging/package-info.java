/**
 * <h1>Logging Package - Debug and Trace Utilities</h1>
 * 
 * <p>This package provides logging utilities to help understand and debug
 * the Grizzly Engine compilation and execution pipeline.
 * 
 * <h2>Quick Start</h2>
 * 
 * <pre>{@code
 * // Enable debug logging
 * GrizzlyLogger.setLevel(GrizzlyLogger.LogLevel.DEBUG);
 * 
 * // Now compile and execute - you'll see detailed output
 * GrizzlyTemplate template = engine.compileFromString(code);
 * Map<String, Object> result = template.executeRaw(input);
 * }</pre>
 * 
 * <h2>Log Levels</h2>
 * 
 * <table border="1">
 *   <tr><th>Level</th><th>Use Case</th><th>Example Output</th></tr>
 *   <tr><td>TRACE</td><td>Very detailed</td><td>Every variable read/write</td></tr>
 *   <tr><td>DEBUG</td><td>Development</td><td>Tokens, AST nodes, function calls</td></tr>
 *   <tr><td>INFO</td><td>Normal</td><td>Compilation started/completed</td></tr>
 *   <tr><td>WARN</td><td>Warnings</td><td>Deprecated features</td></tr>
 *   <tr><td>ERROR</td><td>Errors only</td><td>Exceptions</td></tr>
 *   <tr><td>OFF</td><td>Production</td><td>(no output)</td></tr>
 * </table>
 * 
 * <h2>Example Output (DEBUG level)</h2>
 * 
 * <pre>{@code
 * [INFO ] [ENGINE     ] Starting compilation pipeline
 * [INFO ] [LEXER      ] Starting tokenization (123 chars)
 * [DEBUG] [LEXER      ] === TOKENS (25 total) ===
 * [DEBUG] [LEXER      ]   1. DEF at 1:1
 * [DEBUG] [LEXER      ]   2. IDENTIFIER("transform") at 1:5
 * ...
 * [INFO ] [PARSER     ] Parsing complete (1 functions)
 * [DEBUG] [PARSER     ] === AST ===
 * [DEBUG] [PARSER     ] Program
 * [DEBUG] [PARSER     ]   └── FunctionDef: transform(INPUT)
 * ...
 * [INFO ] [INTERPRETER] Execution complete in 5ms
 * }</pre>
 * 
 * <h2>Pretty-Printing</h2>
 * 
 * <p>The logger includes helpers for formatting tokens, AST, and values:
 * 
 * <pre>{@code
 * // Log all tokens
 * GrizzlyLogger.logTokens(tokens);
 * 
 * // Log AST tree
 * GrizzlyLogger.logAST(program);
 * 
 * // Format a value
 * String display = GrizzlyLogger.formatValue(someValue);
 * }</pre>
 * 
 * @see com.grizzly.logging.GrizzlyLogger Main logging class
 */
package com.grizzly.logging;
