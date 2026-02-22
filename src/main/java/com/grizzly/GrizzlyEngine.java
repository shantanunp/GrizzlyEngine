package com.grizzly;

import com.grizzly.lexer.GrizzlyLexer;
import com.grizzly.lexer.Token;
import com.grizzly.mapper.PojoMapper;
import com.grizzly.parser.GrizzlyParser;
import com.grizzly.parser.ast.Program;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grizzly Engine - Transforms data using Python-like templates.
 * 
 * <p>This is your starting point. Create an engine, give it a template,
 * and it transforms your data.
 * 
 * <p><b>Quick Start:</b>
 * <pre>{@code
 * // 1. Create engine
 * GrizzlyEngine engine = new GrizzlyEngine();
 * 
 * // 2. Write template as string
 * String template = """
 *     def transform(INPUT):
 *         OUTPUT = {}
 *         OUTPUT["name"] = INPUT.firstName + " " + INPUT.lastName
 *         return OUTPUT
 *     """;
 * 
 * // 3. Compile template
 * GrizzlyTemplate compiled = engine.compileFromString(template);
 * 
 * // 4. Execute with your data
 * Map<String, Object> input = Map.of("firstName", "John", "lastName", "Doe");
 * Map<String, Object> result = compiled.executeRaw(input);
 * 
 * System.out.println(result.get("name")); // "John Doe"
 * }</pre>
 * 
 * <p><b>For Production (with caching):</b>
 * <pre>{@code
 * GrizzlyEngine engine = new GrizzlyEngine();
 * GrizzlyTemplate template = engine.compile("templates/customer.py");
 * 
 * // Reuse template for many inputs (much faster!)
 * for (Customer customer : customers) {
 *     CustomerDTO output = template.execute(customer, CustomerDTO.class);
 *     process(output);
 * }
 * }</pre>
 */
public class GrizzlyEngine {
    
    private final PojoMapper mapper;
    private final Map<String, GrizzlyTemplate> templateCache;
    private final boolean enableCaching;
    
    /**
     * Create a new Grizzly engine with caching enabled
     */
    public GrizzlyEngine() {
        this(true);
    }
    
    /**
     * Create a new Grizzly engine
     * 
     * @param enableCaching Whether to cache compiled templates
     */
    public GrizzlyEngine(boolean enableCaching) {
        this.mapper = new PojoMapper();
        this.enableCaching = enableCaching;
        this.templateCache = enableCaching ? new ConcurrentHashMap<>() : null;
    }
    
    /**
     * Transform input to output using a template
     * 
     * @param input Input POJO
     * @param templatePath Path to Python template file
     * @param outputClass Output class type
     * @return Output POJO
     */
    public <T> T transform(Object input, String templatePath, Class<T> outputClass) {
        GrizzlyTemplate template = getOrCompile(templatePath);
        return template.execute(input, outputClass);
    }
    
    /**
     * Compile a template from a file
     * 
     * @param templatePath Path to Python template file
     * @return Compiled template
     */
    public GrizzlyTemplate compile(String templatePath) {
        try {
            // Read the template file
            String code = Files.readString(Path.of(templatePath));
            return compileFromString(code);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template file: " + templatePath, e);
        }
    }
    
    /**
     * Compile a template from a string
     * 
     * @param pythonCode Python template code
     * @return Compiled template
     */
    public GrizzlyTemplate compileFromString(String pythonCode) {
        // Step 1: Tokenize (Lexer)
        GrizzlyLexer lexer = new GrizzlyLexer(pythonCode);
        List<Token> tokens = lexer.tokenize();
        
        // Step 2: Parse (Parser → AST)
        GrizzlyParser parser = new GrizzlyParser(tokens);
        Program program = parser.parse();
        
        // Step 3: Create compiled template
        return new GrizzlyTemplate(program, mapper);
    }
    
    /**
     * Clear the template cache
     */
    public void clearCache() {
        if (templateCache != null) {
            templateCache.clear();
        }
    }
    
    /**
     * Remove a specific template from cache
     */
    public void evict(String templatePath) {
        if (templateCache != null) {
            templateCache.remove(templatePath);
        }
    }
    
    /**
     * Get cache statistics
     */
    public int getCacheSize() {
        return templateCache != null ? templateCache.size() : 0;
    }
    
    /**
     * Get the POJO mapper (for advanced usage)
     */
    public PojoMapper getMapper() {
        return mapper;
    }
    
    // === Private methods ===
    
    private GrizzlyTemplate getOrCompile(String templatePath) {
        if (enableCaching) {
            return templateCache.computeIfAbsent(templatePath, this::compile);
        } else {
            return compile(templatePath);
        }
    }
}
