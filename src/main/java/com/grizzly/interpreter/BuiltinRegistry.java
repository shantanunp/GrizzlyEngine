package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.interpreter.GrizzlyInterpreter.BuiltinFunction;
import com.grizzly.types.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for all built-in functions available in Grizzly templates.
 * 
 * <p>Organizes builtins by category for maintainability:
 * <ul>
 *   <li>Core: len, range, str</li>
 *   <li>DateTime: now, parseDate, formatDate, addDays, etc.</li>
 *   <li>Math: Decimal, round</li>
 * </ul>
 */
public final class BuiltinRegistry {
    
    private final Map<String, BuiltinFunction> functions = new HashMap<>();
    
    public BuiltinRegistry() {
        registerCoreFunctions();
        registerDateTimeFunctions();
        registerMathFunctions();
    }
    
    public Map<String, BuiltinFunction> getFunctions() {
        return functions;
    }
    
    public BuiltinFunction get(String name) {
        return functions.get(name);
    }
    
    public boolean contains(String name) {
        return functions.containsKey(name);
    }
    
    // ==================== Argument Validation Helpers ====================
    
    private static void requireArgCount(String funcName, List<Value> args, int expected) {
        if (args.size() != expected) {
            throw new GrizzlyExecutionException(
                funcName + "() takes exactly " + expected + " argument(s), got " + args.size()
            );
        }
    }
    
    private static void requireArgCountRange(String funcName, List<Value> args, int min, int max) {
        if (args.size() < min || args.size() > max) {
            throw new GrizzlyExecutionException(
                funcName + "() takes " + min + "-" + max + " arguments, got " + args.size()
            );
        }
    }
    
    private static <T extends Value> T requireType(String funcName, Value value, Class<T> type, String argDesc) {
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new GrizzlyExecutionException(
            funcName + "() " + argDesc + " must be a " + getTypeName(type) + ", got: " + value.typeName()
        );
    }
    
    private static String getTypeName(Class<? extends Value> type) {
        if (type == StringValue.class) return "string";
        if (type == NumberValue.class) return "number";
        if (type == BoolValue.class) return "bool";
        if (type == ListValue.class) return "list";
        if (type == DictValue.class) return "dict";
        if (type == DateTimeValue.class) return "datetime";
        if (type == DecimalValue.class) return "decimal";
        return type.getSimpleName();
    }
    
    private static String asString(Value value) {
        return switch (value) {
            case StringValue s -> s.value();
            case NumberValue n -> n.toString();
            case BoolValue b -> b.toString();
            case NullValue ignored -> "None";
            default -> value.toString();
        };
    }
    
    private static int toInt(Value value) {
        return switch (value) {
            case NumberValue n -> n.asInt();
            case StringValue s -> {
                try {
                    yield Integer.parseInt(s.value());
                } catch (NumberFormatException e) {
                    throw new GrizzlyExecutionException("Cannot convert '" + s.value() + "' to integer");
                }
            }
            default -> throw new GrizzlyExecutionException("Cannot convert " + value.typeName() + " to integer");
        };
    }
    
    private static long toLong(Value value) {
        return switch (value) {
            case NumberValue n -> n.asLong();
            case StringValue s -> {
                try {
                    yield Long.parseLong(s.value());
                } catch (NumberFormatException e) {
                    throw new GrizzlyExecutionException("Cannot convert '" + s.value() + "' to long");
                }
            }
            default -> throw new GrizzlyExecutionException("Cannot convert " + value.typeName() + " to long");
        };
    }
    
    // ==================== Core Functions ====================
    
    private void registerCoreFunctions() {
        // len()
        functions.put("len", args -> {
            requireArgCount("len", args, 1);
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
        
        // range()
        functions.put("range", args -> {
            requireArgCountRange("range", args, 1, 3);
            
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
            
            java.util.List<Value> result = new java.util.ArrayList<>();
            
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
        
        // str()
        functions.put("str", args -> {
            requireArgCount("str", args, 1);
            Value value = args.get(0);
            
            return switch (value) {
                case NullValue ignored -> new StringValue("None");
                case BoolValue b -> new StringValue(b.value() ? "True" : "False");
                default -> new StringValue(value.toString());
            };
        });
        
        // int() - convert to integer
        functions.put("int", args -> {
            requireArgCount("int", args, 1);
            Value value = args.get(0);
            
            return switch (value) {
                case NumberValue n -> NumberValue.of(n.asInt());
                case StringValue s -> {
                    try {
                        yield NumberValue.of(Integer.parseInt(s.value().strip()));
                    } catch (NumberFormatException e) {
                        try {
                            yield NumberValue.of((int) Double.parseDouble(s.value().strip()));
                        } catch (NumberFormatException e2) {
                            throw new GrizzlyExecutionException(
                                "Cannot convert '" + s.value() + "' to int"
                            );
                        }
                    }
                }
                case BoolValue b -> NumberValue.of(b.value() ? 1 : 0);
                default -> throw new GrizzlyExecutionException(
                    "Cannot convert " + value.typeName() + " to int"
                );
            };
        });
        
        // float() - convert to float
        functions.put("float", args -> {
            requireArgCount("float", args, 1);
            Value value = args.get(0);
            
            return switch (value) {
                case NumberValue n -> NumberValue.of(n.asDouble());
                case StringValue s -> {
                    try {
                        yield NumberValue.of(Double.parseDouble(s.value().strip()));
                    } catch (NumberFormatException e) {
                        throw new GrizzlyExecutionException(
                            "Cannot convert '" + s.value() + "' to float"
                        );
                    }
                }
                case BoolValue b -> NumberValue.of(b.value() ? 1.0 : 0.0);
                default -> throw new GrizzlyExecutionException(
                    "Cannot convert " + value.typeName() + " to float"
                );
            };
        });
        
        // bool() - convert to boolean
        functions.put("bool", args -> {
            requireArgCount("bool", args, 1);
            return BoolValue.of(args.get(0).isTruthy());
        });
        
        // abs() - absolute value
        functions.put("abs", args -> {
            requireArgCount("abs", args, 1);
            Value value = args.get(0);
            
            return switch (value) {
                case NumberValue n -> {
                    if (n.isInteger()) {
                        yield NumberValue.of(Math.abs(n.asLong()));
                    }
                    yield NumberValue.of(Math.abs(n.asDouble()));
                }
                case DecimalValue d -> new DecimalValue(d.getValue().abs());
                default -> throw new GrizzlyExecutionException(
                    "abs() argument must be a number, got: " + value.typeName()
                );
            };
        });
        
        // min() - minimum value
        functions.put("min", args -> {
            if (args.isEmpty()) {
                throw new GrizzlyExecutionException("min() requires at least 1 argument");
            }
            
            if (args.size() == 1 && args.get(0) instanceof ListValue list) {
                if (list.isEmpty()) {
                    throw new GrizzlyExecutionException("min() arg is an empty sequence");
                }
                return findMin(list.items());
            }
            
            return findMin(args);
        });
        
        // max() - maximum value
        functions.put("max", args -> {
            if (args.isEmpty()) {
                throw new GrizzlyExecutionException("max() requires at least 1 argument");
            }
            
            if (args.size() == 1 && args.get(0) instanceof ListValue list) {
                if (list.isEmpty()) {
                    throw new GrizzlyExecutionException("max() arg is an empty sequence");
                }
                return findMax(list.items());
            }
            
            return findMax(args);
        });
        
        // sum() - sum of values
        functions.put("sum", args -> {
            requireArgCount("sum", args, 1);
            ListValue list = requireType("sum", args.get(0), ListValue.class, "argument");
            
            if (list.isEmpty()) {
                return NumberValue.of(0);
            }
            
            double sum = 0;
            for (Value item : list.items()) {
                if (item instanceof NumberValue n) {
                    sum += n.asDouble();
                } else {
                    throw new GrizzlyExecutionException(
                        "sum() requires all elements to be numbers, got: " + item.typeName()
                    );
                }
            }
            return NumberValue.of(sum);
        });
    }
    
    private Value findMin(List<Value> values) {
        Value min = values.get(0);
        double minVal = toDouble(min);
        
        for (int i = 1; i < values.size(); i++) {
            double val = toDouble(values.get(i));
            if (val < minVal) {
                minVal = val;
                min = values.get(i);
            }
        }
        return min;
    }
    
    private Value findMax(List<Value> values) {
        Value max = values.get(0);
        double maxVal = toDouble(max);
        
        for (int i = 1; i < values.size(); i++) {
            double val = toDouble(values.get(i));
            if (val > maxVal) {
                maxVal = val;
                max = values.get(i);
            }
        }
        return max;
    }
    
    private static double toDouble(Value value) {
        return switch (value) {
            case NumberValue n -> n.asDouble();
            case DecimalValue d -> d.toDouble();
            default -> throw new GrizzlyExecutionException(
                "Cannot compare " + value.typeName() + " values"
            );
        };
    }
    
    // ==================== DateTime Functions ====================
    
    private void registerDateTimeFunctions() {
        // now()
        functions.put("now", args -> {
            if (args.size() > 1) {
                throw new GrizzlyExecutionException(
                    "now() takes 0 or 1 argument (optional timezone), got " + args.size()
                );
            }
            
            if (args.isEmpty()) {
                return new DateTimeValue(java.time.ZonedDateTime.now());
            }
            
            String timezone = asString(args.get(0));
            try {
                java.time.ZoneId zone = java.time.ZoneId.of(timezone);
                return new DateTimeValue(java.time.ZonedDateTime.now(zone));
            } catch (Exception e) {
                throw new GrizzlyExecutionException(
                    "Invalid timezone: " + timezone + ". Use formats like 'UTC', 'America/New_York'"
                );
            }
        });
        
        // parseDate()
        functions.put("parseDate", args -> {
            requireArgCountRange("parseDate", args, 2, 3);
            
            String dateString = asString(args.get(0));
            String format = asString(args.get(1));
            
            try {
                java.time.format.DateTimeFormatter formatter = 
                    java.time.format.DateTimeFormatter.ofPattern(format);
                
                java.time.ZoneId zone = args.size() == 3 
                    ? java.time.ZoneId.of(asString(args.get(2)))
                    : java.time.ZoneId.systemDefault();
                
                try {
                    java.time.LocalDateTime local = java.time.LocalDateTime.parse(dateString, formatter);
                    return new DateTimeValue(local.atZone(zone));
                } catch (Exception e) {
                    java.time.LocalDate date = java.time.LocalDate.parse(dateString, formatter);
                    return new DateTimeValue(date.atStartOfDay(zone));
                }
            } catch (Exception e) {
                throw new GrizzlyExecutionException(
                    "Failed to parse date '" + dateString + "' with format '" + format + "': " + e.getMessage()
                );
            }
        });
        
        // formatDate()
        functions.put("formatDate", args -> {
            requireArgCount("formatDate", args, 2);
            DateTimeValue dt = requireType("formatDate", args.get(0), DateTimeValue.class, "first argument");
            String format = asString(args.get(1));
            
            try {
                return new StringValue(dt.format(format));
            } catch (Exception e) {
                throw new GrizzlyExecutionException(
                    "Invalid date format '" + format + "': " + e.getMessage()
                );
            }
        });
        
        // addDays()
        functions.put("addDays", args -> {
            requireArgCount("addDays", args, 2);
            DateTimeValue dt = requireType("addDays", args.get(0), DateTimeValue.class, "first argument");
            return dt.addDays(toLong(args.get(1)));
        });
        
        // addMonths()
        functions.put("addMonths", args -> {
            requireArgCount("addMonths", args, 2);
            DateTimeValue dt = requireType("addMonths", args.get(0), DateTimeValue.class, "first argument");
            return dt.addMonths(toLong(args.get(1)));
        });
        
        // addYears()
        functions.put("addYears", args -> {
            requireArgCount("addYears", args, 2);
            DateTimeValue dt = requireType("addYears", args.get(0), DateTimeValue.class, "first argument");
            return dt.addYears(toLong(args.get(1)));
        });
        
        // addHours()
        functions.put("addHours", args -> {
            requireArgCount("addHours", args, 2);
            DateTimeValue dt = requireType("addHours", args.get(0), DateTimeValue.class, "first argument");
            return dt.addHours(toLong(args.get(1)));
        });
        
        // addMinutes()
        functions.put("addMinutes", args -> {
            requireArgCount("addMinutes", args, 2);
            DateTimeValue dt = requireType("addMinutes", args.get(0), DateTimeValue.class, "first argument");
            return dt.addMinutes(toLong(args.get(1)));
        });
    }
    
    // ==================== Math Functions ====================
    
    private void registerMathFunctions() {
        // Decimal()
        functions.put("Decimal", args -> {
            requireArgCount("Decimal", args, 1);
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
        
        // round()
        functions.put("round", args -> {
            requireArgCount("round", args, 2);
            DecimalValue decimal = requireType("round", args.get(0), DecimalValue.class, "first argument");
            int places = toInt(args.get(1));
            return decimal.round(places);
        });
    }
}
