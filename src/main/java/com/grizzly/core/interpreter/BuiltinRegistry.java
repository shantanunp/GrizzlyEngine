package com.grizzly.core.interpreter;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.interpreter.GrizzlyInterpreter.BuiltinFunction;
import com.grizzly.core.types.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.grizzly.core.interpreter.ValueUtils.*;

/**
 * Registry for all built-in functions available in Grizzly templates.
 * 
 * <p>Organizes builtins by category for maintainability:
 * <ul>
 *   <li>Core: len, range, str, int, float, bool, abs, min, max, sum</li>
 *   <li>DateTime: now, parseDate, formatDate, addDays, addMonths, etc.</li>
 *   <li>Math: Decimal, round</li>
 * </ul>
 */
public final class BuiltinRegistry {
    
    private final Map<String, BuiltinFunction> functions = new HashMap<>();
    
    public BuiltinRegistry() {
        registerCoreFunctions();
        registerIterationFunctions();
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
    
    // ==================== Iteration Functions ====================
    
    private void registerIterationFunctions() {
        // enumerate() - returns list of [index, value] pairs
        functions.put("enumerate", args -> {
            requireArgCountRange("enumerate", args, 1, 2);
            ListValue list = requireType("enumerate", args.get(0), ListValue.class, "first argument");
            int start = args.size() > 1 ? toInt(args.get(1)) : 0;
            
            java.util.List<Value> result = new java.util.ArrayList<>();
            int index = start;
            for (Value item : list.items()) {
                java.util.List<Value> pair = new java.util.ArrayList<>();
                pair.add(NumberValue.of(index));
                pair.add(item);
                result.add(new ListValue(pair));
                index++;
            }
            return new ListValue(result);
        });
        
        // zip() - combine multiple lists into list of tuples
        functions.put("zip", args -> {
            if (args.isEmpty()) {
                return ListValue.empty();
            }
            
            java.util.List<ListValue> lists = new java.util.ArrayList<>();
            int minLen = Integer.MAX_VALUE;
            
            for (Value arg : args) {
                ListValue list = requireType("zip", arg, ListValue.class, "argument");
                lists.add(list);
                minLen = Math.min(minLen, list.size());
            }
            
            java.util.List<Value> result = new java.util.ArrayList<>();
            for (int i = 0; i < minLen; i++) {
                java.util.List<Value> tuple = new java.util.ArrayList<>();
                for (ListValue list : lists) {
                    tuple.add(list.get(i));
                }
                result.add(new ListValue(tuple));
            }
            return new ListValue(result);
        });
        
        // sorted() - return a new sorted list
        functions.put("sorted", args -> {
            requireArgCountRange("sorted", args, 1, 2);
            ListValue list = requireType("sorted", args.get(0), ListValue.class, "first argument");
            boolean reverse = args.size() > 1 && args.get(1).isTruthy();
            
            java.util.List<Value> sorted = new java.util.ArrayList<>(list.items());
            sorted.sort((a, b) -> {
                int cmp;
                if (a instanceof NumberValue na && b instanceof NumberValue nb) {
                    cmp = Double.compare(na.asDouble(), nb.asDouble());
                } else {
                    cmp = asString(a).compareTo(asString(b));
                }
                return reverse ? -cmp : cmp;
            });
            return new ListValue(sorted);
        });
        
        // reversed() - return a new reversed list
        functions.put("reversed", args -> {
            requireArgCount("reversed", args, 1);
            ListValue list = requireType("reversed", args.get(0), ListValue.class, "argument");
            
            java.util.List<Value> reversed = new java.util.ArrayList<>(list.items());
            java.util.Collections.reverse(reversed);
            return new ListValue(reversed);
        });
        
        // any() - True if any element is truthy
        functions.put("any", args -> {
            requireArgCount("any", args, 1);
            ListValue list = requireType("any", args.get(0), ListValue.class, "argument");
            
            for (Value item : list.items()) {
                if (item.isTruthy()) {
                    return BoolValue.TRUE;
                }
            }
            return BoolValue.FALSE;
        });
        
        // all() - True if all elements are truthy
        functions.put("all", args -> {
            requireArgCount("all", args, 1);
            ListValue list = requireType("all", args.get(0), ListValue.class, "argument");
            
            for (Value item : list.items()) {
                if (!item.isTruthy()) {
                    return BoolValue.FALSE;
                }
            }
            return BoolValue.TRUE;
        });
        
        // list() - convert to list
        functions.put("list", args -> {
            if (args.isEmpty()) {
                return ListValue.empty();
            }
            requireArgCount("list", args, 1);
            Value value = args.get(0);
            
            return switch (value) {
                case ListValue l -> new ListValue(new java.util.ArrayList<>(l.items()));
                case StringValue s -> {
                    java.util.List<Value> chars = new java.util.ArrayList<>();
                    for (char c : s.value().toCharArray()) {
                        chars.add(new StringValue(String.valueOf(c)));
                    }
                    yield new ListValue(chars);
                }
                case DictValue d -> {
                    java.util.List<Value> keys = new java.util.ArrayList<>();
                    for (String key : d.entries().keySet()) {
                        keys.add(new StringValue(key));
                    }
                    yield new ListValue(keys);
                }
                default -> throw new GrizzlyExecutionException(
                    "list() argument must be iterable, got: " + value.typeName()
                );
            };
        });
        
        // dict() - convert to dict or create empty
        functions.put("dict", args -> {
            if (args.isEmpty()) {
                return DictValue.empty();
            }
            requireArgCount("dict", args, 1);
            Value value = args.get(0);
            
            if (value instanceof DictValue d) {
                DictValue result = DictValue.empty();
                for (java.util.Map.Entry<String, Value> e : d.entries().entrySet()) {
                    result.put(e.getKey(), e.getValue());
                }
                return result;
            }
            if (value instanceof ListValue list) {
                DictValue result = DictValue.empty();
                for (Value item : list.items()) {
                    if (!(item instanceof ListValue pair) || pair.size() != 2) {
                        throw new GrizzlyExecutionException(
                            "dict() argument must be list of [key, value] pairs"
                        );
                    }
                    String key = asString(pair.get(0));
                    result.put(key, pair.get(1));
                }
                return result;
            }
            throw new GrizzlyExecutionException(
                "dict() argument must be a dict or list of pairs, got: " + value.typeName()
            );
        });
        
        // type() - get type name as string
        functions.put("type", args -> {
            requireArgCount("type", args, 1);
            return new StringValue(args.get(0).typeName());
        });
        
        // isinstance() - check if value is of given type
        functions.put("isinstance", args -> {
            requireArgCount("isinstance", args, 2);
            Value value = args.get(0);
            String typeName = asString(args.get(1));
            
            return BoolValue.of(switch (typeName.toLowerCase()) {
                case "str", "string" -> value instanceof StringValue;
                case "int", "integer" -> value instanceof NumberValue n && n.isInteger();
                case "float", "number" -> value instanceof NumberValue;
                case "bool", "boolean" -> value instanceof BoolValue;
                case "list" -> value instanceof ListValue;
                case "dict" -> value instanceof DictValue;
                case "none", "null" -> value instanceof NullValue;
                case "datetime" -> value instanceof DateTimeValue;
                case "decimal" -> value instanceof DecimalValue;
                default -> false;
            });
        });
        
        // hasattr() - check if dict has key
        functions.put("hasattr", args -> {
            requireArgCount("hasattr", args, 2);
            if (!(args.get(0) instanceof DictValue dict)) {
                return BoolValue.FALSE;
            }
            String key = asString(args.get(1));
            return BoolValue.of(dict.containsKey(key));
        });
        
        // getattr() - get dict key with optional default
        functions.put("getattr", args -> {
            requireArgCountRange("getattr", args, 2, 3);
            if (!(args.get(0) instanceof DictValue dict)) {
                if (args.size() > 2) {
                    return args.get(2);
                }
                throw new GrizzlyExecutionException("getattr() first argument must be a dict");
            }
            String key = asString(args.get(1));
            
            if (dict.containsKey(key)) {
                return dict.get(key);
            }
            if (args.size() > 2) {
                return args.get(2);
            }
            throw new GrizzlyExecutionException("AttributeError: '" + key + "'");
        });
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
