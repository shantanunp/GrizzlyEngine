package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.parser.ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grizzly Interpreter - Executes the AST (Abstract Syntax Tree)
 * 
 * Simple example:
 * 
 * Program AST:
 *   def transform(INPUT):
 *       OUTPUT = {}
 *       OUTPUT["id"] = INPUT.customerId
 *       return OUTPUT
 * 
 * Execution:
 *   1. Create OUTPUT = {}
 *   2. Get INPUT.customerId → "C123"
 *   3. Set OUTPUT["id"] = "C123"
 *   4. Return OUTPUT → {"id": "C123"}
 */
public class GrizzlyInterpreter {
    
    private final Program program;
    private final Map<String, Object> builtinFunctions = new HashMap<>();
    
    public GrizzlyInterpreter(Program program) {
        this.program = program;
        registerBuiltins();
    }
    
    /**
     * Execute the template with input data
     * 
     * @param inputData Map representing the INPUT object
     * @return Map representing the OUTPUT object
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> inputData) {
        // Find the "transform" function
        FunctionDef transformFunc = program.findFunction("transform");
        if (transformFunc == null) {
            throw new GrizzlyExecutionException("No 'transform' function found in template");
        }
        
        // Create execution context
        ExecutionContext context = new ExecutionContext();
        context.set("INPUT", inputData);
        
        // Execute the transform function
        Object result = executeFunction(transformFunc, context);
        
        // Result should be a Map (the OUTPUT)
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        
        throw new GrizzlyExecutionException("transform() must return a dictionary");
    }
    
    /**
     * Execute a function
     */
    private Object executeFunction(FunctionDef func, ExecutionContext context) {
        try {
            // Execute each statement in the function body
            for (Statement stmt : func.body()) {
                Object result = executeStatement(stmt, context);
                
                // If it's a return statement, return immediately
                if (stmt instanceof ReturnStatement) {
                    return result;
                }
            }
            
            return null; // No return statement
            
        } catch (GrizzlyExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new GrizzlyExecutionException(
                "Error executing function '" + func.name() + "': " + e.getMessage(),
                e
            );
        }
    }
    
    /**
     * Execute a single statement
     */
    /**
     * Execute a single statement
     * JDK 21+ Pattern Matching
     */
    private Object executeStatement(Statement stmt, ExecutionContext context) {
        return switch (stmt) {
            case Assignment a -> executeAssignment(a, context);
            case ReturnStatement r -> evaluateExpression(r.value(), context);
            case FunctionCall f -> executeFunctionCall(f, context);
            case IfStatement i -> executeIf(i, context);
            default -> throw new GrizzlyExecutionException(
                "Unknown statement type: " + stmt.getClass().getSimpleName(),
                stmt.lineNumber()
            );
        };
    }
    
    /**
     * Execute an assignment: OUTPUT["id"] = INPUT.customerId
     */
    private Object executeAssignment(Assignment assignment, ExecutionContext context) {
        try {
            // Evaluate the right side (the value)
            Object value = evaluateExpression(assignment.value(), context);
            
            // Set the left side (the target)
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
     * Execute a function call: map_customer(INPUT, OUTPUT)
     */
    private Object executeFunctionCall(FunctionCall call, ExecutionContext context) {
        try {
            // Find the function in the program
            FunctionDef func = program.findFunction(call.functionName());
            if (func == null) {
                throw new GrizzlyExecutionException(
                    "Function '" + call.functionName() + "' not found",
                    call.lineNumber()
                );
            }
            
            // Create a new context for the function
            ExecutionContext funcContext = context.createChild();
            
            // Bind arguments to parameters
            List<String> params = func.params();
            List<Expression> args = call.args();
            
            for (int i = 0; i < params.size() && i < args.size(); i++) {
                Object argValue = evaluateExpression(args.get(i), context);
                funcContext.set(params.get(i), argValue);
            }
            
            // Execute the function
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
     * Execute if/else statement
     */
    private Object executeIf(IfStatement ifStmt, ExecutionContext context) {
        try {
            // Evaluate condition
            Object conditionResult = evaluateExpression(ifStmt.condition(), context);
            
            // Check if condition is true
            boolean isTrue = isTrue(conditionResult);
            
            // Execute appropriate block
            if (isTrue) {
                for (Statement stmt : ifStmt.thenBlock()) {
                    Object result = executeStatement(stmt, context);
                    if (stmt instanceof ReturnStatement) {
                        return result;
                    }
                }
            } else if (ifStmt.elseBlock() != null) {
                for (Statement stmt : ifStmt.elseBlock()) {
                    Object result = executeStatement(stmt, context);
                    if (stmt instanceof ReturnStatement) {
                        return result;
                    }
                }
            }
            
            return null;
            
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
     * Evaluate an expression and return its value
     * JDK 21+ Pattern Matching
     */
    private Object evaluateExpression(Expression expr, ExecutionContext context) {
        return switch (expr) {
            case Identifier i -> context.get(i.name());
            case StringLiteral s -> s.value();
            case DictLiteral d -> new HashMap<String, Object>();
            case DictAccess d -> evaluateDictAccess(d, context);
            case AttrAccess a -> evaluateAttrAccess(a, context);
            case BinaryOp b -> evaluateBinaryOp(b, context);
            default -> throw new GrizzlyExecutionException(
                "Unknown expression type: " + expr.getClass().getSimpleName()
            );
        };
    }
    
    /**
     * Evaluate dictionary access: OUTPUT["id"] or OUTPUT["customer"]["name"]
     */
    @SuppressWarnings("unchecked")
    private Object evaluateDictAccess(DictAccess access, ExecutionContext context) {
        // Get the object (dictionary)
        Object obj = evaluateExpression(access.object(), context);
        
        // Get the key
        Object key = evaluateExpression(access.key(), context);
        
        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).get(key);
        }
        
        throw new GrizzlyExecutionException(
            "Cannot access [" + key + "] on non-dictionary object: " + obj
        );
    }
    
    /**
     * Evaluate attribute access: INPUT.customerId or INPUT.personalInfo.email
     */
    @SuppressWarnings("unchecked")
    private Object evaluateAttrAccess(AttrAccess access, ExecutionContext context) {
        // Get the object
        Object obj = evaluateExpression(access.object(), context);
        
        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).get(access.attr());
        }
        
        throw new GrizzlyExecutionException(
            "Cannot access attribute '" + access.attr() + "' on non-object: " + obj
        );
    }
    
    /**
     * Evaluate binary operation: x == 5, name != "admin"
     */
    private Object evaluateBinaryOp(BinaryOp op, ExecutionContext context) {
        Object left = evaluateExpression(op.left(), context);
        Object right = evaluateExpression(op.right(), context);
        
        return switch (op.operator()) {
            case "==" -> equals(left, right);
            case "!=" -> !equals(left, right);
            case "<" -> compare(left, right) < 0;
            case ">" -> compare(left, right) > 0;
            case "<=" -> compare(left, right) <= 0;
            case ">=" -> compare(left, right) >= 0;
            default -> throw new GrizzlyExecutionException(
                "Unknown operator: " + op.operator()
            );
        };
    }
    
    /**
     * Set a target (left side of assignment)
     */
    @SuppressWarnings("unchecked")
    private void setTarget(Expression target, Object value, ExecutionContext context) {
        if (target instanceof Identifier) {
            Identifier i = (Identifier) target;
            context.set(i.name(), value);
        } else if (target instanceof DictAccess) {
            DictAccess d = (DictAccess) target;
            
            // For nested access like OUTPUT["customer"]["name"]
            // We need to create intermediate maps FIRST
            if (d.object() instanceof DictAccess) {
                ensureMapExists((DictAccess) d.object(), context);
            }
            
            // Now get the dictionary
            Object obj = evaluateExpression(d.object(), context);
            
            if (!(obj instanceof Map)) {
                throw new GrizzlyExecutionException(
                    "Cannot set [key] on non-dictionary object"
                );
            }
            
            Map<String, Object> map = (Map<String, Object>) obj;
            Object key = evaluateExpression(d.key(), context);
            
            // If this is also a nested dict, create it
            if (value == null || !(value instanceof Map)) {
                // Setting a simple value
                map.put((String) key, value);
            } else {
                // Setting a dict value
                map.put((String) key, value);
            }
        } else {
            throw new GrizzlyExecutionException(
                "Cannot assign to: " + target.getClass().getSimpleName()
            );
        }
    }
    
    /**
     * Ensure nested maps exist for assignments like OUTPUT["a"]["b"] = value
     */
    @SuppressWarnings("unchecked")
    private void ensureMapExists(DictAccess access, ExecutionContext context) {
        Object obj = evaluateExpression(access.object(), context);
        
        if (!(obj instanceof Map)) {
            throw new GrizzlyExecutionException("Expected dictionary");
        }
        
        Map<String, Object> map = (Map<String, Object>) obj;
        Object key = evaluateExpression(access.key(), context);
        
        if (!map.containsKey(key) || !(map.get(key) instanceof Map)) {
            map.put((String) key, new HashMap<String, Object>());
        }
    }
    
    /**
     * Check if a value is "truthy"
     */
    private boolean isTrue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }
    
    /**
     * Compare two objects for equality
     */
    private boolean equals(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return left.equals(right);
    }
    
    /**
     * Compare two objects (<, >, <=, >=)
     */
    @SuppressWarnings("unchecked")
    private int compare(Object left, Object right) {
        // Convert to comparable numbers
        Number leftNum = toNumber(left);
        Number rightNum = toNumber(right);
        
        if (leftNum != null && rightNum != null) {
            // Both are numbers, compare as doubles
            return Double.compare(leftNum.doubleValue(), rightNum.doubleValue());
        }
        
        // Fall back to string comparison
        if (left instanceof Comparable && right instanceof Comparable) {
            try {
                return ((Comparable<Object>) left).compareTo(right);
            } catch (ClassCastException e) {
                // Can't compare, treat as strings
                return left.toString().compareTo(right.toString());
            }
        }
        
        throw new GrizzlyExecutionException(
            "Cannot compare " + left.getClass().getSimpleName() + 
            " with " + right.getClass().getSimpleName()
        );
    }
    
    /**
     * Try to convert an object to a Number
     */
    private Number toNumber(Object obj) {
        if (obj instanceof Number) {
            return (Number) obj;
        }
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Register built-in functions
     */
    private void registerBuiltins() {
        // Will add built-in functions in Phase 4
        // For now, just initialize empty
    }
}
