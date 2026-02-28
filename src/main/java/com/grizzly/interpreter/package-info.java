/**
 * <h1>Interpreter Package - Step 3: Execution</h1>
 * 
 * <p>The interpreter walks the AST (Abstract Syntax Tree) and executes each node.
 * It maintains variable state and produces the final output.
 * 
 * <h2>How Execution Works</h2>
 * 
 * <pre>{@code
 * AST Node          →  Interpreter Action
 * ─────────────────────────────────────────
 * Assignment        →  Evaluate value, store in variable
 * BinaryOp          →  Evaluate left & right, apply operator
 * IfStatement       →  Evaluate condition, execute correct branch
 * ForLoop           →  Iterate over collection, execute body
 * ReturnStatement   →  Evaluate value, return it
 * MethodCall        →  Find method, evaluate args, invoke
 * }</pre>
 * 
 * <h2>Execution Context</h2>
 * 
 * <p>Variables are stored in an {@link ExecutionContext}:
 * 
 * <pre>{@code
 * Code:                        Context:
 * ─────────────────────        ─────────────────────
 * def transform(INPUT):        INPUT = {"name": "John"}
 *     OUTPUT = {}              OUTPUT = {}
 *     x = 42                   x = 42
 *     OUTPUT["name"] = x       OUTPUT = {"name": 42}
 * }</pre>
 * 
 * <h2>Type-Safe Values</h2>
 * 
 * <p>All values use the {@link com.grizzly.types.Value} hierarchy:
 * 
 * <table border="1">
 *   <tr><th>Python Type</th><th>Java Type</th></tr>
 *   <tr><td>str</td><td>StringValue</td></tr>
 *   <tr><td>int/float</td><td>NumberValue</td></tr>
 *   <tr><td>bool</td><td>BoolValue</td></tr>
 *   <tr><td>list</td><td>ListValue</td></tr>
 *   <tr><td>dict</td><td>DictValue</td></tr>
 *   <tr><td>None</td><td>NullValue</td></tr>
 * </table>
 * 
 * <h2>Key Classes</h2>
 * 
 * <ul>
 *   <li>{@link com.grizzly.interpreter.GrizzlyInterpreter} - Main interpreter</li>
 *   <li>{@link com.grizzly.interpreter.ExecutionContext} - Variable storage</li>
 *   <li>{@link com.grizzly.interpreter.BuiltinRegistry} - Built-in functions</li>
 *   <li>{@link com.grizzly.interpreter.ValueUtils} - Utility methods</li>
 * </ul>
 * 
 * @see com.grizzly.interpreter.GrizzlyInterpreter Main interpreter class
 * @see com.grizzly.types Type-safe value classes
 * @see com.grizzly.parser The previous step: parsing
 */
package com.grizzly.interpreter;
