/**
 * <h1>Lexer Package - Step 1: Tokenization</h1>
 * 
 * <p>The lexer (tokenizer) is the first step in the compilation pipeline.
 * It reads source code character-by-character and groups them into tokens.
 * 
 * <h2>What is Tokenization?</h2>
 * 
 * <p>Tokenization splits source code into meaningful units:
 * 
 * <pre>{@code
 * Source:  "def transform(INPUT):"
 * Tokens:  [DEF, IDENTIFIER("transform"), LPAREN, IDENTIFIER("INPUT"), RPAREN, COLON]
 * }</pre>
 * 
 * <h2>Token Types</h2>
 * 
 * <table border="1">
 *   <tr><th>Category</th><th>Examples</th><th>Token Type</th></tr>
 *   <tr><td>Keywords</td><td>def, if, for, return</td><td>DEF, IF, FOR, RETURN</td></tr>
 *   <tr><td>Identifiers</td><td>transform, OUTPUT, x</td><td>IDENTIFIER</td></tr>
 *   <tr><td>Literals</td><td>"hello", 42, True</td><td>STRING, NUMBER, TRUE</td></tr>
 *   <tr><td>Operators</td><td>+, -, ==, !=</td><td>PLUS, MINUS, EQ, NE</td></tr>
 *   <tr><td>Delimiters</td><td>( ) [ ] { } , :</td><td>LPAREN, RPAREN, etc.</td></tr>
 *   <tr><td>Structure</td><td>(indentation)</td><td>INDENT, DEDENT, NEWLINE</td></tr>
 * </table>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * GrizzlyLexer lexer = new GrizzlyLexer("x = 42");
 * List<Token> tokens = lexer.tokenize();
 * 
 * for (Token token : tokens) {
 *     System.out.println(token.type() + " = " + token.value());
 * }
 * // Output:
 * // IDENTIFIER = x
 * // ASSIGN = null
 * // NUMBER = 42
 * // EOF = null
 * }</pre>
 * 
 * <h2>Key Classes</h2>
 * 
 * <ul>
 *   <li>{@link com.grizzly.lexer.GrizzlyLexer} - The tokenizer</li>
 *   <li>{@link com.grizzly.lexer.Token} - A single token with type, value, position</li>
 *   <li>{@link com.grizzly.lexer.TokenType} - Enum of all token types</li>
 * </ul>
 * 
 * @see com.grizzly.lexer.GrizzlyLexer Main lexer class
 * @see com.grizzly.parser The next step: parsing tokens into AST
 */
package com.grizzly.core.lexer;
