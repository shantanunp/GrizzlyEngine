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
    /**
     * Parse an indented block of statements (function body).
     *
     * <p>A block is a sequence of statements at the same indentation level.
     * The block ends when we encounter:
     * <ul>
     *   <li>DEDENT token - returning to outer indentation level</li>
     *   <li>DEF token - start of a new function definition</li>
     *   <li>EOF token - end of file</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * def transform(INPUT):
     *     OUTPUT = {}        ← Block starts (after INDENT)
     *     x = 1             ← Part of block
     *     return OUTPUT     ← Part of block
     *                       ← DEDENT here - block ends
     * def helper():         ← New function (DEF)
     * }</pre>
     *
     * @return List of statements in the block
     */
    private List<Statement> parseBlock() {
        List<Statement> statements = new ArrayList<>();

        while (!isAtEnd()) {
            TokenType type = peek().type();

            // Stop at end of block (DEDENT), next function (DEF), or end of file
            if (type == TokenType.DEDENT || type == TokenType.DEF || type == TokenType.EOF) {
                break;
            }

            // Skip newlines and extra indents
            if (type == TokenType.NEWLINE || type == TokenType.INDENT) {
                advance();
                continue;
            }

            statements.add(parseStatement());
            skipNewlines();
        }

        // Consume the DEDENT token if present (clean up for next block)
        if (peek().type() == TokenType.DEDENT) {
            advance();
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
        
        // Assignment
        Expression expr = parseExpression();
        
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
     * Parse comparison (for if conditions)
     */
    private Expression parseComparison() {
        Expression left = parseExpression();
        
        TokenType type = peek().type();
        if (type == TokenType.EQ || type == TokenType.NE || 
            type == TokenType.LT || type == TokenType.GT ||
            type == TokenType.LE || type == TokenType.GE) {
            
            String operator = peek().value() != null ? peek().value() : 
                switch (type) {
                    case EQ -> "==";
                    case NE -> "!=";
                    case LT -> "<";
                    case GT -> ">";
                    case LE -> "<=";
                    case GE -> ">=";
                    default -> "==";
                };
            advance();
            Expression right = parseExpression();
            return new BinaryOp(left, operator, right);
        }
        
        return left;
    }
    
    /**
     * Parse an expression
     */
    private Expression parseExpression() {
        return parsePrimary();
    }
    
    /**
     * Parse primary expressions (identifiers, literals, dict/attr access)
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
        
        // Identifier (variable name)
        if (token.type() == TokenType.IDENTIFIER) {
            String name = token.value();
            advance();
            Expression expr = new Identifier(name);
            
            // Check for dict access or attribute access
            while (true) {
                if (peek().type() == TokenType.LBRACKET) {
                    // Dict access: OUTPUT["key"]
                    advance();
                    Expression key = parseExpression();
                    expect(TokenType.RBRACKET, "Expected ']'");
                    expr = new DictAccess(expr, key);
                } else if (peek().type() == TokenType.DOT) {
                    // Attribute access: INPUT.customerId
                    advance();
                    String attr = expect(TokenType.IDENTIFIER, "Expected attribute name").value();
                    expr = new AttrAccess(expr, attr);
                } else if (peek().type() == TokenType.LPAREN) {
                    // Function call: func(args)
                    advance();
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
                    
                    // Return as a FunctionCall statement wrapped in an expression context
                    // For now, we'll handle this differently
                    // Actually, we need to create a FunctionCall node, but it's a Statement not Expression
                    // Let's handle this case in parseStatement instead
                    break;
                } else {
                    break;
                }
            }
            
            return expr;
        }
        
        throw new GrizzlyParseException("Unexpected token: " + token, token.line(), token.column());
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
