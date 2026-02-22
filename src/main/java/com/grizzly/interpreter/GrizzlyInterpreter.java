package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.parser.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grizzly Interpreter - Executes the AST (Abstract Syntax Tree)
 * WITH COMPLETE LIST AND FOR LOOP SUPPORT
 */
public class GrizzlyInterpreter {
    
    @FunctionalInterface
    private interface BuiltinFunction {
        Object apply(List<Object> args);
    }
    
    private final Program program;
    private final Map<String, BuiltinFunction> builtinFunctions = new HashMap<>();
    
    public GrizzlyInterpreter(Program program) {
        this.program = program;
        registerBuiltins();
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> inputData) {
        FunctionDef transformFunc = program.findFunction("transform");
        if (transformFunc == null) {
            throw new GrizzlyExecutionException("No 'transform' function found in template");
        }
        
        ExecutionContext context = new ExecutionContext();
        context.set("INPUT", inputData);
        
        Object result = executeFunction(transformFunc, context);
        
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        
        throw new GrizzlyExecutionException("transform() must return a Map, got: " + 
            (result != null ? result.getClass().getSimpleName() : "null"));
    }
    
    /**
     * Register all built-in functions available to templates.
     * 
     * <p><b>Currently available:</b>
     * <ul>
     *   <li>{@code len(object)} - Get size/length
     *     <ul>
     *       <li>For lists: {@code len([1,2,3])} → 3</li>
     *       <li>For strings: {@code len("hello")} → 5</li>
     *       <li>For dicts: {@code len({"a":1, "b":2})} → 2</li>
     *     </ul>
     *   </li>
     *   <li>{@code range(stop)} or {@code range(start, stop)} or {@code range(start, stop, step)} - Generate number sequences
     *     <ul>
     *       <li>{@code range(5)} → [0, 1, 2, 3, 4]</li>
     *       <li>{@code range(2, 7)} → [2, 3, 4, 5, 6]</li>
     *       <li>{@code range(0, 10, 2)} → [0, 2, 4, 6, 8]</li>
     *       <li>{@code range(10, 0, -2)} → [10, 8, 6, 4, 2]</li>
     *     </ul>
     *   </li>
     *   <li>{@code now()} or {@code now(timezone)} - Get current datetime
     *     <ul>
     *       <li>{@code now()} → Current datetime in system timezone</li>
     *       <li>{@code now("UTC")} → Current datetime in UTC</li>
     *       <li>{@code now("America/New_York")} → Current datetime in EST/EDT</li>
     *     </ul>
     *   </li>
     *   <li>{@code parseDate(dateString, format)} - Parse string to datetime
     *     <ul>
     *       <li>{@code parseDate("2024-02-22", "yyyy-MM-dd")}</li>
     *       <li>{@code parseDate("22/02/2024", "dd/MM/yyyy")}</li>
     *     </ul>
     *   </li>
     *   <li>{@code formatDate(datetime, format)} - Format datetime to string
     *     <ul>
     *       <li>{@code formatDate(now(), "yyyyMMdd")} → "20240222"</li>
     *       <li>{@code formatDate(now(), "dd/MM/yyyy")} → "22/02/2024"</li>
     *     </ul>
     *   </li>
     *   <li>{@code addDays(datetime, days)} - Add days to datetime</li>
     *   <li>{@code addMonths(datetime, months)} - Add months to datetime</li>
     *   <li>{@code addYears(datetime, years)} - Add years to datetime</li>
     *   <li>{@code addHours(datetime, hours)} - Add hours to datetime</li>
     *   <li>{@code addMinutes(datetime, minutes)} - Add minutes to datetime</li>
     * </ul>
     * 
     * <p><b>Adding new built-ins:</b>
     * <pre>{@code
     * builtinFunctions.put("max", (args) -> {
     *     // Validate args, implement logic, return result
     * });
     * }</pre>
     */
    private void registerBuiltins() {
        // len() function
        builtinFunctions.put("len", (args) -> {
            if (args.size() != 1) {
                throw new GrizzlyExecutionException(
                    "len() takes exactly 1 argument, got " + args.size()
                );
            }
            
            Object obj = args.get(0);
            
            if (obj instanceof List) {
                return ((List<?>) obj).size();
            } else if (obj instanceof Map) {
                return ((Map<?, ?>) obj).size();
            } else if (obj instanceof String) {
                return ((String) obj).length();
            } else {
                throw new GrizzlyExecutionException(
                    "len() argument must be a list, dict, or string, got: " + 
                    (obj != null ? obj.getClass().getSimpleName() : "null")
                );
            }
        });
        
        // range() function - generates sequences of numbers
        builtinFunctions.put("range", (args) -> {
            if (args.size() < 1 || args.size() > 3) {
                throw new GrizzlyExecutionException(
                    "range() takes 1-3 arguments, got " + args.size()
                );
            }
            
            int start, stop, step;
            
            if (args.size() == 1) {
                // range(stop)
                start = 0;
                stop = (int) toNumber(args.get(0));
                step = 1;
            } else if (args.size() == 2) {
                // range(start, stop)
                start = (int) toNumber(args.get(0));
                stop = (int) toNumber(args.get(1));
                step = 1;
            } else {
                // range(start, stop, step)
                start = (int) toNumber(args.get(0));
                stop = (int) toNumber(args.get(1));
                step = (int) toNumber(args.get(2));
                
                if (step == 0) {
                    throw new GrizzlyExecutionException("range() step cannot be zero");
                }
            }
            
            List<Object> result = new ArrayList<>();
            
            if (step > 0) {
                // Positive step: increment
                for (int i = start; i < stop; i += step) {
                    result.add(i);
                }
            } else {
                // Negative step: decrement
                for (int i = start; i > stop; i += step) {
                    result.add(i);
                }
            }
            
            return result;
        });
        
        // now() function - current datetime
        builtinFunctions.put("now", (args) -> {
            if (args.size() > 1) {
                throw new GrizzlyExecutionException(
                    "now() takes 0 or 1 argument (optional timezone), got " + args.size()
                );
            }
            
            if (args.isEmpty()) {
                return new com.grizzly.types.DateTimeValue(java.time.ZonedDateTime.now());
            } else {
                String timezone = args.get(0).toString();
                try {
                    java.time.ZoneId zone = java.time.ZoneId.of(timezone);
                    return new com.grizzly.types.DateTimeValue(java.time.ZonedDateTime.now(zone));
                } catch (Exception e) {
                    throw new GrizzlyExecutionException(
                        "Invalid timezone: " + timezone + ". Use formats like 'UTC', 'America/New_York', 'Asia/Tokyo'"
                    );
                }
            }
        });
        
        // parseDate() function - parse string to datetime
        builtinFunctions.put("parseDate", (args) -> {
            if (args.size() != 2 && args.size() != 3) {
                throw new GrizzlyExecutionException(
                    "parseDate() requires 2 or 3 arguments: (dateString, format, [timezone])"
                );
            }
            
            String dateString = args.get(0).toString();
            String format = args.get(1).toString();
            
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                
                if (args.size() == 3) {
                    // With timezone
                    String timezone = args.get(2).toString();
                    java.time.ZoneId zone = java.time.ZoneId.of(timezone);
                    
                    // Try LocalDateTime first, then LocalDate
                    try {
                        java.time.ZonedDateTime parsed = java.time.ZonedDateTime.parse(dateString, formatter.withZone(zone));
                        return new com.grizzly.types.DateTimeValue(parsed);
                    } catch (Exception e1) {
                        try {
                            java.time.LocalDateTime local = java.time.LocalDateTime.parse(dateString, formatter);
                            return new com.grizzly.types.DateTimeValue(local.atZone(zone));
                        } catch (Exception e2) {
                            java.time.LocalDate date = java.time.LocalDate.parse(dateString, formatter);
                            return new com.grizzly.types.DateTimeValue(date.atStartOfDay(zone));
                        }
                    }
                } else {
                    // Without timezone - use system default
                    java.time.ZoneId zone = java.time.ZoneId.systemDefault();
                    
                    // Try LocalDateTime first, then LocalDate
                    try {
                        java.time.LocalDateTime local = java.time.LocalDateTime.parse(dateString, formatter);
                        return new com.grizzly.types.DateTimeValue(local.atZone(zone));
                    } catch (Exception e) {
                        // Fall back to LocalDate (date-only format like yyyy-MM-dd)
                        java.time.LocalDate date = java.time.LocalDate.parse(dateString, formatter);
                        return new com.grizzly.types.DateTimeValue(date.atStartOfDay(zone));
                    }
                }
            } catch (Exception e) {
                throw new GrizzlyExecutionException(
                    "Failed to parse date '" + dateString + "' with format '" + format + "': " + e.getMessage()
                );
            }
        });
        
        // formatDate() function - format datetime to string
        builtinFunctions.put("formatDate", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "formatDate() requires 2 arguments: (datetime, format)"
                );
            }
            
            if (!(args.get(0) instanceof com.grizzly.types.DateTimeValue)) {
                throw new GrizzlyExecutionException(
                    "formatDate() first argument must be a datetime, got: " + args.get(0).getClass().getSimpleName()
                );
            }
            
            com.grizzly.types.DateTimeValue dt = (com.grizzly.types.DateTimeValue) args.get(0);
            String format = args.get(1).toString();
            
            try {
                return dt.format(format);
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
            
            if (!(args.get(0) instanceof com.grizzly.types.DateTimeValue)) {
                throw new GrizzlyExecutionException(
                    "addDays() first argument must be a datetime"
                );
            }
            
            com.grizzly.types.DateTimeValue dt = (com.grizzly.types.DateTimeValue) args.get(0);
            long days = (long) toNumber(args.get(1));
            return dt.addDays(days);
        });
        
        // addMonths() function
        builtinFunctions.put("addMonths", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addMonths() requires 2 arguments: (datetime, months)"
                );
            }
            
            if (!(args.get(0) instanceof com.grizzly.types.DateTimeValue)) {
                throw new GrizzlyExecutionException(
                    "addMonths() first argument must be a datetime"
                );
            }
            
            com.grizzly.types.DateTimeValue dt = (com.grizzly.types.DateTimeValue) args.get(0);
            long months = (long) toNumber(args.get(1));
            return dt.addMonths(months);
        });
        
        // addYears() function
        builtinFunctions.put("addYears", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addYears() requires 2 arguments: (datetime, years)"
                );
            }
            
            if (!(args.get(0) instanceof com.grizzly.types.DateTimeValue)) {
                throw new GrizzlyExecutionException(
                    "addYears() first argument must be a datetime"
                );
            }
            
            com.grizzly.types.DateTimeValue dt = (com.grizzly.types.DateTimeValue) args.get(0);
            long years = (long) toNumber(args.get(1));
            return dt.addYears(years);
        });
        
        // addHours() function
        builtinFunctions.put("addHours", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addHours() requires 2 arguments: (datetime, hours)"
                );
            }
            
            if (!(args.get(0) instanceof com.grizzly.types.DateTimeValue)) {
                throw new GrizzlyExecutionException(
                    "addHours() first argument must be a datetime"
                );
            }
            
            com.grizzly.types.DateTimeValue dt = (com.grizzly.types.DateTimeValue) args.get(0);
            long hours = (long) toNumber(args.get(1));
            return dt.addHours(hours);
        });
        
        // addMinutes() function
        builtinFunctions.put("addMinutes", (args) -> {
            if (args.size() != 2) {
                throw new GrizzlyExecutionException(
                    "addMinutes() requires 2 arguments: (datetime, minutes)"
                );
            }
            
            if (!(args.get(0) instanceof com.grizzly.types.DateTimeValue)) {
                throw new GrizzlyExecutionException(
                    "addMinutes() first argument must be a datetime"
                );
            }
            
            com.grizzly.types.DateTimeValue dt = (com.grizzly.types.DateTimeValue) args.get(0);
            long minutes = (long) toNumber(args.get(1));
            return dt.addMinutes(minutes);
        });
    }
    
    private Object executeFunction(FunctionDef function, ExecutionContext context) {
        for (Statement stmt : function.body()) {
            Object result = executeStatement(stmt, context);
            
            if (stmt instanceof ReturnStatement) {
                return result;
            }
        }
        
        return null;
    }
    
    private Object executeStatement(Statement stmt, ExecutionContext context) {
        return switch (stmt) {
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
    
    private Object executeAssignment(Assignment assignment, ExecutionContext context) {
        try {
            Object value = evaluateExpression(assignment.value(), context);
            setTarget(assignment.target(), value, context);
            return value;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error in assignment: " + e.getMessage(),
                assignment.lineNumber()
            );
        }
    }
    
    /**
     * Execute a function call - either built-in or user-defined.
     * 
     * <p><b>Checks built-ins first!</b>
     * <pre>{@code
     * len(items)
     * 
     * Steps:
     * 1. Check: Is "len" a built-in? YES!
     * 2. Evaluate arguments: items → [1, 2, 3]
     * 3. Call built-in len function
     * 4. Return: 3
     * }</pre>
     * 
     * <p><b>User-defined function:</b>
     * <pre>{@code
     * result = helper(INPUT)
     * 
     * Steps:
     * 1. Check built-ins: Not found
     * 2. Find "helper" function in program
     * 3. Create NEW context (fresh variable space)
     * 4. Evaluate INPUT → map object
     * 5. Bind: param name → INPUT value
     * 6. Execute helper's body in new context
     * 7. Return helper's return value
     * }</pre>
     * 
     * <p><b>Why new context?</b> Function isolation!
     * <pre>{@code
     * x = 10
     * 
     * def helper(INPUT):
     *     x = 20        // This x is separate!
     *     return x
     * 
     * result = helper(INPUT)  // result = 20
     * print(x)                // x still = 10 (unchanged!)
     * }</pre>
     * 
     * <p><b>Multiple parameters:</b>
     * <pre>{@code
     * def process(data, mode):
     *     ...
     * 
     * process(INPUT, "fast")
     * 
     * Binds:
     * data → INPUT value
     * mode → "fast"
     * }</pre>
     * 
     * @param call The FunctionCall AST node (function name and arguments)
     * @param context Current execution context
     * @return The function's return value
     */
    private Object executeFunctionCall(FunctionCall call, ExecutionContext context) {
        try {
            // Check if it's a builtin function
            if (builtinFunctions.containsKey(call.functionName())) {
                List<Object> args = new ArrayList<>();
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
                Object argValue = evaluateExpression(call.args().get(i), context);
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
    
    /**
     * Execute an if/else statement - choose which branch to run.
     * 
     * <p><b>Simple if (no else):</b>
     * <pre>{@code
     * if age >= 18:
     *     status = "adult"
     * 
     * Steps:
     * 1. Evaluate condition: age >= 18 → true or false
     * 2. If true: execute "status = adult"
     * 3. If false: skip the block
     * 4. Continue after if
     * }</pre>
     * 
     * <p><b>If with else:</b>
     * <pre>{@code
     * if age >= 18:
     *     status = "adult"
     * else:
     *     status = "minor"
     * 
     * Steps:
     * 1. Evaluate: age >= 18
     * 2. If true: run "adult" block, skip else
     * 3. If false: skip if block, run "minor" block
     * }</pre>
     * 
     * <p><b>Early return in if block:</b>
     * <pre>{@code
     * if found:
     *     return result  // Exits immediately!
     * more = code        // This won't run if we returned
     * }</pre>
     * 
     * <p><b>How conditions become true/false:</b>
     * <ul>
     *   <li>{@code null} → false</li>
     *   <li>{@code true/false} → itself</li>
     *   <li>{@code 0} → false, any other number → true</li>
     *   <li>{@code ""} (empty string) → false, any text → true</li>
     * </ul>
     * 
     * @param ifStmt The IfStatement AST node (condition, then block, else block)
     * @param context Execution context
     * @return null normally, or return value if block contains return statement
     */
    /**
     * Execute an if/elif/else statement - choose which branch to run.
     * 
     * <p><b>Simple if (no else):</b>
     * <pre>{@code
     * if age >= 18:
     *     status = "adult"
     * 
     * Steps:
     * 1. Evaluate condition: age >= 18 → true or false
     * 2. If true: execute "status = adult"
     * 3. If false: skip the block
     * 4. Continue after if
     * }</pre>
     * 
     * <p><b>If with elif (NEW!):</b>
     * <pre>{@code
     * if x < 0:
     *     status = "negative"
     * elif x == 0:
     *     status = "zero"
     * elif x == 1:
     *     status = "one"
     * else:
     *     status = "many"
     * 
     * Steps:
     * 1. Evaluate first condition: x < 0
     * 2. If true: run "negative" block, SKIP all elif/else
     * 3. If false: try first elif: x == 0
     * 4. If true: run "zero" block, SKIP remaining elif/else
     * 5. If false: try second elif: x == 1
     * 6. If true: run "one" block, SKIP else
     * 7. If false: run else block
     * }</pre>
     * 
     * <p><b>Early return in any branch:</b>
     * <pre>{@code
     * if found:
     *     return result  // Exits immediately!
     * elif fallback:
     *     return default // Also exits immediately!
     * }</pre>
     * 
     * @param ifStmt The IfStatement AST node
     * @param context Execution context
     * @return null normally, or return value if block contains return statement
     */
    private Object executeIf(IfStatement ifStmt, ExecutionContext context) {
        try {
            // Try main if condition
            Object conditionValue = evaluateExpression(ifStmt.condition(), context);
            boolean condition = isTrue(conditionValue);
            
            if (condition) {
                // Execute if block
                for (Statement stmt : ifStmt.thenBlock()) {
                    Object result = executeStatement(stmt, context);
                    if (stmt instanceof ReturnStatement) {
                        return result;
                    }
                }
                return null; // Executed if block, skip elif/else
            }
            
            // Try elif branches (if any)
            for (IfStatement.ElifBranch elifBranch : ifStmt.elifBranches()) {
                Object elifConditionValue = evaluateExpression(elifBranch.condition(), context);
                boolean elifCondition = isTrue(elifConditionValue);
                
                if (elifCondition) {
                    // Execute this elif block
                    for (Statement stmt : elifBranch.statements()) {
                        Object result = executeStatement(stmt, context);
                        if (stmt instanceof ReturnStatement) {
                            return result;
                        }
                    }
                    return null; // Executed elif block, skip remaining elif/else
                }
            }
            
            // Execute else block (if present and no if/elif matched)
            if (ifStmt.elseBlock() != null) {
                for (Statement stmt : ifStmt.elseBlock()) {
                    Object result = executeStatement(stmt, context);
                    if (stmt instanceof ReturnStatement) {
                        return result;
                    }
                }
            }
            
            return null;
        } catch (com.grizzly.exception.BreakException | com.grizzly.exception.ContinueException e) {
            // Let these pass through to the enclosing loop
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
    
    /**
     * Execute a for loop: {@code for item in items:}
     * 
     * <p>Iterates over a list, executing the loop body once for each element.
     * The loop variable is set to each element in sequence.
     * 
     * <p><b>Examples:</b>
     * <pre>{@code
     * // Simple iteration
     * for customer in INPUT.customers:
     *     OUTPUT["names"].append(customer.name)
     * // Sets customer to each element, executes body
     * 
     * // Early return
     * for item in items:
     *     if item.id == searchId:
     *         return item  // Exits loop immediately
     * 
     * // Nested loops
     * for dept in INPUT.departments:
     *     for emp in dept.employees:
     *         OUTPUT["all"].append(emp.name)
     * }</pre>
     * 
     * <p><b>Execution flow:</b>
     * <ol>
     *   <li>Evaluate iterable expression to get list</li>
     *   <li>For each item: set variable in context, execute body</li>
     *   <li>If return statement encountered, propagate return value</li>
     * </ol>
     * 
     * @param forLoop The ForLoop AST node
     * @param context Execution context for variable storage
     * @return null if loop completes, or return value if body contains return
     * @throws GrizzlyExecutionException if iterable is invalid
     */
    /**
     * Execute a for loop with support for break and continue.
     * 
     * <p><b>Break example:</b>
     * <pre>{@code
     * for item in items:
     *     if item.id == searchId:
     *         break  // Exit loop immediately
     * }</pre>
     * 
     * <p><b>Continue example:</b>
     * <pre>{@code
     * for num in range(10):
     *     if num % 2 == 0:
     *         continue  // Skip even numbers
     *     OUTPUT["odds"].append(num)
     * }</pre>
     * 
     * @param forLoop The ForLoop AST node
     * @param context Execution context
     * @return null if loop completes, or return value if body contains return
     */
    private Object executeForLoop(ForLoop forLoop, ExecutionContext context) {
        try {
            Object iterableObj = evaluateExpression(forLoop.iterable(), context);
            
            if (iterableObj == null) {
                throw new GrizzlyExecutionException(
                    "Cannot iterate over null",
                    forLoop.lineNumber()
                );
            }
            
            List<?> items;
            if (iterableObj instanceof List) {
                items = (List<?>) iterableObj;
            } else {
                throw new GrizzlyExecutionException(
                    "Can only iterate over lists, got: " + iterableObj.getClass().getSimpleName(),
                    forLoop.lineNumber()
                );
            }
            
            // Iterate over items - labeled for break/continue
            itemLoop: for (Object item : items) {
                context.set(forLoop.variable(), item);
                
                // Execute loop body statements
                for (Statement stmt : forLoop.body()) {
                    try {
                        Object result = executeStatement(stmt, context);
                        
                        // Handle return statements
                        if (stmt instanceof ReturnStatement) {
                            return result;
                        }
                    } catch (com.grizzly.exception.BreakException e) {
                        // Break out of item loop
                        break itemLoop;
                    } catch (com.grizzly.exception.ContinueException e) {
                        // Continue to next item
                        continue itemLoop;
                    }
                }
            }
            
            return null;
            
        } catch (com.grizzly.exception.BreakException | com.grizzly.exception.ContinueException e) {
            // These should have been caught by the loop above
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
    
    private Object evaluateExpression(Expression expr, ExecutionContext context) {
        try {
            return switch (expr) {
                case NumberLiteral n -> n.value();
                case StringLiteral s -> s.value();
                case Identifier i -> context.get(i.name());
                case DictLiteral d -> new HashMap<String, Object>();
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
    
    /**
     * Get a value from a dict or list using bracket notation.
     * 
     * <p><b>Works for both dicts AND lists!</b>
     * 
     * <p><b>Dict example:</b>
     * <pre>{@code
     * value = OUTPUT["name"]
     * 
     * Steps:
     * 1. Evaluate OUTPUT → get the dict object
     * 2. Evaluate "name" → get string "name"
     * 3. Call dict.get("name") → return value
     * }</pre>
     * 
     * <p><b>List with positive index:</b>
     * <pre>{@code
     * first = items[0]    // First element
     * second = items[1]   // Second element
     * }</pre>
     * 
     * <p><b>List with NEGATIVE index (Python-style!):</b>
     * <pre>{@code
     * last = items[-1]      // Last element
     * secondLast = items[-2] // Second-to-last element
     * 
     * How it works:
     * items = ["a", "b", "c", "d"]
     * items[-1] → items[4 + (-1)] → items[3] → "d"
     * items[-2] → items[4 + (-2)] → items[2] → "c"
     * }</pre>
     * 
     * <p><b>Chained access:</b>
     * <pre>{@code
     * city = INPUT["user"]["address"]["city"]
     * 
     * Steps:
     * 1. Get INPUT["user"] → dict
     * 2. Get that["address"] → dict
     * 3. Get that["city"] → value
     * }</pre>
     * 
     * @param dictAccess The DictAccess AST node (object and key expressions)
     * @param context Execution context
     * @return The value at that key/index
     */
    @SuppressWarnings("unchecked")
    private Object evaluateDictAccess(DictAccess dictAccess, ExecutionContext context) {
        Object obj = evaluateExpression(dictAccess.object(), context);
        Object key = evaluateExpression(dictAccess.key(), context);
        
        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).get(key.toString());
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            int index = ((Number) toNumber(key)).intValue();
            
            // Handle negative indices: -1 = last, -2 = second-to-last, etc.
            if (index < 0) {
                index = list.size() + index;
            }
            
            // Bounds check
            if (index < 0 || index >= list.size()) {
                throw new GrizzlyExecutionException(
                    "List index out of range: " + ((Number) toNumber(key)).intValue() + 
                    " (list size: " + list.size() + ")"
                );
            }
            
            return list.get(index);
        }
        
        throw new GrizzlyExecutionException(
            "Cannot access key '" + key + "' on object of type " + 
            (obj != null ? obj.getClass().getSimpleName() : "null")
        );
    }
    
    @SuppressWarnings("unchecked")
    private Object evaluateAttrAccess(AttrAccess attrAccess, ExecutionContext context) {
        Object obj = evaluateExpression(attrAccess.object(), context);
        
        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).get(attrAccess.attr());
        }
        
        throw new GrizzlyExecutionException(
            "Cannot access attribute '" + attrAccess.attr() + "' on object of type " +
            (obj != null ? obj.getClass().getSimpleName() : "null")
        );
    }
    
    /**
     * Evaluate binary operations: +, -, *, /, //, %, **, ==, !=, <, >, <=, >=
     * 
     * <p><b>Smart behavior:</b>
     * <ul>
     *   <li>+ with strings → concatenation: {@code "Hi" + "!" = "Hi!"}</li>
     *   <li>+ with lists → concatenation: {@code [1,2] + [3,4] = [1,2,3,4]}</li>
     *   <li>+ with numbers → addition: {@code 5 + 3 = 8}</li>
     *   <li>* with string and number → repetition: {@code 3 * "ha" = "hahaha"}</li>
     *   <li>* with list and number → repetition: {@code 2 * [1,2] = [1,2,1,2]}</li>
     *   <li>* with numbers → multiplication: {@code 5 * 3 = 15}</li>
     * </ul>
     * 
     * <p><b>Math examples:</b>
     * <pre>{@code
     * 2 + 3 * 4    → 14 (precedence handled by parser)
     * 10 - 5       → 5
     * 8 / 5        → 1.6 (division always returns float)
     * 17 // 3      → 5 (floor division)
     * 17 % 3       → 2 (remainder)
     * 5 ** 2       → 25 (power)
     * }</pre>
     * 
     * @param binaryOp The BinaryOp AST node
     * @param context Execution context
     * @return Result of the operation
     */
    private Object evaluateBinaryOp(BinaryOp binaryOp, ExecutionContext context) {
        Object left = evaluateExpression(binaryOp.left(), context);
        Object right = evaluateExpression(binaryOp.right(), context);
        
        String operator = binaryOp.operator();
        
        return switch (operator) {
            case "+" -> evaluatePlus(left, right);
            case "-" -> evaluateNumericOp(left, right, "-");
            case "*" -> evaluateStar(left, right);
            case "/" -> evaluateNumericOp(left, right, "/");
            case "//" -> evaluateNumericOp(left, right, "//");
            case "%" -> evaluateNumericOp(left, right, "%");
            case "**" -> evaluateNumericOp(left, right, "**");
            case "==" -> evaluateEquality(left, right, true);
            case "!=" -> evaluateEquality(left, right, false);
            case "<", ">", "<=", ">=" -> evaluateComparison(left, right, operator);
            default -> throw new GrizzlyExecutionException("Unknown operator: " + operator);
        };
    }
    
    /**
     * Evaluate equality (== or !=) with numeric type coercion.
     * 
     * <p>Handles numeric comparisons properly: 0 == 0.0, "5" == 5, etc.
     * 
     * @param left Left operand
     * @param right Right operand  
     * @param checkEqual true for ==, false for !=
     * @return boolean result
     */
    private boolean evaluateEquality(Object left, Object right, boolean checkEqual) {
        // Try numeric comparison if both could be numbers
        boolean leftIsNumeric = left instanceof Number || 
                               (left instanceof String && isNumericString((String) left));
        boolean rightIsNumeric = right instanceof Number || 
                                (right instanceof String && isNumericString((String) right));
        
        if (leftIsNumeric && rightIsNumeric) {
            try {
                double leftNum = toNumber(left);
                double rightNum = toNumber(right);
                boolean equal = Math.abs(leftNum - rightNum) < 1e-10;
                return checkEqual ? equal : !equal;
            } catch (Exception e) {
                // Fall through to object equality
            }
        }
        
        // Regular object equality
        boolean equal = java.util.Objects.equals(left, right);
        return checkEqual ? equal : !equal;
    }
    
    /**
     * Check if a string represents a number.
     */
    private boolean isNumericString(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Handle + operator: concatenation for strings/lists, addition for numbers
     */
    @SuppressWarnings("unchecked")
    private Object evaluatePlus(Object left, Object right) {
        // String concatenation
        if (left instanceof String || right instanceof String) {
            return left.toString() + right.toString();
        }
        
        // List concatenation: [1,2] + [3,4] = [1,2,3,4]
        if (left instanceof List && right instanceof List) {
            List<Object> result = new ArrayList<>((List<?>) left);
            result.addAll((List<?>) right);
            return result;
        }
        
        // Numeric addition
        return evaluateNumericOp(left, right, "+");
    }
    
    /**
     * Handle * operator: repetition for strings/lists, multiplication for numbers
     */
    @SuppressWarnings("unchecked")
    private Object evaluateStar(Object left, Object right) {
        // String repetition: 3 * "ha" = "hahaha"
        if (left instanceof Number && right instanceof String) {
            int count = ((Number) left).intValue();
            return ((String) right).repeat(Math.max(0, count));
        }
        if (left instanceof String && right instanceof Number) {
            int count = ((Number) right).intValue();
            return ((String) left).repeat(Math.max(0, count));
        }
        
        // List repetition: 2 * [1,2] = [1,2,1,2]
        if (left instanceof Number && right instanceof List) {
            int count = ((Number) left).intValue();
            List<Object> list = (List<Object>) right;
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < Math.max(0, count); i++) {
                result.addAll(list);
            }
            return result;
        }
        if (left instanceof List && right instanceof Number) {
            int count = ((Number) right).intValue();
            List<Object> list = (List<Object>) left;
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < Math.max(0, count); i++) {
                result.addAll(list);
            }
            return result;
        }
        
        // Numeric multiplication
        return evaluateNumericOp(left, right, "*");
    }
    
    /**
     * Evaluate a list literal expression to create a new ArrayList.
     * 
     * <p>Evaluates each element expression and collects them into a mutable ArrayList.
     * 
     * <p><b>Examples:</b>
     * <pre>{@code
     * items = []                    // Result: new ArrayList<>()
     * numbers = [1, 2, 3]          // Result: ArrayList ["1", "2", "3"]
     * values = [INPUT.x, INPUT.y]  // Result: ArrayList with evaluated values
     * lengths = [len("hi"), len("bye")] // Result: ArrayList [2, 3]
     * }</pre>
     * 
     * <p>Elements are evaluated left-to-right. The returned list is mutable.
     * 
     * @param listLiteral The ListLiteral AST node
     * @param context Execution context for evaluating elements
     * @return New mutable ArrayList containing evaluated elements
     * @throws GrizzlyExecutionException if element evaluation fails
     */
    private List<Object> evaluateListLiteral(ListLiteral listLiteral, ExecutionContext context) {
        List<Object> result = new ArrayList<>();
        
        for (Expression element : listLiteral.elements()) {
            Object value = evaluateExpression(element, context);
            result.add(value);
        }
        
        return result;
    }
    
    /**
     * Evaluate a method call expression: {@code object.method(args)}.
     * 
     * <p>Supports list methods and string methods.
     * 
     * <p><b>List Methods:</b>
     * <ul>
     *   <li>{@code append(value)} - Add element: {@code items.append("x")} → returns null</li>
     *   <li>{@code extend(list)} - Add all: {@code items.extend([1,2,3])} → returns null</li>
     * </ul>
     * 
     * <p><b>String Methods:</b>
     * <ul>
     *   <li>{@code upper()} - Uppercase: {@code "hi".upper()} → "HI"</li>
     *   <li>{@code lower()} - Lowercase: {@code "HI".lower()} → "hi"</li>
     *   <li>{@code strip()} - Trim: {@code " hi ".strip()} → "hi"</li>
     * </ul>
     * 
     * <p><b>Examples:</b>
     * <pre>{@code
     * items.append("value")              // Modifies list, returns null
     * result = name.upper()              // Returns new string "NAME"
     * OUTPUT["items"].append(customer)   // Chained access + method call
     * }</pre>
     * 
     * @param methodCall The MethodCall AST node
     * @param context Execution context
     * @return Method's return value (null for void methods)
     * @throws GrizzlyExecutionException if method unknown or args invalid
     */
    @SuppressWarnings("unchecked")
    private Object evaluateMethodCall(MethodCall methodCall, ExecutionContext context) {
        Object obj = evaluateExpression(methodCall.object(), context);
        String methodName = methodCall.methodName();
        
        if (obj == null) {
            throw new GrizzlyExecutionException(
                "Cannot call method '" + methodName + "' on null object"
            );
        }
        
        if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            
            return switch (methodName) {
                case "append" -> {
                    if (methodCall.arguments().size() != 1) {
                        throw new GrizzlyExecutionException(
                            "append() takes exactly 1 argument, got " + methodCall.arguments().size()
                        );
                    }
                    Object value = evaluateExpression(methodCall.arguments().get(0), context);
                    list.add(value);
                    yield null;
                }
                
                case "extend" -> {
                    if (methodCall.arguments().size() != 1) {
                        throw new GrizzlyExecutionException(
                            "extend() takes exactly 1 argument, got " + methodCall.arguments().size()
                        );
                    }
                    Object value = evaluateExpression(methodCall.arguments().get(0), context);
                    if (!(value instanceof List)) {
                        throw new GrizzlyExecutionException("extend() argument must be a list");
                    }
                    list.addAll((List<?>) value);
                    yield null;
                }
                
                default -> throw new GrizzlyExecutionException("Unknown list method: " + methodName);
            };
        }
        
        if (obj instanceof String) {
            String str = (String) obj;
            
            return switch (methodName) {
                case "upper" -> str.toUpperCase();
                case "lower" -> str.toLowerCase();
                case "strip" -> str.strip();
                default -> throw new GrizzlyExecutionException("Unknown string method: " + methodName);
            };
        }
        
        throw new GrizzlyExecutionException(
            "Object of type " + obj.getClass().getSimpleName() + 
            " does not have method '" + methodName + "'"
        );
    }
    
    /**
     * Evaluate a function call expression: {@code func(args)}.
     * 
     * <p>Handles function calls in expression contexts (assignments, arguments, etc.).
     * 
     * <p><b>Built-in Functions:</b>
     * <ul>
     *   <li>{@code len(object)} - Get size: {@code len([1,2,3])} → 3</li>
     * </ul>
     * 
     * <p><b>Examples:</b>
     * <pre>{@code
     * count = len(INPUT.customers)         // Built-in in assignment
     * sizes = [len(a), len(b)]            // Built-in as list element
     * result = helper(INPUT)               // User function
     * total = len(process(INPUT.data))    // Nested calls
     * }</pre>
     * 
     * <p><b>Execution:</b> Checks built-ins first, then user-defined functions.
     * 
     * @param funcCall The FunctionCallExpression AST node
     * @param context Execution context
     * @return The function's return value
     * @throws GrizzlyExecutionException if function not found
     */
    private Object evaluateFunctionCallExpression(FunctionCallExpression funcCall, ExecutionContext context) {
        // Check if it's a builtin function
        if (builtinFunctions.containsKey(funcCall.functionName())) {
            List<Object> args = new ArrayList<>();
            for (Expression argExpr : funcCall.args()) {
                args.add(evaluateExpression(argExpr, context));
            }
            return builtinFunctions.get(funcCall.functionName()).apply(args);
        }
        
        // Otherwise find user-defined function
        FunctionDef func = program.findFunction(funcCall.functionName());
        if (func == null) {
            throw new GrizzlyExecutionException(
                "Function '" + funcCall.functionName() + "' not found"
            );
        }
        
        ExecutionContext funcContext = new ExecutionContext();
        
        for (int i = 0; i < func.params().size(); i++) {
            Object argValue = evaluateExpression(funcCall.args().get(i), context);
            funcContext.set(func.params().get(i), argValue);
        }
        
        return executeFunction(func, funcContext);
    }
    
    /**
     * Set a value to a target expression (variable, dict key, or attribute).
     * 
     * <p><b>Auto-vivification magic:</b> Automatically creates missing intermediate objects!
     * 
     * <p><b>Simple case - Variable:</b>
     * <pre>{@code
     * x = 5
     * // Sets "x" to 5 in context
     * }</pre>
     * 
     * <p><b>Dict access:</b>
     * <pre>{@code
     * OUTPUT["name"] = "Alice"
     * // Sets OUTPUT's "name" key to "Alice"
     * }</pre>
     * 
     * <p><b>Nested dict - AUTO-VIVIFICATION:</b>
     * <pre>{@code
     * OUTPUT["profile"]["email"] = "test@example.com"
     * 
     * Problem: OUTPUT["profile"] doesn't exist yet (null)!
     * Solution: Auto-create OUTPUT["profile"] = {} first
     * Then: Set OUTPUT["profile"]["email"] = "test@example.com"
     * }</pre>
     * 
     * <p><b>How auto-vivification works:</b>
     * <ol>
     *   <li>Try to get OUTPUT["profile"] → returns null</li>
     *   <li>Detect we're accessing nested dict (DictAccess inside DictAccess)</li>
     *   <li>Create new empty HashMap: {}</li>
     *   <li>Set OUTPUT["profile"] = {} (recursive call)</li>
     *   <li>Now set OUTPUT["profile"]["email"] = value</li>
     * </ol>
     * 
     * @param target What to assign to (Identifier, DictAccess, or AttrAccess)
     * @param value What value to assign
     * @param context Execution context for variable storage
     */
    @SuppressWarnings("unchecked")
    private void setTarget(Expression target, Object value, ExecutionContext context) {
        switch (target) {
            case Identifier i -> context.set(i.name(), value);
            case DictAccess d -> {
                Object obj = evaluateExpression(d.object(), context);
                Object key = evaluateExpression(d.key(), context);
                
                // Auto-vivification: if obj is null and we're accessing through another DictAccess,
                // create the intermediate dictionary
                if (obj == null && d.object() instanceof DictAccess) {
                    // Create new empty dict for the parent
                    Map<String, Object> newDict = new HashMap<>();
                    setTarget(d.object(), newDict, context);
                    // Now set our value in the newly created dict
                    newDict.put(key.toString(), value);
                } else if (obj instanceof Map) {
                    ((Map<String, Object>) obj).put(key.toString(), value);
                } else if (obj instanceof List) {
                    List<Object> list = (List<Object>) obj;
                    int index = ((Number) toNumber(key)).intValue();
                    
                    // Handle negative indices
                    if (index < 0) {
                        index = list.size() + index;
                    }
                    
                    // Bounds check
                    if (index < 0 || index >= list.size()) {
                        throw new GrizzlyExecutionException(
                            "List index out of range: " + ((Number) toNumber(key)).intValue() + 
                            " (list size: " + list.size() + ")"
                        );
                    }
                    
                    list.set(index, value);
                } else {
                    throw new GrizzlyExecutionException(
                        "Cannot set key on object of type " + 
                        (obj != null ? obj.getClass().getSimpleName() : "null")
                    );
                }
            }
            case AttrAccess a -> {
                Object obj = evaluateExpression(a.object(), context);
                if (obj instanceof Map) {
                    ((Map<String, Object>) obj).put(a.attr(), value);
                } else {
                    throw new GrizzlyExecutionException(
                        "Cannot set attribute on object of type " +
                        (obj != null ? obj.getClass().getSimpleName() : "null")
                    );
                }
            }
            default -> throw new GrizzlyExecutionException(
                "Cannot assign to expression of type " + target.getClass().getSimpleName()
            );
        }
    }
    
    private boolean isTrue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }
    
    private Object evaluateNumericOp(Object left, Object right, String operator) {
        double l = toNumber(left);
        double r = toNumber(right);
        
        // Check if both are integers
        boolean bothIntegers = (l == Math.floor(l)) && (r == Math.floor(r));
        
        double result = switch (operator) {
            case "+" -> l + r;
            case "-" -> l - r;
            case "*" -> l * r;
            case "/" -> l / r;  // Always returns double (Python behavior)
            case "//" -> Math.floor(l / r);  // Floor division
            case "%" -> l % r;  // Modulo/remainder
            case "**" -> Math.pow(l, r);  // Power
            default -> throw new GrizzlyExecutionException("Unknown numeric operator: " + operator);
        };
        
        // Return integer if both inputs were integers and result is whole number
        // Exception: division always returns double
        if (bothIntegers && result == Math.floor(result) && !operator.equals("/")) {
            return (int) result;
        }
        return result;
    }
    
    private boolean evaluateComparison(Object left, Object right, String operator) {
        double l = toNumber(left);
        double r = toNumber(right);
        
        return switch (operator) {
            case "<" -> l < r;
            case ">" -> l > r;
            case "<=" -> l <= r;
            case ">=" -> l >= r;
            default -> throw new GrizzlyExecutionException("Unknown comparison operator: " + operator);
        };
    }
    
    private double toNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new GrizzlyExecutionException("Cannot convert '" + value + "' to number");
            }
        }
        throw new GrizzlyExecutionException("Cannot convert " + value.getClass().getSimpleName() + " to number");
    }
    
    /**
     * Internal class to signal loop control flow (break/continue).
     * 
     * <p>Used to propagate break/continue up the call stack to the enclosing loop.
     */
}

