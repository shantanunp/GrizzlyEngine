package com.grizzly.core.logging;

import com.grizzly.core.lexer.Token;
import com.grizzly.core.parser.ast.*;
import com.grizzly.core.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

/**
 * Logging utility for Grizzly Engine.
 * 
 * <p>Uses SLF4J for logging, allowing clients to plug in their preferred
 * logging implementation (Logback, Log4j2, java.util.logging, etc.).
 * 
 * <h2>Client Setup</h2>
 * <p>Add a logging implementation to your project:
 * 
 * <pre>{@code
 * // Gradle - using Logback
 * implementation 'ch.qos.logback:logback-classic:1.4.14'
 * 
 * // Or using SLF4J Simple (for quick console output)
 * runtimeOnly 'org.slf4j:slf4j-simple:2.0.11'
 * }</pre>
 * 
 * <h2>Configuring Log Levels</h2>
 * <p>Configure via your logging framework (e.g., logback.xml):
 * 
 * <pre>{@code
 * <logger name="com.grizzly.core.logging" level="DEBUG"/>
 * }</pre>
 * 
 * <p>Or for SLF4J Simple, set system property:
 * <pre>{@code
 * -Dorg.slf4j.simpleLogger.log.com.grizzly.core.logging=debug
 * }</pre>
 * 
 * <h2>What Gets Logged</h2>
 * <ul>
 *   <li><b>DEBUG</b> - Tokens, AST structure, function calls</li>
 *   <li><b>TRACE</b> - Variable reads/writes, detailed execution flow</li>
 *   <li><b>INFO</b> - Compilation start/complete, execution timing</li>
 *   <li><b>WARN/ERROR</b> - Issues and exceptions</li>
 * </ul>
 */
public class GrizzlyLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(GrizzlyLogger.class);
    
    // ==================== Logging Methods ====================
    
    public static void trace(String component, String message) {
        if (logger.isTraceEnabled()) {
            logger.trace("[{}] {}", component, message);
        }
    }
    
    public static void trace(String component, Supplier<String> messageSupplier) {
        if (logger.isTraceEnabled()) {
            logger.trace("[{}] {}", component, messageSupplier.get());
        }
    }
    
    public static void debug(String component, String message) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] {}", component, message);
        }
    }
    
    public static void debug(String component, Supplier<String> messageSupplier) {
        if (logger.isDebugEnabled()) {
            logger.debug("[{}] {}", component, messageSupplier.get());
        }
    }
    
    public static void info(String component, String message) {
        logger.info("[{}] {}", component, message);
    }
    
    public static void warn(String component, String message) {
        logger.warn("[{}] {}", component, message);
    }
    
    public static void error(String component, String message) {
        logger.error("[{}] {}", component, message);
    }
    
    public static void error(String component, String message, Throwable throwable) {
        logger.error("[{}] {}", component, message, throwable);
    }
    
    // ==================== Token Logging ====================
    
    /**
     * Log all tokens with pretty formatting (DEBUG level).
     */
    public static void logTokens(List<Token> tokens) {
        if (!logger.isDebugEnabled()) return;
        
        debug("LEXER", "=== TOKENS (" + tokens.size() + " total) ===");
        int i = 1;
        for (Token token : tokens) {
            debug("LEXER", String.format("  %3d. %s", i++, formatToken(token)));
        }
        debug("LEXER", "=== END TOKENS ===");
    }
    
    public static String formatToken(Token token) {
        String valueStr = token.value() != null ? "(\"" + truncate(token.value(), 20) + "\")" : "";
        return String.format("%s%s at %d:%d", token.type(), valueStr, token.line(), token.column());
    }
    
    // ==================== AST Logging ====================
    
    /**
     * Log the AST with tree-style formatting (DEBUG level).
     */
    public static void logAST(Program program) {
        if (!logger.isDebugEnabled()) return;
        
        debug("PARSER", "=== AST ===");
        debug("PARSER", "Program");
        
        for (int i = 0; i < program.imports().size(); i++) {
            ImportStatement imp = program.imports().get(i);
            boolean isLast = i == program.imports().size() - 1 && program.functions().isEmpty();
            String prefix = isLast ? "  └── " : "  ├── ";
            debug("PARSER", prefix + "Import: " + imp.moduleName());
        }
        
        for (int i = 0; i < program.functions().size(); i++) {
            FunctionDef func = program.functions().get(i);
            boolean isLast = i == program.functions().size() - 1;
            logFunction(func, isLast);
        }
        
        debug("PARSER", "=== END AST ===");
    }
    
    private static void logFunction(FunctionDef func, boolean isLastFunc) {
        String prefix = isLastFunc ? "  └── " : "  ├── ";
        String childPrefix = isLastFunc ? "      " : "  │   ";
        
        String params = String.join(", ", func.params());
        debug("PARSER", prefix + "FunctionDef: " + func.name() + "(" + params + ")");
        
        for (int i = 0; i < func.body().size(); i++) {
            Statement stmt = func.body().get(i);
            boolean isLast = i == func.body().size() - 1;
            logStatement(stmt, childPrefix, isLast, 1);
        }
    }
    
    private static void logStatement(Statement stmt, String basePrefix, boolean isLast, int depth) {
        String connector = isLast ? "└── " : "├── ";
        String prefix = basePrefix + connector;
        String childPrefix = basePrefix + (isLast ? "    " : "│   ");
        
        switch (stmt) {
            case Assignment a -> 
                debug("PARSER", prefix + "Assignment: " + formatExpression(a.target()) + 
                    " = " + formatExpression(a.value()));
            case ReturnStatement r -> 
                debug("PARSER", prefix + "Return: " + formatExpression(r.value()));
            case IfStatement i -> {
                debug("PARSER", prefix + "If: " + formatExpression(i.condition()));
                for (int j = 0; j < i.thenBlock().size(); j++) {
                    logStatement(i.thenBlock().get(j), childPrefix, 
                        j == i.thenBlock().size() - 1 && i.elseBlock() == null, depth + 1);
                }
                if (i.elseBlock() != null) {
                    debug("PARSER", childPrefix + "Else:");
                    for (int j = 0; j < i.elseBlock().size(); j++) {
                        logStatement(i.elseBlock().get(j), childPrefix + "    ", 
                            j == i.elseBlock().size() - 1, depth + 1);
                    }
                }
            }
            case ForLoop f -> {
                debug("PARSER", prefix + "For: " + f.variable() + " in " + 
                    formatExpression(f.iterable()));
                for (int j = 0; j < f.body().size(); j++) {
                    logStatement(f.body().get(j), childPrefix, 
                        j == f.body().size() - 1, depth + 1);
                }
            }
            case FunctionCall fc -> 
                debug("PARSER", prefix + "FunctionCall: " + fc.functionName() + "(...)");
            case ExpressionStatement e -> 
                debug("PARSER", prefix + "Expression: " + formatExpression(e.expression()));
            default -> 
                debug("PARSER", prefix + stmt.getClass().getSimpleName());
        }
    }
    
    public static String formatExpression(Expression expr) {
        return switch (expr) {
            case Identifier id -> id.name();
            case StringLiteral s -> "\"" + truncate(s.value(), 15) + "\"";
            case NumberLiteral n -> String.valueOf(n.value());
            case BooleanLiteral b -> b.value() ? "True" : "False";
            case NullLiteral ignored -> "None";
            case DictLiteral d -> "{" + d.entries().size() + " entries}";
            case ListLiteral l -> "[" + l.elements().size() + " elements]";
            case AttrAccess a -> formatExpression(a.object()) + "." + a.attr();
            case DictAccess d -> formatExpression(d.object()) + "[" + formatExpression(d.key()) + "]";
            case MethodCall m -> formatExpression(m.object()) + "." + m.methodName() + "(...)";
            case BinaryOp b -> formatExpression(b.left()) + " " + b.operator() + " " + 
                formatExpression(b.right());
            case FunctionCallExpression f -> f.functionName() + "(...)";
            default -> expr.getClass().getSimpleName();
        };
    }
    
    // ==================== Interpreter Logging ====================
    
    public static void logExecution(Statement stmt, String action) {
        if (!logger.isTraceEnabled()) return;
        trace("INTERPRETER", action + ": " + stmt.getClass().getSimpleName() + 
            " at line " + stmt.lineNumber());
    }
    
    public static void logAssignment(String variable, Value value) {
        if (!logger.isTraceEnabled()) return;
        trace("INTERPRETER", "SET " + variable + " = " + formatValue(value));
    }
    
    public static void logVariableRead(String variable, Value value) {
        if (!logger.isTraceEnabled()) return;
        trace("INTERPRETER", "GET " + variable + " → " + formatValue(value));
    }
    
    public static void logFunctionCall(String name, List<Value> args) {
        if (!logger.isDebugEnabled()) return;
        debug("INTERPRETER", "CALL " + name + "(" + args.size() + " args)");
    }
    
    public static void logFunctionReturn(String name, Value result) {
        if (!logger.isDebugEnabled()) return;
        debug("INTERPRETER", "RETURN from " + name + " → " + formatValue(result));
    }
    
    public static String formatValue(Value value) {
        return switch (value) {
            case StringValue s -> "\"" + truncate(s.value(), 30) + "\"";
            case NumberValue n -> n.toString();
            case BoolValue b -> b.value() ? "True" : "False";
            case NullValue ignored -> "None";
            case ListValue l -> "list[" + l.size() + "]";
            case DictValue d -> "dict{" + d.size() + " keys}";
            default -> value.typeName() + "(...)";
        };
    }
    
    // ==================== Utility Methods ====================
    
    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
    
    public static void separator(String component) {
        if (!logger.isDebugEnabled()) return;
        debug(component, "─".repeat(50));
    }
}
