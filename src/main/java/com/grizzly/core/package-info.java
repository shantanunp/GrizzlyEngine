/**
 * Core Grizzly Engine - Format-agnostic transformation engine.
 * 
 * <p>This package contains the core components of the Grizzly engine:
 * <ul>
 *   <li>{@link com.grizzly.core.GrizzlyEngine} - Entry point for compilation</li>
 *   <li>{@link com.grizzly.core.GrizzlyTemplate} - Compiled template for execution</li>
 * </ul>
 * 
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code lexer} - Tokenization of template code</li>
 *   <li>{@code parser} - AST generation from tokens</li>
 *   <li>{@code interpreter} - AST execution</li>
 *   <li>{@code types} - Value type hierarchy (DictValue, ListValue, etc.)</li>
 *   <li>{@code exception} - Exception types</li>
 *   <li>{@code logging} - Logging infrastructure</li>
 * </ul>
 * 
 * <h2>Architecture</h2>
 * <p>The core engine is completely format-agnostic. It operates on
 * {@link com.grizzly.core.types.DictValue} input/output and has no knowledge
 * of JSON, XML, or any other serialization format.
 * 
 * <p>Format handling is done by the {@code com.grizzly.format} package.
 * 
 * @see com.grizzly.format For format-specific handlers
 * @see com.grizzly.format.json.JsonTemplate For JSON convenience wrapper
 */
package com.grizzly.core;
