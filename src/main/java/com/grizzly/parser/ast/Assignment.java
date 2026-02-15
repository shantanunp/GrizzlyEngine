package com.grizzly.parser.ast;

import java.util.Objects;

public class Assignment implements Statement {
    private final Expression target;
    private final Expression value;
    private final int lineNumber;
    
    public Assignment(Expression target, Expression value, int lineNumber) {
        this.target = target;
        this.value = value;
        this.lineNumber = lineNumber;
    }
    
    public Expression getTarget() { return target; }
    public Expression getValue() { return value; }
    
    @Override
    public int getLineNumber() { return lineNumber; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return lineNumber == that.lineNumber &&
               Objects.equals(target, that.target) &&
               Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(target, value, lineNumber);
    }
}
