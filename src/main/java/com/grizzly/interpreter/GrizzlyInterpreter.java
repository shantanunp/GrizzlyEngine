package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.parser.ast.*;
import com.grizzly.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.grizzly.interpreter.ValueUtils.*;

/**
 * Grizzly Interpreter - Executes the AST (Abstract Syntax Tree).
 * 
 * <p>Uses type-safe Value hierarchy instead of raw Object types.
 * Includes production safeguards for loop limits, recursion depth, and timeouts.
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
     * @param inputData Input data as Java Map (from JSON)
     * @return Output data as Java Map (for JSON serialization)
     */
    public Map<String, Object> execute(Map<String, Object> inputData) {
        executionStartTime = System.currentTimeMillis();
        currentRecursionDepth = 0;
        
        ExecutionContext globalContext = new ExecutionContext();
        for (ImportStatement importStmt : program.imports()) {
            executeStatement(importStmt, globalContext);
        }
        
        FunctionDef transformFunc = program.findFunction("transform");
        if (transformFunc == null) {
            throw new GrizzlyExecutionException("No 'transform' function found in template");
        }
        
        ExecutionContext context = new ExecutionContext();
        DictValue input = ValueConverter.fromJavaMap(inputData);
        context.set("INPUT", input);
        
        Value result = executeFunction(transformFunc, context);
        
        if (result instanceof DictValue dict) {
            return ValueConverter.toJavaMap(dict);
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
            case BreakStatement b -> throw new com.grizzly.exception.BreakException();
            case ContinueStatement c -> throw new com.grizzly.exception.ContinueException();
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
        
        ExecutionContext funcContext = new ExecutionContext();
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
        } catch (com.grizzly.exception.BreakException | com.grizzly.exception.ContinueException e) {
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
            
            if (!(iterableVal instanceof ListValue list)) {
                if (iterableVal instanceof NullValue) {
                    throw new GrizzlyExecutionException(
                        "Cannot iterate over null",
                        forLoop.lineNumber()
                    );
                }
                throw new GrizzlyExecutionException(
                    "Can only iterate over lists, got: " + iterableVal.typeName(),
                    forLoop.lineNumber()
                );
            }
            
            int iterations = 0;
            itemLoop: for (Value item : list) {
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
                    } catch (com.grizzly.exception.BreakException e) {
                        break itemLoop;
                    } catch (com.grizzly.exception.ContinueException e) {
                        continue itemLoop;
                    }
                }
            }
            
            return NullValue.INSTANCE;
            
        } catch (com.grizzly.exception.BreakException | com.grizzly.exception.ContinueException e) {
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
                case Identifier i -> context.get(i.name());
                case DictLiteral d -> DictValue.empty();
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
        Value keyVal = evaluateExpression(dictAccess.key(), context);
        
        return switch (obj) {
            case DictValue dict -> {
                String key = asString(keyVal);
                yield dict.get(key);
            }
            case ListValue list -> {
                int index = toInt(keyVal);
                if (index < 0) {
                    index = list.size() + index;
                }
                if (index < 0 || index >= list.size()) {
                    throw new GrizzlyExecutionException(
                        "List index out of range: " + toInt(keyVal) + " (list size: " + list.size() + ")"
                    );
                }
                yield list.get(index);
            }
            default -> throw new GrizzlyExecutionException(
                "Cannot access key on object of type " + obj.typeName()
            );
        };
    }
    
    private Value evaluateAttrAccess(AttrAccess attrAccess, ExecutionContext context) {
        Value obj = evaluateExpression(attrAccess.object(), context);
        
        if (obj instanceof DictValue dict) {
            return dict.get(attrAccess.attr());
        }
        
        throw new GrizzlyExecutionException(
            "Cannot access attribute '" + attrAccess.attr() + "' on object of type " + obj.typeName()
        );
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
    
}
