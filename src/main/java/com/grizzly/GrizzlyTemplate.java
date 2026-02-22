package com.grizzly;

import com.grizzly.interpreter.GrizzlyInterpreter;
import com.grizzly.mapper.PojoMapper;
import com.grizzly.parser.ast.Program;

import java.util.Map;

/**
 * A compiled template ready to transform data.
 * 
 * <p>Think of this like a recipe. You compile it once, then use it many times.
 * 
 * <p><b>Why compile once?</b>
 * <br>Compiling is slow (parses the template). Executing is fast.
 * <br>So compile once, execute many times = better performance!
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * // Compile once
 * GrizzlyTemplate template = engine.compile("transform.py");
 * 
 * // Execute many times (fast!)
 * for (Customer customer : customers) {
 *     CustomerDTO result = template.execute(customer, CustomerDTO.class);
 *     save(result);
 * }
 * }</pre>
 */
public class GrizzlyTemplate {
    
    private final Program program;
    private final GrizzlyInterpreter interpreter;
    private final PojoMapper mapper;
    
    public GrizzlyTemplate(Program program, PojoMapper mapper) {
        this.program = program;
        this.interpreter = new GrizzlyInterpreter(program);
        this.mapper = mapper;
    }
    
    /**
     * Transform input data to output data.
     * 
     * <p>Converts your Java object to a Map, runs the template, converts back to Java.
     * 
     * @param input Your input object (any POJO)
     * @param outputClass What type you want back
     * @return Transformed output object
     */
    public <T> T execute(Object input, Class<T> outputClass) {
        // Convert input POJO to Map (Python INPUT)
        Map<String, Object> inputMap = mapper.pojoToMap(input);
        
        // Execute the template (Python execution)
        Map<String, Object> outputMap = interpreter.execute(inputMap);
        
        // Convert output Map to POJO
        return mapper.mapToPojo(outputMap, outputClass);
    }
    
    /**
     * Execute with Map input/output (for advanced usage)
     */
    public Map<String, Object> executeRaw(Map<String, Object> inputMap) {
        return interpreter.execute(inputMap);
    }
    
    /**
     * Get the parsed program (for inspection)
     */
    public Program getProgram() {
        return program;
    }
}
