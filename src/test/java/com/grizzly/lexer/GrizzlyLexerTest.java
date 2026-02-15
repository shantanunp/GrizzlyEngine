package com.grizzly.lexer;

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
        assertThat(tokens.get(0).getType()).isEqualTo(TokenType.DEF);
        assertThat(tokens.stream().filter(t -> t.getType() == TokenType.IDENTIFIER))
            .anyMatch(t -> "transform".equals(t.getValue()));
    }
    
    @Test
    void shouldTokenizeAssignment() {
        String code = "OUTPUT[\"field\"] = INPUT.value";
        
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        
        assertThat(tokens).extracting(Token::getType)
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
        
        assertThat(tokens.stream().filter(t -> t.getType() == TokenType.STRING))
            .anyMatch(t -> "hello world".equals(t.getValue()));
    }
}
