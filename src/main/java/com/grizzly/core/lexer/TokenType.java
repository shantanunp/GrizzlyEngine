package com.grizzly.core.lexer;

/**
 * Token types for Grizzly Python subset.
 *
 * <p>Reserved extensions (not standard Python): {@code SAFE_DOT} ({@code ?.}),
 * {@code SAFE_LBRACKET} ({@code ?[}). See docs/PYTHON_EXTENSIONS.md.
 */
public enum TokenType {
    // Keywords
    DEF,        // def
    LAMBDA,     // lambda
    IF,         // if
    ELIF,       // elif
    ELSE,       // else
    RETURN,     // return
    FOR,        // for
    IN,         // in
    MATCH,      // match (Python 3.10+ structural pattern matching)
    CASE,       // case
    BREAK,      // break
    CONTINUE,   // continue
    TRUE,       // True (Python style)
    FALSE,      // False (Python style)
    NONE,       // None
    IMPORT,     // import
    FROM,       // from (for future use)
    AND,        // and
    OR,         // or
    NOT,        // not
    
    // Literals
    IDENTIFIER, // variable names, function names
    STRING,     // "string literal"
    FSTRING,    // f"format string with {expr}"
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
    
    // Safe navigation operators
    SAFE_DOT,       // ?.
    SAFE_LBRACKET,  // ?[
    
    // Special
    NEWLINE,    // \n
    INDENT,     // indentation
    DEDENT,     // dedentation
    EOF,        // end of file
    COMMENT     // # comment
}
