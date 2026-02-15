package com.grizzly;

import com.grizzly.interpreter.GrizzlyInterpreter;
import com.grizzly.mapper.PojoMapper;
import com.grizzly.parser.ast.Program;

import java.util.Map;

/**
 * A compiled Grizzly template
 * 
 * This represents a parsed and ready-to-execute template.
 * You can execute it multiple times with different input data.
 * 
 * Example:
 * 
 * GrizzlyTemplate template = engine.compile("transform.py");
 * 
 * // Execute many times
 * for (Customer customer : customers) {
 *     CustomerDTO dto = template.execute(customer, CustomerDTO.class);
 * }
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
     * Execute the template with input data
     * 
     * @param input Input POJO
     * @param outputClass Output class type
     * @return Output POJO
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
