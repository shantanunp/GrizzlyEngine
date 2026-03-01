package com.grizzly.core;

import com.grizzly.core.interpreter.InterpreterConfig;
import com.grizzly.core.lexer.GrizzlyLexer;
import com.grizzly.core.lexer.Token;
import com.grizzly.core.logging.GrizzlyLogger;
import com.grizzly.core.parser.GrizzlyParser;
import com.grizzly.core.parser.ast.Program;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The core Grizzly Engine - compiles Python-like templates.
 * 
 * <p>This is the format-agnostic entry point for template compilation.
 * It produces {@link GrizzlyTemplate} instances that work with
 * {@link com.grizzly.core.types.DictValue} input/output.
 * 
 * <h2>Compilation Pipeline</h2>
 * <pre>{@code
 * ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
 * │   Source    │     │   Tokens    │     │    AST      │     │  Template   │
 * │   Code      │────▶│   (List)    │────▶│   (Tree)    │────▶│  (Ready)    │
 * └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
 *        │                  │                   │                   │
 *     LEXER              PARSER            TEMPLATE              EXECUTE
 * }</pre>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Compile a template
 * GrizzlyEngine engine = new GrizzlyEngine();
 * GrizzlyTemplate template = engine.compile(templateCode);
 * 
 * // Execute with DictValue (core API)
 * DictValue output = template.execute(input);
 * 
 * // Or use format-specific wrappers
 * JsonTemplate jsonTemplate = new JsonTemplate(template);
 * String jsonOutput = jsonTemplate.transform(jsonInput);
 * }</pre>
 * 
 * @see GrizzlyTemplate The compiled template
 * @see com.grizzly.format.json.JsonTemplate JSON convenience wrapper
 */
public class GrizzlyEngine {
    
    private final Map<String, GrizzlyTemplate> templateCache;
    private final boolean enableCaching;
    private final InterpreterConfig config;
    
    /**
     * Create a new engine with default settings and caching enabled.
     */
    public GrizzlyEngine() {
        this(true, InterpreterConfig.defaults());
    }
    
    /**
     * Create a new engine with specified caching.
     * 
     * @param enableCaching Whether to cache compiled templates
     */
    public GrizzlyEngine(boolean enableCaching) {
        this(enableCaching, InterpreterConfig.defaults());
    }
    
    /**
     * Create a new engine with custom configuration (caching enabled).
     * 
     * @param config Interpreter configuration for all templates
     */
    public GrizzlyEngine(InterpreterConfig config) {
        this(true, config);
    }
    
    /**
     * Create a new engine with custom configuration.
     * 
     * @param enableCaching Whether to cache compiled templates
     * @param config Interpreter configuration for all templates
     */
    public GrizzlyEngine(boolean enableCaching, InterpreterConfig config) {
        this.enableCaching = enableCaching;
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.templateCache = enableCaching ? new ConcurrentHashMap<>() : null;
    }
    
    /**
     * Compile a template from a string.
     * 
     * @param templateCode The Python-like template code
     * @return Compiled template ready for execution
     * @throws com.grizzly.core.exception.GrizzlyParseException if syntax is invalid
     */
    public GrizzlyTemplate compile(String templateCode) {
        Objects.requireNonNull(templateCode, "templateCode cannot be null");
        
        GrizzlyLogger.info("ENGINE", "Starting compilation pipeline");
        GrizzlyLogger.separator("ENGINE");
        
        // Step 1: Tokenize (Lexer)
        GrizzlyLogger.debug("ENGINE", "Step 1/2: Tokenization (Lexer)");
        GrizzlyLexer lexer = new GrizzlyLexer(templateCode);
        List<Token> tokens = lexer.tokenize();
        
        // Step 2: Parse (Parser → AST)
        GrizzlyLogger.debug("ENGINE", "Step 2/2: Parsing (Parser → AST)");
        GrizzlyParser parser = new GrizzlyParser(tokens);
        Program program = parser.parse();
        
        GrizzlyLogger.separator("ENGINE");
        GrizzlyLogger.info("ENGINE", "Compilation complete");
        
        return new GrizzlyTemplate(program, config);
    }
    
    /**
     * Compile a template from a file.
     * 
     * @param templatePath Path to the template file
     * @return Compiled template ready for execution
     * @throws RuntimeException if file cannot be read
     * @throws com.grizzly.core.exception.GrizzlyParseException if syntax is invalid
     */
    public GrizzlyTemplate compileFromFile(String templatePath) {
        if (enableCaching) {
            return templateCache.computeIfAbsent(templatePath, this::doCompileFromFile);
        }
        return doCompileFromFile(templatePath);
    }
    
    private GrizzlyTemplate doCompileFromFile(String templatePath) {
        try {
            String code = Files.readString(Path.of(templatePath));
            return compile(code);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template file: " + templatePath, e);
        }
    }
    
    /**
     * Clear the template cache.
     */
    public void clearCache() {
        if (templateCache != null) {
            templateCache.clear();
        }
    }
    
    /**
     * Remove a specific template from cache.
     * 
     * @param templatePath The template path to evict
     */
    public void evict(String templatePath) {
        if (templateCache != null) {
            templateCache.remove(templatePath);
        }
    }
    
    /**
     * Get the number of cached templates.
     * 
     * @return Cache size
     */
    public int getCacheSize() {
        return templateCache != null ? templateCache.size() : 0;
    }
}
