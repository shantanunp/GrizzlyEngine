package com.grizzly.lexer;

import com.grizzly.exception.GrizzlyParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lexer (Tokenizer) - Converts Python template text into tokens
 * 
 * Example:
 * Input:  "def transform(INPUT):"
 * Output: [DEF, IDENTIFIER("transform"), LPAREN, IDENTIFIER("INPUT"), RPAREN, COLON]
 * 
 * This is Step 1 of the compilation process:
 * Text → [Lexer] → Tokens → [Parser] → AST → [Interpreter] → Result
 */
public class GrizzlyLexer {
    
    // Python keywords we recognize
    private static final Map<String, TokenType> KEYWORDS = Map.of(
        "def", TokenType.DEF,
        "if", TokenType.IF,
        "else", TokenType.ELSE,
        "return", TokenType.RETURN
    );
    
    // Constants
    private static final int SPACES_PER_INDENT = 4;
    private static final char EOF_CHAR = '\0';
    
    // Input and state
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private int currentIndent = 0;
    
    public GrizzlyLexer(String source) {
        this.source = source;
    }
    
    /**
     * Main entry point - tokenize the entire source
     */
    public List<Token> tokenize() {
        while (!isAtEnd()) {
            tokenizeNext();
        }
        
        addFinalTokens();
        return tokens;
    }
    
    /**
     * Add DEDENT and EOF tokens at the end
     */
    private void addFinalTokens() {
        // Close any open indentation levels
        while (currentIndent > 0) {
            addToken(TokenType.DEDENT);
            currentIndent--;
        }
        
        addToken(TokenType.EOF);
    }
    
    /**
     * Process the next character(s) and create appropriate token
     */
    private void tokenizeNext() {
        char c = currentChar();
        
        // Try each token type in order
        if (isWhitespace(c)) {
            handleIndentation();
        } else if (c == '\n') {
            handleNewline();
        } else if (c == '#') {
            skipComment();
        } else if (isQuote(c)) {
            tokenizeString(c);
        } else if (Character.isDigit(c)) {
            tokenizeNumber();
        } else if (isIdentifierStart(c)) {
            tokenizeIdentifier();
        } else {
            tokenizeSymbol(c);
        }
    }
    
    // ========== Character Classification ==========
    
    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }
    
    private boolean isQuote(char c) {
        return c == '"' || c == '\'';
    }
    
    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }
    
    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    
    // ========== Token Creation Methods ==========
    
    /**
     * Handle Python indentation (spaces/tabs at line start)
     */
    private void handleIndentation() {
        // Only count indentation at start of line
        if (column != 1) {
            advance();
            return;
        }
        
        int spaces = countSpaces();
        int newIndentLevel = spaces / SPACES_PER_INDENT;
        
        // Emit INDENT tokens if we went deeper
        while (newIndentLevel > currentIndent) {
            addToken(TokenType.INDENT);
            currentIndent++;
        }
        
        // Emit DEDENT tokens if we went back
        while (newIndentLevel < currentIndent) {
            addToken(TokenType.DEDENT);
            currentIndent--;
        }
    }
    
    /**
     * Count spaces at current position (tab = 4 spaces)
     */
    private int countSpaces() {
        int spaces = 0;
        while (!isAtEnd() && isWhitespace(currentChar())) {
            spaces += (currentChar() == '\t') ? SPACES_PER_INDENT : 1;
            advance();
        }
        return spaces;
    }
    
    /**
     * Handle newline character
     */
    private void handleNewline() {
        addToken(TokenType.NEWLINE);
        advance();
        line++;
        column = 1;
    }
    
    /**
     * Skip comment (# to end of line)
     */
    private void skipComment() {
        while (!isAtEnd() && currentChar() != '\n') {
            advance();
        }
    }
    
    /**
     * Tokenize string literal ("hello" or 'world')
     */
    private void tokenizeString(char quote) {
        int startColumn = column;
        advance(); // Skip opening quote
        
        StringBuilder content = new StringBuilder();
        
        while (!isAtEnd() && currentChar() != quote) {
            if (currentChar() == '\\') {
                content.append(handleEscapeSequence());
            } else {
                content.append(currentChar());
                advance();
            }
        }
        
        if (isAtEnd()) {
            throw error("Unterminated string", startColumn);
        }
        
        advance(); // Skip closing quote
        addToken(TokenType.STRING, content.toString(), startColumn);
    }
    
    /**
     * Handle escape sequences in strings (\n, \t, \\, etc.)
     */
    private char handleEscapeSequence() {
        advance(); // Skip backslash
        if (isAtEnd()) return '\\';
        
        char escaped = currentChar();
        advance();
        
        return switch (escaped) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case '\\' -> '\\';
            case '"' -> '"';
            case '\'' -> '\'';
            default -> escaped;
        };
    }
    
    /**
     * Tokenize number (42 or 3.14)
     */
    private void tokenizeNumber() {
        int startColumn = column;
        StringBuilder number = new StringBuilder();
        
        while (!isAtEnd() && (Character.isDigit(currentChar()) || currentChar() == '.')) {
            number.append(currentChar());
            advance();
        }
        
        addToken(TokenType.NUMBER, number.toString(), startColumn);
    }
    
    /**
     * Tokenize identifier or keyword (transform, OUTPUT, def, if, etc.)
     */
    private void tokenizeIdentifier() {
        int startColumn = column;
        StringBuilder identifier = new StringBuilder();
        
        while (!isAtEnd() && isIdentifierPart(currentChar())) {
            identifier.append(currentChar());
            advance();
        }
        
        String text = identifier.toString();
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type, text, startColumn);
    }
    
    /**
     * Tokenize symbols and operators (, ), {, }, ==, !=, etc.)
     */
    private void tokenizeSymbol(char c) {
        switch (c) {
            case '(' -> addTokenAndAdvance(TokenType.LPAREN);
            case ')' -> addTokenAndAdvance(TokenType.RPAREN);
            case '{' -> addTokenAndAdvance(TokenType.LBRACE);
            case '}' -> addTokenAndAdvance(TokenType.RBRACE);
            case '[' -> addTokenAndAdvance(TokenType.LBRACKET);
            case ']' -> addTokenAndAdvance(TokenType.RBRACKET);
            case ',' -> addTokenAndAdvance(TokenType.COMMA);
            case '.' -> addTokenAndAdvance(TokenType.DOT);
            case ':' -> addTokenAndAdvance(TokenType.COLON);
            case '=' -> tokenizeEquals();
            case '!' -> tokenizeNotEquals();
            case '<' -> tokenizeLessThan();
            case '>' -> tokenizeGreaterThan();
            default -> throw error("Unexpected character '" + c + "'");
        }
    }
    
    /**
     * Tokenize = or ==
     */
    private void tokenizeEquals() {
        advance();
        if (currentChar() == '=') {
            addTokenAndAdvance(TokenType.EQ);
        } else {
            addToken(TokenType.ASSIGN);
        }
    }
    
    /**
     * Tokenize !=
     */
    private void tokenizeNotEquals() {
        advance();
        if (currentChar() == '=') {
            addTokenAndAdvance(TokenType.NE);
        } else {
            throw error("Expected '=' after '!'");
        }
    }
    
    /**
     * Tokenize < or <=
     */
    private void tokenizeLessThan() {
        advance();
        if (currentChar() == '=') {
            addTokenAndAdvance(TokenType.LE);
        } else {
            addToken(TokenType.LT);
        }
    }
    
    /**
     * Tokenize > or >=
     */
    private void tokenizeGreaterThan() {
        advance();
        if (currentChar() == '=') {
            addTokenAndAdvance(TokenType.GE);
        } else {
            addToken(TokenType.GT);
        }
    }
    
    // ========== Token Addition Helpers ==========
    
    private void addToken(TokenType type) {
        tokens.add(new Token(type, line, column));
    }
    
    private void addToken(TokenType type, String value, int startColumn) {
        tokens.add(new Token(type, value, line, startColumn));
    }
    
    private void addTokenAndAdvance(TokenType type) {
        addToken(type);
        advance();
    }
    
    // ========== Character Navigation ==========
    
    private char currentChar() {
        return isAtEnd() ? EOF_CHAR : source.charAt(position);
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
    
    // ========== Error Handling ==========
    
    private GrizzlyParseException error(String message) {
        return new GrizzlyParseException(message + " at " + line + ":" + column);
    }
    
    private GrizzlyParseException error(String message, int col) {
        return new GrizzlyParseException(message + " at " + line + ":" + col);
    }
}
