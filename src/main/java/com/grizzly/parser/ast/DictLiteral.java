package com.grizzly.parser.ast;

public class DictLiteral implements Expression {
    @Override
    public String toString() {
        return "{}";
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof DictLiteral;
    }
    
    @Override
    public int hashCode() {
        return 0;
    }
}
