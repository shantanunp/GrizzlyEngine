package com.grizzly.parser.ast;

import java.util.List;
import java.util.Objects;

public class FunctionDef implements ASTNode {
    private final String name;
    private final List<String> params;
    private final List<Statement> body;
    private final int lineNumber;
    
    public FunctionDef(String name, List<String> params, List<Statement> body, int lineNumber) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.lineNumber = lineNumber;
    }
    
    public String getName() { return name; }
    public List<String> getParams() { return params; }
    public List<Statement> getBody() { return body; }
    public int getLineNumber() { return lineNumber; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionDef that = (FunctionDef) o;
        return lineNumber == that.lineNumber &&
               Objects.equals(name, that.name) &&
               Objects.equals(params, that.params) &&
               Objects.equals(body, that.body);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, params, body, lineNumber);
    }
}
