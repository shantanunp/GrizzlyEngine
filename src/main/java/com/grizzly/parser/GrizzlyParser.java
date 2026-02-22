package com.grizzly.parser;

import com.grizzly.exception.GrizzlyParseException;
import com.grizzly.lexer.Token;
import com.grizzly.lexer.TokenType;
import com.grizzly.parser.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses tokens into an Abstract Syntax Tree (AST)
 * 
 * Simple example:
 * 
 * Input tokens: [DEF, IDENTIFIER("transform"), LPAREN, IDENTIFIER("INPUT"), RPAREN, COLON, ...]
 * Output AST:   Program([FunctionDef("transform", ["INPUT"], [...])])
 */
public class GrizzlyParser {
    
    private final List<Token> tokens;
    private int position = 0;
    
    public GrizzlyParser(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    /**
     * Parse all tokens into a Program
     */
    public Program parse() {
        List<FunctionDef> functions = new ArrayList<>();
        
        // Skip initial newlines and comments
        skipNewlines();
        
        // Parse all functions
        while (!isAtEnd()) {
            if (peek().type() == TokenType.DEF) {
                functions.add(parseFunction());
                skipNewlines(); // Skip blank lines between functions
            } else if (peek().type() == TokenType.NEWLINE || peek().type() == TokenType.COMMENT) {
                advance();
            } else if (peek().type() == TokenType.EOF) {
                break;
            } else {
                advance(); // Skip unknown tokens for now
            }
        }
        
        if (functions.isEmpty()) {
            throw new GrizzlyParseException("No functions found in template");
        }
        
        return new Program(functions);
    }
    
    /**
     * Parse a function definition
     * Example: def transform(INPUT):
     */
    private FunctionDef parseFunction() {
        int lineNumber = peek().line();
        
        // Consume 'def'
        expect(TokenType.DEF, "Expected 'def'");
        
        // Get function name
        String name = expect(TokenType.IDENTIFIER, "Expected function name").value();
        
        // Parse parameters
        expect(TokenType.LPAREN, "Expected '('");
        List<String> params = new ArrayList<>();
        
        if (peek().type() != TokenType.RPAREN) {
            do {
                params.add(expect(TokenType.IDENTIFIER, "Expected parameter name").value());
                if (peek().type() == TokenType.COMMA) {
                    advance();
                }
            } while (peek().type() != TokenType.RPAREN);
        }
        
        expect(TokenType.RPAREN, "Expected ')'");
        expect(TokenType.COLON, "Expected ':' after function signature");
        skipNewlines();
        
        // Skip INDENT if present (flexible for multiple functions)
        if (peek().type() == TokenType.INDENT) {
            advance();
        }
        
        // Parse function body
        List<Statement> body = parseBlock();
        
        if (body.isEmpty()) {
            throw new GrizzlyParseException(
                "Function body cannot be empty at line " + lineNumber,
                lineNumber,
                1
            );
        }
        
        return new FunctionDef(name, params, body, lineNumber);
    }
    
    /**
     * Parse an indented block of statements
     */
    private List<Statement> parseBlock() {
        List<Statement> statements = new ArrayList<>();
        
        while (!isAtEnd()) {
            TokenType type = peek().type();
            
            // Stop at next function definition or EOF
            if (type == TokenType.DEF || type == TokenType.EOF) {
                break;
            }
            
            // Skip newlines and dedents
            if (type == TokenType.NEWLINE || type == TokenType.DEDENT || type == TokenType.INDENT) {
                advance();
                continue;
            }
            
            statements.add(parseStatement());
            skipNewlines();
        }
        
        return statements;
    }
    
    /**
     * Parse a single statement
     */
    private Statement parseStatement() {
        int lineNumber = peek().line();
        
        // Return statement
        if (peek().type() == TokenType.RETURN) {
            advance();
            Expression value = parseExpression();
            return new ReturnStatement(value, lineNumber);
        }
        
        // If statement
        if (peek().type() == TokenType.IF) {
            return parseIfStatement();
        }
        
        // For loop
        if (peek().type() == TokenType.FOR) {
            return parseForLoop();
        }
        
        // Check for function call (identifier followed by '(')
        if (peek().type() == TokenType.IDENTIFIER) {
            Token identToken = peek();
            int savePos = position;
            advance();
            
            if (peek().type() == TokenType.LPAREN) {
                // It's a function call
                String functionName = identToken.value();
                advance(); // Skip '('
                
                List<Expression> args = new ArrayList<>();
                if (peek().type() != TokenType.RPAREN) {
                    do {
                        args.add(parseExpression());
                        if (peek().type() == TokenType.COMMA) {
                            advance();
                        }
                    } while (peek().type() != TokenType.RPAREN);
                }
                
                expect(TokenType.RPAREN, "Expected ')'");
                return new FunctionCall(functionName, args, lineNumber);
            } else {
                // Not a function call, restore position
                position = savePos;
            }
        }
        
        // Try to parse as expression (might be assignment or method call)
        Expression expr = parseExpression();
        
        // Check if it's a method call expression (like list.append())
        if (expr instanceof MethodCall) {
            // Wrap it in an ExpressionStatement
            return new ExpressionStatement(expr, lineNumber);
        }
        
        // Check if it's an assignment
        if (peek().type() == TokenType.ASSIGN) {
            advance();
            Expression value = parseExpression();
            return new Assignment(expr, value, lineNumber);
        }
        
        throw new GrizzlyParseException("Expected statement at line " + lineNumber);
    }
    
    /**
     * Parse if statement
     */
    private IfStatement parseIfStatement() {
        int lineNumber = peek().line();
        
        expect(TokenType.IF, "Expected 'if'");
        Expression condition = parseComparison();
        expect(TokenType.COLON, "Expected ':' after if condition");
        skipNewlines();
        
        // Skip INDENT if present (flexible)
        if (peek().type() == TokenType.INDENT) {
            advance();
        }
        
        List<Statement> thenBlock = parseIfBlock();
        
        if (thenBlock.isEmpty()) {
            throw new GrizzlyParseException(
                "'if' block cannot be empty at line " + lineNumber,
                lineNumber,
                1
            );
        }
        
        List<Statement> elseBlock = null;
        if (peek().type() == TokenType.ELSE) {
            int elseLine = peek().line();
            advance();
            expect(TokenType.COLON, "Expected ':' after else");
            skipNewlines();
            
            // Skip INDENT if present (flexible)
            if (peek().type() == TokenType.INDENT) {
                advance();
            }
            
            elseBlock = parseIfBlock();
            
            if (elseBlock.isEmpty()) {
                throw new GrizzlyParseException(
                    "'else' block cannot be empty at line " + elseLine,
                    elseLine,
                    1
                );
            }
        }
        
        return new IfStatement(condition, thenBlock, elseBlock, lineNumber);
    }
    
    /**
     * Parse if/else block (stops at ELSE, DEDENT, DEF, or EOF)
     */
    private List<Statement> parseIfBlock() {
        List<Statement> statements = new ArrayList<>();
        
        while (!isAtEnd()) {
            TokenType type = peek().type();
            
            // Stop at else, dedent, next function, or EOF
            if (type == TokenType.ELSE || type == TokenType.DEDENT || 
                type == TokenType.DEF || type == TokenType.EOF) {
                break;
            }
            
            // Skip newlines and indents
            if (type == TokenType.NEWLINE || type == TokenType.INDENT) {
                advance();
                continue;
            }
            
            statements.add(parseStatement());
            skipNewlines();
        }
        
        // Consume trailing DEDENT if present
        if (peek().type() == TokenType.DEDENT) {
            advance();
        }
        
        return statements;
    }
    
    /**
     * Parse a for loop statement: {@code for item in items:}
     * 
     * <p>A for loop iterates over an iterable expression (typically a list) and executes
     * a block of statements for each element.
     * 
     * <p><b>Example 1 - Simple iteration:</b>
     * <pre>{@code
     * for customer in INPUT.customers:
     *     OUTPUT["names"].append(customer.name)
     * }</pre>
     * 
     * <p><b>Example 2 - With conditional:</b>
     * <pre>{@code
     * for person in INPUT.people:
     *     if person.age >= 18:
     *         OUTPUT["adults"].append(person.name)
     * }</pre>
     * 
     * <p><b>Token sequence expected:</b>
     * FOR → IDENTIFIER → IN → Expression → COLON → NEWLINE → INDENT → Body → DEDENT
     * 
     * @return ForLoop AST node containing variable name, iterable expression, and body statements
     * @throws GrizzlyParseException if syntax is invalid or loop body is empty
     */
    private ForLoop parseForLoop() {
        int lineNumber = peek().line();
        
        expect(TokenType.FOR, "Expected 'for'");
        
        // Get loop variable name
        String variable = expect(TokenType.IDENTIFIER, "Expected variable name").value();
        
        expect(TokenType.IN, "Expected 'in'");
        
        // Parse iterable expression
        Expression iterable = parseExpression();
        
        expect(TokenType.COLON, "Expected ':' after for statement");
        skipNewlines();
        
        // Skip INDENT if present
        if (peek().type() == TokenType.INDENT) {
            advance();
        }
        
        // Parse loop body
        List<Statement> body = parseForBlock();
        
        if (body.isEmpty()) {
            throw new GrizzlyParseException(
                "'for' loop body cannot be empty at line " + lineNumber,
                lineNumber,
                1
            );
        }
        
        return new ForLoop(variable, iterable, body, lineNumber);
    }
    
    /**
     * Parse the body of a for loop.
     * 
     * <p>A for loop body is a sequence of indented statements that execute for each
     * iteration. Unlike {@code parseIfBlock()}, this does NOT stop at IF tokens
     * because if statements are valid inside for loops.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * for customer in INPUT.customers:
     *     user = {}                    ← Assignment
     *     if customer.age >= 18:       ← If statement (NOT a block terminator!)
     *         user["status"] = "adult"
     *     OUTPUT["users"].append(user) ← Method call
     * ← DEDENT here - block ends
     * }</pre>
     * 
     * <p><b>Stopping conditions:</b>
     * DEDENT (end of indentation), FOR (next loop), DEF (new function), or EOF
     * 
     * @return List of statements in the for loop body
     */
    private List<Statement> parseForBlock() {
        List<Statement> statements = new ArrayList<>();
        
        while (!isAtEnd()) {
            TokenType type = peek().type();
            
            // Stop at dedent, next for/def, or EOF
            // NOTE: Don't stop at IF - it's a statement inside the loop!
            if (type == TokenType.DEDENT || type == TokenType.FOR || 
                type == TokenType.DEF || type == TokenType.EOF) {
                break;
            }
            
            // Skip newlines and indents
            if (type == TokenType.NEWLINE || type == TokenType.INDENT) {
                advance();
                continue;
            }
            
            statements.add(parseStatement());
            skipNewlines();
        }
        
        // Consume trailing DEDENT if present
        if (peek().type() == TokenType.DEDENT) {
            advance();
        }
        
        return statements;
    }
    
    /**
     * Parse an expression with proper operator precedence.
     * 
     * <p>Precedence (lowest to highest):
     * <ol>
     *   <li>Comparison: ==, !=, <, >, <=, >=</li>
     *   <li>Addition/Subtraction: +, -</li>
     *   <li>Multiplication/Division: *, /, //, %</li>
     *   <li>Power: **</li>
     * </ol>
     * 
     * <p><b>Examples:</b>
     * <pre>{@code
     * 2 + 3 * 4        → 2 + (3 * 4) = 14  (not 20!)
     * 5 ** 2 + 1       → (5 ** 2) + 1 = 26
     * 10 - 2 * 3       → 10 - (2 * 3) = 4
     * "a" + "b" + "c"  → (("a" + "b") + "c") = "abc"
     * }</pre>
     * 
     * @return Expression AST node with proper precedence
     */
    private Expression parseExpression() {
        return parseComparison();
    }
    
    private Expression parseComparison() {
        Expression left = parseAddition();
        
        while (true) {
            TokenType op = peek().type();
            if (op == TokenType.EQ || op == TokenType.NE || 
                op == TokenType.LT || op == TokenType.GT ||
                op == TokenType.LE || op == TokenType.GE) {
                advance();
                Expression right = parseAddition();
                left = new BinaryOp(left, tokenTypeToOp(op), right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private Expression parseAddition() {
        Expression left = parseMultiplication();
        
        while (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
            TokenType op = peek().type();
            advance();
            Expression right = parseMultiplication();
            left = new BinaryOp(left, tokenTypeToOp(op), right);
        }
        
        return left;
    }
    
    private Expression parseMultiplication() {
        Expression left = parsePower();
        
        while (true) {
            TokenType op = peek().type();
            if (op == TokenType.STAR || op == TokenType.SLASH || 
                op == TokenType.DOUBLESLASH || op == TokenType.PERCENT) {
                advance();
                Expression right = parsePower();
                left = new BinaryOp(left, tokenTypeToOp(op), right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private Expression parsePower() {
        Expression left = parsePrimary();
        
        // ** is right-associative: 2 ** 3 ** 2 = 2 ** (3 ** 2) = 512
        if (peek().type() == TokenType.DOUBLESTAR) {
            advance();
            Expression right = parsePower();  // Recursive for right-associativity
            return new BinaryOp(left, "**", right);
        }
        
        return left;
    }
    
    private String tokenTypeToOp(TokenType type) {
        return switch (type) {
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case DOUBLESLASH -> "//";
            case PERCENT -> "%";
            case DOUBLESTAR -> "**";
            case EQ -> "==";
            case NE -> "!=";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            default -> throw new GrizzlyParseException("Unknown operator: " + type);
        };
    }
    
    /**
     * Parse primary expressions (identifiers, literals, dict/attr access)
     */
    /**
     * Parse the most basic expressions and chain them together.
     * 
     * <p><b>What's a "primary" expression?</b> The simplest building blocks:
     * <ul>
     *   <li>Literals: {@code "hello"}, {@code 42}, {@code []}, {@code {}}</li>
     *   <li>Variables: {@code x}, {@code INPUT}, {@code OUTPUT}</li>
     * </ul>
     * 
     * <p><b>The magic: Chaining!</b> After getting a basic expression, we can chain more:
     * <pre>{@code
     * INPUT.user.address.city
     * ^^^^^  dot   dot    dot
     * Start  |     |      |
     *    Attribute chains keep going!
     * 
     * OUTPUT["users"][0].name
     * ^^^^^^   [  ]  [ ] dot
     * Start  Dict List Attr
     *        access access access
     * }</pre>
     * 
     * <p><b>Example 1 - Simple identifier:</b>
     * <pre>{@code
     * x
     * 
     * Parse: See IDENTIFIER "x"
     * Return: Identifier("x")
     * Done!
     * }</pre>
     * 
     * <p><b>Example 2 - Attribute access:</b>
     * <pre>{@code
     * INPUT.name
     * 
     * Parse:
     * 1. See IDENTIFIER "INPUT" → create Identifier("INPUT")
     * 2. See DOT → continue chaining
     * 3. See IDENTIFIER "name" → wrap in AttrAccess
     * 4. No more dots/brackets → done
     * 
     * Return: AttrAccess(Identifier("INPUT"), "name")
     * }</pre>
     * 
     * <p><b>Example 3 - Dict access:</b>
     * <pre>{@code
     * OUTPUT["key"]
     * 
     * Parse:
     * 1. See IDENTIFIER "OUTPUT" → Identifier("OUTPUT")
     * 2. See LBRACKET → continue chaining
     * 3. Parse "key" expression → StringLiteral("key")
     * 4. See RBRACKET → close bracket
     * 5. Wrap in DictAccess
     * 
     * Return: DictAccess(Identifier("OUTPUT"), StringLiteral("key"))
     * }</pre>
     * 
     * <p><b>Example 4 - Method call:</b>
     * <pre>{@code
     * items.append(value)
     * 
     * Parse:
     * 1. See IDENTIFIER "items" → Identifier("items")
     * 2. See DOT → continue chaining
     * 3. See IDENTIFIER "append"
     * 4. See LPAREN → this is a method call!
     * 5. Parse arguments: value
     * 6. See RPAREN → close
     * 7. Wrap in MethodCall
     * 
     * Return: MethodCall(Identifier("items"), "append", [Identifier("value")])
     * }</pre>
     * 
     * <p><b>Example 5 - Complex chaining:</b>
     * <pre>{@code
     * INPUT.users[0].name.upper()
     * 
     * Parse steps:
     * 1. INPUT → Identifier("INPUT")
     * 2. .users → AttrAccess(prev, "users")
     * 3. [0] → DictAccess(prev, NumberLiteral("0"))
     * 4. .name → AttrAccess(prev, "name")
     * 5. .upper() → MethodCall(prev, "upper", [])
     * 
     * Each step wraps the previous result!
     * }</pre>
     * 
     * <p><b>The loop:</b> Keep chaining while we see:
     * <ul>
     *   <li>{@code [} → Dict/list access</li>
     *   <li>{@code .} → Attribute or method</li>
     *   <li>{@code (} → Function call</li>
     * </ul>
     * 
     * @return Expression (could be Identifier, AttrAccess, DictAccess, MethodCall, etc.)
     */
    private Expression parsePrimary() {
        Token token = peek();
        
        // String literal
        if (token.type() == TokenType.STRING) {
            advance();
            return new StringLiteral(token.value());
        }
        
        // Number literal
        if (token.type() == TokenType.NUMBER) {
            advance();
            return new StringLiteral(token.value()); // Treat numbers as strings for now
        }
        
        // Dict literal: {}
        if (token.type() == TokenType.LBRACE) {
            advance();
            expect(TokenType.RBRACE, "Expected '}'");
            return new DictLiteral();
        }
        
        // List literal: [] or [1, 2, 3]
        if (token.type() == TokenType.LBRACKET) {
            return parseListLiteral();
        }
        
        // Identifier (variable name)
        if (token.type() == TokenType.IDENTIFIER) {
            String name = token.value();
            advance();
            Expression expr = new Identifier(name);
            
            // Check for dict access, list access, attribute access, or method call
            while (true) {
                if (peek().type() == TokenType.LBRACKET) {
                    // Could be dict access or list access: obj["key"] or list[0]
                    advance();
                    Expression key = parseExpression();
                    expect(TokenType.RBRACKET, "Expected ']'");
                    
                    // Use DictAccess for now (works for both dict and list)
                    expr = new DictAccess(expr, key);
                    
                } else if (peek().type() == TokenType.DOT) {
                    // Could be attribute access or method call
                    advance();
                    String attr = expect(TokenType.IDENTIFIER, "Expected attribute name").value();
                    
                    // Check if it's a method call
                    if (peek().type() == TokenType.LPAREN) {
                        advance(); // Skip '('
                        List<Expression> args = new ArrayList<>();
                        
                        if (peek().type() != TokenType.RPAREN) {
                            do {
                                args.add(parseExpression());
                                if (peek().type() == TokenType.COMMA) {
                                    advance();
                                }
                            } while (peek().type() != TokenType.RPAREN);
                        }
                        
                        expect(TokenType.RPAREN, "Expected ')'");
                        expr = new MethodCall(expr, attr, args);
                    } else {
                        // Regular attribute access
                        expr = new AttrAccess(expr, attr);
                    }
                    
                } else if (peek().type() == TokenType.LPAREN) {
                    // Function call expression: len(items), helper(x)
                    advance(); // Skip '('
                    List<Expression> args = new ArrayList<>();
                    
                    if (peek().type() != TokenType.RPAREN) {
                        do {
                            args.add(parseExpression());
                            if (peek().type() == TokenType.COMMA) {
                                advance();
                            }
                        } while (peek().type() != TokenType.RPAREN);
                    }
                    
                    expect(TokenType.RPAREN, "Expected ')'");
                    
                    // Return as FunctionCallExpression
                    return new FunctionCallExpression(name, args);
                } else {
                    break;
                }
            }
            
            return expr;
        }
        
        throw new GrizzlyParseException("Unexpected token: " + token, token.line(), token.column());
    }
    
    /**
     * Parse a list literal expression: {@code []} or {@code [1, 2, 3]}.
     * 
     * <p>List literals create new list objects with zero or more initial elements.
     * 
     * <p><b>Examples:</b>
     * <pre>{@code
     * items = []                           // Empty list
     * numbers = [1, 2, 3]                  // List with literals
     * ids = [INPUT.id1, INPUT.id2]         // List with expressions
     * mixed = [1, "hello", INPUT.value]    // Mixed types
     * }</pre>
     * 
     * <p><b>Token sequence:</b>
     * LBRACKET → [Expression → COMMA → Expression → ...] → RBRACKET
     * 
     * @return ListLiteral AST node containing list of element expressions
     * @throws GrizzlyParseException if syntax is invalid
     */
    private ListLiteral parseListLiteral() {
        expect(TokenType.LBRACKET, "Expected '['");
        
        List<Expression> elements = new ArrayList<>();
        
        if (peek().type() != TokenType.RBRACKET) {
            do {
                elements.add(parseExpression());
                if (peek().type() == TokenType.COMMA) {
                    advance();
                }
            } while (peek().type() != TokenType.RBRACKET);
        }
        
        expect(TokenType.RBRACKET, "Expected ']'");
        
        return new ListLiteral(elements);
    }
    
    // === Helper methods ===
    
    private Token peek() {
        if (position >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // Return EOF
        }
        return tokens.get(position);
    }
    
    private Token advance() {
        if (!isAtEnd()) position++;
        return tokens.get(position - 1);
    }
    
    private boolean isAtEnd() {
        if (position >= tokens.size()) return true;
        return tokens.get(position).type() == TokenType.EOF;
    }
    
    private Token expect(TokenType type, String message) {
        if (peek().type() != type) {
            throw new GrizzlyParseException(
                message + ", got " + peek().type(), 
                peek().line(), 
                peek().column()
            );
        }
        return advance();
    }
    
    private void skipNewlines() {
        while (!isAtEnd() && peek().type() == TokenType.NEWLINE) {
            advance();
        }
    }
}
