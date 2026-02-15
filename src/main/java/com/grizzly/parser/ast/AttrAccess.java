package com.grizzly.parser.ast;

import java.util.Objects;

public class AttrAccess implements Expression {
    private final Expression object;
    private final String attr;
    
    public AttrAccess(Expression object, String attr) {
        this.object = object;
        this.attr = attr;
    }
    
    public Expression getObject() { return object; }
    public String getAttr() { return attr; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttrAccess that = (AttrAccess) o;
        return Objects.equals(object, that.object) &&
               Objects.equals(attr, that.attr);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(object, attr);
    }
    
    @Override
    public String toString() {
        return String.format("%s.%s", object, attr);
    }
}
