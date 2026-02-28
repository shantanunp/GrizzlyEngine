package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.parser.ast.*;
import com.grizzly.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    
    // Execution state for safeguards
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
            if (builtins.contains(call.functionName())) {
                List<Value> args = new ArrayList<>();
                for (Expression argExpr : call.args()) {
                    args.add(evaluateExpression(argExpr, context));
                }
                return builtins.get(call.functionName()).apply(args);
            }
            
            FunctionDef func = program.findFunction(call.functionName());
            if (func == null) {
                throw new GrizzlyExecutionException(
                    "Function '" + call.functionName() + "' not found",
                    call.lineNumber()
                );
            }
            
            ExecutionContext funcContext = new ExecutionContext();
            
            for (int i = 0; i < func.params().size(); i++) {
                Value argValue = evaluateExpression(call.args().get(i), context);
                funcContext.set(func.params().get(i), argValue);
            }
            
            return executeFunction(func, funcContext);
            
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error calling function '" + call.functionName() + "': " + e.getMessage(),
                call.lineNumber()
            );
        }
    }
    
    private Value executeIf(IfStatement ifStmt, ExecutionContext context) {
        try {
            Value conditionValue = evaluateExpression(ifStmt.condition(), context);
            
            if (conditionValue.isTruthy()) {
                for (Statement stmt : ifStmt.thenBlock()) {
                    Value result = executeStatement(stmt, context);
                    if (stmt instanceof ReturnStatement) {
                        return result;
                    }
                }
                return NullValue.INSTANCE;
            }
            
            for (IfStatement.ElifBranch elifBranch : ifStmt.elifBranches()) {
                Value elifConditionValue = evaluateExpression(elifBranch.condition(), context);
                
                if (elifConditionValue.isTruthy()) {
                    for (Statement stmt : elifBranch.statements()) {
                        Value result = executeStatement(stmt, context);
                        if (stmt instanceof ReturnStatement) {
                            return result;
                        }
                    }
                    return NullValue.INSTANCE;
                }
            }
            
            if (ifStmt.elseBlock() != null) {
                for (Statement stmt : ifStmt.elseBlock()) {
                    Value result = executeStatement(stmt, context);
                    if (stmt instanceof ReturnStatement) {
                        return result;
                    }
                }
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
            case "==" -> BoolValue.of(evaluateEquality(left, right, true));
            case "!=" -> BoolValue.of(evaluateEquality(left, right, false));
            case "<", ">", "<=", ">=" -> BoolValue.of(evaluateComparison(left, right, operator));
            case "and" -> left.isTruthy() ? right : left;
            case "or" -> left.isTruthy() ? left : right;
            case "in" -> BoolValue.of(evaluateIn(left, right));
            case "not in" -> BoolValue.of(!evaluateIn(left, right));
            default -> throw new GrizzlyExecutionException("Unknown operator: " + operator);
        };
    }
    
    private boolean evaluateIn(Value left, Value right) {
        return switch (right) {
            case ListValue list -> {
                for (Value item : list.items()) {
                    if (evaluateEquality(left, item, true)) {
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
    
    private boolean evaluateEquality(Value left, Value right, boolean checkEqual) {
        if (left instanceof NumberValue ln && right instanceof NumberValue rn) {
            boolean equal = Math.abs(ln.asDouble() - rn.asDouble()) < 1e-10;
            return checkEqual ? equal : !equal;
        }
        
        if (left instanceof StringValue ls && right instanceof NumberValue) {
            try {
                double leftNum = Double.parseDouble(ls.value());
                double rightNum = ((NumberValue) right).asDouble();
                boolean equal = Math.abs(leftNum - rightNum) < 1e-10;
                return checkEqual ? equal : !equal;
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        
        if (left instanceof NumberValue && right instanceof StringValue rs) {
            try {
                double leftNum = ((NumberValue) left).asDouble();
                double rightNum = Double.parseDouble(rs.value());
                boolean equal = Math.abs(leftNum - rightNum) < 1e-10;
                return checkEqual ? equal : !equal;
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        
        boolean equal = left.equals(right);
        return checkEqual ? equal : !equal;
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
        return switch (methodName) {
            case "append" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException(
                        "append() takes exactly 1 argument, got " + arguments.size()
                    );
                }
                Value value = evaluateExpression(arguments.get(0), context);
                list.append(value);
                yield NullValue.INSTANCE;
            }
            
            case "extend" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException(
                        "extend() takes exactly 1 argument, got " + arguments.size()
                    );
                }
                Value value = evaluateExpression(arguments.get(0), context);
                if (!(value instanceof ListValue other)) {
                    throw new GrizzlyExecutionException("extend() argument must be a list");
                }
                list.extend(other);
                yield NullValue.INSTANCE;
            }
            
            case "pop" -> {
                if (list.isEmpty()) {
                    throw new GrizzlyExecutionException("pop from empty list");
                }
                if (arguments.isEmpty()) {
                    yield list.items().remove(list.size() - 1);
                } else {
                    int index = toInt(evaluateExpression(arguments.get(0), context));
                    if (index < 0) index = list.size() + index;
                    if (index < 0 || index >= list.size()) {
                        throw new GrizzlyExecutionException("pop index out of range");
                    }
                    yield list.items().remove(index);
                }
            }
            
            case "insert" -> {
                if (arguments.size() != 2) {
                    throw new GrizzlyExecutionException(
                        "insert() takes exactly 2 arguments, got " + arguments.size()
                    );
                }
                int index = toInt(evaluateExpression(arguments.get(0), context));
                Value value = evaluateExpression(arguments.get(1), context);
                if (index < 0) index = Math.max(0, list.size() + index);
                if (index > list.size()) index = list.size();
                list.items().add(index, value);
                yield NullValue.INSTANCE;
            }
            
            case "reverse" -> {
                java.util.Collections.reverse(list.items());
                yield NullValue.INSTANCE;
            }
            
            case "sort" -> {
                list.items().sort((a, b) -> {
                    if (a instanceof NumberValue na && b instanceof NumberValue nb) {
                        return Double.compare(na.asDouble(), nb.asDouble());
                    }
                    return asString(a).compareTo(asString(b));
                });
                yield NullValue.INSTANCE;
            }
            
            case "index" -> {
                if (arguments.isEmpty()) {
                    throw new GrizzlyExecutionException("index() requires at least 1 argument");
                }
                Value needle = evaluateExpression(arguments.get(0), context);
                for (int i = 0; i < list.size(); i++) {
                    if (evaluateEquality(list.get(i), needle, true)) {
                        yield NumberValue.of(i);
                    }
                }
                throw new GrizzlyExecutionException("Value not found in list");
            }
            
            case "count" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("count() takes exactly 1 argument");
                }
                Value needle = evaluateExpression(arguments.get(0), context);
                int count = 0;
                for (Value item : list.items()) {
                    if (evaluateEquality(item, needle, true)) {
                        count++;
                    }
                }
                yield NumberValue.of(count);
            }
            
            default -> throw new GrizzlyExecutionException("Unknown list method: " + methodName);
        };
    }
    
    private Value evaluateStringMethod(StringValue str, String methodName,
                                       List<Expression> arguments, ExecutionContext context) {
        return switch (methodName) {
            case "upper" -> new StringValue(str.value().toUpperCase());
            case "lower" -> new StringValue(str.value().toLowerCase());
            case "strip" -> new StringValue(str.value().strip());
            case "lstrip" -> new StringValue(str.value().stripLeading());
            case "rstrip" -> new StringValue(str.value().stripTrailing());
            
            case "split" -> {
                String sep = arguments.isEmpty() 
                    ? null 
                    : asString(evaluateExpression(arguments.get(0), context));
                
                String[] parts = sep == null 
                    ? str.value().split("\\s+")
                    : str.value().split(java.util.regex.Pattern.quote(sep), -1);
                
                List<Value> result = new ArrayList<>();
                for (String part : parts) {
                    if (sep != null || !part.isEmpty()) {
                        result.add(new StringValue(part));
                    }
                }
                yield new ListValue(result);
            }
            
            case "join" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("join() takes exactly 1 argument");
                }
                Value arg = evaluateExpression(arguments.get(0), context);
                if (!(arg instanceof ListValue list)) {
                    throw new GrizzlyExecutionException("join() argument must be a list");
                }
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Value item : list.items()) {
                    if (!first) sb.append(str.value());
                    sb.append(asString(item));
                    first = false;
                }
                yield new StringValue(sb.toString());
            }
            
            case "replace" -> {
                if (arguments.size() != 2) {
                    throw new GrizzlyExecutionException("replace() takes exactly 2 arguments");
                }
                String old = asString(evaluateExpression(arguments.get(0), context));
                String replacement = asString(evaluateExpression(arguments.get(1), context));
                yield new StringValue(str.value().replace(old, replacement));
            }
            
            case "startswith" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("startswith() takes exactly 1 argument");
                }
                String prefix = asString(evaluateExpression(arguments.get(0), context));
                yield BoolValue.of(str.value().startsWith(prefix));
            }
            
            case "endswith" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("endswith() takes exactly 1 argument");
                }
                String suffix = asString(evaluateExpression(arguments.get(0), context));
                yield BoolValue.of(str.value().endsWith(suffix));
            }
            
            case "contains" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("contains() takes exactly 1 argument");
                }
                String substr = asString(evaluateExpression(arguments.get(0), context));
                yield BoolValue.of(str.value().contains(substr));
            }
            
            case "find" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("find() takes exactly 1 argument");
                }
                String substr = asString(evaluateExpression(arguments.get(0), context));
                yield NumberValue.of(str.value().indexOf(substr));
            }
            
            case "count" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("count() takes exactly 1 argument");
                }
                String substr = asString(evaluateExpression(arguments.get(0), context));
                int count = 0;
                int idx = 0;
                while ((idx = str.value().indexOf(substr, idx)) != -1) {
                    count++;
                    idx += substr.length();
                }
                yield NumberValue.of(count);
            }
            
            case "isdigit" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(Character::isDigit));
            case "isalpha" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(Character::isLetter));
            case "isalnum" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(Character::isLetterOrDigit));
            case "isspace" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(Character::isWhitespace));
            
            case "zfill" -> {
                if (arguments.size() != 1) {
                    throw new GrizzlyExecutionException("zfill() takes exactly 1 argument");
                }
                int width = toInt(evaluateExpression(arguments.get(0), context));
                String s = str.value();
                if (s.length() >= width) {
                    yield str;
                }
                boolean negative = s.startsWith("-");
                String digits = negative ? s.substring(1) : s;
                String padded = "0".repeat(width - s.length()) + digits;
                yield new StringValue(negative ? "-" + padded : padded);
            }
            
            default -> throw new GrizzlyExecutionException("Unknown string method: " + methodName);
        };
    }
    
    private Value evaluateFunctionCallExpression(FunctionCallExpression funcCall, ExecutionContext context) {
        if (builtins.contains(funcCall.functionName())) {
            List<Value> args = new ArrayList<>();
            for (Expression argExpr : funcCall.args()) {
                args.add(evaluateExpression(argExpr, context));
            }
            return builtins.get(funcCall.functionName()).apply(args);
        }
        
        FunctionDef func = program.findFunction(funcCall.functionName());
        if (func == null) {
            throw new GrizzlyExecutionException(
                "Function '" + funcCall.functionName() + "' not found"
            );
        }
        
        ExecutionContext funcContext = new ExecutionContext();
        
        for (int i = 0; i < func.params().size(); i++) {
            Value argValue = evaluateExpression(funcCall.args().get(i), context);
            funcContext.set(func.params().get(i), argValue);
        }
        
        return executeFunction(func, funcContext);
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
    
    // ==================== Helper Methods ====================
    
    private String asString(Value value) {
        return switch (value) {
            case StringValue s -> s.value();
            case NumberValue n -> n.toString();
            case BoolValue b -> b.toString();
            case NullValue ignored -> "None";
            default -> value.toString();
        };
    }
    
    private double toDouble(Value value) {
        return switch (value) {
            case NumberValue n -> n.asDouble();
            case StringValue s -> {
                try {
                    yield Double.parseDouble(s.value());
                } catch (NumberFormatException e) {
                    throw new GrizzlyExecutionException("Cannot convert '" + s.value() + "' to number");
                }
            }
            case DecimalValue d -> d.toDouble();
            default -> throw new GrizzlyExecutionException("Cannot convert " + value.typeName() + " to number");
        };
    }
    
    private int toInt(Value value) {
        return (int) toDouble(value);
    }
}
