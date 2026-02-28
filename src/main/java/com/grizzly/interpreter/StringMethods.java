package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.grizzly.interpreter.ValueUtils.*;

/**
 * String method implementations for the Grizzly interpreter.
 * 
 * <p>Supports Python-like string methods:
 * upper, lower, strip, split, join, replace, startswith, endswith, etc.
 */
public final class StringMethods {
    
    private StringMethods() {}
    
    /**
     * Evaluate a method call on a string value.
     * 
     * @param str The string value
     * @param methodName The method name
     * @param args Evaluated arguments
     * @return The result
     */
    public static Value evaluate(StringValue str, String methodName, List<Value> args) {
        return switch (methodName) {
            // Case transformation
            case "upper" -> new StringValue(str.value().toUpperCase());
            case "lower" -> new StringValue(str.value().toLowerCase());
            case "capitalize" -> capitalize(str.value());
            case "title" -> title(str.value());
            case "swapcase" -> swapcase(str.value());
            
            // Whitespace handling
            case "strip" -> new StringValue(str.value().strip());
            case "lstrip" -> new StringValue(str.value().stripLeading());
            case "rstrip" -> new StringValue(str.value().stripTrailing());
            
            // Splitting and joining
            case "split" -> split(str.value(), args);
            case "join" -> join(str.value(), args);
            case "splitlines" -> splitlines(str.value());
            
            // Search and replace
            case "replace" -> replace(str.value(), args);
            case "find" -> find(str.value(), args);
            case "rfind" -> rfind(str.value(), args);
            case "index" -> index(str.value(), args);
            case "count" -> count(str.value(), args);
            
            // Prefix/suffix checks
            case "startswith" -> startswith(str.value(), args);
            case "endswith" -> endswith(str.value(), args);
            case "contains" -> contains(str.value(), args);
            
            // Character type checks
            case "isdigit" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(Character::isDigit));
            case "isalpha" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(Character::isLetter));
            case "isalnum" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(Character::isLetterOrDigit));
            case "isspace" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(Character::isWhitespace));
            case "islower" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().filter(Character::isLetter).allMatch(Character::isLowerCase));
            case "isupper" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().filter(Character::isLetter).allMatch(Character::isUpperCase));
            case "isnumeric" -> BoolValue.of(!str.value().isEmpty() && str.value().chars().allMatch(c -> Character.isDigit(c) || Character.getType(c) == Character.OTHER_NUMBER));
            
            // Padding
            case "zfill" -> zfill(str.value(), args);
            case "ljust" -> ljust(str.value(), args);
            case "rjust" -> rjust(str.value(), args);
            case "center" -> center(str.value(), args);
            
            default -> throw new GrizzlyExecutionException("Unknown string method: " + methodName);
        };
    }
    
    private static Value capitalize(String s) {
        if (s.isEmpty()) return new StringValue(s);
        return new StringValue(Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase());
    }
    
    private static Value title(String s) {
        if (s.isEmpty()) return new StringValue(s);
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return new StringValue(sb.toString());
    }
    
    private static Value swapcase(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append(Character.toLowerCase(c));
            } else if (Character.isLowerCase(c)) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return new StringValue(sb.toString());
    }
    
    private static Value split(String s, List<Value> args) {
        String sep = args.isEmpty() ? null : asString(args.get(0));
        int maxSplit = args.size() > 1 ? toInt(args.get(1)) : -1;
        
        String[] parts;
        if (sep == null) {
            parts = s.strip().split("\\s+", maxSplit > 0 ? maxSplit + 1 : -1);
        } else {
            parts = s.split(java.util.regex.Pattern.quote(sep), maxSplit > 0 ? maxSplit + 1 : -1);
        }
        
        List<Value> result = new ArrayList<>();
        for (String part : parts) {
            if (sep != null || !part.isEmpty()) {
                result.add(new StringValue(part));
            }
        }
        return new ListValue(result);
    }
    
    private static Value splitlines(String s) {
        String[] lines = s.split("\\r?\\n", -1);
        List<Value> result = new ArrayList<>();
        for (String line : lines) {
            result.add(new StringValue(line));
        }
        return new ListValue(result);
    }
    
    private static Value join(String sep, List<Value> args) {
        requireArgCount("join", args, 1);
        ListValue list = requireType("join", args.get(0), ListValue.class, "argument");
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Value item : list.items()) {
            if (!first) sb.append(sep);
            sb.append(asString(item));
            first = false;
        }
        return new StringValue(sb.toString());
    }
    
    private static Value replace(String s, List<Value> args) {
        requireArgCountRange("replace", args, 2, 3);
        String old = asString(args.get(0));
        String replacement = asString(args.get(1));
        int count = args.size() > 2 ? toInt(args.get(2)) : -1;
        
        if (count < 0) {
            return new StringValue(s.replace(old, replacement));
        }
        
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        int replacements = 0;
        int idx;
        while ((idx = s.indexOf(old, lastEnd)) != -1 && (count < 0 || replacements < count)) {
            result.append(s, lastEnd, idx);
            result.append(replacement);
            lastEnd = idx + old.length();
            replacements++;
        }
        result.append(s.substring(lastEnd));
        return new StringValue(result.toString());
    }
    
    private static Value find(String s, List<Value> args) {
        requireMinArgs("find", args, 1);
        String substr = asString(args.get(0));
        int start = args.size() > 1 ? toInt(args.get(1)) : 0;
        int end = args.size() > 2 ? toInt(args.get(2)) : s.length();
        
        String searchRegion = s.substring(Math.max(0, start), Math.min(s.length(), end));
        int idx = searchRegion.indexOf(substr);
        return NumberValue.of(idx >= 0 ? idx + start : -1);
    }
    
    private static Value rfind(String s, List<Value> args) {
        requireMinArgs("rfind", args, 1);
        String substr = asString(args.get(0));
        int start = args.size() > 1 ? toInt(args.get(1)) : 0;
        int end = args.size() > 2 ? toInt(args.get(2)) : s.length();
        
        String searchRegion = s.substring(Math.max(0, start), Math.min(s.length(), end));
        int idx = searchRegion.lastIndexOf(substr);
        return NumberValue.of(idx >= 0 ? idx + start : -1);
    }
    
    private static Value index(String s, List<Value> args) {
        Value result = find(s, args);
        if (((NumberValue) result).asInt() < 0) {
            throw new GrizzlyExecutionException("substring not found");
        }
        return result;
    }
    
    private static Value count(String s, List<Value> args) {
        requireArgCount("count", args, 1);
        String substr = asString(args.get(0));
        
        if (substr.isEmpty()) {
            return NumberValue.of(s.length() + 1);
        }
        
        int count = 0;
        int idx = 0;
        while ((idx = s.indexOf(substr, idx)) != -1) {
            count++;
            idx += substr.length();
        }
        return NumberValue.of(count);
    }
    
    private static Value startswith(String s, List<Value> args) {
        requireMinArgs("startswith", args, 1);
        String prefix = asString(args.get(0));
        int start = args.size() > 1 ? toInt(args.get(1)) : 0;
        int end = args.size() > 2 ? toInt(args.get(2)) : s.length();
        
        String region = s.substring(Math.max(0, start), Math.min(s.length(), end));
        return BoolValue.of(region.startsWith(prefix));
    }
    
    private static Value endswith(String s, List<Value> args) {
        requireMinArgs("endswith", args, 1);
        String suffix = asString(args.get(0));
        int start = args.size() > 1 ? toInt(args.get(1)) : 0;
        int end = args.size() > 2 ? toInt(args.get(2)) : s.length();
        
        String region = s.substring(Math.max(0, start), Math.min(s.length(), end));
        return BoolValue.of(region.endsWith(suffix));
    }
    
    private static Value contains(String s, List<Value> args) {
        requireArgCount("contains", args, 1);
        String substr = asString(args.get(0));
        return BoolValue.of(s.contains(substr));
    }
    
    private static Value zfill(String s, List<Value> args) {
        requireArgCount("zfill", args, 1);
        int width = toInt(args.get(0));
        
        if (s.length() >= width) {
            return new StringValue(s);
        }
        
        boolean negative = s.startsWith("-");
        String digits = negative ? s.substring(1) : s;
        String padded = "0".repeat(width - s.length()) + digits;
        return new StringValue(negative ? "-" + padded : padded);
    }
    
    private static Value ljust(String s, List<Value> args) {
        requireMinArgs("ljust", args, 1);
        int width = toInt(args.get(0));
        String fillchar = args.size() > 1 ? asString(args.get(1)) : " ";
        
        if (fillchar.length() != 1) {
            throw new GrizzlyExecutionException("ljust() fill character must be a single character");
        }
        
        if (s.length() >= width) {
            return new StringValue(s);
        }
        
        return new StringValue(s + fillchar.repeat(width - s.length()));
    }
    
    private static Value rjust(String s, List<Value> args) {
        requireMinArgs("rjust", args, 1);
        int width = toInt(args.get(0));
        String fillchar = args.size() > 1 ? asString(args.get(1)) : " ";
        
        if (fillchar.length() != 1) {
            throw new GrizzlyExecutionException("rjust() fill character must be a single character");
        }
        
        if (s.length() >= width) {
            return new StringValue(s);
        }
        
        return new StringValue(fillchar.repeat(width - s.length()) + s);
    }
    
    private static Value center(String s, List<Value> args) {
        requireMinArgs("center", args, 1);
        int width = toInt(args.get(0));
        String fillchar = args.size() > 1 ? asString(args.get(1)) : " ";
        
        if (fillchar.length() != 1) {
            throw new GrizzlyExecutionException("center() fill character must be a single character");
        }
        
        if (s.length() >= width) {
            return new StringValue(s);
        }
        
        int totalPadding = width - s.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        
        return new StringValue(fillchar.repeat(leftPadding) + s + fillchar.repeat(rightPadding));
    }
}
