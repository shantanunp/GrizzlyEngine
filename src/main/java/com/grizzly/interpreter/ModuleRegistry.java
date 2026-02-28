package com.grizzly.interpreter;

import com.grizzly.exception.GrizzlyExecutionException;
import com.grizzly.interpreter.GrizzlyInterpreter.BuiltinFunction;
import com.grizzly.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.grizzly.interpreter.ValueUtils.*;

/**
 * Registry for module functions (like Python's import system).
 * 
 * <p>Supported modules:
 * <ul>
 *   <li>re - Regular expressions (match, search, findall, sub, split)</li>
 * </ul>
 * 
 * <p>Usage in templates:
 * <pre>{@code
 * import re
 * 
 * match = re.match(r"^\d+", text)
 * result = re.sub(r"\s+", " ", text)
 * }</pre>
 */
public final class ModuleRegistry {
    
    private final Map<String, Map<String, BuiltinFunction>> modules = new HashMap<>();
    
    public ModuleRegistry() {
        registerReModule();
    }
    
    public Map<String, Map<String, BuiltinFunction>> getModules() {
        return modules;
    }
    
    public boolean containsModule(String name) {
        return modules.containsKey(name);
    }
    
    public Map<String, BuiltinFunction> getModule(String name) {
        return modules.get(name);
    }
    
    // ==================== RE Module ====================
    
    private void registerReModule() {
        Map<String, BuiltinFunction> re = new HashMap<>();
        
        // re.match() - Match pattern at start of string
        re.put("match", args -> {
            requireArgCount("re.match()", args, 2);
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                
                if (m.matches()) {
                    DictValue match = DictValue.empty();
                    match.put("matched", BoolValue.TRUE);
                    match.put("value", new StringValue(text));
                    
                    // Add captured groups
                    ListValue groups = ListValue.empty();
                    for (int i = 0; i <= m.groupCount(); i++) {
                        String group = m.group(i);
                        groups.append(group != null ? new StringValue(group) : NullValue.INSTANCE);
                    }
                    match.put("groups", groups);
                    
                    return match;
                }
                return NullValue.INSTANCE;
            } catch (PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.search() - Search for pattern anywhere in string
        re.put("search", args -> {
            requireArgCount("re.search()", args, 2);
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                
                if (m.find()) {
                    DictValue match = DictValue.empty();
                    match.put("matched", BoolValue.TRUE);
                    match.put("value", new StringValue(m.group()));
                    match.put("start", NumberValue.of(m.start()));
                    match.put("end", NumberValue.of(m.end()));
                    
                    // Add captured groups
                    ListValue groups = ListValue.empty();
                    for (int i = 0; i <= m.groupCount(); i++) {
                        String group = m.group(i);
                        groups.append(group != null ? new StringValue(group) : NullValue.INSTANCE);
                    }
                    match.put("groups", groups);
                    
                    return match;
                }
                return NullValue.INSTANCE;
            } catch (PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.findall() - Find all matches
        re.put("findall", args -> {
            requireArgCount("re.findall()", args, 2);
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                List<Value> matches = new ArrayList<>();
                
                while (m.find()) {
                    matches.add(new StringValue(m.group()));
                }
                
                return new ListValue(matches);
            } catch (PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.sub() - Replace pattern matches
        re.put("sub", args -> {
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
            } catch (PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.split() - Split by pattern
        re.put("split", args -> {
            requireArgCount("re.split()", args, 2);
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                String[] parts = text.split(pattern);
                List<Value> result = new ArrayList<>();
                for (String part : parts) {
                    result.add(new StringValue(part));
                }
                return new ListValue(result);
            } catch (PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        modules.put("re", re);
    }
}
