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
            if (peek().getType() == TokenType.DEF) {
                functions.add(parseFunction());
                skipNewlines(); // Skip blank lines between functions
            } else if (peek().getType() == TokenType.NEWLINE || peek().getType() == TokenType.COMMENT) {
                advance();
            } else if (peek().getType() == TokenType.EOF) {
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
        int lineNumber = peek().getLine();
        
        // Consume 'def'
        expect(TokenType.DEF, "Expected 'def'");
        
        // Get function name
        String name = expect(TokenType.IDENTIFIER, "Expected function name").getValue();
        
        // Parse parameters
        expect(TokenType.LPAREN, "Expected '('");
        List<String> params = new ArrayList<>();
        
        if (peek().getType() != TokenType.RPAREN) {
            do {
                params.add(expect(TokenType.IDENTIFIER, "Expected parameter name").getValue());
                if (peek().getType() == TokenType.COMMA) {
                    advance();
                }
            } while (peek().getType() != TokenType.RPAREN);
        }
        
        expect(TokenType.RPAREN, "Expected ')'");
        expect(TokenType.COLON, "Expected ':'");
        skipNewlines();
        
        // Parse function body (just parse statements until we hit another def or EOF)
        List<Statement> body = parseBlock();
        
        return new FunctionDef(name, params, body, lineNumber);
    }
    
    /**
     * Parse an indented block of statements
     */
    private List<Statement> parseBlock() {
        List<Statement> statements = new ArrayList<>();
        
        while (!isAtEnd()) {
            TokenType type = peek().getType();
            
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
        int lineNumber = peek().getLine();
        
        // Return statement
        if (peek().getType() == TokenType.RETURN) {
            advance();
            Expression value = parseExpression();
            return new ReturnStatement(value, lineNumber);
        }
        
        // If statement
        if (peek().getType() == TokenType.IF) {
            return parseIfStatement();
        }
        
        // Check for function call (identifier followed by '(')
        if (peek().getType() == TokenType.IDENTIFIER) {
            Token identToken = peek();
            int savePos = position;
            advance();
            
            if (peek().getType() == TokenType.LPAREN) {
                // It's a function call
                String functionName = identToken.getValue();
                advance(); // Skip '('
                
                List<Expression> args = new ArrayList<>();
                if (peek().getType() != TokenType.RPAREN) {
                    do {
                        args.add(parseExpression());
                        if (peek().getType() == TokenType.COMMA) {
                            advance();
                        }
                    } while (peek().getType() != TokenType.RPAREN);
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
        if (peek().getType() == TokenType.ASSIGN) {
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
        int lineNumber = peek().getLine();
        
        expect(TokenType.IF, "Expected 'if'");
        Expression condition = parseComparison();
        expect(TokenType.COLON, "Expected ':'");
        skipNewlines();
        
        // Skip optional INDENT token
        if (peek().getType() == TokenType.INDENT) {
            advance();
        }
        
        List<Statement> thenBlock = parseIfBlock();
        
        List<Statement> elseBlock = null;
        if (peek().getType() == TokenType.ELSE) {
            advance();
            expect(TokenType.COLON, "Expected ':'");
            skipNewlines();
            
            // Skip optional INDENT token
            if (peek().getType() == TokenType.INDENT) {
                advance();
            }
            
            elseBlock = parseIfBlock();
        }
        
        return new IfStatement(condition, thenBlock, elseBlock, lineNumber);
    }
    
    /**
     * Parse if/else block (stops at ELSE, DEDENT, DEF, or EOF)
     */
    private List<Statement> parseIfBlock() {
        List<Statement> statements = new ArrayList<>();
        
        while (!isAtEnd()) {
            TokenType type = peek().getType();
            
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
        if (peek().getType() == TokenType.DEDENT) {
            advance();
        }
        
        return statements;
    }
    
    /**
     * Parse comparison (for if conditions)
     */
    private Expression parseComparison() {
        Expression left = parseExpression();
        
        TokenType type = peek().getType();
        if (type == TokenType.EQ || type == TokenType.NE || 
            type == TokenType.LT || type == TokenType.GT ||
            type == TokenType.LE || type == TokenType.GE) {
            
            String operator = peek().getValue() != null ? peek().getValue() : 
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
        if (token.getType() == TokenType.STRING) {
            advance();
            return new StringLiteral(token.getValue());
        }
        
        // Number literal
        if (token.getType() == TokenType.NUMBER) {
            advance();
            return new StringLiteral(token.getValue()); // Treat numbers as strings for now
        }
        
        // Dict literal: {}
        if (token.getType() == TokenType.LBRACE) {
            advance();
            expect(TokenType.RBRACE, "Expected '}'");
            return new DictLiteral();
        }
        
        // Identifier (variable name)
        if (token.getType() == TokenType.IDENTIFIER) {
            String name = token.getValue();
            advance();
            Expression expr = new Identifier(name);
            
            // Check for dict access or attribute access
            while (true) {
                if (peek().getType() == TokenType.LBRACKET) {
                    // Dict access: OUTPUT["key"]
                    advance();
                    Expression key = parseExpression();
                    expect(TokenType.RBRACKET, "Expected ']'");
                    expr = new DictAccess(expr, key);
                } else if (peek().getType() == TokenType.DOT) {
                    // Attribute access: INPUT.customerId
                    advance();
                    String attr = expect(TokenType.IDENTIFIER, "Expected attribute name").getValue();
                    expr = new AttrAccess(expr, attr);
                } else if (peek().getType() == TokenType.LPAREN) {
                    // Function call: func(args)
                    advance();
                    List<Expression> args = new ArrayList<>();
                    
                    if (peek().getType() != TokenType.RPAREN) {
                        do {
                            args.add(parseExpression());
                            if (peek().getType() == TokenType.COMMA) {
                                advance();
                            }
                        } while (peek().getType() != TokenType.RPAREN);
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
        
        throw new GrizzlyParseException("Unexpected token: " + token, token.getLine(), token.getColumn());
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
        return tokens.get(position).getType() == TokenType.EOF;
    }
    
    private Token expect(TokenType type, String message) {
        if (peek().getType() != type) {
            throw new GrizzlyParseException(
                message + ", got " + peek().getType(), 
                peek().getLine(), 
                peek().getColumn()
            );
        }
        return advance();
    }
    
    private void skipNewlines() {
        while (!isAtEnd() && peek().getType() == TokenType.NEWLINE) {
            advance();
        }
    }
}
