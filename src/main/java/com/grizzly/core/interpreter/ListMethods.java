package com.grizzly.core.interpreter;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.grizzly.core.interpreter.ValueUtils.*;

/**
 * List method implementations for the Grizzly interpreter.
 * 
 * <p>Supports Python-like list methods:
 * append, extend, insert, pop, remove, sort, reverse, index, count, copy, clear
 */
public final class ListMethods {
    
    private ListMethods() {}
    
    /**
     * Evaluate a method call on a list value.
     * 
     * @param list The list value
     * @param methodName The method name
     * @param args Evaluated arguments
     * @return The result
     */
    public static Value evaluate(ListValue list, String methodName, List<Value> args) {
        return switch (methodName) {
            // Mutating methods
            case "append" -> append(list, args);
            case "extend" -> extend(list, args);
            case "insert" -> insert(list, args);
            case "pop" -> pop(list, args);
            case "remove" -> remove(list, args);
            case "clear" -> clear(list);
            case "reverse" -> reverse(list);
            case "sort" -> sort(list, args);
            
            // Non-mutating methods
            case "index" -> index(list, args);
            case "count" -> count(list, args);
            case "copy" -> copy(list);
            
            default -> throw new GrizzlyExecutionException("Unknown list method: " + methodName);
        };
    }
    
    private static Value append(ListValue list, List<Value> args) {
        requireArgCount("append", args, 1);
        list.append(args.get(0));
        return NullValue.INSTANCE;
    }
    
    private static Value extend(ListValue list, List<Value> args) {
        requireArgCount("extend", args, 1);
        ListValue other = requireType("extend", args.get(0), ListValue.class, "argument");
        list.extend(other);
        return NullValue.INSTANCE;
    }
    
    private static Value insert(ListValue list, List<Value> args) {
        requireArgCount("insert", args, 2);
        int index = toInt(args.get(0));
        Value value = args.get(1);
        
        if (index < 0) {
            index = Math.max(0, list.size() + index);
        }
        if (index > list.size()) {
            index = list.size();
        }
        
        list.items().add(index, value);
        return NullValue.INSTANCE;
    }
    
    private static Value pop(ListValue list, List<Value> args) {
        if (list.isEmpty()) {
            throw new GrizzlyExecutionException("pop from empty list");
        }
        
        int index;
        if (args.isEmpty()) {
            index = list.size() - 1;
        } else {
            index = toInt(args.get(0));
            if (index < 0) {
                index = list.size() + index;
            }
        }
        
        if (index < 0 || index >= list.size()) {
            throw new GrizzlyExecutionException("pop index out of range");
        }
        
        return list.items().remove(index);
    }
    
    private static Value remove(ListValue list, List<Value> args) {
        requireArgCount("remove", args, 1);
        Value target = args.get(0);
        
        for (int i = 0; i < list.size(); i++) {
            if (areEqual(list.get(i), target)) {
                list.items().remove(i);
                return NullValue.INSTANCE;
            }
        }
        
        throw new GrizzlyExecutionException("list.remove(x): x not in list");
    }
    
    private static Value clear(ListValue list) {
        list.items().clear();
        return NullValue.INSTANCE;
    }
    
    private static Value reverse(ListValue list) {
        Collections.reverse(list.items());
        return NullValue.INSTANCE;
    }
    
    private static Value sort(ListValue list, List<Value> args) {
        boolean reverse = false;
        if (!args.isEmpty()) {
            reverse = args.get(0).isTruthy();
        }
        
        final boolean descending = reverse;
        
        list.items().sort((a, b) -> {
            int cmp;
            if (a instanceof NumberValue na && b instanceof NumberValue nb) {
                cmp = Double.compare(na.asDouble(), nb.asDouble());
            } else {
                cmp = asString(a).compareTo(asString(b));
            }
            return descending ? -cmp : cmp;
        });
        
        return NullValue.INSTANCE;
    }
    
    private static Value index(ListValue list, List<Value> args) {
        requireMinArgs("index", args, 1);
        Value target = args.get(0);
        int start = args.size() > 1 ? toInt(args.get(1)) : 0;
        int end = args.size() > 2 ? toInt(args.get(2)) : list.size();
        
        if (start < 0) start = Math.max(0, list.size() + start);
        if (end < 0) end = Math.max(0, list.size() + end);
        end = Math.min(end, list.size());
        
        for (int i = start; i < end; i++) {
            if (areEqual(list.get(i), target)) {
                return NumberValue.of(i);
            }
        }
        
        throw new GrizzlyExecutionException("Value not found in list");
    }
    
    private static Value count(ListValue list, List<Value> args) {
        requireArgCount("count", args, 1);
        Value target = args.get(0);
        
        int count = 0;
        for (Value item : list.items()) {
            if (areEqual(item, target)) {
                count++;
            }
        }
        
        return NumberValue.of(count);
    }
    
    private static Value copy(ListValue list) {
        return new ListValue(new ArrayList<>(list.items()));
    }
}
