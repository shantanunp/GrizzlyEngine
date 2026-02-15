package com.grizzly.parser.ast;

import java.util.Objects;

public class StringLiteral implements Expression {
    private final String value;
    
    public StringLiteral(String value) {
        this.value = value;
    }
    
    public String getValue() { return value; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringLiteral that = (StringLiteral) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
