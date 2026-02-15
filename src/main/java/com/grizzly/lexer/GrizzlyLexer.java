package com.grizzly.lexer;

import com.grizzly.exception.GrizzlyParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tokenizes Python template into tokens
 */
public class GrizzlyLexer {
    
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    
    static {
        KEYWORDS.put("def", TokenType.DEF);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("return", TokenType.RETURN);
    }
    
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private int currentIndent = 0;
    
    public GrizzlyLexer(String source) {
        this.source = source;
    }
    
    public List<Token> tokenize() {
        while (!isAtEnd()) {
            tokenizeNext();
        }
        
        // Add final dedents
        while (currentIndent > 0) {
            tokens.add(new Token(TokenType.DEDENT, line, column));
            currentIndent--;
        }
        
        tokens.add(new Token(TokenType.EOF, line, column));
        return tokens;
    }
    
    private void tokenizeNext() {
        char c = peek();
        
        // Skip whitespace (but track indentation at line start)
        if (c == ' ' || c == '\t') {
            handleIndentation();
            return;
        }
        
        // Newline
        if (c == '\n') {
            tokens.add(new Token(TokenType.NEWLINE, line, column));
            advance();
            line++;
            column = 1;
            return;
        }
        
        // Comments
        if (c == '#') {
            skipComment();
            return;
        }
        
        // String literals
        if (c == '"' || c == '\'') {
            tokenizeString(c);
            return;
        }
        
        // Numbers
        if (Character.isDigit(c)) {
            tokenizeNumber();
            return;
        }
        
        // Identifiers and keywords
        if (Character.isLetter(c) || c == '_') {
            tokenizeIdentifier();
            return;
        }
        
        // Operators and delimiters
        switch (c) {
            case '(' -> addToken(TokenType.LPAREN);
            case ')' -> addToken(TokenType.RPAREN);
            case '{' -> addToken(TokenType.LBRACE);
            case '}' -> addToken(TokenType.RBRACE);
            case '[' -> addToken(TokenType.LBRACKET);
            case ']' -> addToken(TokenType.RBRACKET);
            case ',' -> addToken(TokenType.COMMA);
            case '.' -> addToken(TokenType.DOT);
            case ':' -> addToken(TokenType.COLON);
            case '=' -> {
                advance();
                if (peek() == '=') {
                    addToken(TokenType.EQ);
                } else {
                    position--;
                    column--;
                    addToken(TokenType.ASSIGN);
                }
            }
            case '!' -> {
                advance();
                if (peek() == '=') {
                    addToken(TokenType.NE);
                } else {
                    throw new GrizzlyParseException("Unexpected character '!' at " + line + ":" + column);
                }
            }
            case '<' -> {
                advance();
                if (peek() == '=') {
                    addToken(TokenType.LE);
                } else {
                    position--;
                    column--;
                    addToken(TokenType.LT);
                }
            }
            case '>' -> {
                advance();
                if (peek() == '=') {
                    addToken(TokenType.GE);
                } else {
                    position--;
                    column--;
                    addToken(TokenType.GT);
                }
            }
            default -> throw new GrizzlyParseException(
                "Unexpected character '" + c + "' at " + line + ":" + column);
        }
    }
    
    private void handleIndentation() {
        // Only count indentation at start of line
        if (column != 1) {
            advance();
            return;
        }
        
        int spaces = 0;
        while (!isAtEnd() && (peek() == ' ' || peek() == '\t')) {
            if (peek() == '\t') spaces += 4;
            else spaces++;
            advance();
        }
        
        int indent = spaces / 4;
        
        while (indent > currentIndent) {
            tokens.add(new Token(TokenType.INDENT, line, column));
            currentIndent++;
        }
        
        while (indent < currentIndent) {
            tokens.add(new Token(TokenType.DEDENT, line, column));
            currentIndent--;
        }
    }
    
    private void skipComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }
    
    private void tokenizeString(char quote) {
        advance(); // Skip opening quote
        int startColumn = column - 1;
        StringBuilder sb = new StringBuilder();
        
        // Handle triple-quoted strings (docstrings)
        if (peek() == quote && peekNext() == quote) {
            advance();
            advance();
            // Triple-quoted string
            while (!isAtEnd()) {
                if (peek() == quote && peekNext() == quote && peekAhead(2) == quote) {
                    advance();
                    advance();
                    advance();
                    break;
                }
                if (peek() == '\n') {
                    line++;
                    column = 0;
                }
                sb.append(peek());
                advance();
            }
        } else {
            // Single-quoted string
            while (!isAtEnd() && peek() != quote) {
                if (peek() == '\\') {
                    advance();
                    if (!isAtEnd()) {
                        char escaped = peek();
                        sb.append(switch (escaped) {
                            case 'n' -> '\n';
                            case 't' -> '\t';
                            case 'r' -> '\r';
                            case '\\' -> '\\';
                            case '"' -> '"';
                            case '\'' -> '\'';
                            default -> escaped;
                        });
                        advance();
                    }
                } else {
                    sb.append(peek());
                    advance();
                }
            }
            
            if (isAtEnd()) {
                throw new GrizzlyParseException("Unterminated string at " + line + ":" + startColumn);
            }
            
            advance(); // Skip closing quote
        }
        
        tokens.add(new Token(TokenType.STRING, sb.toString(), line, startColumn));
    }
    
    private void tokenizeNumber() {
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        
        while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) {
            sb.append(peek());
            advance();
        }
        
        tokens.add(new Token(TokenType.NUMBER, sb.toString(), line, startColumn));
    }
    
    private void tokenizeIdentifier() {
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(peek());
            advance();
        }
        
        String identifier = sb.toString();
        TokenType type = KEYWORDS.getOrDefault(identifier, TokenType.IDENTIFIER);
        tokens.add(new Token(type, identifier, line, startColumn));
    }
    
    private void addToken(TokenType type) {
        tokens.add(new Token(type, line, column));
        advance();
    }
    
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(position);
    }
    
    private char peekNext() {
        if (position + 1 >= source.length()) return '\0';
        return source.charAt(position + 1);
    }
    
    private char peekAhead(int offset) {
        if (position + offset >= source.length()) return '\0';
        return source.charAt(position + offset);
    }
    
    private void advance() {
        if (!isAtEnd()) {
            position++;
            column++;
        }
    }
    
    private boolean isAtEnd() {
        return position >= source.length();
    }
}
