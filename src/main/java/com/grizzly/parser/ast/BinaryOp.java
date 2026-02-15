package com.grizzly.parser.ast;

import java.util.Objects;

public class BinaryOp implements Expression {
    private final Expression left;
    private final String operator;
    private final Expression right;
    
    public BinaryOp(Expression left, String operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
    
    public Expression getLeft() { return left; }
    public String getOperator() { return operator; }
    public Expression getRight() { return right; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryOp that = (BinaryOp) o;
        return Objects.equals(left, that.left) &&
               Objects.equals(operator, that.operator) &&
               Objects.equals(right, that.right);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(left, operator, right);
    }
}
