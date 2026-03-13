package com.grizzly.core.interpreter;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.interpreter.GrizzlyInterpreter.BuiltinFunction;
import com.grizzly.core.logging.GrizzlyLogger;
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
 *   <li>DateTime (kept): now, formatDate</li>
 *   <li>Math: round (Decimal via from decimal import Decimal)</li>
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
        functions.put("len", (args, kw) -> {
            requireArgCount("len", args, 1);
            Value val = args.get(0);
            
            if (val instanceof ListValue l) {
                return NumberValue.of(l.size());
            } else if (val instanceof DictValue d) {
                return NumberValue.of(d.size());
            } else if (val instanceof StringValue s) {
                return NumberValue.of(s.value().length());
            } else {
                throw new GrizzlyExecutionException(
                    "len() argument must be a list, dict, or string, got: " + val.typeName()
                );
            }
        });
        
        // range()
        functions.put("range", (args, kw) -> {
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
        
        // str() - Python-compliant: str("hello") → "hello" (no extra quotes)
        functions.put("str", (args, kw) -> {
            requireArgCount("str", args, 1);
            return new StringValue(asString(args.get(0)));
        });
        
        // int() - Python-compliant: int("3.5") raises ValueError (no silent truncation)
        functions.put("int", (args, kw) -> {
            requireArgCount("int", args, 1);
            Value value = args.get(0);
            
            if (value instanceof NumberValue n) {
                return NumberValue.of(n.asInt());
            } else if (value instanceof StringValue s) {
                String stripped = s.value().strip();
                try {
                    return NumberValue.of(Integer.parseInt(stripped));
                } catch (NumberFormatException e) {
                    throw new GrizzlyExecutionException(
                        "invalid literal for int() with base 10: '" + s.value() + "'"
                    );
                }
            } else if (value instanceof BoolValue b) {
                return NumberValue.of(b.value() ? 1 : 0);
            } else {
                throw new GrizzlyExecutionException(
                    "int() argument must be a string or a number, not '" + value.typeName() + "'"
                );
            }
        });
        
        // float() - convert to float
        functions.put("float", (args, kw) -> {
            requireArgCount("float", args, 1);
            Value value = args.get(0);
            
            if (value instanceof NumberValue n) {
                return NumberValue.of(n.asDouble());
            } else if (value instanceof StringValue s) {
                try {
                    return NumberValue.of(Double.parseDouble(s.value().strip()));
                } catch (NumberFormatException e) {
                    throw new GrizzlyExecutionException(
                        "Cannot convert '" + s.value() + "' to float"
                    );
                }
            } else if (value instanceof BoolValue b) {
                return NumberValue.of(b.value() ? 1.0 : 0.0);
            } else {
                throw new GrizzlyExecutionException(
                    "Cannot convert " + value.typeName() + " to float"
                );
            }
        });
        
        // bool() - convert to boolean
        functions.put("bool", (args, kw) -> {
            requireArgCount("bool", args, 1);
            return BoolValue.of(args.get(0).isTruthy());
        });
        
        // abs() - absolute value
        functions.put("abs", (args, kw) -> {
            requireArgCount("abs", args, 1);
            Value value = args.get(0);
            
            if (value instanceof NumberValue n) {
                if (n.isInteger()) {
                    return NumberValue.of(Math.abs(n.asLong()));
                }
                return NumberValue.of(Math.abs(n.asDouble()));
            } else if (value instanceof DecimalValue d) {
                return new DecimalValue(d.getValue().abs());
            } else {
                throw new GrizzlyExecutionException(
                    "abs() argument must be a number, got: " + value.typeName()
                );
            }
        });
        
        // min() - minimum value
        functions.put("min", (args, kw) -> {
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
        functions.put("max", (args, kw) -> {
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
        functions.put("sum", (args, kw) -> {
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
        
        // print() - Python: print(*values, sep=' ', end='\n'). Logs via GrizzlyLogger.
        functions.put("print", (args, kw) -> {
            String sep = kw.containsKey("sep") ? asString(kw.get("sep")) : " ";
            String end = kw.containsKey("end") ? asString(kw.get("end")) : "\n";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(sep);
                sb.append(asString(args.get(i)));
            }
            sb.append(end);
            com.grizzly.core.logging.GrizzlyLogger.info("PRINT", sb.toString());
            return NullValue.INSTANCE;
        });
    }

    private Value findMin(List<Value> values) {
        Value min = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (ValueUtils.compareForSort(values.get(i), min) < 0) {
                min = values.get(i);
            }
        }
        return min;
    }

    private Value findMax(List<Value> values) {
        Value max = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (ValueUtils.compareForSort(values.get(i), max) > 0) {
                max = values.get(i);
            }
        }
        return max;
    }
    
    // ==================== Iteration Functions ====================
    
    private void registerIterationFunctions() {
        // enumerate() - returns list of [index, value] pairs
        functions.put("enumerate", (args, kw) -> {
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
        functions.put("zip", (args, kw) -> {
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
        
        // sorted() - return a new sorted list. Python: sorted(iterable, key=None, reverse=False)
        functions.put("sorted", (args, kw) -> {
            requireArgCountRange("sorted", args, 1, 2);
            ListValue list = requireType("sorted", args.get(0), ListValue.class, "first argument");
            boolean reverse = kw.containsKey("reverse") ? kw.get("reverse").isTruthy()
                : (args.size() > 1 && args.get(1).isTruthy());
            java.util.List<Value> sorted = new java.util.ArrayList<>(list.items());
            sorted.sort((a, b) -> {
                int cmp = ValueUtils.compareForSort(a, b);
                return reverse ? -cmp : cmp;
            });
            return new ListValue(sorted);
        });
        
        // reversed() - return a new reversed list
        functions.put("reversed", (args, kw) -> {
            requireArgCount("reversed", args, 1);
            ListValue list = requireType("reversed", args.get(0), ListValue.class, "argument");
            
            java.util.List<Value> reversed = new java.util.ArrayList<>(list.items());
            java.util.Collections.reverse(reversed);
            return new ListValue(reversed);
        });
        
        // any() - True if any element is truthy
        functions.put("any", (args, kw) -> {
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
        functions.put("all", (args, kw) -> {
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
        functions.put("list", (args, kw) -> {
            if (args.isEmpty()) {
                return ListValue.empty();
            }
            requireArgCount("list", args, 1);
            Value value = args.get(0);
            
            if (value instanceof ListValue l) {
                return new ListValue(new java.util.ArrayList<>(l.items()));
            } else if (value instanceof StringValue s) {
                java.util.List<Value> chars = new java.util.ArrayList<>();
                for (char c : s.value().toCharArray()) {
                    chars.add(new StringValue(String.valueOf(c)));
                }
                return new ListValue(chars);
            } else if (value instanceof DictValue d) {
                java.util.List<Value> keys = new java.util.ArrayList<>();
                for (String key : d.entries().keySet()) {
                    keys.add(new StringValue(key));
                }
                return new ListValue(keys);
            } else {
                throw new GrizzlyExecutionException(
                    "list() argument must be iterable, got: " + value.typeName()
                );
            }
        });
        
        // dict() - convert to dict or create empty
        functions.put("dict", (args, kw) -> {
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
        functions.put("type", (args, kw) -> {
            requireArgCount("type", args, 1);
            return new StringValue(args.get(0).typeName());
        });
        
        // isinstance() - Python-compatible: only str, int, float, bool, list, dict, type(None)
        functions.put("isinstance", (args, kw) -> {
            requireArgCount("isinstance", args, 2);
            Value value = args.get(0);
            String typeName = asString(args.get(1));
            
            return BoolValue.of(switch (typeName) {
                case "str" -> value instanceof StringValue;
                case "int" -> value instanceof NumberValue n && n.isInteger();
                case "float" -> value instanceof NumberValue;
                case "bool" -> value instanceof BoolValue;
                case "list" -> value instanceof ListValue;
                case "dict" -> value instanceof DictValue;
                case "None", "type(None)" -> value instanceof NullValue;
                case "datetime" -> value instanceof DateTimeValue;
                case "decimal" -> value instanceof DecimalValue;
                default -> false;
            });
        });
        
        // hasattr() - check if dict has key
        functions.put("hasattr", (args, kw) -> {
            requireArgCount("hasattr", args, 2);
            if (!(args.get(0) instanceof DictValue dict)) {
                return BoolValue.FALSE;
            }
            String key = asString(args.get(1));
            return BoolValue.of(dict.containsKey(key));
        });
        
        // getattr() - get dict key with optional default
        functions.put("getattr", (args, kw) -> {
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
        functions.put("now", (args, kw) -> {
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
        
        // formatDate()
        functions.put("formatDate", (args, kw) -> {
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
    }
    
    // ==================== Math Functions ====================
    
    private void registerMathFunctions() {
        // round(number[, ndigits]) - Python-compliant with banker's rounding (HALF_EVEN)
        functions.put("round", (args, kw) -> {
            requireArgCountRange("round", args, 1, 2);
            Value value = args.get(0);
            int ndigits = args.size() > 1 ? toInt(args.get(1)) : 0;
            if (value instanceof DecimalValue d) {
                return d.round(ndigits);
            }
            if (value instanceof NumberValue n) {
                java.math.BigDecimal bd = java.math.BigDecimal.valueOf(n.asDouble());
                bd = bd.setScale(ndigits, java.math.RoundingMode.HALF_EVEN);
                if (ndigits <= 0) {
                    return NumberValue.of(bd.intValue());
                }
                return NumberValue.of(bd.doubleValue());
            }
            throw new GrizzlyExecutionException(
                "round() argument must be a number, got: " + value.typeName()
            );
        });
    }
}
