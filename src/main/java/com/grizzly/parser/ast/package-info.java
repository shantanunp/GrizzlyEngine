/**
 * <h1>AST Package - Abstract Syntax Tree Node Definitions</h1>
 * 
 * <p>This package contains all the node types that make up the Abstract Syntax Tree.
 * Each node type represents a different construct in the Grizzly language.
 * 
 * <h2>Node Hierarchy</h2>
 * 
 * <pre>{@code
 * Node
 *   ├── Statement (things that DO something)
 *   │     ├── FunctionDef      def name(params): body
 *   │     ├── Assignment       target = value
 *   │     ├── IfStatement      if cond: body elif: else:
 *   │     ├── ForLoop          for item in items: body
 *   │     ├── ReturnStatement  return value
 *   │     ├── BreakStatement   break
 *   │     └── ContinueStatement continue
 *   │
 *   └── Expression (things that PRODUCE a value)
 *         ├── Identifier       x, INPUT, OUTPUT
 *         ├── StringLiteral    "hello"
 *         ├── NumberLiteral    42, 3.14
 *         ├── BooleanLiteral   True, False
 *         ├── NullLiteral      None
 *         ├── ListLiteral      [1, 2, 3]
 *         ├── DictLiteral      {"key": "value"}
 *         ├── BinaryOp         a + b, x == y
 *         ├── AttrAccess       INPUT.name
 *         ├── DictAccess       OUTPUT["key"]
 *         ├── MethodCall       list.append(item)
 *         └── FunctionCallExpression  len(items)
 * }</pre>
 * 
 * <h2>Example AST</h2>
 * 
 * <pre>{@code
 * Code:
 * def transform(INPUT):
 *     OUTPUT = {}
 *     OUTPUT["name"] = INPUT.firstName
 *     return OUTPUT
 * 
 * AST:
 * Program
 *   └── FunctionDef("transform", ["INPUT"])
 *         ├── Assignment(Identifier("OUTPUT"), DictLiteral{})
 *         ├── Assignment(DictAccess(OUTPUT, "name"), AttrAccess(INPUT, "firstName"))
 *         └── ReturnStatement(Identifier("OUTPUT"))
 * }</pre>
 * 
 * @see com.grizzly.parser.GrizzlyParser Builds the AST from tokens
 * @see com.grizzly.interpreter.GrizzlyInterpreter Executes the AST
 */
package com.grizzly.parser.ast;
