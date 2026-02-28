package com.grizzly.lexer;

/**
 * Token types for Grizzly Python subset
 */
public enum TokenType {
    // Keywords
    DEF,        // def
    IF,         // if
    ELIF,       // elif
    ELSE,       // else
    RETURN,     // return
    FOR,        // for
    IN,         // in
    BREAK,      // break
    CONTINUE,   // continue
    TRUE,       // true
    FALSE,      // false
    
    // Literals
    IDENTIFIER, // variable names, function names
    STRING,     // "string literal"
    NUMBER,     // 123, 45.67
    
    // Operators
    ASSIGN,     // =
    PLUS,       // +
    MINUS,      // -
    STAR,       // *
    SLASH,      // /
    DOUBLESLASH,// //
    PERCENT,    // %
    DOUBLESTAR, // **
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
