package com.grizzly.parser.ast;

import java.util.List;
import java.util.Map;

/**
 * AST node representing a dictionary literal: {} or {"key": value, ...}
 */
public record DictLiteral(List<Entry> entries) implements Expression {
    
    /**
     * A single key-value entry in a dict literal.
     */
    public record Entry(Expression key, Expression value) {}
    
    /**
     * Create an empty dict literal.
     */
    public static DictLiteral empty() {
        return new DictLiteral(List.of());
    }
    
    @Override
    public String toString() {
        if (entries.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(entries.get(i).key()).append(": ").append(entries.get(i).value());
        }
        return sb.append("}").toString();
    }
}
