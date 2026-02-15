package com.grizzly.parser.ast;

import java.util.Objects;

public class DictAccess implements Expression {
    private final Expression object;
    private final Expression key;
    
    public DictAccess(Expression object, Expression key) {
        this.object = object;
        this.key = key;
    }
    
    public Expression getObject() { return object; }
    public Expression getKey() { return key; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DictAccess that = (DictAccess) o;
        return Objects.equals(object, that.object) &&
               Objects.equals(key, that.key);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(object, key);
    }
    
    @Override
    public String toString() {
        return String.format("%s[%s]", object, key);
    }
}
