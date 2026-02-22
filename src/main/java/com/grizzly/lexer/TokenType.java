package com.grizzly.lexer;

/**
 * Token types for Grizzly Python subset
 */
public enum TokenType {
    // Keywords
    DEF,        // def
    IF,         // if
    ELSE,       // else
    RETURN,     // return
    FOR,        // for
    IN,         // in
    
    // Literals
    IDENTIFIER, // variable names, function names
    STRING,     // "string literal"
    NUMBER,     // 123, 45.67
    
    // Operators
    ASSIGN,     // =
    PLUS,       // +
    EQ,         // ==
    NE,         // !=
    LT,         // <
    GT,         // >
    LE,         // <=
    GE,         // >=
    
    // Delimiters
    LPAREN,     // (
    RPAREN,     // )
    LBRACE,     // {
    RBRACE,     // }
    LBRACKET,   // [
    RBRACKET,   // ]
    COMMA,      // ,
    DOT,        // .
    COLON,      // :
    
    // Special
    NEWLINE,    // \n
    INDENT,     // indentation
    DEDENT,     // dedentation
    EOF,        // end of file
    COMMENT     // # comment
}
