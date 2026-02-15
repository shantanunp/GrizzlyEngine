package com.grizzly.parser.ast;

import java.util.Objects;

public class ReturnStatement implements Statement {
    private final Expression value;
    private final int lineNumber;
    
    public ReturnStatement(Expression value, int lineNumber) {
        this.value = value;
        this.lineNumber = lineNumber;
    }
    
    public Expression getValue() { return value; }
    
    @Override
    public int getLineNumber() { return lineNumber; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnStatement that = (ReturnStatement) o;
        return lineNumber == that.lineNumber &&
               Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, lineNumber);
    }
}
