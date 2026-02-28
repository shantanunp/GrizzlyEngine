package com.grizzly;

import com.grizzly.format.FormatRegistry;
import com.grizzly.interpreter.GrizzlyInterpreter;
import com.grizzly.mapper.PojoMapper;
import com.grizzly.parser.ast.Program;
import com.grizzly.types.DictValue;

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
    
    // ==================== Format-Aware Methods ====================
    
    /**
     * Transform with explicit input and output formats.
     * 
     * <p>This is the most flexible method, supporting any registered format.
     * 
     * <h3>Supported Formats:</h3>
     * <ul>
     *   <li>"json" - JSON format</li>
     *   <li>"xml" - XML format</li>
     * </ul>
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * // JSON to XML
     * String xml = template.transform(jsonInput, "json", "xml");
     * 
     * // XML to JSON
     * String json = template.transform(xmlInput, "xml", "json");
     * }</pre>
     * 
     * @param input The input content string
     * @param inputFormat The input format ("json", "xml", etc.)
     * @param outputFormat The output format ("json", "xml", etc.)
     * @return The transformed output string
     * @throws com.grizzly.format.FormatException if format is not supported
     * @throws com.grizzly.exception.GrizzlyExecutionException if transformation fails
     */
    public String transform(String input, String inputFormat, String outputFormat) {
        Objects.requireNonNull(input, "input cannot be null");
        Objects.requireNonNull(inputFormat, "inputFormat cannot be null");
        Objects.requireNonNull(outputFormat, "outputFormat cannot be null");
        
        FormatRegistry registry = FormatRegistry.defaultRegistry();
        DictValue inputValue = registry.getReader(inputFormat).read(input);
        DictValue outputValue = interpreter.executeTyped(inputValue);
        return registry.getWriter(outputFormat).write(outputValue);
    }
    
    /**
     * Transform with explicit formats using a custom registry.
     * 
     * <p>Use this when you have registered custom format handlers.
     * 
     * @param input The input content string
     * @param inputFormat The input format
     * @param outputFormat The output format
     * @param registry The format registry to use
     * @return The transformed output string
     */
    public String transform(String input, String inputFormat, String outputFormat, 
                           FormatRegistry registry) {
        Objects.requireNonNull(input, "input cannot be null");
        Objects.requireNonNull(inputFormat, "inputFormat cannot be null");
        Objects.requireNonNull(outputFormat, "outputFormat cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        
        DictValue inputValue = registry.getReader(inputFormat).read(input);
        DictValue outputValue = interpreter.executeTyped(inputValue);
        return registry.getWriter(outputFormat).write(outputValue);
    }
    
    /**
     * Transform JSON input to JSON output.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * String jsonOutput = template.transformJson(jsonInput);
     * }</pre>
     * 
     * @param jsonInput JSON input string
     * @return JSON output string
     */
    public String transformJson(String jsonInput) {
        return transform(jsonInput, "json", "json");
    }
    
    /**
     * Transform JSON input to XML output.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * String xmlOutput = template.transformJsonToXml(jsonInput);
     * }</pre>
     * 
     * @param jsonInput JSON input string
     * @return XML output string
     */
    public String transformJsonToXml(String jsonInput) {
        return transform(jsonInput, "json", "xml");
    }
    
    /**
     * Transform XML input to JSON output.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * String jsonOutput = template.transformXmlToJson(xmlInput);
     * }</pre>
     * 
     * @param xmlInput XML input string
     * @return JSON output string
     */
    public String transformXmlToJson(String xmlInput) {
        return transform(xmlInput, "xml", "json");
    }
    
    /**
     * Transform XML input to XML output.
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * String xmlOutput = template.transformXml(xmlInput);
     * }</pre>
     * 
     * @param xmlInput XML input string
     * @return XML output string
     */
    public String transformXml(String xmlInput) {
        return transform(xmlInput, "xml", "xml");
    }
    
    /**
     * Execute with type-safe DictValue input and output.
     * 
     * <p>This method is useful when working directly with the Value hierarchy,
     * bypassing format conversion.
     * 
     * @param input Input data as DictValue
     * @return Output data as DictValue
     */
    public DictValue executeTyped(DictValue input) {
        Objects.requireNonNull(input, "input cannot be null");
        return interpreter.executeTyped(input);
    }
}
