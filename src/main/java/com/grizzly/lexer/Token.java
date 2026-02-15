package com.grizzly.lexer;

/**
 * Represents a single token in the Python template
 */
public class Token {
    private final TokenType type;
    private final String value;
    private final int line;
    private final int column;
    
    public Token(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }
    
    public Token(TokenType type, int line, int column) {
        this(type, null, line, column);
    }
    
    public TokenType getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    @Override
    public String toString() {
        if (value != null) {
            return String.format("%s(%s) at %d:%d", type, value, line, column);
        }
        return String.format("%s at %d:%d", type, line, column);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return line == token.line && 
               column == token.column && 
               type == token.type && 
               java.util.Objects.equals(value, token.value);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, value, line, column);
    }
}
