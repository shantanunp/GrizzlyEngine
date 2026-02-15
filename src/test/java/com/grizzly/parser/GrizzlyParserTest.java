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
        
        assertThat(program.getFunctions()).hasSize(1);
        
        FunctionDef func = program.getFunctions().get(0);
        assertThat(func.getName()).isEqualTo("transform");
        assertThat(func.getParams()).containsExactly("INPUT");
        assertThat(func.getBody()).hasSize(2); // Assignment + Return
    }
    
    @Test
    void shouldParseAssignment() {
        String code = """
            def transform(INPUT):
                OUTPUT["id"] = INPUT.customerId
                return OUTPUT
            """;
        
        Program program = parse(code);
        FunctionDef func = program.getFunctions().get(0);
        
        Statement stmt = func.getBody().get(0);
        assertThat(stmt).isInstanceOf(Assignment.class);
        
        Assignment assignment = (Assignment) stmt;
        assertThat(assignment.getTarget()).isInstanceOf(DictAccess.class);
        assertThat(assignment.getValue()).isInstanceOf(AttrAccess.class);
    }
    
    @Test
    void shouldParseNestedDictAccess() {
        String code = """
            def transform(INPUT):
                OUTPUT["customer"]["name"] = INPUT.firstName
                return OUTPUT
            """;
        
        Program program = parse(code);
        FunctionDef func = program.getFunctions().get(0);
        Assignment assignment = (Assignment) func.getBody().get(0);
        
        // OUTPUT["customer"]["name"] is nested DictAccess
        DictAccess outer = (DictAccess) assignment.getTarget();
        assertThat(outer.getKey()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) outer.getKey()).getValue()).isEqualTo("name");
        
        assertThat(outer.getObject()).isInstanceOf(DictAccess.class);
        DictAccess inner = (DictAccess) outer.getObject();
        assertThat(((StringLiteral) inner.getKey()).getValue()).isEqualTo("customer");
    }
    
    @Test
    void shouldParseNestedAttrAccess() {
        String code = """
            def transform(INPUT):
                OUTPUT["email"] = INPUT.personalInfo.email
                return OUTPUT
            """;
        
        Program program = parse(code);
        FunctionDef func = program.getFunctions().get(0);
        Assignment assignment = (Assignment) func.getBody().get(0);
        
        // INPUT.personalInfo.email is nested AttrAccess
        AttrAccess outer = (AttrAccess) assignment.getValue();
        assertThat(outer.getAttr()).isEqualTo("email");
        
        assertThat(outer.getObject()).isInstanceOf(AttrAccess.class);
        AttrAccess inner = (AttrAccess) outer.getObject();
        assertThat(inner.getAttr()).isEqualTo("personalInfo");
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
        
        assertThat(program.getFunctions()).hasSize(2);
        assertThat(program.getFunctions().get(0).getName()).isEqualTo("transform");
        assertThat(program.getFunctions().get(1).getName()).isEqualTo("map_customer");
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
        FunctionDef func = program.getFunctions().get(0);
        
        Statement stmt = func.getBody().get(0);
        assertThat(stmt).isInstanceOf(IfStatement.class);
        
        IfStatement ifStmt = (IfStatement) stmt;
        assertThat(ifStmt.getCondition()).isInstanceOf(BinaryOp.class);
        assertThat(ifStmt.getThenBlock()).hasSize(1);
        assertThat(ifStmt.getElseBlock()).hasSize(1);
    }
    
    // Helper method
    private Program parse(String code) {
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        GrizzlyParser parser = new GrizzlyParser(tokens);
        return parser.parse();
    }
}
