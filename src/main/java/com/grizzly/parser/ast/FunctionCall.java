package com.grizzly.parser.ast;

import java.util.List;
import java.util.Objects;

public class FunctionCall implements Statement {
    private final String functionName;
    private final List<Expression> args;
    private final int lineNumber;
    
    public FunctionCall(String functionName, List<Expression> args, int lineNumber) {
        this.functionName = functionName;
        this.args = args;
        this.lineNumber = lineNumber;
    }
    
    public String getFunctionName() { return functionName; }
    public List<Expression> getArgs() { return args; }
    
    @Override
    public int getLineNumber() { return lineNumber; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionCall that = (FunctionCall) o;
        return lineNumber == that.lineNumber &&
               Objects.equals(functionName, that.functionName) &&
               Objects.equals(args, that.args);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(functionName, args, lineNumber);
    }
}
