package com.grizzly.parser;

import com.grizzly.lexer.GrizzlyLexer;
import com.grizzly.lexer.Token;
import com.grizzly.parser.ast.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GrizzlyParserTest {
    
    @Test
    void shouldParseSimpleFunction() {
        String code = """
            def transform(INPUT):
                OUTPUT = {}
                return OUTPUT
            """;
        
        Program program = parse(code);
        
        assertThat(program.functions()).hasSize(1);
        
        FunctionDef func = program.functions().get(0);
        assertThat(func.name()).isEqualTo("transform");
        assertThat(func.params()).containsExactly("INPUT");
        assertThat(func.body()).hasSize(2); // Assignment + Return
    }
    
    @Test
    void shouldParseAssignment() {
        String code = """
            def transform(INPUT):
                OUTPUT["id"] = INPUT.customerId
                return OUTPUT
            """;
        
        Program program = parse(code);
        FunctionDef func = program.functions().get(0);
        
        Statement stmt = func.body().get(0);
        assertThat(stmt).isInstanceOf(Assignment.class);
        
        Assignment assignment = (Assignment) stmt;
        assertThat(assignment.target()).isInstanceOf(DictAccess.class);
        assertThat(assignment.value()).isInstanceOf(AttrAccess.class);
    }
    
    @Test
    void shouldParseNestedDictAccess() {
        String code = """
            def transform(INPUT):
                OUTPUT["customer"]["name"] = INPUT.firstName
                return OUTPUT
            """;
        
        Program program = parse(code);
        FunctionDef func = program.functions().get(0);
        Assignment assignment = (Assignment) func.body().get(0);
        
        // OUTPUT["customer"]["name"] is nested DictAccess
        DictAccess outer = (DictAccess) assignment.target();
        assertThat(outer.key()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) outer.key()).value()).isEqualTo("name");
        
        assertThat(outer.object()).isInstanceOf(DictAccess.class);
        DictAccess inner = (DictAccess) outer.object();
        assertThat(((StringLiteral) inner.key()).value()).isEqualTo("customer");
    }
    
    @Test
    void shouldParseNestedAttrAccess() {
        String code = """
            def transform(INPUT):
                OUTPUT["email"] = INPUT.personalInfo.email
                return OUTPUT
            """;
        
        Program program = parse(code);
        FunctionDef func = program.functions().get(0);
        Assignment assignment = (Assignment) func.body().get(0);
        
        // INPUT.personalInfo.email is nested AttrAccess
        AttrAccess outer = (AttrAccess) assignment.value();
        assertThat(outer.attr()).isEqualTo("email");
        
        assertThat(outer.object()).isInstanceOf(AttrAccess.class);
        AttrAccess inner = (AttrAccess) outer.object();
        assertThat(inner.attr()).isEqualTo("personalInfo");
    }
    
    @Test
    void shouldParseMultipleFunctions() {
        String code = """
            def transform(INPUT):
                OUTPUT = {}
                return OUTPUT
            
            def map_customer(INPUT, OUTPUT):
                OUTPUT["name"] = INPUT.firstName
            """;
        
        Program program = parse(code);
        
        assertThat(program.functions()).hasSize(2);
        assertThat(program.functions().get(0).name()).isEqualTo("transform");
        assertThat(program.functions().get(1).name()).isEqualTo("map_customer");
    }
    
    @Test
    void shouldParseIfStatement() {
        String code = """
            def transform(INPUT):
                if INPUT.type == "PREMIUM":
                    OUTPUT["level"] = "GOLD"
                else:
                    OUTPUT["level"] = "SILVER"
                return OUTPUT
            """;
        
        Program program = parse(code);
        FunctionDef func = program.functions().get(0);
        
        Statement stmt = func.body().get(0);
        assertThat(stmt).isInstanceOf(IfStatement.class);
        
        IfStatement ifStmt = (IfStatement) stmt;
        assertThat(ifStmt.condition()).isInstanceOf(BinaryOp.class);
        assertThat(ifStmt.thenBlock()).hasSize(1);
        assertThat(ifStmt.elseBlock()).hasSize(1);
    }
    
    // Helper method
    private Program parse(String code) {
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        GrizzlyParser parser = new GrizzlyParser(tokens);
        return parser.parse();
    }
}
