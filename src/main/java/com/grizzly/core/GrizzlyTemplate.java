package com.grizzly.core;

import com.grizzly.core.interpreter.GrizzlyInterpreter;
import com.grizzly.core.interpreter.InterpreterConfig;
import com.grizzly.core.parser.ast.Program;
import com.grizzly.core.types.DictValue;
import com.grizzly.core.types.ValueConverter;

import java.util.Map;
import java.util.Objects;

/**
 * A compiled template ready to transform data.
 * 
 * <p>This is the core, format-agnostic template class. It works with
 * {@link DictValue} input and output, with no knowledge of JSON, XML,
 * or any other data format.
 * 
 * <p><b>Why format-agnostic?</b>
 * <br>This allows the core engine to be used with any data format.
 * Format-specific wrappers (like JsonTemplate) handle conversion.
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Pattern 1: Direct DictValue (for format wrappers)</h3>
 * <pre>{@code
 * GrizzlyTemplate template = engine.compile(templateCode);
 * 
 * DictValue input = DictValue.empty();
 * input.put("name", new StringValue("John"));
 * 
 * DictValue output = template.execute(input);
 * }</pre>
 * 
 * <h3>Pattern 2: With Java Map (convenience)</h3>
 * <pre>{@code
 * Map<String, Object> input = Map.of("name", "John");
 * Map<String, Object> output = template.executeRaw(input);
 * }</pre>
 * 
 * @see com.grizzly.format.json.JsonTemplate For JSON convenience wrapper
 */
public class GrizzlyTemplate {
    
    private final Program program;
    private final GrizzlyInterpreter interpreter;
    
    /**
     * Create a template from a parsed program with default config.
     * 
     * @param program The parsed AST program
     * @throws NullPointerException if program is null
     */
    public GrizzlyTemplate(Program program) {
        this(program, InterpreterConfig.defaults());
    }
    
    /**
     * Create a template from a parsed program with custom config.
     * 
     * @param program The parsed AST program
     * @param config The interpreter configuration
     * @throws NullPointerException if program or config is null
     */
    public GrizzlyTemplate(Program program, InterpreterConfig config) {
        this.program = Objects.requireNonNull(program, "program cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
        this.interpreter = new GrizzlyInterpreter(program, config);
    }
    
    /**
     * Execute the template with type-safe DictValue input and output.
     * 
     * <p>This is the primary execution method. Format-specific wrappers
     * use this method after converting their input to DictValue.
     * 
     * @param input Input data as DictValue
     * @return Output data as DictValue
     * @throws NullPointerException if input is null
     * @throws com.grizzly.core.exception.GrizzlyExecutionException if transformation fails
     */
    public DictValue execute(DictValue input) {
        Objects.requireNonNull(input, "input cannot be null");
        return interpreter.executeTyped(input);
    }
    
    /**
     * Execute the template with Java Map input and output.
     * 
     * <p>This is a convenience method for working with standard Java Maps,
     * typically from JSON parsing via Jackson or similar.
     * 
     * @param inputMap Input data as a Map
     * @return Output data as a Map
     * @throws NullPointerException if inputMap is null
     * @throws com.grizzly.core.exception.GrizzlyExecutionException if transformation fails
     */
    public Map<String, Object> executeRaw(Map<String, Object> inputMap) {
        Objects.requireNonNull(inputMap, "inputMap cannot be null");
        DictValue input = ValueConverter.fromJavaMap(inputMap);
        DictValue output = interpreter.executeTyped(input);
        return ValueConverter.toJavaMap(output);
    }
    
    /**
     * Get the parsed program (for inspection/debugging).
     * 
     * @return The AST program
     */
    public Program getProgram() {
        return program;
    }
}
