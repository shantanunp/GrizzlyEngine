package com.grizzly.parser.ast;

import java.util.List;
import java.util.Objects;

public class IfStatement implements Statement {
    private final Expression condition;
    private final List<Statement> thenBlock;
    private final List<Statement> elseBlock;
    private final int lineNumber;
    
    public IfStatement(Expression condition, List<Statement> thenBlock, List<Statement> elseBlock, int lineNumber) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
        this.lineNumber = lineNumber;
    }
    
    public Expression getCondition() { return condition; }
    public List<Statement> getThenBlock() { return thenBlock; }
    public List<Statement> getElseBlock() { return elseBlock; }
    
    @Override
    public int getLineNumber() { return lineNumber; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IfStatement that = (IfStatement) o;
        return lineNumber == that.lineNumber &&
               Objects.equals(condition, that.condition) &&
               Objects.equals(thenBlock, that.thenBlock) &&
               Objects.equals(elseBlock, that.elseBlock);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(condition, thenBlock, elseBlock, lineNumber);
    }
}
