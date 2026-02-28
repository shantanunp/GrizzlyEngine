/**
 * <h1>Parser Package - Step 2: AST Construction</h1>
 * 
 * <p>The parser takes tokens from the lexer and builds an Abstract Syntax Tree (AST).
 * This tree represents the grammatical structure of your code.
 * 
 * <h2>What is an AST?</h2>
 * 
 * <p>An AST is a tree where each node represents a part of your code:
 * 
 * <pre>{@code
 * Code: OUTPUT["name"] = INPUT.firstName
 * 
 * AST:
 *   Assignment
 *     ├── target: DictAccess
 *     │             ├── object: Identifier("OUTPUT")
 *     │             └── key: StringLiteral("name")
 *     └── value: AttrAccess
 *                   ├── object: Identifier("INPUT")
 *                   └── attribute: "firstName"
 * }</pre>
 * 
 * <h2>Node Types</h2>
 * 
 * <h3>Statements (actions):</h3>
 * <ul>
 *   <li>FunctionDef - Function definition</li>
 *   <li>Assignment - Variable assignment</li>
 *   <li>IfStatement - Conditional branching</li>
 *   <li>ForLoop - Iteration</li>
 *   <li>ReturnStatement - Return value</li>
 * </ul>
 * 
 * <h3>Expressions (values):</h3>
 * <ul>
 *   <li>Identifier - Variable reference</li>
 *   <li>StringLiteral, NumberLiteral - Literal values</li>
 *   <li>BinaryOp - Operations (a + b, x == y)</li>
 *   <li>AttrAccess, DictAccess - Property access</li>
 *   <li>MethodCall - Method invocation</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * // After tokenizing
 * GrizzlyParser parser = new GrizzlyParser(tokens);
 * Program program = parser.parse();
 * 
 * // Access functions
 * FunctionDef transform = program.findFunction("transform");
 * System.out.println("Parameters: " + transform.parameters());
 * }</pre>
 * 
 * @see com.grizzly.parser.GrizzlyParser Main parser class
 * @see com.grizzly.parser.ast AST node definitions
 * @see com.grizzly.lexer The previous step: tokenization
 * @see com.grizzly.interpreter The next step: execution
 */
package com.grizzly.core.parser;
