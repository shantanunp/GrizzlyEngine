package com.grizzly.core.interpreter;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.logging.GrizzlyLogger;
import com.grizzly.core.parser.GrizzlyParser;
import com.grizzly.core.parser.ast.*;
import com.grizzly.core.types.*;
import com.grizzly.core.validation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.grizzly.core.interpreter.ValueUtils.*;

/**
 * Interpreter - Step 3 of the Compilation Pipeline.
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
 * <caption>Type mapping</caption>
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
     * Supports positional and keyword arguments (Python-compliant).
     */
    @FunctionalInterface
    public interface BuiltinFunction {
        Value apply(List<Value> args, Map<String, Value> keywordArgs, CallableInvoker invoker);
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
        
        ExecutionContext context = globalContext.createChild();
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
        } catch (com.grizzly.core.exception.ReturnException e) {
            return e.getValue();
        } finally {
            currentRecursionDepth--;
        }
    }
    
    private Value executeStatement(Statement stmt, ExecutionContext context) {
        if (stmt instanceof ImportStatement i) {
            if (!modules.containsModule(i.moduleName())) {
                throw new GrizzlyExecutionException(
                    "Unknown module: " + i.moduleName(),
                    i.lineNumber()
                );
            }
            if (i.isFromImport()) {
                for (String name : i.importedNames()) {
                    Value export = modules.getModuleExport(i.moduleName(), name);
                    if (export == null) {
                        throw new GrizzlyExecutionException(
                            "Cannot import '" + name + "' from '" + i.moduleName() + "'",
                            i.lineNumber()
                        );
                    }
                    context.set(name, export);
                }
            } else {
                DictValue modVal = modules.getModuleValue(i.moduleName());
                if (modVal != null) {
                    context.set(i.moduleName(), modVal);
                }
            }
            return NullValue.INSTANCE;
        } else if (stmt instanceof Assignment a) {
            return executeAssignment(a, context);
        } else if (stmt instanceof ReturnStatement r) {
            throw new com.grizzly.core.exception.ReturnException(evaluateExpression(r.value(), context));
        } else if (stmt instanceof FunctionCall f) {
            return executeFunctionCall(f, context);
        } else if (stmt instanceof IfStatement i) {
            return executeIf(i, context);
        } else if (stmt instanceof ForLoop forLoop) {
            return executeForLoop(forLoop, context);
        } else if (stmt instanceof SwitchStatement sw) {
            return executeSwitch(sw, context);
        } else if (stmt instanceof ExpressionStatement e) {
            return evaluateExpression(e.expression(), context);
        } else if (stmt instanceof BreakStatement) {
            throw new com.grizzly.core.exception.BreakException();
        } else if (stmt instanceof ContinueStatement) {
            throw new com.grizzly.core.exception.ContinueException();
        } else {
            throw new GrizzlyExecutionException(
                "Unknown statement type: " + stmt.getClass().getSimpleName(),
                stmt.lineNumber()
            );
        }
    }
    
    private Value executeAssignment(Assignment assignment, ExecutionContext context) {
        try {
            Value value = evaluateExpression(assignment.value(), context);
            setTarget(assignment.target(), value, context);
            return value;
        } catch (com.grizzly.core.exception.ReturnException e) {
            throw e;
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
            var evaluated = evaluateCallArguments(call.arguments(), context);
            return invokeFunction(call.functionName(), evaluated.positional(), evaluated.keyword(), context, call.lineNumber());
        } catch (com.grizzly.core.exception.ReturnException e) {
            throw e;
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error calling function '" + call.functionName() + "': " + e.getMessage(),
                call.lineNumber()
            );
        }
    }
    
    private Value invokeFunction(String name, List<Value> args, Map<String, Value> kw,
                                 ExecutionContext context, int lineNumber) {
        
        Value ctxVal = context.getOrNull(name);
        if (ctxVal instanceof CallableValue cv) {
            return cv.call(args, kw, this::invokeCallable);
        }
        if (ctxVal instanceof LambdaValue lv) {
            return lv.call(args, kw, this::evaluateExpression);
        }
        if (builtins.contains(name)) {
            return builtins.get(name).apply(args, kw, this::invokeCallable);
        }
        
        FunctionDef func = program.findFunction(name);
        if (func == null) {
            throw new GrizzlyExecutionException(
                "Function '" + name + "' not found",
                lineNumber
            );
        }
        
        // Python-compliant: bind positional, *args, keyword, keyword-only
        ExecutionContext funcContext = context.createChild();
        List<String> params = func.params();
        List<Expression> defaultExprs = func.defaultExprs();
        int starParamIndex = func.starParamIndex();
        Map<String, Value> bound = new HashMap<>();
        
        int numRegular = (starParamIndex >= 0) ? starParamIndex : params.size();
        int argIdx = 0;
        for (int i = 0; i < numRegular; i++) {
            if (argIdx < args.size()) {
                bound.put(params.get(i), args.get(argIdx++));
            }
        }
        if (starParamIndex >= 0) {
            List<Value> starArgs = new ArrayList<>();
            while (argIdx < args.size()) {
                starArgs.add(args.get(argIdx++));
            }
            bound.put(params.get(starParamIndex), new ListValue(starArgs));
        } else if (argIdx < args.size()) {
            throw new GrizzlyExecutionException(
                name + "() takes " + params.size() + " positional argument(s) but " + args.size() + " were given",
                lineNumber
            );
        }
        for (Map.Entry<String, Value> e : kw.entrySet()) {
            String p = e.getKey();
            if (!params.contains(p)) {
                throw new GrizzlyExecutionException(
                    name + "() got an unexpected keyword argument '" + p + "'",
                    lineNumber
                );
            }
            if (bound.containsKey(p)) {
                throw new GrizzlyExecutionException(
                    name + "() got multiple values for argument '" + p + "'",
                    lineNumber
                );
            }
            bound.put(p, e.getValue());
        }
        for (int i = 0; i < params.size(); i++) {
            String p = params.get(i);
            if (bound.containsKey(p)) {
                funcContext.set(p, bound.get(p));
            } else if (defaultExprs != null && i < defaultExprs.size() && defaultExprs.get(i) != null) {
                funcContext.set(p, evaluateExpression(defaultExprs.get(i), context));
            } else {
                throw new GrizzlyExecutionException(
                    name + "() missing required argument: '" + p + "'",
                    lineNumber
                );
            }
        }
        
        return executeFunction(func, funcContext);
    }
    
    private Map<String, Value> evaluateKeywordArguments(Map<String, Expression> kwExprs, ExecutionContext context) {
        if (kwExprs == null || kwExprs.isEmpty()) return Map.of();
        Map<String, Value> result = new HashMap<>();
        for (Map.Entry<String, Expression> e : kwExprs.entrySet()) {
            result.put(e.getKey(), evaluateExpression(e.getValue(), context));
        }
        return result;
    }
    
    /**
     * Evaluate call arguments including *unpack and **unpack.
     * Python: f(a, *list, b=1, **dict)
     */
    private record EvaluatedCallArgs(List<Value> positional, Map<String, Value> keyword) {}
    
    private EvaluatedCallArgs evaluateCallArguments(List<CallArgument> arguments, ExecutionContext context) {
        if (arguments == null || arguments.isEmpty()) {
            return new EvaluatedCallArgs(List.of(), Map.of());
        }
        List<Value> positional = new ArrayList<>();
        Map<String, Value> keyword = new HashMap<>();
        for (CallArgument arg : arguments) {
            if (arg instanceof CallArgument.Positional pos) {
                positional.add(evaluateExpression(pos.expr(), context));
            } else if (arg instanceof CallArgument.Starred star) {
                Value val = evaluateExpression(star.expr(), context);
                positional.addAll(toIterableList(val, 0));
            } else if (arg instanceof CallArgument.Keyword kw) {
                keyword.put(kw.name(), evaluateExpression(kw.expr(), context));
            } else if (arg instanceof CallArgument.DoubleStarred ds) {
                Value val = evaluateExpression(ds.expr(), context);
                if (!(val instanceof DictValue dict)) {
                    throw new GrizzlyExecutionException(
                        "** argument must be a dict, got " + val.typeName()
                    );
                }
                for (Map.Entry<String, Value> e : dict.entries().entrySet()) {
                    keyword.put(e.getKey(), e.getValue());
                }
            }
        }
        return new EvaluatedCallArgs(positional, keyword);
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
        } catch (com.grizzly.core.exception.BreakException | com.grizzly.core.exception.ContinueException | com.grizzly.core.exception.ReturnException e) {
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
    
    private Value executeSwitch(SwitchStatement sw, ExecutionContext context) {
        try {
            Value exprValue = evaluateExpression(sw.expression(), context);
            for (SwitchStatement.CaseBranch branch : sw.caseBranches()) {
                Value caseValue = evaluateExpression(branch.value(), context);
                if (areEqual(exprValue, caseValue)) {
                    return executeBlock(branch.statements(), context);
                }
            }
            if (sw.defaultBlock() != null && !sw.defaultBlock().isEmpty()) {
                return executeBlock(sw.defaultBlock(), context);
            }
            return NullValue.INSTANCE;
        } catch (com.grizzly.core.exception.BreakException | com.grizzly.core.exception.ContinueException | com.grizzly.core.exception.ReturnException e) {
            throw e;
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error in switch statement: " + e.getMessage(),
                sw.lineNumber()
            );
        }
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
                
                // Tuple unpacking: for k, v in items -> unpack sequence to variables
                List<String> vars = forLoop.variables();
                if (vars.size() == 1) {
                    context.set(vars.get(0), item);
                } else {
                    List<Value> unpacked = toUnpackableSequence(item, forLoop.lineNumber());
                    if (unpacked.size() != vars.size()) {
                        String msg = unpacked.size() < vars.size()
                            ? "not enough values to unpack (expected " + vars.size() + ", got " + unpacked.size() + ")"
                            : "too many values to unpack (expected " + vars.size() + ", got " + unpacked.size() + ")";
                        throw new GrizzlyExecutionException(msg, forLoop.lineNumber());
                    }
                    for (int vi = 0; vi < vars.size(); vi++) {
                        context.set(vars.get(vi), unpacked.get(vi));
                    }
                }
                
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
                    } catch (com.grizzly.core.exception.ReturnException e) {
                        throw e;
                    }
                }
            }
            
            return NullValue.INSTANCE;
            
        } catch (com.grizzly.core.exception.BreakException | com.grizzly.core.exception.ContinueException e) {
            throw new GrizzlyExecutionException(
                "break/continue outside of loop at line " + forLoop.lineNumber(),
                forLoop.lineNumber()
            );
        } catch (com.grizzly.core.exception.ReturnException e) {
            throw e;
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
            if (expr instanceof NumberLiteral n) {
                return new NumberValue(n.value());
            } else if (expr instanceof StringLiteral s) {
                return new StringValue(s.value());
            } else if (expr instanceof BooleanLiteral b) {
                return BoolValue.of(b.value());
            } else if (expr instanceof NullLiteral) {
                return NullValue.INSTANCE;
            } else if (expr instanceof Identifier i) {
                return context.get(i.name());
            } else if (expr instanceof DictLiteral d) {
                return evaluateDictLiteral(d, context);
            } else if (expr instanceof DictComprehension dc) {
                return evaluateDictComprehension(dc, context);
            } else if (expr instanceof ListLiteral l) {
                return evaluateListLiteral(l, context);
            } else if (expr instanceof ListComprehension lc) {
                return evaluateListComprehension(lc, context);
            } else if (expr instanceof DictAccess d) {
                return evaluateDictAccess(d, context);
            } else if (expr instanceof SliceExpression s) {
                return evaluateSliceExpression(s, context);
            } else if (expr instanceof AttrAccess a) {
                return evaluateAttrAccess(a, context);
            } else if (expr instanceof BinaryOp b) {
                return evaluateBinaryOp(b, context);
            } else if (expr instanceof MethodCall m) {
                return evaluateMethodCall(m, context);
            } else if (expr instanceof FunctionCallExpression f) {
                return evaluateFunctionCallExpression(f, context);
            } else if (expr instanceof CallExpression c) {
                return evaluateCallExpression(c, context);
            } else if (expr instanceof ConditionalExpression c) {
                return evaluateConditionalExpression(c, context);
            } else if (expr instanceof FStringLiteral f) {
                return evaluateFStringLiteral(f, context);
            } else if (expr instanceof LambdaExpression le) {
                return new LambdaValue(le, context);
            } else {
                throw new GrizzlyExecutionException(
                    "Unknown expression type: " + expr.getClass().getSimpleName()
                );
            }
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
        
        if (obj instanceof DictValue dict) {
            Value result = dict.getOrNull(asString(keyVal));
            if (result == null) {
                return handleKeyNotFound(fullPath, asString(keyVal), safe, tracker, 0);
            }
            trackSuccess(fullPath, result, tracker, 0);
            return result;
        } else if (obj instanceof ListValue list) {
            int index = toInt(keyVal);
            int originalIndex = index;
            if (index < 0) {
                index = list.size() + index;
            }
            if (index < 0 || index >= list.size()) {
                return handleIndexOutOfBounds(fullPath, originalIndex, list.size(), safe, tracker, 0);
            }
            Value result = list.get(index);
            trackSuccess(fullPath, result, tracker, 0);
            return result;
        } else {
            if (safe || config.nullHandling() == NullHandling.SAFE || 
                config.nullHandling() == NullHandling.SILENT) {
                return NullValue.INSTANCE;
            }
            throw new GrizzlyExecutionException(
                "Cannot access key on object of type " + obj.typeName()
            );
        }
    }
    
    private Value evaluateSliceExpression(SliceExpression slice, ExecutionContext context) {
        Value obj = evaluateExpression(slice.object(), context);
        boolean safe = slice.safe();
        if (obj instanceof NullValue) {
            if (safe || config.nullHandling() == NullHandling.SAFE || config.nullHandling() == NullHandling.SILENT) {
                return NullValue.INSTANCE;
            }
            throw new GrizzlyExecutionException("Cannot slice None");
        }
        Integer start = slice.start() == null ? null : toInt(evaluateExpression(slice.start(), context));
        Integer end = slice.end() == null ? null : toInt(evaluateExpression(slice.end(), context));
        Integer step = slice.step() == null ? null : toInt(evaluateExpression(slice.step(), context));
        if (step != null && step == 0) {
            throw new GrizzlyExecutionException("slice step cannot be zero");
        }
        if (obj instanceof StringValue s) {
            return new StringValue(pythonSlice(s.value(), start, end, step));
        }
        if (obj instanceof ListValue list) {
            List<Value> items = list.items();
            return new ListValue(pythonSliceList(items, start, end, step));
        }
        if (safe || config.nullHandling() == NullHandling.SAFE || config.nullHandling() == NullHandling.SILENT) {
            return NullValue.INSTANCE;
        }
        throw new GrizzlyExecutionException("Cannot slice object of type " + obj.typeName());
    }
    
    private static String pythonSlice(String s, Integer start, Integer end, Integer step) {
        int len = s.length();
        int stp = step != null ? step : 1;
        int st = resolveSliceStart(start, len, stp);
        int en = resolveSliceEnd(end, len, stp);
        StringBuilder sb = new StringBuilder();
        if (stp > 0) {
            for (int i = st; i < en; i += stp) sb.append(s.charAt(i));
        } else {
            for (int i = st; i > en; i += stp) sb.append(s.charAt(i));
        }
        return sb.toString();
    }
    
    private static List<Value> pythonSliceList(List<Value> items, Integer start, Integer end, Integer step) {
        int len = items.size();
        int stp = step != null ? step : 1;
        int st = resolveSliceStart(start, len, stp);
        int en = resolveSliceEnd(end, len, stp);
        List<Value> result = new ArrayList<>();
        if (stp > 0) {
            for (int i = st; i < en; i += stp) result.add(items.get(i));
        } else {
            for (int i = st; i > en; i += stp) result.add(items.get(i));
        }
        return result;
    }
    
    /** Python slice start: None -> 0 if step>0 else len-1; negative adds len. */
    private static int resolveSliceStart(Integer start, int len, int step) {
        int s = start != null ? (start < 0 ? len + start : start) : (step > 0 ? 0 : len - 1);
        return Math.max(0, Math.min(s, len));
    }
    
    /** Python slice end: None -> len if step>0 else -1; negative adds len. */
    private static int resolveSliceEnd(Integer end, int len, int step) {
        if (end == null) return step > 0 ? len : -1;
        int e = end < 0 ? len + end : end;
        return step > 0 ? Math.max(0, Math.min(e, len)) : e;
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
        if (expr instanceof Identifier id) {
            return id.name();
        } else if (expr instanceof AttrAccess attr) {
            return buildExpressionPath(attr.object()) + 
                (attr.safe() ? "?." : ".") + attr.attr();
        } else if (expr instanceof DictAccess dict) {
            return buildExpressionPath(dict.object()) + 
                (dict.safe() ? "?[" : "[") + getKeyString(dict.key(), null) + "]";
        } else {
            return expr.toString();
        }
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
        String operator = binaryOp.operator();
        
        // Short-circuit evaluation for 'and' and 'or'
        if ("and".equals(operator)) {
            Value left = evaluateExpression(binaryOp.left(), context);
            if (!left.isTruthy()) {
                return left; // Short-circuit: return falsy left without evaluating right
            }
            return evaluateExpression(binaryOp.right(), context);
        }
        
        if ("or".equals(operator)) {
            Value left = evaluateExpression(binaryOp.left(), context);
            if (left.isTruthy()) {
                return left; // Short-circuit: return truthy left without evaluating right
            }
            return evaluateExpression(binaryOp.right(), context);
        }
        
        // For all other operators, evaluate both sides
        Value left = evaluateExpression(binaryOp.left(), context);
        Value right = evaluateExpression(binaryOp.right(), context);
        
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
            case "not" -> BoolValue.of(!right.isTruthy());
            case "in" -> BoolValue.of(evaluateIn(left, right));
            case "not in" -> BoolValue.of(!evaluateIn(left, right));
            default -> throw new GrizzlyExecutionException("Unknown operator: " + operator);
        };
    }
    
    private Value evaluateConditionalExpression(ConditionalExpression c, ExecutionContext context) {
        Value cond = evaluateExpression(c.condition(), context);
        if (cond.isTruthy()) {
            return evaluateExpression(c.thenExpr(), context);
        }
        return evaluateExpression(c.elseExpr(), context);
    }
    
    private Value evaluateFStringLiteral(FStringLiteral f, ExecutionContext context) {
        String raw = f.value();
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            int brace = raw.indexOf('{', i);
            if (brace < 0) {
                out.append(raw.substring(i));
                break;
            }
            out.append(raw, i, brace);
            int depth = 1;
            int j = brace + 1;
            while (j < raw.length() && depth > 0) {
                char c = raw.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                j++;
            }
            if (depth != 0) {
                throw new GrizzlyExecutionException("Unclosed '{' in f-string");
            }
            String exprSource = raw.substring(brace + 1, j - 1).trim();
            if (!exprSource.isEmpty()) {
                try {
                    Expression expr = GrizzlyParser.parseExpressionFromSource(exprSource);
                    Value val = evaluateExpression(expr, context);
                    out.append(val != null ? ValueUtils.asString(val) : "None");
                } catch (Exception e) {
                    throw new GrizzlyExecutionException("F-string interpolation failed: " + e.getMessage());
                }
            }
            i = j;
        }
        return new StringValue(out.toString());
    }
    
    private boolean evaluateIn(Value left, Value right) {
        if (right instanceof ListValue list) {
            for (Value item : list.items()) {
                if (areEqual(left, item)) {
                    return true;
                }
            }
            return false;
        } else if (right instanceof DictValue dict) {
            String key = asString(left);
            return dict.containsKey(key);
        } else if (right instanceof StringValue str) {
            String needle = asString(left);
            return str.value().contains(needle);
        } else {
            throw new GrizzlyExecutionException(
                "Cannot use 'in' operator with " + right.typeName()
            );
        }
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
    
    private ListValue evaluateListComprehension(ListComprehension lc, ExecutionContext context) {
        Value iterableVal = evaluateExpression(lc.iterable(), context);
        List<Value> items = toIterableList(iterableVal, 0);
        List<Value> result = new ArrayList<>();
        for (Value item : items) {
            context.set(lc.variable(), item);
            if (lc.condition() != null && !evaluateExpression(lc.condition(), context).isTruthy()) {
                continue;
            }
            result.add(evaluateExpression(lc.element(), context));
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
    
    private DictValue evaluateDictComprehension(DictComprehension dc, ExecutionContext context) {
        Value iterableVal = evaluateExpression(dc.iterable(), context);
        List<Value> items = toIterableList(iterableVal, 0);
        DictValue result = DictValue.empty();
        for (Value item : items) {
            context.set(dc.variable(), item);
            if (dc.condition() != null && !evaluateExpression(dc.condition(), context).isTruthy()) {
                continue;
            }
            String key = asString(evaluateExpression(dc.keyExpr(), context));
            Value value = evaluateExpression(dc.valueExpr(), context);
            result.put(key, value);
        }
        return result;
    }
    
    private Value evaluateMethodCall(MethodCall methodCall, ExecutionContext context) {
        Value obj = null;
        if (methodCall.object() instanceof Identifier id) {
            // Prefer context (e.g. "from datetime import datetime" → datetime is the class with strptime)
            Value contextVal = context.get(id.name());
            if (contextVal != null) {
                obj = contextVal;
            } else if (modules.containsModule(id.name())) {
                String moduleName = id.name();
                String methodName = methodCall.methodName();
                Map<String, BuiltinFunction> modFns = modules.getModule(moduleName);
                if (modFns != null && modFns.containsKey(methodName)) {
                    var ev = evaluateCallArguments(methodCall.arguments(), context);
                    return modFns.get(methodName).apply(ev.positional(), ev.keyword(), this::invokeCallable);
                }
                DictValue modVal = modules.getModuleValue(moduleName);
                if (modVal != null && modVal.containsKey(methodName)) {
                    Value attr = modVal.get(methodName);
                    if (attr instanceof CallableValue cv) {
                        var ev = evaluateCallArguments(methodCall.arguments(), context);
                        return cv.call(ev.positional(), ev.keyword(), this::invokeCallable);
                    }
                }
                if (modFns != null || modVal != null) {
                    throw new GrizzlyExecutionException(
                        "Module '" + moduleName + "' has no function '" + methodName + "'"
                    );
                }
            }
        }
        
        if (obj == null) {
            obj = evaluateExpression(methodCall.object(), context);
        }
        String methodName = methodCall.methodName();
        
        if (obj instanceof CallableValue cv) {
            var ev = evaluateCallArguments(methodCall.arguments(), context);
            return cv.call(ev.positional(), ev.keyword(), this::invokeCallable);
        }
        
        if (obj instanceof NullValue) {
            throw new GrizzlyExecutionException(
                "Cannot call method '" + methodName + "' on null object"
            );
        }
        
        var ev = evaluateCallArguments(methodCall.arguments(), context);
        if (obj instanceof ListValue list) {
            return evaluateListMethod(list, methodName, ev.positional(), ev.keyword(), context);
        }
        
        if (obj instanceof StringValue str) {
            return evaluateStringMethod(str, methodName, ev.positional(), context);
        }
        
        if (obj instanceof DictValue dict) {
            return evaluateDictMethod(dict, methodName, ev.positional(), context);
        }
        
        if (obj instanceof DateTimeValue dt) {
            return evaluateDateTimeMethod(dt, methodName, ev.positional(), context);
        }
        
        if (obj instanceof ReMatchValue m) {
            return evaluateReMatchMethod(m, methodName, ev.positional(), context);
        }
        
        throw new GrizzlyExecutionException(
            "Object of type " + obj.typeName() + " does not have method '" + methodName + "'"
        );
    }
    
    private Value evaluateListMethod(ListValue list, String methodName, 
                                     List<Value> args, Map<String, Value> kw, ExecutionContext context) {
        return ListMethods.evaluate(list, methodName, args, kw, this::invokeCallable);
    }
    
    private Value evaluateStringMethod(StringValue str, String methodName,
                                       List<Value> args, ExecutionContext context) {
        return StringMethods.evaluate(str, methodName, args);
    }
    
    private Value evaluateDictMethod(DictValue dict, String methodName,
                                     List<Value> args, ExecutionContext context) {
        return DictMethods.evaluate(dict, methodName, args, this::invokeCallable);
    }
    
    private Value evaluateDateTimeMethod(DateTimeValue dt, String methodName,
                                         List<Value> arguments, ExecutionContext context) {
        return switch (methodName) {
            case "strftime" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("strftime() takes exactly 1 argument, got " + arguments.size());
                }
                String fmt = asString(arguments.get(0));
                yield new StringValue(dt.format(pythonToJavaDateFormat(fmt)));
            }
            default -> throw new GrizzlyExecutionException(
                "datetime object has no attribute '" + methodName + "'"
            );
        };
    }
    
    private Value evaluateReMatchMethod(ReMatchValue m, String methodName,
                                        List<Value> arguments, ExecutionContext context) {
        return switch (methodName) {
            case "group" -> {
                int idx = arguments.isEmpty() ? 0 : toInt(arguments.get(0));
                yield m.group(idx);
            }
            case "groups" -> m.groupsList();
            default -> throw new GrizzlyExecutionException(
                "Match object has no attribute '" + methodName + "'"
            );
        };
    }
    
    private List<Value> evaluateArguments(List<Expression> arguments, ExecutionContext context) {
        List<Value> args = new ArrayList<>();
        for (Expression arg : arguments) {
            args.add(evaluateExpression(arg, context));
        }
        return args;
    }
    
    private Value evaluateFunctionCallExpression(FunctionCallExpression funcCall, ExecutionContext context) {
        var evaluated = evaluateCallArguments(funcCall.arguments(), context);
        return invokeFunction(funcCall.functionName(), evaluated.positional(), evaluated.keyword(), context, 0);
    }
    
    private Value evaluateCallExpression(CallExpression call, ExecutionContext context) {
        Value callee = evaluateExpression(call.callee(), context);
        var evaluated = evaluateCallArguments(call.arguments(), context);
        return invokeCallable(callee, evaluated.positional(), evaluated.keyword());
    }
    
    /** Invoke a callable (function or lambda) with given arguments. */
    Value invokeCallable(Value callee, List<Value> args, Map<String, Value> kw) {
        if (callee instanceof CallableValue cv) {
            return cv.call(args, kw, this::invokeCallable);
        }
        if (callee instanceof LambdaValue lv) {
            return lv.call(args, kw, this::evaluateExpression);
        }
        throw new GrizzlyExecutionException(
            "object of type " + callee.typeName() + " is not callable"
        );
    }
    
    private void setTarget(Expression target, Value value, ExecutionContext context) {
        if (target instanceof Identifier i) {
            context.set(i.name(), value);
        } else if (target instanceof DictAccess d) {
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
        } else if (target instanceof AttrAccess a) {
            Value obj = evaluateExpression(a.object(), context);
            if (obj instanceof DictValue dict) {
                dict.put(a.attr(), value);
            } else {
                throw new GrizzlyExecutionException(
                    "Cannot set attribute on object of type " + obj.typeName()
                );
            }
        } else {
            throw new GrizzlyExecutionException(
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
        if (left instanceof StringValue || right instanceof StringValue) {
            if (!(left instanceof StringValue)) {
                throw new GrizzlyExecutionException(
                    "unorderable types: " + left.typeName() + "() and " + right.typeName() + "()"
                );
            }
            if (!(right instanceof StringValue)) {
                throw new GrizzlyExecutionException(
                    "unorderable types: " + left.typeName() + "() and " + right.typeName() + "()"
                );
            }
            int cmp = ((StringValue) left).value().compareTo(((StringValue) right).value());
            return switch (operator) {
                case "<" -> cmp < 0;
                case ">" -> cmp > 0;
                case "<=" -> cmp <= 0;
                case ">=" -> cmp >= 0;
                default -> throw new GrizzlyExecutionException("Unknown comparison operator: " + operator);
            };
        }
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
        if (!(left instanceof NumberValue) || !(right instanceof NumberValue)) {
            throw new GrizzlyExecutionException(
                "unorderable types: " + left.typeName() + "() and " + right.typeName() + "()"
            );
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
     * Convert a Value to a sequence for tuple unpacking (for k, v in items).
     * Supports ListValue and StringValue. Python: each item must be iterable with matching length.
     */
    private List<Value> toUnpackableSequence(Value value, int lineNumber) {
        if (value instanceof ListValue list) {
            return list.items();
        }
        if (value instanceof StringValue str) {
            List<Value> chars = new ArrayList<>();
            for (char c : str.value().toCharArray()) {
                chars.add(new StringValue(String.valueOf(c)));
            }
            return chars;
        }
        throw new GrizzlyExecutionException(
            "cannot unpack " + value.typeName() + " object", lineNumber
        );
    }
    
    /**
     * Convert a Value to an iterable list of Values.
     * Supports lists, strings (char iteration), and dicts (key iteration).
     */
    private List<Value> toIterableList(Value value, int lineNumber) {
        if (value instanceof ListValue list) {
            return list.items();
        } else if (value instanceof StringValue str) {
            List<Value> chars = new ArrayList<>();
            for (char c : str.value().toCharArray()) {
                chars.add(new StringValue(String.valueOf(c)));
            }
            return chars;
        } else if (value instanceof DictValue dict) {
            List<Value> keys = new ArrayList<>();
            for (String key : dict.entries().keySet()) {
                keys.add(new StringValue(key));
            }
            return keys;
        } else if (value instanceof NullValue) {
            throw new GrizzlyExecutionException(
                "Cannot iterate over None", lineNumber
            );
        } else {
            throw new GrizzlyExecutionException(
                "Cannot iterate over " + value.typeName(), lineNumber
            );
        }
    }
    
    /** Convert Python strftime format (%Y, %m, etc.) to Java DateTimeFormatter pattern. */
    private static String pythonToJavaDateFormat(String fmt) {
        return fmt
            .replace("%Y", "yyyy")
            .replace("%m", "MM")
            .replace("%d", "dd")
            .replace("%H", "HH")
            .replace("%M", "mm")
            .replace("%S", "ss")
            .replace("%f", "SSSSSS")
            .replace("%y", "yy")
            .replace("%B", "MMMM")
            .replace("%b", "MMM")
            .replace("%A", "EEEE")
            .replace("%a", "EEE");
    }
}
