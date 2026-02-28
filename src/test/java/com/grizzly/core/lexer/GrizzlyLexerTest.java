package com.grizzly.core.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GrizzlyLexerTest {
    
    @Test
    void shouldTokenizeSimpleFunction() {
        String code = """
            def transform(INPUT):
                OUTPUT = {}
                return OUTPUT
            """;
        
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        
        assertThat(tokens).isNotEmpty();
        assertThat(tokens.get(0).type()).isEqualTo(TokenType.DEF);
        assertThat(tokens.stream().filter(t -> t.type() == TokenType.IDENTIFIER))
            .anyMatch(t -> "transform".equals(t.value()));
    }
    
    @Test
    void shouldTokenizeAssignment() {
        String code = "OUTPUT[\"field\"] = INPUT.value";
        
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        
        assertThat(tokens).extracting(Token::type)
            .contains(
                TokenType.IDENTIFIER,  // OUTPUT
                TokenType.LBRACKET,    // [
                TokenType.STRING,      // "field"
                TokenType.RBRACKET,    // ]
                TokenType.ASSIGN,      // =
                TokenType.IDENTIFIER,  // INPUT
                TokenType.DOT,         // .
                TokenType.IDENTIFIER   // value
            );
    }
    
    @Test
    void shouldTokenizeStringLiteral() {
        String code = "x = \"hello world\"";
        
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        
        assertThat(tokens.stream().filter(t -> t.type() == TokenType.STRING))
            .anyMatch(t -> "hello world".equals(t.value()));
    }
    
    @Test
    void shouldTokenizeNumbers() {
        String code = "age = 42";
        
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        
        assertThat(tokens.stream().filter(t -> t.type() == TokenType.NUMBER))
            .anyMatch(t -> "42".equals(t.value()));
    }
    
    @Test
    void shouldTokenizeComparisons() {
        String code = "if x >= 18 and y != 5";
        
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        
        assertThat(tokens).extracting(Token::type)
            .contains(TokenType.IF, TokenType.GE, TokenType.NE);
    }
    
    @Test
    void shouldHandleComments() {
        String code = """
            # This is a comment
            x = 5  # inline comment
            """;
        
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        
        // Should have tokens but comments are skipped
        assertThat(tokens).isNotEmpty();
        assertThat(tokens).extracting(Token::type)
            .contains(TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.NUMBER);
    }
}
