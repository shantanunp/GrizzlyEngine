package com.grizzly;

import com.grizzly.lexer.GrizzlyLexer;
import com.grizzly.lexer.Token;
import com.grizzly.logging.GrizzlyLogger;
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
 * <h1>Grizzly Engine - The Main Entry Point</h1>
 * 
 * <p>The GrizzlyEngine is your starting point for transforming data using Python-like
 * templates. It orchestrates the entire compilation pipeline:
 * 
 * <pre>{@code
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                         GRIZZLY ENGINE PIPELINE                             │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │                                                                             │
 * │  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐              │
 * │  │  Source  │───▶│  LEXER   │───▶│  PARSER  │───▶│INTERPRETER│───▶ Result  │
 * │  │   Code   │    │          │    │          │    │           │              │
 * │  └──────────┘    └──────────┘    └──────────┘    └──────────┘              │
 * │                       │              │               │                      │
 * │                       ▼              ▼               ▼                      │
 * │                   Tokens           AST           Execute                    │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * }</pre>
 * 
 * <h2>Quick Start (5 Minutes)</h2>
 * 
 * <pre>{@code
 * // 1. Create engine
 * GrizzlyEngine engine = new GrizzlyEngine();
 * 
 * // 2. Write a simple template
 * String template = """
 *     def transform(INPUT):
 *         OUTPUT = {}
 *         OUTPUT["fullName"] = INPUT["firstName"] + " " + INPUT["lastName"]
 *         OUTPUT["greeting"] = "Hello, " + OUTPUT["fullName"] + "!"
 *         return OUTPUT
 *     """;
 * 
 * // 3. Compile the template (one-time cost)
 * GrizzlyTemplate compiled = engine.compileFromString(template);
 * 
 * // 4. Execute with your data (fast, can repeat many times)
 * Map<String, Object> input = Map.of("firstName", "John", "lastName", "Doe");
 * Map<String, Object> result = compiled.executeRaw(input);
 * 
 * System.out.println(result.get("greeting")); // "Hello, John Doe!"
 * }</pre>
 * 
 * <h2>Understanding the Pipeline</h2>
 * 
 * <h3>Step 1: LEXER (Tokenization)</h3>
 * <p>The lexer reads your source code character-by-character and groups them into tokens:
 * <pre>{@code
 * Input:  "OUTPUT = {}"
 * Output: [IDENTIFIER("OUTPUT"), ASSIGN, LBRACE, RBRACE]
 * }</pre>
 * <p>See: {@link com.grizzly.lexer.GrizzlyLexer}
 * 
 * <h3>Step 2: PARSER (AST Construction)</h3>
 * <p>The parser reads tokens and builds a tree structure representing the code:
 * <pre>{@code
 * Tokens: [IDENTIFIER("OUTPUT"), ASSIGN, LBRACE, RBRACE]
 * AST:    Assignment(target=Identifier("OUTPUT"), value=DictLiteral{})
 * }</pre>
 * <p>See: {@link com.grizzly.parser.GrizzlyParser}
 * 
 * <h3>Step 3: INTERPRETER (Execution)</h3>
 * <p>The interpreter walks the AST and executes each node:
 * <pre>{@code
 * AST:    Assignment(OUTPUT = {})
 * Action: Create empty dict, store in variable "OUTPUT"
 * }</pre>
 * <p>See: {@link com.grizzly.interpreter.GrizzlyInterpreter}
 * 
 * <h2>Production Usage with Caching</h2>
 * 
 * <p>For production, compile once and reuse many times:
 * 
 * <pre>{@code
 * // At startup: compile template once
 * GrizzlyEngine engine = new GrizzlyEngine();
 * GrizzlyTemplate template = engine.compile("templates/customer-transform.py");
 * 
 * // For each request: execute with different data (fast!)
 * for (Customer customer : customers) {
 *     CustomerDTO output = template.execute(customer, CustomerDTO.class);
 *     processOutput(output);
 * }
 * }</pre>
 * 
 * <h2>Enabling Debug Logging</h2>
 * 
 * <p>To see what's happening inside the engine, enable logging:
 * 
 * <pre>{@code
 * // See all steps in the pipeline
 * GrizzlyLogger.setLevel(GrizzlyLogger.LogLevel.DEBUG);
 * 
 * // Now compile and execute
 * GrizzlyTemplate template = engine.compileFromString(code);
 * 
 * // Output shows:
 * // [INFO ] [LEXER      ] Starting tokenization (45 chars)
 * // [DEBUG] [LEXER      ] Token: DEF at 1:1
 * // [DEBUG] [LEXER      ] Token: IDENTIFIER("transform") at 1:5
 * // ...
 * // [INFO ] [PARSER     ] Parsing complete (1 functions)
 * // [DEBUG] [PARSER     ] === AST ===
 * // [DEBUG] [PARSER     ] Program
 * // [DEBUG] [PARSER     ]   └── FunctionDef: transform(INPUT)
 * // ...
 * // [INFO ] [INTERPRETER] Execution complete in 5ms
 * }</pre>
 * 
 * <h2>Template Syntax Guide</h2>
 * 
 * <pre>{@code
 * # Variables
 * x = 42
 * name = "John"
 * 
 * # Dictionaries (like JSON objects)
 * OUTPUT = {}
 * OUTPUT["key"] = "value"
 * person = {"name": "John", "age": 30}
 * 
 * # Lists (arrays)
 * items = [1, 2, 3]
 * items.append(4)
 * 
 * # Attribute access (for input data)
 * firstName = INPUT.firstName
 * city = INPUT.address.city
 * 
 * # Conditions
 * if INPUT.age >= 18:
 *     OUTPUT["status"] = "adult"
 * else:
 *     OUTPUT["status"] = "minor"
 * 
 * # Loops
 * for item in INPUT.items:
 *     OUTPUT["items"].append(item.name)
 * 
 * # Functions
 * def helper(x):
 *     return x * 2
 * 
 * def transform(INPUT):
 *     OUTPUT = {}
 *     OUTPUT["doubled"] = helper(INPUT.value)
 *     return OUTPUT
 * }</pre>
 * 
 * @see com.grizzly.lexer.GrizzlyLexer Step 1: Tokenization
 * @see com.grizzly.parser.GrizzlyParser Step 2: Parsing
 * @see com.grizzly.interpreter.GrizzlyInterpreter Step 3: Execution
 * @see GrizzlyTemplate Compiled template for execution
 * @see GrizzlyLogger Logging utility for debugging
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
     * Compile a template from a string.
     * 
     * <p>This method runs the full compilation pipeline:
     * <ol>
     *   <li><b>Lexer</b>: Converts source code to tokens</li>
     *   <li><b>Parser</b>: Converts tokens to AST (Abstract Syntax Tree)</li>
     *   <li><b>Template</b>: Wraps AST for execution</li>
     * </ol>
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * String code = """
     *     def transform(INPUT):
     *         OUTPUT = {}
     *         OUTPUT["name"] = INPUT.firstName
     *         return OUTPUT
     *     """;
     * 
     * GrizzlyTemplate template = engine.compileFromString(code);
     * Map<String, Object> result = template.executeRaw(input);
     * }</pre>
     * 
     * @param pythonCode Python template code as a string
     * @return Compiled template ready for execution
     * @throws com.grizzly.exception.GrizzlyParseException if syntax is invalid
     */
    public GrizzlyTemplate compileFromString(String pythonCode) {
        GrizzlyLogger.info("ENGINE", "Starting compilation pipeline");
        GrizzlyLogger.separator("ENGINE");
        
        // Step 1: Tokenize (Lexer)
        GrizzlyLogger.debug("ENGINE", "Step 1/3: Tokenization (Lexer)");
        GrizzlyLexer lexer = new GrizzlyLexer(pythonCode);
        List<Token> tokens = lexer.tokenize();
        
        // Step 2: Parse (Parser → AST)
        GrizzlyLogger.debug("ENGINE", "Step 2/3: Parsing (Parser → AST)");
        GrizzlyParser parser = new GrizzlyParser(tokens);
        Program program = parser.parse();
        
        // Step 3: Create compiled template
        GrizzlyLogger.debug("ENGINE", "Step 3/3: Creating compiled template");
        GrizzlyTemplate template = new GrizzlyTemplate(program, mapper);
        
        GrizzlyLogger.separator("ENGINE");
        GrizzlyLogger.info("ENGINE", "Compilation complete");
        
        return template;
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
