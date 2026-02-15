package com.grizzly.parser.ast;

import java.util.List;
import java.util.Objects;

public class Program implements ASTNode {
    private final List<FunctionDef> functions;
    
    public Program(List<FunctionDef> functions) {
        this.functions = functions;
    }
    
    public List<FunctionDef> getFunctions() {
        return functions;
    }
    
    public FunctionDef findFunction(String name) {
        return functions.stream()
            .filter(f -> f.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Program program = (Program) o;
        return Objects.equals(functions, program.functions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(functions);
    }
}
