package com.grizzly.parser.ast;

import java.util.List;

/**
 * Represents the entire Python template
 * Modern Java Record (JDK 16+, perfect for immutable data)
 */
public record Program(List<ImportStatement> imports, List<FunctionDef> functions) implements ASTNode {
    
    public FunctionDef findFunction(String name) {
        return functions.stream()
            .filter(f -> f.name().equals(name))
            .findFirst()
            .orElse(null);
    }
}
