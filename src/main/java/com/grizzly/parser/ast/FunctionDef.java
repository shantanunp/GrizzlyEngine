package com.grizzly.parser.ast;
import java.util.List;
public record FunctionDef(String name, List<String> params, List<Statement> body, int lineNumber) implements ASTNode {}
