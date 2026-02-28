package com.grizzly.core.logging;

import com.grizzly.core.lexer.Token;
import com.grizzly.core.parser.ast.*;
import com.grizzly.core.types.*;

import java.util.List;
import java.util.function.Supplier;

/**
 * Logging utility for Grizzly Engine.
 * 
 * <p>Provides configurable logging levels and pretty-printing for:
 * <ul>
 *   <li><b>Tokens</b> - Output from the Lexer</li>
 *   <li><b>AST Nodes</b> - Output from the Parser</li>
 *   <li><b>Values</b> - Runtime values during interpretation</li>
 *   <li><b>Execution Trace</b> - Step-by-step execution flow</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Enable debug logging
 * GrizzlyLogger.setLevel(LogLevel.DEBUG);
 * 
 * // Log messages
 * GrizzlyLogger.debug("LEXER", "Starting tokenization...");
 * GrizzlyLogger.info("PARSER", "Parsed function: transform");
 * 
 * // Pretty-print tokens
 * GrizzlyLogger.logTokens(tokens);
 * 
 * // Pretty-print AST
 * GrizzlyLogger.logAST(program);
 * }</pre>
 * 
 * <h2>Log Levels (from most to least verbose):</h2>
 * <ul>
 *   <li>{@code TRACE} - Very detailed execution steps</li>
 *   <li>{@code DEBUG} - Development/debugging information</li>
 *   <li>{@code INFO} - General operational information</li>
 *   <li>{@code WARN} - Warning messages</li>
 *   <li>{@code ERROR} - Error messages only</li>
 *   <li>{@code OFF} - No logging</li>
 * </ul>
 * 
 * <h2>Understanding the Pipeline with Logging:</h2>
 * <pre>{@code
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Source Code                                                   │
 * │  ────────────                                                  │
 * │  def transform(INPUT):                                         │
 * │      OUTPUT = {}                                               │
 * │      OUTPUT["name"] = INPUT.firstName                          │
 * │      return OUTPUT                                             │
 * └────────────────────────────┬────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  LEXER (Tokenizer)                                             │
 * │  ─────────────────                                             │
 * │  Reads characters one-by-one and groups them into tokens.      │
 * │                                                                │
 * │  Example: "def transform" → [DEF, IDENTIFIER("transform")]     │
 * │                                                                │
 * │  Enable logging to see: GrizzlyLogger.setLevel(LogLevel.DEBUG) │
 * └────────────────────────────┬────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  TOKENS (List<Token>)                                          │
 * │  ────────────────────                                          │
 * │  [DEF, IDENTIFIER("transform"), LPAREN, IDENTIFIER("INPUT"),   │
 * │   RPAREN, COLON, NEWLINE, INDENT, ...]                         │
 * └────────────────────────────┬────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  PARSER                                                        │
 * │  ──────                                                        │
 * │  Reads tokens and builds a tree structure (AST).               │
 * │  Checks grammar: "def must be followed by identifier, then ("  │
 * │                                                                │
 * │  Example: DEF + IDENTIFIER → FunctionDef node                  │
 * └────────────────────────────┬────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  AST (Abstract Syntax Tree)                                    │
 * │  ──────────────────────────                                    │
 * │  Program                                                       │
 * │    └── FunctionDef("transform", ["INPUT"])                     │
 * │          └── body:                                             │
 * │              ├── Assignment(OUTPUT = {})                       │
 * │              ├── Assignment(OUTPUT["name"] = INPUT.firstName)  │
 * │              └── ReturnStatement(OUTPUT)                       │
 * └────────────────────────────┬────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  INTERPRETER                                                   │
 * │  ───────────                                                   │
 * │  Walks the AST and executes each node.                         │
 * │  Maintains variables in ExecutionContext.                      │
 * │                                                                │
 * │  Steps:                                                        │
 * │  1. Visit FunctionDef → create context, bind INPUT             │
 * │  2. Visit Assignment → evaluate {}, store in OUTPUT            │
 * │  3. Visit Assignment → evaluate INPUT.firstName, store result  │
 * │  4. Visit ReturnStatement → return OUTPUT value                │
 * └────────────────────────────┬────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  RESULT (Map<String, Object>)                                  │
 * │  ────────────────────────────                                  │
 * │  {"name": "John"}                                              │
 * └─────────────────────────────────────────────────────────────────┘
 * }</pre>
 */
public class GrizzlyLogger {
    
    /**
     * Logging levels from most verbose to least verbose.
     */
    public enum LogLevel {
        /** Very detailed execution steps (variable reads/writes, every operation) */
        TRACE(0),
        /** Development/debugging info (AST nodes, token streams) */
        DEBUG(1),
        /** General operational info (compilation started, execution completed) */
        INFO(2),
        /** Warning messages (deprecated features, potential issues) */
        WARN(3),
        /** Error messages only */
        ERROR(4),
        /** No logging at all */
        OFF(5);
        
        private final int priority;
        
        LogLevel(int priority) {
            this.priority = priority;
        }
        
        boolean isEnabled(LogLevel threshold) {
            return this.priority >= threshold.priority;
        }
    }
    
    // Current log level (default: OFF for production)
    private static LogLevel currentLevel = LogLevel.OFF;
    
    // Output destination (can be redirected)
    private static java.io.PrintStream output = System.out;
    
    // ==================== Configuration ====================
    
    /**
     * Set the logging level.
     * 
     * <pre>{@code
     * // Enable all debug messages
     * GrizzlyLogger.setLevel(LogLevel.DEBUG);
     * 
     * // Only warnings and errors
     * GrizzlyLogger.setLevel(LogLevel.WARN);
     * 
     * // Disable logging (production)
     * GrizzlyLogger.setLevel(LogLevel.OFF);
     * }</pre>
     */
    public static void setLevel(LogLevel level) {
        currentLevel = level;
    }
    
    /**
     * Get the current logging level.
     */
    public static LogLevel getLevel() {
        return currentLevel;
    }
    
    /**
     * Check if a specific level is enabled.
     */
    public static boolean isEnabled(LogLevel level) {
        return level.isEnabled(currentLevel);
    }
    
    /**
     * Redirect output to a different stream (useful for testing).
     */
    public static void setOutput(java.io.PrintStream stream) {
        output = stream;
    }
    
    // ==================== Logging Methods ====================
    
    /**
     * Log a TRACE level message.
     */
    public static void trace(String component, String message) {
        log(LogLevel.TRACE, component, message);
    }
    
    /**
     * Log a TRACE level message (lazy evaluation).
     */
    public static void trace(String component, Supplier<String> messageSupplier) {
        if (isEnabled(LogLevel.TRACE)) {
            log(LogLevel.TRACE, component, messageSupplier.get());
        }
    }
    
    /**
     * Log a DEBUG level message.
     */
    public static void debug(String component, String message) {
        log(LogLevel.DEBUG, component, message);
    }
    
    /**
     * Log a DEBUG level message (lazy evaluation).
     */
    public static void debug(String component, Supplier<String> messageSupplier) {
        if (isEnabled(LogLevel.DEBUG)) {
            log(LogLevel.DEBUG, component, messageSupplier.get());
        }
    }
    
    /**
     * Log an INFO level message.
     */
    public static void info(String component, String message) {
        log(LogLevel.INFO, component, message);
    }
    
    /**
     * Log a WARN level message.
     */
    public static void warn(String component, String message) {
        log(LogLevel.WARN, component, message);
    }
    
    /**
     * Log an ERROR level message.
     */
    public static void error(String component, String message) {
        log(LogLevel.ERROR, component, message);
    }
    
    /**
     * Log an ERROR level message with exception.
     */
    public static void error(String component, String message, Throwable throwable) {
        log(LogLevel.ERROR, component, message + ": " + throwable.getMessage());
    }
    
    private static void log(LogLevel level, String component, String message) {
        if (level.isEnabled(currentLevel)) {
            String prefix = String.format("[%-5s] [%-11s] ", level, component);
            output.println(prefix + message);
        }
    }
    
    // ==================== Token Logging ====================
    
    /**
     * Log all tokens with pretty formatting.
     * 
     * <p>Example output:
     * <pre>{@code
     * [DEBUG] [LEXER      ] === TOKENS (15 total) ===
     * [DEBUG] [LEXER      ]   1. DEF at 1:1
     * [DEBUG] [LEXER      ]   2. IDENTIFIER("transform") at 1:5
     * [DEBUG] [LEXER      ]   3. LPAREN at 1:14
     * ...
     * }</pre>
     */
    public static void logTokens(List<Token> tokens) {
        if (!isEnabled(LogLevel.DEBUG)) return;
        
        debug("LEXER", "=== TOKENS (" + tokens.size() + " total) ===");
        int i = 1;
        for (Token token : tokens) {
            debug("LEXER", String.format("  %3d. %s", i++, formatToken(token)));
        }
        debug("LEXER", "=== END TOKENS ===");
    }
    
    /**
     * Format a single token for display.
     */
    public static String formatToken(Token token) {
        String valueStr = token.value() != null ? "(\"" + truncate(token.value(), 20) + "\")" : "";
        return String.format("%s%s at %d:%d", token.type(), valueStr, token.line(), token.column());
    }
    
    // ==================== AST Logging ====================
    
    /**
     * Log the AST with tree-style formatting.
     * 
     * <p>Example output:
     * <pre>{@code
     * [DEBUG] [PARSER     ] === AST ===
     * [DEBUG] [PARSER     ] Program
     * [DEBUG] [PARSER     ]   └── FunctionDef: transform(INPUT)
     * [DEBUG] [PARSER     ]         ├── Assignment: OUTPUT = DictLiteral{}
     * [DEBUG] [PARSER     ]         ├── Assignment: OUTPUT["name"] = ...
     * [DEBUG] [PARSER     ]         └── ReturnStatement: OUTPUT
     * }</pre>
     */
    public static void logAST(Program program) {
        if (!isEnabled(LogLevel.DEBUG)) return;
        
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
            case Assignment a -> {
                debug("PARSER", prefix + "Assignment: " + formatExpression(a.target()) + 
                    " = " + formatExpression(a.value()));
            }
            case ReturnStatement r -> {
                debug("PARSER", prefix + "Return: " + formatExpression(r.value()));
            }
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
            case FunctionCall fc -> {
                debug("PARSER", prefix + "FunctionCall: " + fc.functionName() + "(...)");
            }
            case ExpressionStatement e -> {
                debug("PARSER", prefix + "Expression: " + formatExpression(e.expression()));
            }
            default -> {
                debug("PARSER", prefix + stmt.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * Format an expression for logging.
     */
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
    
    /**
     * Log execution of a statement.
     */
    public static void logExecution(Statement stmt, String action) {
        if (!isEnabled(LogLevel.TRACE)) return;
        trace("INTERPRETER", action + ": " + stmt.getClass().getSimpleName() + 
            " at line " + stmt.lineNumber());
    }
    
    /**
     * Log a variable assignment.
     */
    public static void logAssignment(String variable, Value value) {
        if (!isEnabled(LogLevel.TRACE)) return;
        trace("INTERPRETER", "SET " + variable + " = " + formatValue(value));
    }
    
    /**
     * Log a variable read.
     */
    public static void logVariableRead(String variable, Value value) {
        if (!isEnabled(LogLevel.TRACE)) return;
        trace("INTERPRETER", "GET " + variable + " → " + formatValue(value));
    }
    
    /**
     * Log a function call.
     */
    public static void logFunctionCall(String name, List<Value> args) {
        if (!isEnabled(LogLevel.DEBUG)) return;
        debug("INTERPRETER", "CALL " + name + "(" + args.size() + " args)");
    }
    
    /**
     * Log a function return.
     */
    public static void logFunctionReturn(String name, Value result) {
        if (!isEnabled(LogLevel.DEBUG)) return;
        debug("INTERPRETER", "RETURN from " + name + " → " + formatValue(result));
    }
    
    /**
     * Format a Value for display.
     */
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
    
    /**
     * Create a visual separator line.
     */
    public static void separator(String component) {
        if (!isEnabled(LogLevel.DEBUG)) return;
        debug(component, "─".repeat(50));
    }
}
