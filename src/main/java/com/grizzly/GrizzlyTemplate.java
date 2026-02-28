package com.grizzly;

import com.grizzly.interpreter.GrizzlyInterpreter;
import com.grizzly.mapper.PojoMapper;
import com.grizzly.parser.ast.Program;

import java.util.Map;
import java.util.Objects;

/**
 * A compiled template ready to transform data.
 * 
 * <p>Think of this like a recipe. You compile it once, then use it many times.
 * 
 * <p><b>Why compile once?</b>
 * <br>Compiling is slow (parses the template). Executing is fast.
 * <br>So compile once, execute many times = better performance!
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Pattern 1: POJO to POJO (recommended for typed data)</h3>
 * <pre>{@code
 * GrizzlyTemplate template = engine.compile("transform.py");
 * 
 * for (Customer customer : customers) {
 *     CustomerDTO result = template.execute(customer, CustomerDTO.class);
 *     save(result);
 * }
 * }</pre>
 * 
 * <h3>Pattern 2: Map to Map (for JSON/dynamic data)</h3>
 * <pre>{@code
 * GrizzlyTemplate template = engine.compileFromString(code);
 * 
 * Map<String, Object> input = Map.of("name", "John", "age", 30);
 * Map<String, Object> output = template.executeRaw(input);
 * }</pre>
 * 
 * <h3>Pattern 3: Type-safe POJO input (generic method)</h3>
 * <pre>{@code
 * Customer customer = new Customer("John", 30);
 * CustomerDTO result = template.executePojo(customer, CustomerDTO.class);
 * }</pre>
 */
public class GrizzlyTemplate {
    
    private final Program program;
    private final GrizzlyInterpreter interpreter;
    private final PojoMapper mapper;
    
    /**
     * Create a template from a parsed program.
     * 
     * @param program The parsed AST program
     * @param mapper The POJO mapper for conversions
     * @throws NullPointerException if program or mapper is null
     */
    public GrizzlyTemplate(Program program, PojoMapper mapper) {
        this.program = Objects.requireNonNull(program, "program cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
        this.interpreter = new GrizzlyInterpreter(program);
    }
    
    /**
     * Transform input POJO to output POJO.
     * 
     * <p>Converts your Java object to a Map, runs the template, converts back to Java.
     * This is the most convenient method when working with typed POJOs.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * Customer customer = new Customer("John", 30);
     * CustomerDTO result = template.execute(customer, CustomerDTO.class);
     * }</pre>
     * 
     * @param <I> Input type (inferred from input parameter)
     * @param <O> Output type (specified by outputClass)
     * @param input Your input object (any POJO, not null)
     * @param outputClass The class of the output object
     * @return Transformed output object
     * @throws NullPointerException if input or outputClass is null
     * @throws com.grizzly.exception.GrizzlyExecutionException if transformation fails
     */
    public <I, O> O execute(I input, Class<O> outputClass) {
        Objects.requireNonNull(input, "input cannot be null");
        Objects.requireNonNull(outputClass, "outputClass cannot be null");
        
        // Convert input POJO to Map (Python INPUT)
        Map<String, Object> inputMap = mapper.pojoToMap(input);
        
        // Execute the template (Python execution)
        Map<String, Object> outputMap = interpreter.execute(inputMap);
        
        // Convert output Map to POJO
        return mapper.mapToPojo(outputMap, outputClass);
    }
    
    /**
     * Transform input Map to output Map.
     * 
     * <p>This is the most direct method, ideal for JSON/dynamic data where
     * you're working with Maps directly.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * Map<String, Object> input = Map.of("firstName", "John", "lastName", "Doe");
     * Map<String, Object> output = template.executeRaw(input);
     * String json = objectMapper.writeValueAsString(output);
     * }</pre>
     * 
     * @param inputMap Input data as a Map (typically from JSON parsing)
     * @return Output data as a Map (suitable for JSON serialization)
     * @throws NullPointerException if inputMap is null
     * @throws com.grizzly.exception.GrizzlyExecutionException if transformation fails
     */
    public Map<String, Object> executeRaw(Map<String, Object> inputMap) {
        Objects.requireNonNull(inputMap, "inputMap cannot be null");
        return interpreter.execute(inputMap);
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
