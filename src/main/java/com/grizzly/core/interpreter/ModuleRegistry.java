package com.grizzly.core.interpreter;

import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.core.interpreter.GrizzlyInterpreter.BuiltinFunction;
import com.grizzly.core.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.grizzly.core.interpreter.ValueUtils.*;

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
    
    /** Module attributes (for import datetime: datetime.datetime, datetime.timedelta) */
    private final Map<String, DictValue> moduleAttributes = new HashMap<>();
    
    public ModuleRegistry() {
        registerReModule();
        registerDatetimeModule();
        registerDecimalModule();
    }
    
    public Map<String, Map<String, BuiltinFunction>> getModules() {
        return modules;
    }
    
    public boolean containsModule(String name) {
        return modules.containsKey(name) || moduleAttributes.containsKey(name);
    }
    
    public Map<String, BuiltinFunction> getModule(String name) {
        return modules.get(name);
    }
    
    /** Get module as DictValue for import datetime (datetime.datetime.now, etc.) */
    public DictValue getModuleValue(String name) {
        return moduleAttributes.get(name);
    }
    
    /** Get exported name for from X import Y (e.g. from decimal import Decimal) */
    public Value getModuleExport(String moduleName, String exportName) {
        DictValue mod = moduleAttributes.get(moduleName);
        if (mod != null && mod.containsKey(exportName)) {
            return mod.get(exportName);
        }
        Map<String, BuiltinFunction> modFns = modules.get(moduleName);
        if (modFns != null && modFns.containsKey(exportName)) {
            return new CallableValue(modFns.get(exportName));
        }
        return null;
    }
    
    // ==================== RE Module ====================
    
    private void registerReModule() {
        Map<String, BuiltinFunction> re = new HashMap<>();
        
        // re.match() - Match pattern at start of string. Python-compatible: returns Match with .group(), .groups()
        re.put("match", (args, kw) -> {
            requireArgCount("re.match()", args, 2);
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                
                if (m.lookingAt()) {
                    List<Value> groups = new ArrayList<>();
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String g = m.group(i);
                        groups.add(g != null ? new StringValue(g) : NullValue.INSTANCE);
                    }
                    return new ReMatchValue(m.group(0), groups);
                }
                return NullValue.INSTANCE;
            } catch (PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.search() - Search for pattern anywhere. Python-compatible: returns Match with .group(), .groups()
        re.put("search", (args, kw) -> {
            requireArgCount("re.search()", args, 2);
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                
                if (m.find()) {
                    List<Value> groups = new ArrayList<>();
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String g = m.group(i);
                        groups.add(g != null ? new StringValue(g) : NullValue.INSTANCE);
                    }
                    return new ReMatchValue(m.group(0), groups);
                }
                return NullValue.INSTANCE;
            } catch (PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.findall() - Python-compliant: returns groups when present
        re.put("findall", (args, kw) -> {
            requireArgCount("re.findall()", args, 2);
            String pattern = asString(args.get(0));
            String text = asString(args.get(1));
            
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                List<Value> matches = new ArrayList<>();
                int groupCount = p.matcher("").groupCount();
                
                while (m.find()) {
                    if (groupCount == 0) {
                        matches.add(new StringValue(m.group()));
                    } else if (groupCount == 1) {
                        String g = m.group(1);
                        matches.add(g != null ? new StringValue(g) : NullValue.INSTANCE);
                    } else {
                        List<Value> groups = new ArrayList<>();
                        for (int i = 1; i <= groupCount; i++) {
                            String g = m.group(i);
                            groups.add(g != null ? new StringValue(g) : NullValue.INSTANCE);
                        }
                        matches.add(new ListValue(groups));
                    }
                }
                
                return new ListValue(matches);
            } catch (PatternSyntaxException e) {
                throw new GrizzlyExecutionException(
                    "Invalid regex pattern: " + pattern + " - " + e.getMessage()
                );
            }
        });
        
        // re.sub() - Replace pattern matches. Python: \1, \g<1> for backrefs; convert to Java $1
        re.put("sub", (args, kw) -> {
            if (args.size() != 3) {
                throw new GrizzlyExecutionException(
                    "re.sub() requires 3 arguments: re.sub(pattern, replacement, text)"
                );
            }
            
            String pattern = asString(args.get(0));
            String replacement = pythonToJavaBackrefs(asString(args.get(1)));
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
        re.put("split", (args, kw) -> {
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
        // Also add as DictValue for "import re" (re.match, re.search, etc. in context)
        DictValue reMod = DictValue.empty();
        for (Map.Entry<String, BuiltinFunction> e : re.entrySet()) {
            reMod.put(e.getKey(), new CallableValue(e.getValue()));
        }
        moduleAttributes.put("re", reMod);
    }
    
    /** Convert Python strftime %Y, %m, etc. to Java DateTimeFormatter pattern. */
    private static String pythonToJavaDateFormat(String fmt) {
        return fmt
            .replace("%Y", "yyyy")
            .replace("%m", "MM")
            .replace("%d", "dd")
            .replace("%H", "HH")
            .replace("%M", "mm")
            .replace("%S", "ss")
            .replace("%f", "SSSSSS")
            .replace("%y", "yy")
            .replace("%B", "MMMM")
            .replace("%b", "MMM")
            .replace("%A", "EEEE")
            .replace("%a", "EEE");
    }
    
    /** Convert Python \1, \g<1>, \g<name> to Java $1, $2 */
    private static String pythonToJavaBackrefs(String replacement) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < replacement.length()) {
            if (replacement.charAt(i) == '\\' && i + 1 < replacement.length()) {
                char next = replacement.charAt(i + 1);
                if (next >= '1' && next <= '9') {
                    sb.append('$').append(next);
                    i += 2;
                    continue;
                }
                if (next == 'g' && i + 2 < replacement.length() && replacement.charAt(i + 2) == '<') {
                    int end = replacement.indexOf('>', i + 3);
                    if (end > 0) {
                        String num = replacement.substring(i + 3, end);
                        if (num.matches("\\d+")) {
                            sb.append('$').append(num);
                            i = end + 1;
                            continue;
                        }
                    }
                }
            }
            sb.append(replacement.charAt(i));
            i++;
        }
        return sb.toString();
    }
    
    // ==================== Datetime Module ====================
    
    private void registerDatetimeModule() {
        DictValue datetimeClass = DictValue.empty();
        datetimeClass.put("now", new CallableValue((args, kw) -> {
            if (args.size() > 1) {
                throw new GrizzlyExecutionException("datetime.now() takes 0 or 1 argument (timezone)");
            }
            if (args.isEmpty()) {
                return new DateTimeValue(java.time.ZonedDateTime.now());
            }
            String tz = asString(args.get(0));
            return new DateTimeValue(java.time.ZonedDateTime.now(java.time.ZoneId.of(tz)));
        }));
        datetimeClass.put("strptime", new CallableValue((args, kw) -> {
            requireArgCount("datetime.strptime()", args, 2);
            String s = asString(args.get(0));
            String fmt = pythonToJavaDateFormat(asString(args.get(1)));
            try {
                var dt = java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern(fmt));
                return new DateTimeValue(dt.atZone(java.time.ZoneId.systemDefault()));
            } catch (Exception e) {
                var d = java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern(fmt));
                return new DateTimeValue(d.atStartOfDay(java.time.ZoneId.systemDefault()));
            }
        }));
        
        DictValue timedeltaClass = DictValue.empty();
        timedeltaClass.put("__call", new CallableValue((args, kw) -> {
            long days = args.size() > 0 ? toLong(args.get(0)) : 0;
            long seconds = args.size() > 1 ? toLong(args.get(1)) : 0;
            var td = java.time.Duration.ofDays(days).plusSeconds(seconds);
            return new DateTimeValue(java.time.ZonedDateTime.now().plus(td));
        }));
        
        DictValue datetimeMod = DictValue.empty();
        datetimeMod.put("datetime", datetimeClass);
        datetimeMod.put("timedelta", timedeltaClass);
        moduleAttributes.put("datetime", datetimeMod);
    }
    
    // ==================== Decimal Module ====================
    
    private void registerDecimalModule() {
        DictValue decimalMod = DictValue.empty();
        decimalMod.put("Decimal", new CallableValue((args, kw) -> {
            requireArgCount("Decimal()", args, 1);
            Value v = args.get(0);
            if (v instanceof StringValue s) {
                return new DecimalValue(s.value());
            }
            if (v instanceof NumberValue n) {
                return n.isInteger() ? new DecimalValue(n.asInt()) : new DecimalValue(String.valueOf(n.asDouble()));
            }
            throw new GrizzlyExecutionException("Decimal() argument must be a string or number");
        }));
        moduleAttributes.put("decimal", decimalMod);
    }
}
