package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.grizzly.interpreter.ValueUtils.*;

/**
 * Dict method implementations for the Grizzly interpreter.
 * 
 * <p>Supports Python-like dict methods:
 * get, keys, values, items, pop, update, clear, copy, setdefault
 */
public final class DictMethods {
    
    private DictMethods() {}
    
    /**
     * Evaluate a method call on a dict value.
     * 
     * @param dict The dict value
     * @param methodName The method name
     * @param args Evaluated arguments
     * @return The result
     */
    public static Value evaluate(DictValue dict, String methodName, List<Value> args) {
        return switch (methodName) {
            case "get" -> get(dict, args);
            case "keys" -> keys(dict, args);
            case "values" -> values(dict, args);
            case "items" -> items(dict, args);
            case "pop" -> pop(dict, args);
            case "update" -> update(dict, args);
            case "clear" -> clear(dict);
            case "copy" -> copy(dict);
            case "setdefault" -> setdefault(dict, args);
            
            default -> throw new GrizzlyExecutionException("Unknown dict method: " + methodName);
        };
    }
    
    private static Value get(DictValue dict, List<Value> args) {
        requireMinArgs("get", args, 1);
        String key = asString(args.get(0));
        Value defaultValue = args.size() > 1 ? args.get(1) : NullValue.INSTANCE;
        
        if (dict.containsKey(key)) {
            return dict.get(key);
        }
        return defaultValue;
    }
    
    private static Value keys(DictValue dict, List<Value> args) {
        List<Value> keyList = new ArrayList<>();
        for (String key : dict.entries().keySet()) {
            keyList.add(new StringValue(key));
        }
        return new ListValue(keyList);
    }
    
    private static Value values(DictValue dict, List<Value> args) {
        List<Value> valueList = new ArrayList<>(dict.entries().values());
        return new ListValue(valueList);
    }
    
    private static Value items(DictValue dict, List<Value> args) {
        List<Value> itemList = new ArrayList<>();
        for (Map.Entry<String, Value> entry : dict.entries().entrySet()) {
            List<Value> pair = new ArrayList<>();
            pair.add(new StringValue(entry.getKey()));
            pair.add(entry.getValue());
            itemList.add(new ListValue(pair));
        }
        return new ListValue(itemList);
    }
    
    private static Value pop(DictValue dict, List<Value> args) {
        requireMinArgs("pop", args, 1);
        String key = asString(args.get(0));
        
        if (dict.containsKey(key)) {
            Value value = dict.get(key);
            dict.entries().remove(key);
            return value;
        }
        
        if (args.size() > 1) {
            return args.get(1);
        }
        
        throw new GrizzlyExecutionException("KeyError: '" + key + "'");
    }
    
    private static Value update(DictValue dict, List<Value> args) {
        requireArgCount("update", args, 1);
        DictValue other = requireType("update", args.get(0), DictValue.class, "argument");
        
        for (Map.Entry<String, Value> entry : other.entries().entrySet()) {
            dict.put(entry.getKey(), entry.getValue());
        }
        
        return NullValue.INSTANCE;
    }
    
    private static Value clear(DictValue dict) {
        dict.entries().clear();
        return NullValue.INSTANCE;
    }
    
    private static Value copy(DictValue dict) {
        DictValue result = DictValue.empty();
        for (Map.Entry<String, Value> entry : dict.entries().entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    private static Value setdefault(DictValue dict, List<Value> args) {
        requireMinArgs("setdefault", args, 1);
        String key = asString(args.get(0));
        Value defaultValue = args.size() > 1 ? args.get(1) : NullValue.INSTANCE;
        
        if (!dict.containsKey(key)) {
            dict.put(key, defaultValue);
        }
        
        return dict.get(key);
    }
}
