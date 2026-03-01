package com.grizzly.core.interpreter;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.logging.GrizzlyLogger;
import com.grizzly.core.parser.ast.*;
import com.grizzly.core.types.*;
import com.grizzly.core.validation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.grizzly.core.interpreter.ValueUtils.*;

/**
 * <h1>Interpreter - Step 3 of the Compilation Pipeline</h1>
 * 
 * <p>The Interpreter takes the AST (Abstract Syntax Tree) produced by the Parser
 * and <b>executes</b> it. It walks through each node in the tree and performs
 * the corresponding action.
 * 
 * <h2>What is Interpretation?</h2>
 * 
 * <p>Interpretation is the process of executing code by walking the AST:
 * 
 * <pre>{@code
 * For each node in the AST:
 *   - If it's an Assignment: evaluate the value, store in variable
 *   - If it's a BinaryOp: evaluate left, evaluate right, apply operator
 *   - If it's an IfStatement: evaluate condition, execute correct branch
 *   - If it's a ReturnStatement: evaluate value, return it
 *   - etc.
 * }</pre>
 * 
 * <h2>The Compilation Pipeline</h2>
 * 
 * <pre>{@code
 * ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
 * │   Source    │     │   Tokens    │     │    AST      │     │   Result    │
 * │   Code      │────▶│   (List)    │────▶│   (Tree)    │────▶│   (Map)     │
 * │             │     │             │     │             │     │             │
 * └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
 *        │                  │                   │                   │
 *     LEXER              PARSER            INTERPRETER          OUTPUT
 *                                         (this class)
 * }</pre>
 * 
 * <h2>How Execution Works</h2>
 * 
 * <p>The interpreter maintains an {@link ExecutionContext} that stores variables:
 * 
 * <pre>{@code
 * Code:                           Context (variables):
 * ────────────────────────────    ────────────────────
 * def transform(INPUT):           INPUT = {"name": "John"}
 *     OUTPUT = {}                 OUTPUT = {}
 *     OUTPUT["greeting"] = ...    OUTPUT = {"greeting": "Hello, John!"}
 *     return OUTPUT               (returns OUTPUT value)
 * }</pre>
 * 
 * <h2>Step-by-Step Execution Example</h2>
 * 
 * <pre>{@code
 * AST:
 *   FunctionDef("transform")
 *     ├── Assignment(OUTPUT = {})
 *     ├── Assignment(OUTPUT["name"] = INPUT.firstName)
 *     └── ReturnStatement(OUTPUT)
 * 
 * Execution trace:
 * ┌────────────────────────────────────────────────────────────────────┐
 * │ 1. ENTER FunctionDef "transform"                                  │
 * │    Context: {INPUT: {"firstName": "John", "lastName": "Doe"}}     │
 * │                                                                   │
 * │ 2. EXECUTE Assignment: OUTPUT = {}                                │
 * │    - Evaluate right side: {} → DictValue{}                        │
 * │    - Store in context: OUTPUT = DictValue{}                       │
 * │    Context: {INPUT: {...}, OUTPUT: {}}                            │
 * │                                                                   │
 * │ 3. EXECUTE Assignment: OUTPUT["name"] = INPUT.firstName           │
 * │    - Evaluate target: OUTPUT["name"] → DictAccess                 │
 * │    - Evaluate value: INPUT.firstName → "John"                     │
 * │    - Store: OUTPUT["name"] = "John"                               │
 * │    Context: {INPUT: {...}, OUTPUT: {"name": "John"}}              │
 * │                                                                   │
 * │ 4. EXECUTE ReturnStatement: return OUTPUT                         │
 * │    - Evaluate: OUTPUT → {"name": "John"}                          │
 * │    - Return this value                                            │
 * │                                                                   │
 * │ 5. EXIT FunctionDef, returning: {"name": "John"}                  │
 * └────────────────────────────────────────────────────────────────────┘
 * }</pre>
 * 
 * <h2>Type-Safe Values</h2>
 * 
 * <p>The interpreter uses a type-safe {@link Value} hierarchy instead of raw Objects:
 * 
 * <table border="1">
 *   <tr><th>Python Type</th><th>Java Value Class</th><th>Example</th></tr>
 *   <tr><td>str</td><td>{@link StringValue}</td><td>"hello"</td></tr>
 *   <tr><td>int/float</td><td>{@link NumberValue}</td><td>42, 3.14</td></tr>
 *   <tr><td>bool</td><td>{@link BoolValue}</td><td>True, False</td></tr>
 *   <tr><td>list</td><td>{@link ListValue}</td><td>[1, 2, 3]</td></tr>
 *   <tr><td>dict</td><td>{@link DictValue}</td><td>{"key": "value"}</td></tr>
 *   <tr><td>None</td><td>{@link NullValue}</td><td>None</td></tr>
 * </table>
 * 
 * <h2>Production Safeguards</h2>
 * 
 * <p>The interpreter includes safeguards to prevent runaway code:
 * 
 * <ul>
 *   <li><b>Loop Limit</b>: Maximum iterations per loop (default: 10,000)</li>
 *   <li><b>Recursion Depth</b>: Maximum function call depth (default: 100)</li>
 *   <li><b>Execution Timeout</b>: Maximum execution time (default: 30 seconds)</li>
 * </ul>
 * 
 * <pre>{@code
 * // Configure safeguards
 * InterpreterConfig config = InterpreterConfig.builder()
 *     .maxLoopIterations(5000)
 *     .maxRecursionDepth(50)
 *     .executionTimeout(Duration.ofSeconds(10))
 *     .build();
 * 
 * GrizzlyInterpreter interpreter = new GrizzlyInterpreter(program, config);
 * }</pre>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // After lexing and parsing...
 * Program program = parser.parse();
 * 
 * // Create interpreter
 * GrizzlyInterpreter interpreter = new GrizzlyInterpreter(program);
 * 
 * // Execute with input data
 * Map<String, Object> input = Map.of("firstName", "John", "lastName", "Doe");
 * Map<String, Object> result = interpreter.execute(input);
 * 
 * System.out.println(result); // {"name": "John Doe"}
 * }</pre>
 * 
 * <h2>Debugging</h2>
 * 
 * <p>Enable logging to see the execution trace:
 * 
 * <pre>{@code
 * GrizzlyLogger.setLevel(GrizzlyLogger.LogLevel.TRACE);
 * Map<String, Object> result = interpreter.execute(input);
 * 
 * // Output:
 * // [TRACE] [INTERPRETER] ENTER FunctionDef: transform
 * // [TRACE] [INTERPRETER] SET OUTPUT = dict{0 keys}
 * // [TRACE] [INTERPRETER] GET INPUT → dict{2 keys}
 * // [TRACE] [INTERPRETER] GET INPUT.firstName → "John"
 * // [TRACE] [INTERPRETER] SET OUTPUT["name"] = "John"
 * // [TRACE] [INTERPRETER] RETURN from transform → dict{1 keys}
 * }</pre>
 * 
 * @see GrizzlyParser The previous step: converts tokens to AST
 * @see ExecutionContext Stores variables during execution
 * @see Value The type-safe value hierarchy
 * @see InterpreterConfig Configuration for safeguards
 */
public class GrizzlyInterpreter {
    
    /**
     * Functional interface for built-in functions.
     */
    @FunctionalInterface
    public interface BuiltinFunction {
        Value apply(List<Value> args);
    }
    
    private final Program program;
    private final InterpreterConfig config;
    private final BuiltinRegistry builtins;
    private final ModuleRegistry modules;
    
    private int currentRecursionDepth = 0;
    private long executionStartTime;
    
    public GrizzlyInterpreter(Program program) {
        this(program, InterpreterConfig.defaults());
    }
    
    public GrizzlyInterpreter(Program program, InterpreterConfig config) {
        this.program = program;
        this.config = config;
        this.builtins = new BuiltinRegistry();
        this.modules = new ModuleRegistry();
    }
    
    /**
     * Execute the transform function with input data.
     * 
     * <p>This is the main entry point for executing a compiled template.
     * It finds the 'transform' function, binds the input data to INPUT,
     * executes the function body, and returns the result.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * GrizzlyInterpreter interpreter = new GrizzlyInterpreter(program);
     * 
     * Map<String, Object> input = Map.of("name", "John");
     * Map<String, Object> output = interpreter.execute(input);
     * 
     * System.out.println(output.get("greeting")); // "Hello, John!"
     * }</pre>
     * 
     * @param inputData Input data as Java Map (typically from JSON)
     * @return Output data as Java Map (for JSON serialization)
     * @throws GrizzlyExecutionException if transform function is not found,
     *         doesn't return a dict, or any runtime error occurs
     */
    public Map<String, Object> execute(Map<String, Object> inputData) {
        DictValue input = ValueConverter.fromJavaMap(inputData);
        DictValue output = executeTyped(input);
        return ValueConverter.toJavaMap(output);
    }
    
    /**
     * Execute the transform function with type-safe DictValue input and output.
     * 
     * <p>This method is used internally for format-agnostic execution.
     * It avoids the Map-to-DictValue conversion overhead when working
     * directly with the type-safe Value hierarchy.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * // Used by format handlers
     * DictValue input = xmlReader.read(xmlString);
     * DictValue output = interpreter.executeTyped(input);
     * String json = jsonWriter.write(output);
     * }</pre>
     * 
     * @param input Input data as DictValue
     * @return Output data as DictValue
     * @throws GrizzlyExecutionException if transform function is not found,
     *         doesn't return a dict, or any runtime error occurs
     */
    public DictValue executeTyped(DictValue input) {
        AccessTracker tracker = new AccessTracker(config.isTrackingEnabled());
        return executeTyped(input, tracker);
    }
    
    /**
     * Execute the transform function with access tracking.
     * 
     * <p>This method provides full access tracking for validation reports.
     * Use this when you need to know which property accesses failed.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * AccessTracker tracker = new AccessTracker(true);
     * DictValue output = interpreter.executeTyped(input, tracker);
     * ValidationReport report = tracker.generateReport();
     * }</pre>
     * 
     * @param input   Input data as DictValue
     * @param tracker AccessTracker for recording property accesses
     * @return Output data as DictValue
     * @throws GrizzlyExecutionException if transform function is not found,
     *         doesn't return a dict, or any runtime error occurs
     */
    public DictValue executeTyped(DictValue input, AccessTracker tracker) {
        GrizzlyLogger.info("INTERPRETER", "Starting execution (mode: " + config.nullHandling() + ")");
        executionStartTime = System.currentTimeMillis();
        currentRecursionDepth = 0;
        
        ExecutionContext globalContext = new ExecutionContext(tracker);
        for (ImportStatement importStmt : program.imports()) {
            executeStatement(importStmt, globalContext);
        }
        
        FunctionDef transformFunc = program.findFunction("transform");
        if (transformFunc == null) {
            throw new GrizzlyExecutionException("No 'transform' function found in template");
        }
        
        ExecutionContext context = new ExecutionContext(tracker);
        context.set("INPUT", input);
        GrizzlyLogger.debug("INTERPRETER", "Bound INPUT with " + input.size() + " keys");
        
        GrizzlyLogger.debug("INTERPRETER", "Calling transform()");
        Value result = executeFunction(transformFunc, context);
        
        if (result instanceof DictValue dict) {
            long elapsed = System.currentTimeMillis() - executionStartTime;
            GrizzlyLogger.info("INTERPRETER", "Execution complete in " + elapsed + "ms, " +
                "output has " + dict.size() + " keys");
            
            if (tracker.isEnabled()) {
                GrizzlyLogger.debug("INTERPRETER", "Access tracking: " + tracker.size() + " accesses recorded");
            }
            
            return dict;
        }
        
        throw new GrizzlyExecutionException("transform() must return a dict, got: " + 
            result.typeName());
    }
    
    // ==================== Safeguard Checks ====================
    
    private void checkTimeout() {
        long elapsed = System.currentTimeMillis() - executionStartTime;
        if (elapsed > config.executionTimeout().toMillis()) {
            throw new GrizzlyExecutionException(
                "Execution timeout exceeded (" + config.executionTimeout().toSeconds() + "s)"
            );
        }
    }
    
    private void checkRecursionDepth() {
        if (currentRecursionDepth > config.maxRecursionDepth()) {
            throw new GrizzlyExecutionException(
                "Maximum recursion depth exceeded (" + config.maxRecursionDepth() + ")"
            );
        }
    }
    
    // ==================== Execution Methods ====================
    
    private Value executeFunction(FunctionDef function, ExecutionContext context) {
        currentRecursionDepth++;
        checkRecursionDepth();
        checkTimeout();
        
        try {
            for (Statement stmt : function.body()) {
                Value result = executeStatement(stmt, context);
                
                if (stmt instanceof ReturnStatement) {
                    return result;
                }
            }
            
            return NullValue.INSTANCE;
        } finally {
            currentRecursionDepth--;
        }
    }
    
    private Value executeStatement(Statement stmt, ExecutionContext context) {
        return switch (stmt) {
            case ImportStatement i -> {
                if (!modules.containsModule(i.moduleName())) {
                    throw new GrizzlyExecutionException(
                        "Unknown module: " + i.moduleName(),
                        i.lineNumber()
                    );
                }
                yield NullValue.INSTANCE;
            }
            case Assignment a -> executeAssignment(a, context);
            case ReturnStatement r -> evaluateExpression(r.value(), context);
            case FunctionCall f -> executeFunctionCall(f, context);
            case IfStatement i -> executeIf(i, context);
            case ForLoop forLoop -> executeForLoop(forLoop, context);
            case ExpressionStatement e -> evaluateExpression(e.expression(), context);
            case BreakStatement b -> throw new com.grizzly.core.exception.BreakException();
            case ContinueStatement c -> throw new com.grizzly.core.exception.ContinueException();
            default -> throw new GrizzlyExecutionException(
                "Unknown statement type: " + stmt.getClass().getSimpleName(),
                stmt.lineNumber()
            );
        };
    }
    
    private Value executeAssignment(Assignment assignment, ExecutionContext context) {
        try {
            Value value = evaluateExpression(assignment.value(), context);
            setTarget(assignment.target(), value, context);
            return value;
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error in assignment: " + e.getMessage(),
                assignment.lineNumber()
            );
        }
    }
    
    private Value executeFunctionCall(FunctionCall call, ExecutionContext context) {
        try {
            return invokeFunction(call.functionName(), call.args(), context, call.lineNumber());
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error calling function '" + call.functionName() + "': " + e.getMessage(),
                call.lineNumber()
            );
        }
    }
    
    private Value invokeFunction(String name, List<Expression> argExprs, 
                                 ExecutionContext context, int lineNumber) {
        if (builtins.contains(name)) {
            List<Value> args = evaluateArguments(argExprs, context);
            return builtins.get(name).apply(args);
        }
        
        FunctionDef func = program.findFunction(name);
        if (func == null) {
            throw new GrizzlyExecutionException(
                "Function '" + name + "' not found",
                lineNumber
            );
        }
        
        ExecutionContext funcContext = context.createChild();
        for (int i = 0; i < func.params().size(); i++) {
            Value argValue = evaluateExpression(argExprs.get(i), context);
            funcContext.set(func.params().get(i), argValue);
        }
        
        return executeFunction(func, funcContext);
    }
    
    private Value executeIf(IfStatement ifStmt, ExecutionContext context) {
        try {
            if (evaluateExpression(ifStmt.condition(), context).isTruthy()) {
                return executeBlock(ifStmt.thenBlock(), context);
            }
            
            for (IfStatement.ElifBranch elifBranch : ifStmt.elifBranches()) {
                if (evaluateExpression(elifBranch.condition(), context).isTruthy()) {
                    return executeBlock(elifBranch.statements(), context);
                }
            }
            
            if (ifStmt.elseBlock() != null) {
                return executeBlock(ifStmt.elseBlock(), context);
            }
            
            return NullValue.INSTANCE;
        } catch (com.grizzly.core.exception.BreakException | com.grizzly.core.exception.ContinueException e) {
            throw e;
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error in if statement: " + e.getMessage(),
                ifStmt.lineNumber()
            );
        }
    }
    
    private Value executeBlock(List<Statement> statements, ExecutionContext context) {
        for (Statement stmt : statements) {
            Value result = executeStatement(stmt, context);
            if (stmt instanceof ReturnStatement) {
                return result;
            }
        }
        return NullValue.INSTANCE;
    }
    
    private Value executeForLoop(ForLoop forLoop, ExecutionContext context) {
        try {
            Value iterableVal = evaluateExpression(forLoop.iterable(), context);
            
            List<Value> items = toIterableList(iterableVal, forLoop.lineNumber());
            
            int iterations = 0;
            itemLoop: for (Value item : items) {
                // Check loop iteration limit
                iterations++;
                if (iterations > config.maxLoopIterations()) {
                    throw new GrizzlyExecutionException(
                        "Maximum loop iterations exceeded (" + config.maxLoopIterations() + ") at line " + forLoop.lineNumber(),
                        forLoop.lineNumber()
                    );
                }
                
                // Check timeout periodically (every 1000 iterations)
                if (iterations % 1000 == 0) {
                    checkTimeout();
                }
                
                context.set(forLoop.variable(), item);
                
                for (Statement stmt : forLoop.body()) {
                    try {
                        Value result = executeStatement(stmt, context);
                        
                        if (stmt instanceof ReturnStatement) {
                            return result;
                        }
                    } catch (com.grizzly.core.exception.BreakException e) {
                        break itemLoop;
                    } catch (com.grizzly.core.exception.ContinueException e) {
                        continue itemLoop;
                    }
                }
            }
            
            return NullValue.INSTANCE;
            
        } catch (com.grizzly.core.exception.BreakException | com.grizzly.core.exception.ContinueException e) {
            throw new GrizzlyExecutionException(
                "break/continue outside of loop at line " + forLoop.lineNumber(),
                forLoop.lineNumber()
            );
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error in for loop: " + e.getMessage(),
                forLoop.lineNumber()
            );
        }
    }
    
    private Value evaluateExpression(Expression expr, ExecutionContext context) {
        try {
            return switch (expr) {
                case NumberLiteral n -> new NumberValue(n.value());
                case StringLiteral s -> new StringValue(s.value());
                case BooleanLiteral b -> BoolValue.of(b.value());
                case NullLiteral ignored -> NullValue.INSTANCE;
                case Identifier i -> context.get(i.name());
                case DictLiteral d -> evaluateDictLiteral(d, context);
                case ListLiteral l -> evaluateListLiteral(l, context);
                case DictAccess d -> evaluateDictAccess(d, context);
                case AttrAccess a -> evaluateAttrAccess(a, context);
                case BinaryOp b -> evaluateBinaryOp(b, context);
                case MethodCall m -> evaluateMethodCall(m, context);
                case FunctionCallExpression f -> evaluateFunctionCallExpression(f, context);
                default -> throw new GrizzlyExecutionException(
                    "Unknown expression type: " + expr.getClass().getSimpleName()
                );
            };
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error evaluating expression: " + e.getMessage()
            );
        }
    }
    
    private Value evaluateDictAccess(DictAccess dictAccess, ExecutionContext context) {
        Value obj = evaluateExpression(dictAccess.object(), context);
        boolean safe = dictAccess.safe();
        String basePath = buildExpressionPath(dictAccess.object());
        AccessTracker tracker = context.getAccessTracker();
        
        // Handle null object
        if (obj instanceof NullValue) {
            String keyStr = getKeyString(dictAccess.key(), context);
            String fullPath = basePath + "[" + keyStr + "]";
            return handleNullAccess(fullPath, basePath, safe, tracker, 0);
        }
        
        Value keyVal = evaluateExpression(dictAccess.key(), context);
        String keyStr = getKeyString(keyVal);
        String fullPath = basePath + "[" + keyStr + "]";
        
        return switch (obj) {
            case DictValue dict -> {
                Value result = dict.getOrNull(asString(keyVal));
                if (result == null) {
                    yield handleKeyNotFound(fullPath, asString(keyVal), safe, tracker, 0);
                }
                trackSuccess(fullPath, result, tracker, 0);
                yield result;
            }
            case ListValue list -> {
                int index = toInt(keyVal);
                int originalIndex = index;
                if (index < 0) {
                    index = list.size() + index;
                }
                if (index < 0 || index >= list.size()) {
                    yield handleIndexOutOfBounds(fullPath, originalIndex, list.size(), safe, tracker, 0);
                }
                Value result = list.get(index);
                trackSuccess(fullPath, result, tracker, 0);
                yield result;
            }
            default -> {
                if (safe || config.nullHandling() == NullHandling.SAFE || 
                    config.nullHandling() == NullHandling.SILENT) {
                    yield NullValue.INSTANCE;
                }
                throw new GrizzlyExecutionException(
                    "Cannot access key on object of type " + obj.typeName()
                );
            }
        };
    }
    
    private Value evaluateAttrAccess(AttrAccess attrAccess, ExecutionContext context) {
        Value obj = evaluateExpression(attrAccess.object(), context);
        boolean safe = attrAccess.safe();
        String basePath = buildExpressionPath(attrAccess.object());
        String attr = attrAccess.attr();
        String fullPath = basePath + "." + attr;
        AccessTracker tracker = context.getAccessTracker();
        
        // Handle null object
        if (obj instanceof NullValue) {
            return handleNullAccess(fullPath, basePath, safe, tracker, 0);
        }
        
        if (obj instanceof DictValue dict) {
            Value result = dict.getOrNull(attr);
            if (result == null) {
                return handleKeyNotFound(fullPath, attr, safe, tracker, 0);
            }
            trackSuccess(fullPath, result, tracker, 0);
            return result;
        }
        
        // Object is not a dict
        if (safe || config.nullHandling() == NullHandling.SAFE || 
            config.nullHandling() == NullHandling.SILENT) {
            return NullValue.INSTANCE;
        }
        
        throw new GrizzlyExecutionException(
            "Cannot access attribute '" + attr + "' on object of type " + obj.typeName()
        );
    }
    
    // ==================== Safe Access Helpers ====================
    
    private Value handleNullAccess(String path, String brokenAt, boolean safe, AccessTracker tracker, int lineNumber) {
        switch (config.nullHandling()) {
            case STRICT -> {
                if (safe) {
                    tracker.recordPathBroken(path, brokenAt, lineNumber, true);
                    return NullValue.INSTANCE;
                }
                throw new GrizzlyExecutionException(
                    "Cannot access '" + path + "' - '" + brokenAt + "' is null. " +
                    "Use ?. for safe navigation or switch to SAFE mode."
                );
            }
            case SAFE -> {
                tracker.recordPathBroken(path, brokenAt, lineNumber, safe);
                return NullValue.INSTANCE;
            }
            case SILENT -> {
                return NullValue.INSTANCE;
            }
        }
        return NullValue.INSTANCE;
    }
    
    private Value handleKeyNotFound(String path, String key, boolean safe, AccessTracker tracker, int lineNumber) {
        switch (config.nullHandling()) {
            case STRICT -> {
                if (safe) {
                    tracker.recordKeyNotFound(path, key, lineNumber, true);
                    return NullValue.INSTANCE;
                }
                throw new GrizzlyExecutionException(
                    "Key not found: '" + key + "' in path '" + path + "'. " +
                    "Use ?. for safe navigation or switch to SAFE mode."
                );
            }
            case SAFE -> {
                tracker.recordKeyNotFound(path, key, lineNumber, safe);
                return NullValue.INSTANCE;
            }
            case SILENT -> {
                return NullValue.INSTANCE;
            }
        }
        return NullValue.INSTANCE;
    }
    
    private Value handleIndexOutOfBounds(String path, int index, int listSize, boolean safe, AccessTracker tracker, int lineNumber) {
        switch (config.nullHandling()) {
            case STRICT -> {
                if (safe) {
                    tracker.recordIndexOutOfBounds(path, index, listSize, lineNumber, true);
                    return NullValue.INSTANCE;
                }
                throw new GrizzlyExecutionException(
                    "List index out of range: " + index + " (list size: " + listSize + ") in path '" + path + "'"
                );
            }
            case SAFE -> {
                tracker.recordIndexOutOfBounds(path, index, listSize, lineNumber, safe);
                return NullValue.INSTANCE;
            }
            case SILENT -> {
                return NullValue.INSTANCE;
            }
        }
        return NullValue.INSTANCE;
    }
    
    private void trackSuccess(String path, Value value, AccessTracker tracker, int lineNumber) {
        tracker.recordSuccess(path, value, lineNumber);
    }
    
    private String buildExpressionPath(Expression expr) {
        return switch (expr) {
            case Identifier id -> id.name();
            case AttrAccess attr -> buildExpressionPath(attr.object()) + 
                (attr.safe() ? "?." : ".") + attr.attr();
            case DictAccess dict -> buildExpressionPath(dict.object()) + 
                (dict.safe() ? "?[" : "[") + getKeyString(dict.key(), null) + "]";
            default -> expr.toString();
        };
    }
    
    private String getKeyString(Expression keyExpr, ExecutionContext context) {
        if (keyExpr instanceof StringLiteral str) {
            return "\"" + str.value() + "\"";
        }
        if (keyExpr instanceof NumberLiteral num) {
            return String.valueOf(num.value());
        }
        if (context != null) {
            Value keyVal = evaluateExpression(keyExpr, context);
            return getKeyString(keyVal);
        }
        return keyExpr.toString();
    }
    
    private String getKeyString(Value keyVal) {
        if (keyVal instanceof StringValue str) {
            return "\"" + str.value() + "\"";
        }
        if (keyVal instanceof NumberValue num) {
            return String.valueOf(num.value());
        }
        return keyVal.toString();
    }
    
    private Value evaluateBinaryOp(BinaryOp binaryOp, ExecutionContext context) {
        Value left = evaluateExpression(binaryOp.left(), context);
        Value right = evaluateExpression(binaryOp.right(), context);
        
        String operator = binaryOp.operator();
        
        return switch (operator) {
            case "+" -> evaluatePlus(left, right);
            case "-" -> evaluateNumericOp(left, right, "-");
            case "*" -> evaluateStar(left, right);
            case "/" -> evaluateNumericOp(left, right, "/");
            case "//" -> evaluateNumericOp(left, right, "//");
            case "%" -> evaluateNumericOp(left, right, "%");
            case "**" -> evaluateNumericOp(left, right, "**");
            case "==" -> BoolValue.of(areEqual(left, right));
            case "!=" -> BoolValue.of(!areEqual(left, right));
            case "<", ">", "<=", ">=" -> BoolValue.of(evaluateComparison(left, right, operator));
            case "and" -> left.isTruthy() ? right : left;
            case "or" -> left.isTruthy() ? left : right;
            case "not" -> BoolValue.of(!right.isTruthy());
            case "in" -> BoolValue.of(evaluateIn(left, right));
            case "not in" -> BoolValue.of(!evaluateIn(left, right));
            default -> throw new GrizzlyExecutionException("Unknown operator: " + operator);
        };
    }
    
    private boolean evaluateIn(Value left, Value right) {
        return switch (right) {
            case ListValue list -> {
                for (Value item : list.items()) {
                    if (areEqual(left, item)) {
                        yield true;
                    }
                }
                yield false;
            }
            case DictValue dict -> {
                String key = asString(left);
                yield dict.containsKey(key);
            }
            case StringValue str -> {
                String needle = asString(left);
                yield str.value().contains(needle);
            }
            default -> throw new GrizzlyExecutionException(
                "Cannot use 'in' operator with " + right.typeName()
            );
        };
    }
    
    private Value evaluatePlus(Value left, Value right) {
        if (left instanceof DecimalValue ld && right instanceof DecimalValue rd) {
            return ld.add(rd);
        }
        
        if (left instanceof StringValue || right instanceof StringValue) {
            String ls = left instanceof StringValue s ? s.value() : left.toString();
            String rs = right instanceof StringValue s ? s.value() : right.toString();
            return new StringValue(ls + rs);
        }
        
        if (left instanceof ListValue ll && right instanceof ListValue rl) {
            List<Value> result = new ArrayList<>(ll.items());
            result.addAll(rl.items());
            return new ListValue(result);
        }
        
        return evaluateNumericOp(left, right, "+");
    }
    
    private Value evaluateStar(Value left, Value right) {
        if (left instanceof NumberValue n && right instanceof StringValue s) {
            int count = n.asInt();
            return new StringValue(s.value().repeat(Math.max(0, count)));
        }
        if (left instanceof StringValue s && right instanceof NumberValue n) {
            int count = n.asInt();
            return new StringValue(s.value().repeat(Math.max(0, count)));
        }
        
        if (left instanceof NumberValue n && right instanceof ListValue l) {
            int count = n.asInt();
            List<Value> result = new ArrayList<>();
            for (int i = 0; i < Math.max(0, count); i++) {
                result.addAll(l.items());
            }
            return new ListValue(result);
        }
        if (left instanceof ListValue l && right instanceof NumberValue n) {
            int count = n.asInt();
            List<Value> result = new ArrayList<>();
            for (int i = 0; i < Math.max(0, count); i++) {
                result.addAll(l.items());
            }
            return new ListValue(result);
        }
        
        return evaluateNumericOp(left, right, "*");
    }
    
    private ListValue evaluateListLiteral(ListLiteral listLiteral, ExecutionContext context) {
        List<Value> result = new ArrayList<>();
        
        for (Expression element : listLiteral.elements()) {
            Value value = evaluateExpression(element, context);
            result.add(value);
        }
        
        return new ListValue(result);
    }
    
    private DictValue evaluateDictLiteral(DictLiteral dictLiteral, ExecutionContext context) {
        DictValue result = DictValue.empty();
        
        for (DictLiteral.Entry entry : dictLiteral.entries()) {
            String key = asString(evaluateExpression(entry.key(), context));
            Value value = evaluateExpression(entry.value(), context);
            result.put(key, value);
        }
        
        return result;
    }
    
    private Value evaluateMethodCall(MethodCall methodCall, ExecutionContext context) {
        if (methodCall.object() instanceof Identifier id) {
            String moduleName = id.name();
            String methodName = methodCall.methodName();
            
            if (modules.containsModule(moduleName)) {
                Map<String, BuiltinFunction> module = modules.getModule(moduleName);
                if (module.containsKey(methodName)) {
                    List<Value> args = new ArrayList<>();
                    for (Expression argExpr : methodCall.arguments()) {
                        args.add(evaluateExpression(argExpr, context));
                    }
                    return module.get(methodName).apply(args);
                } else {
                    throw new GrizzlyExecutionException(
                        "Module '" + moduleName + "' has no function '" + methodName + "'"
                    );
                }
            }
        }
        
        Value obj = evaluateExpression(methodCall.object(), context);
        String methodName = methodCall.methodName();
        
        if (obj instanceof NullValue) {
            throw new GrizzlyExecutionException(
                "Cannot call method '" + methodName + "' on null object"
            );
        }
        
        if (obj instanceof ListValue list) {
            return evaluateListMethod(list, methodName, methodCall.arguments(), context);
        }
        
        if (obj instanceof StringValue str) {
            return evaluateStringMethod(str, methodName, methodCall.arguments(), context);
        }
        
        if (obj instanceof DictValue dict) {
            return evaluateDictMethod(dict, methodName, methodCall.arguments(), context);
        }
        
        throw new GrizzlyExecutionException(
            "Object of type " + obj.typeName() + " does not have method '" + methodName + "'"
        );
    }
    
    private Value evaluateListMethod(ListValue list, String methodName, 
                                     List<Expression> arguments, ExecutionContext context) {
        List<Value> args = evaluateArguments(arguments, context);
        return ListMethods.evaluate(list, methodName, args);
    }
    
    private Value evaluateStringMethod(StringValue str, String methodName,
                                       List<Expression> arguments, ExecutionContext context) {
        List<Value> args = evaluateArguments(arguments, context);
        return StringMethods.evaluate(str, methodName, args);
    }
    
    private Value evaluateDictMethod(DictValue dict, String methodName,
                                     List<Expression> arguments, ExecutionContext context) {
        List<Value> args = evaluateArguments(arguments, context);
        return DictMethods.evaluate(dict, methodName, args);
    }
    
    private List<Value> evaluateArguments(List<Expression> arguments, ExecutionContext context) {
        List<Value> args = new ArrayList<>();
        for (Expression arg : arguments) {
            args.add(evaluateExpression(arg, context));
        }
        return args;
    }
    
    private Value evaluateFunctionCallExpression(FunctionCallExpression funcCall, ExecutionContext context) {
        return invokeFunction(funcCall.functionName(), funcCall.args(), context, 0);
    }
    
    private void setTarget(Expression target, Value value, ExecutionContext context) {
        switch (target) {
            case Identifier i -> context.set(i.name(), value);
            case DictAccess d -> {
                Value obj = evaluateExpression(d.object(), context);
                Value keyVal = evaluateExpression(d.key(), context);
                
                if (obj instanceof NullValue && d.object() instanceof DictAccess) {
                    DictValue newDict = DictValue.empty();
                    setTarget(d.object(), newDict, context);
                    newDict.put(asString(keyVal), value);
                } else if (obj instanceof DictValue dict) {
                    dict.put(asString(keyVal), value);
                } else if (obj instanceof ListValue list) {
                    int index = toInt(keyVal);
                    if (index < 0) {
                        index = list.size() + index;
                    }
                    if (index < 0 || index >= list.size()) {
                        throw new GrizzlyExecutionException(
                            "List index out of range: " + toInt(keyVal) + " (list size: " + list.size() + ")"
                        );
                    }
                    list.set(index, value);
                } else {
                    throw new GrizzlyExecutionException(
                        "Cannot set key on object of type " + obj.typeName()
                    );
                }
            }
            case AttrAccess a -> {
                Value obj = evaluateExpression(a.object(), context);
                if (obj instanceof DictValue dict) {
                    dict.put(a.attr(), value);
                } else {
                    throw new GrizzlyExecutionException(
                        "Cannot set attribute on object of type " + obj.typeName()
                    );
                }
            }
            default -> throw new GrizzlyExecutionException(
                "Cannot assign to expression of type " + target.getClass().getSimpleName()
            );
        }
    }
    
    private Value evaluateNumericOp(Value left, Value right, String operator) {
        if (left instanceof DecimalValue || right instanceof DecimalValue) {
            DecimalValue l = (left instanceof DecimalValue ld) 
                ? ld 
                : new DecimalValue(asString(left));
            DecimalValue r = (right instanceof DecimalValue rd) 
                ? rd 
                : new DecimalValue(asString(right));
            
            return switch (operator) {
                case "+" -> l.add(r);
                case "-" -> l.subtract(r);
                case "*" -> l.multiply(r);
                case "/" -> l.divide(r);
                case "//" -> new DecimalValue(
                    l.getValue().divideToIntegralValue(r.getValue())
                );
                case "%" -> new DecimalValue(
                    l.getValue().remainder(r.getValue())
                );
                case "**" -> {
                    double result = Math.pow(l.toDouble(), r.toDouble());
                    yield new DecimalValue(String.valueOf(result));
                }
                default -> throw new GrizzlyExecutionException("Unknown numeric operator: " + operator);
            };
        }
        
        double l = toDouble(left);
        double r = toDouble(right);
        
        boolean bothIntegers = (l == Math.floor(l)) && (r == Math.floor(r));
        
        double result = switch (operator) {
            case "+" -> l + r;
            case "-" -> l - r;
            case "*" -> l * r;
            case "/" -> l / r;
            case "//" -> Math.floor(l / r);
            case "%" -> l % r;
            case "**" -> Math.pow(l, r);
            default -> throw new GrizzlyExecutionException("Unknown numeric operator: " + operator);
        };
        
        if (bothIntegers && result == Math.floor(result) && !operator.equals("/")) {
            return NumberValue.of((int) result);
        }
        return NumberValue.of(result);
    }
    
    private boolean evaluateComparison(Value left, Value right, String operator) {
        if (left instanceof DecimalValue || right instanceof DecimalValue) {
            DecimalValue l = (left instanceof DecimalValue ld) 
                ? ld 
                : new DecimalValue(asString(left));
            DecimalValue r = (right instanceof DecimalValue rd) 
                ? rd 
                : new DecimalValue(asString(right));
            
            return switch (operator) {
                case "<" -> l.lessThan(r);
                case ">" -> l.greaterThan(r);
                case "<=" -> l.lessThanOrEqual(r);
                case ">=" -> l.greaterThanOrEqual(r);
                default -> throw new GrizzlyExecutionException("Unknown comparison operator: " + operator);
            };
        }
        
        double l = toDouble(left);
        double r = toDouble(right);
        
        return switch (operator) {
            case "<" -> l < r;
            case ">" -> l > r;
            case "<=" -> l <= r;
            case ">=" -> l >= r;
            default -> throw new GrizzlyExecutionException("Unknown comparison operator: " + operator);
        };
    }
    
    /**
     * Convert a Value to an iterable list of Values.
     * Supports lists, strings (char iteration), and dicts (key iteration).
     */
    private List<Value> toIterableList(Value value, int lineNumber) {
        return switch (value) {
            case ListValue list -> list.items();
            case StringValue str -> {
                List<Value> chars = new ArrayList<>();
                for (char c : str.value().toCharArray()) {
                    chars.add(new StringValue(String.valueOf(c)));
                }
                yield chars;
            }
            case DictValue dict -> {
                List<Value> keys = new ArrayList<>();
                for (String key : dict.entries().keySet()) {
                    keys.add(new StringValue(key));
                }
                yield keys;
            }
            case NullValue ignored -> throw new GrizzlyExecutionException(
                "Cannot iterate over None", lineNumber
            );
            default -> throw new GrizzlyExecutionException(
                "Cannot iterate over " + value.typeName(), lineNumber
            );
        };
    }
}
