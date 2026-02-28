package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.parser.ast.*;
import com.grizzly.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grizzly Interpreter - Executes the AST (Abstract Syntax Tree)
 * 
 * <p>Uses type-safe Value hierarchy instead of raw Object types.
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
    private final Map<String, BuiltinFunction> builtinFunctions = new HashMap<>();
    private final Map<String, Map<String, BuiltinFunction>> modules = new HashMap<>();
    
    public GrizzlyInterpreter(Program program) {
        this.program = program;
        registerBuiltins();
        registerModules();
    }
    
    /**
     * Execute the transform function with input data.
     * 
     * @param inputData Input data as Java Map (from JSON)
     * @return Output data as Java Map (for JSON serialization)
     */
    public Map<String, Object> execute(Map<String, Object> inputData) {
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
    
    /**
     * Sets up all the built-in functions.
     */
    private void registerBuiltins() {
        // len() function
        builtinFunctions.put("len", (args) -> {
            if (args.size() != 1) {
                throw new GrizzlyExecutionException(
                    "len() takes exactly 1 argument, got " + args.size()
                );
            }
            
            Value val = args.get(0);
            
            return switch (val) {
                case ListValue l -> NumberValue.of(l.size());
                case DictValue d -> NumberValue.of(d.size());
                case StringValue s -> NumberValue.of(s.value().length());
                default -> throw new GrizzlyExecutionException(
                    "len() argument must be a list, dict, or string, got: " + val.typeName()
                );
            };
        });
        
        // range() function
        builtinFunctions.put("range", (args) -> {
            if (args.size() < 1 || args.size() > 3) {
                throw new GrizzlyExecutionException(
                    "range() takes 1-3 arguments, got " + args.size()
                );
            }
            
            int start, stop, step;
            
            if (args.size() == 1) {
                start = 0;
                stop = toInt(args.get(0));
                step = 1;
            } else if (args.size() == 2) {
                start = toInt(args.get(0));
                stop = toInt(args.get(1));
                step = 1;
            } else {
                start = toInt(args.get(0));
                stop = toInt(args.get(1));
                step = toInt(args.get(2));
                
                if (step == 0) {
                    throw new GrizzlyExecutionException("range() step cannot be zero");
                }
            }
            
            List<Value> result = new ArrayList<>();
            
            if (step > 0) {
                for (int i = start; i < stop; i += step) {
                    result.add(NumberValue.of(i));
                }
            } else {
                for (int i = start; i > stop; i += step) {
                    result.add(NumberValue.of(i));
                }
            }
            
            return new ListValue(result);
        });
        
        // now() function
        builtinFunctions.put("now", (args) -> {
            if (args.size() > 1) {
                throw new GrizzlyExecutionException(
                    "now() takes 0 or 1 argument (optional timezone), got " + args.size()
                );
            }
            
            if (args.isEmpty()) {
                return new DateTimeValue(java.time.ZonedDateTime.now());
            } else {
                String timezone = asString(args.get(0));
                try {
                    java.time.ZoneId zone = java.time.ZoneId.of(timezone);
                    return new DateTimeValue(java.time.ZonedDateTime.now(zone));
                } catch (Exception e) {
                    throw new GrizzlyExecutionException(
                        "Invalid timezone: " + timezone + ". Use formats like 'UTC', 'America/New_York', 'Asia/Tokyo'"
                    );
                }
            }
        });
        
        // parseDate() function
        builtinFunctions.put("parseDate", (args) -> {
            if (args.size() != 2 && args.size() != 3) {
                throw new GrizzlyExecutionException(
                    "parseDate() requires 2 or 3 arguments: (dateString, format, [timezone])"
                );
            }
            
            String dateString = asString(args.get(0));
            String format = asString(args.get(1));
            
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                
                if (args.size() == 3) {
                    String timezone = asString(args.get(2));
                    java.time.ZoneId zone = java.time.ZoneId.of(timezone);
                    
                    try {
                        java.time.ZonedDateTime parsed = java.time.ZonedDateTime.parse(dateString, formatter.withZone(zone));
                        return new DateTimeValue(parsed);
                    } catch (Exception e1) {
                        try {
                            java.time.LocalDateTime local = java.time.LocalDateTime.parse(dateString, formatter);
                            return new DateTimeValue(local.atZone(zone));
                        } catch (Exception e2) {
                            java.time.LocalDate date = java.time.LocalDate.parse(dateString, formatter);
                            return new DateTimeValue(date.atStartOfDay(zone));
                        }
                    }
                } else {
                    java.time.ZoneId zone = java.time.ZoneId.systemDefault();
                    
                    try {
                        java.time.LocalDateTime local = java.time.LocalDateTime.parse(dateString, formatter);
                        return new DateTimeValue(local.atZone(zone));
                    } catch (Exception e) {
                        java.time.LocalDate date = java.time.LocalDate.parse(dateString, formatter);
                        return new DateTimeValue(date.atStartOfDay(zone));
                    }
                }
            } catch (Exception e) {
                throw new GrizzlyExecutionException(
                    "Failed to parse date '" + dateString + "' with format '" + format + "': " + e.getMessage()
                );
            }
        });
        
        // formatDate() function
        builtinFunctions.put("formatDate", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "formatDate() requires 2 arguments: (datetime, format)"
                );
            }
            
            if (!(args.get(0) instanceof DateTimeValue dt)) {
                throw new GrizzlyExecutionException(
                    "formatDate() first argument must be a datetime, got: " + args.get(0).typeName()
                );
            }
            
            String format = asString(args.get(1));
            
            try {
                return new StringValue(dt.format(format));
            } catch (Exception e) {
                throw new GrizzlyExecutionException(
                    "Invalid date format '" + format + "': " + e.getMessage()
                );
            }
        });
        
        // addDays() function
        builtinFunctions.put("addDays", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addDays() requires 2 arguments: (datetime, days)"
                );
            }
            
            if (!(args.get(0) instanceof DateTimeValue dt)) {
                throw new GrizzlyExecutionException(
                    "addDays() first argument must be a datetime"
                );
            }
            
            long days = toLong(args.get(1));
            return dt.addDays(days);
        });
        
        // addMonths() function
        builtinFunctions.put("addMonths", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addMonths() requires 2 arguments: (datetime, months)"
                );
            }
            
            if (!(args.get(0) instanceof DateTimeValue dt)) {
                throw new GrizzlyExecutionException(
                    "addMonths() first argument must be a datetime"
                );
            }
            
            long months = toLong(args.get(1));
            return dt.addMonths(months);
        });
        
        // addYears() function
        builtinFunctions.put("addYears", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addYears() requires 2 arguments: (datetime, years)"
                );
            }
            
            if (!(args.get(0) instanceof DateTimeValue dt)) {
                throw new GrizzlyExecutionException(
                    "addYears() first argument must be a datetime"
                );
            }
            
            long years = toLong(args.get(1));
            return dt.addYears(years);
        });
        
        // addHours() function
        builtinFunctions.put("addHours", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addHours() requires 2 arguments: (datetime, hours)"
                );
            }
            
            if (!(args.get(0) instanceof DateTimeValue dt)) {
                throw new GrizzlyExecutionException(
                    "addHours() first argument must be a datetime"
                );
            }
            
            long hours = toLong(args.get(1));
            return dt.addHours(hours);
        });
        
        // addMinutes() function
        builtinFunctions.put("addMinutes", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addMinutes() requires 2 arguments: (datetime, minutes)"
                );
            }
            
            if (!(args.get(0) instanceof DateTimeValue dt)) {
                throw new GrizzlyExecutionException(
                    "addMinutes() first argument must be a datetime"
                );
            }
            
            long minutes = toLong(args.get(1));
            return dt.addMinutes(minutes);
        });
        
        // Decimal() function
        builtinFunctions.put("Decimal", (args) -> {
            if (args.size() != 1) {
                throw new GrizzlyExecutionException(
                    "Decimal() requires exactly 1 argument: Decimal(\"123.45\")"
                );
            }
            
            Value value = args.get(0);
            
            return switch (value) {
                case StringValue s -> new DecimalValue(s.value());
                case NumberValue n -> {
                    if (n.isInteger()) {
                        yield new DecimalValue(n.asInt());
                    }
                    yield new DecimalValue(String.valueOf(n.asDouble()));
                }
                default -> throw new GrizzlyExecutionException(
                    "Decimal() argument must be a string or number, got: " + value.typeName()
                );
            };
        });
        
        // round() function
        builtinFunctions.put("round", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "round() requires 2 arguments: round(decimal, places)"
                );
            }
            
            if (!(args.get(0) instanceof DecimalValue decimal)) {
                throw new GrizzlyExecutionException(
                    "round() first argument must be a Decimal, got: " + args.get(0).typeName()
                );
            }
            
            int places = toInt(args.get(1));
            return decimal.round(places);
        });
        
        // str() function
        builtinFunctions.put("str", (args) -> {
            if (args.size() != 1) {
                throw new GrizzlyExecutionException(
                    "str() requires exactly 1 argument: str(value)"
                );
            }
            
            Value value = args.get(0);
            
            return switch (value) {
                case NullValue ignored -> new StringValue("None");
                case BoolValue b -> new StringValue(b.value() ? "True" : "False");
                default -> new StringValue(value.toString());
            };
        });
    }
    
    /**
     * Register module functions (re, etc.)
     */
    private void registerModules() {
        Map<String, BuiltinFunction> reModule = new HashMap<>();
        
        // re.match()
        reModule.put("match", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "re.match() requires 2 arguments: re.match(pattern, text)"
                );
            }
            
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(text);
                
                if (m.matches()) {
                    DictValue match = DictValue.empty();
                    match.put("matched", BoolValue.TRUE);
                    match.put("value", new StringValue(text));
                    return match;
                }
                return NullValue.INSTANCE;
            } catch (java.util.regex.PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.search()
        reModule.put("search", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "re.search() requires 2 arguments: re.search(pattern, text)"
                );
            }
            
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(text);
                
                if (m.find()) {
                    DictValue match = DictValue.empty();
                    match.put("matched", BoolValue.TRUE);
                    match.put("value", new StringValue(m.group()));
                    match.put("start", NumberValue.of(m.start()));
                    match.put("end", NumberValue.of(m.end()));
                    return match;
                }
                return NullValue.INSTANCE;
            } catch (java.util.regex.PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.findall()
        reModule.put("findall", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "re.findall() requires 2 arguments: re.findall(pattern, text)"
                );
            }
            
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            List<Value> matches = new ArrayList<>();
            
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(text);
                
                while (m.find()) {
                    matches.add(new StringValue(m.group()));
                }
                
                return new ListValue(matches);
            } catch (java.util.regex.PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.sub()
        reModule.put("sub", (args) -> {
            if (args.size() != 3) {
                throw new GrizzlyExecutionException(
                    "re.sub() requires 3 arguments: re.sub(pattern, replacement, text)"
                );
            }
            
            String pattern = asString(args.get(0));
            String replacement = asString(args.get(1));
            String text = asString(args.get(2));
            
            try {
                return new StringValue(text.replaceAll(pattern, replacement));
            } catch (java.util.regex.PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.split()
        reModule.put("split", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "re.split() requires 2 arguments: re.split(pattern, text)"
                );
            }
            
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                String[] parts = text.split(pattern);
                List<Value> result = new ArrayList<>();
                for (String part : parts) {
                    result.add(new StringValue(part));
                }
                return new ListValue(result);
            } catch (java.util.regex.PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        modules.put("re", reModule);
    }
    
    private Value executeFunction(FunctionDef function, ExecutionContext context) {
        for (Statement stmt : function.body()) {
            Value result = executeStatement(stmt, context);
            
            if (stmt instanceof ReturnStatement) {
                return result;
            }
        }
        
        return NullValue.INSTANCE;
    }
    
    private Value executeStatement(Statement stmt, ExecutionContext context) {
        return switch (stmt) {
            case ImportStatement i -> {
                if (!modules.containsKey(i.moduleName())) {
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
            if (builtinFunctions.containsKey(call.functionName())) {
                List<Value> args = new ArrayList<>();
                for (Expression argExpr : call.args()) {
                    args.add(evaluateExpression(argExpr, context));
                }
                return builtinFunctions.get(call.functionName()).apply(args);
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
            
            itemLoop: for (Value item : list) {
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
            default -> throw new GrizzlyExecutionException("Unknown operator: " + operator);
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
            
            if (modules.containsKey(moduleName)) {
                Map<String, BuiltinFunction> module = modules.get(moduleName);
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
            return switch (methodName) {
                case "append" -> {
                    if (methodCall.arguments().size() != 1) {
                        throw new GrizzlyExecutionException(
                            "append() takes exactly 1 argument, got " + methodCall.arguments().size()
                        );
                    }
                    Value value = evaluateExpression(methodCall.arguments().get(0), context);
                    list.append(value);
                    yield NullValue.INSTANCE;
                }
                
                case "extend" -> {
                    if (methodCall.arguments().size() != 1) {
                        throw new GrizzlyExecutionException(
                            "extend() takes exactly 1 argument, got " + methodCall.arguments().size()
                        );
                    }
                    Value value = evaluateExpression(methodCall.arguments().get(0), context);
                    if (!(value instanceof ListValue other)) {
                        throw new GrizzlyExecutionException("extend() argument must be a list");
                    }
                    list.extend(other);
                    yield NullValue.INSTANCE;
                }
                
                default -> throw new GrizzlyExecutionException("Unknown list method: " + methodName);
            };
        }
        
        if (obj instanceof StringValue str) {
            return switch (methodName) {
                case "upper" -> new StringValue(str.value().toUpperCase());
                case "lower" -> new StringValue(str.value().toLowerCase());
                case "strip" -> new StringValue(str.value().strip());
                default -> throw new GrizzlyExecutionException("Unknown string method: " + methodName);
            };
        }
        
        throw new GrizzlyExecutionException(
            "Object of type " + obj.typeName() + " does not have method '" + methodName + "'"
        );
    }
    
    private Value evaluateFunctionCallExpression(FunctionCallExpression funcCall, ExecutionContext context) {
        if (builtinFunctions.containsKey(funcCall.functionName())) {
            List<Value> args = new ArrayList<>();
            for (Expression argExpr : funcCall.args()) {
                args.add(evaluateExpression(argExpr, context));
            }
            return builtinFunctions.get(funcCall.functionName()).apply(args);
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
    
    private long toLong(Value value) {
        return (long) toDouble(value);
    }
}
