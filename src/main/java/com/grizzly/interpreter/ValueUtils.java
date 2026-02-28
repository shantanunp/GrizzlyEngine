package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.types.*;

import java.util.List;

/**
 * Shared utility methods for working with Value types.
 * 
 * <p>Centralizes common operations to avoid duplication across
 * BuiltinRegistry, ModuleRegistry, and GrizzlyInterpreter.
 */
public final class ValueUtils {
    
    private ValueUtils() {}
    
    // ==================== Type Conversion ====================
    
    /**
     * Convert a Value to its string representation.
     */
    public static String asString(Value value) {
        return switch (value) {
            case StringValue s -> s.value();
            case NumberValue n -> n.toString();
            case BoolValue b -> b.toString();
            case NullValue ignored -> "None";
            default -> value.toString();
        };
    }
    
    /**
     * Convert a Value to double, throwing if not numeric.
     */
    public static double toDouble(Value value) {
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
    
    /**
     * Convert a Value to int, throwing if not numeric.
     */
    public static int toInt(Value value) {
        return switch (value) {
            case NumberValue n -> n.asInt();
            case StringValue s -> {
                try {
                    yield Integer.parseInt(s.value());
                } catch (NumberFormatException e) {
                    try {
                        yield (int) Double.parseDouble(s.value());
                    } catch (NumberFormatException e2) {
                        throw new GrizzlyExecutionException("Cannot convert '" + s.value() + "' to integer");
                    }
                }
            }
            case DecimalValue d -> d.toInt();
            default -> throw new GrizzlyExecutionException("Cannot convert " + value.typeName() + " to integer");
        };
    }
    
    /**
     * Convert a Value to long, throwing if not numeric.
     */
    public static long toLong(Value value) {
        return switch (value) {
            case NumberValue n -> n.asLong();
            case StringValue s -> {
                try {
                    yield Long.parseLong(s.value());
                } catch (NumberFormatException e) {
                    try {
                        yield (long) Double.parseDouble(s.value());
                    } catch (NumberFormatException e2) {
                        throw new GrizzlyExecutionException("Cannot convert '" + s.value() + "' to long");
                    }
                }
            }
            case DecimalValue d -> (long) d.toDouble();
            default -> throw new GrizzlyExecutionException("Cannot convert " + value.typeName() + " to long");
        };
    }
    
    // ==================== Argument Validation ====================
    
    /**
     * Validate exact argument count.
     */
    public static void requireArgCount(String funcName, List<Value> args, int expected) {
        if (args.size() != expected) {
            throw new GrizzlyExecutionException(
                funcName + "() takes exactly " + expected + " argument(s), got " + args.size()
            );
        }
    }
    
    /**
     * Validate argument count range.
     */
    public static void requireArgCountRange(String funcName, List<Value> args, int min, int max) {
        if (args.size() < min || args.size() > max) {
            throw new GrizzlyExecutionException(
                funcName + "() takes " + min + "-" + max + " arguments, got " + args.size()
            );
        }
    }
    
    /**
     * Validate minimum argument count.
     */
    public static void requireMinArgs(String funcName, List<Value> args, int min) {
        if (args.size() < min) {
            throw new GrizzlyExecutionException(
                funcName + "() requires at least " + min + " argument(s), got " + args.size()
            );
        }
    }
    
    /**
     * Require argument to be a specific type and cast it.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Value> T requireType(String funcName, Value value, Class<T> type, String argDesc) {
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new GrizzlyExecutionException(
            funcName + "() " + argDesc + " must be a " + getTypeName(type) + ", got: " + value.typeName()
        );
    }
    
    /**
     * Get human-readable type name for a Value class.
     */
    public static String getTypeName(Class<? extends Value> type) {
        if (type == StringValue.class) return "string";
        if (type == NumberValue.class) return "number";
        if (type == BoolValue.class) return "bool";
        if (type == ListValue.class) return "list";
        if (type == DictValue.class) return "dict";
        if (type == DateTimeValue.class) return "datetime";
        if (type == DecimalValue.class) return "decimal";
        if (type == NullValue.class) return "null";
        return type.getSimpleName().replace("Value", "").toLowerCase();
    }
    
    // ==================== Equality & Comparison ====================
    
    /**
     * Check equality between two values with numeric coercion.
     */
    public static boolean areEqual(Value left, Value right) {
        if (left instanceof NumberValue ln && right instanceof NumberValue rn) {
            return Math.abs(ln.asDouble() - rn.asDouble()) < 1e-10;
        }
        
        if (left instanceof StringValue ls && right instanceof NumberValue rn) {
            try {
                double leftNum = Double.parseDouble(ls.value());
                return Math.abs(leftNum - rn.asDouble()) < 1e-10;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        if (left instanceof NumberValue ln && right instanceof StringValue rs) {
            try {
                double rightNum = Double.parseDouble(rs.value());
                return Math.abs(ln.asDouble() - rightNum) < 1e-10;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return left.equals(right);
    }
    
    /**
     * Compare two numeric values.
     * 
     * @return negative if left < right, 0 if equal, positive if left > right
     */
    public static int compareNumeric(Value left, Value right) {
        double l = toDouble(left);
        double r = toDouble(right);
        return Double.compare(l, r);
    }
}
