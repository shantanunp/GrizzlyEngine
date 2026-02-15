# Grizzly Engine - Phase 1 Complete! 🐻

## ✅ What's Been Built

### Core Infrastructure
1. **Gradle Build** (JDK 21+)
   - Only Jackson dependency (~1.5MB)
   - Maven publishing ready
   - JUnit 5 + AssertJ for testing

2. **Lexer (Tokenizer)** - COMPLETE ✅
   - Handles all Python tokens we need
   - Indentation tracking (INDENT/DEDENT)
   - Line/column numbers for errors
   - String literals (including triple-quoted docstrings)
   - Comments, numbers, identifiers
   - All operators: =, ==, !=, <, >, etc.
   - Full test coverage

3. **Token Types** - COMPLETE ✅
   - Keywords: def, if, else, return
   - Literals: strings, numbers, identifiers
   - Operators: all comparison & assignment
   - Delimiters: ( ) { } [ ] , . :

4. **Exception Handling** - COMPLETE ✅
   - `GrizzlyParseException` - Template syntax errors
   - `GrizzlyExecutionException` - Runtime errors
   - Both include line/column numbers

## 📋 Next Steps: Phase 2

### 1. AST Nodes (Abstract Syntax Tree)

Create these classes in `src/main/java/com/grizzly/parser/ast/`:

```java
// Base
interface ASTNode {}

// Top level
class Program implements ASTNode {
    List<FunctionDef> functions;
}

class FunctionDef implements ASTNode {
    String name;
    List<String> params;
    List<Statement> body;
    int lineNumber;
}

// Statements
interface Statement extends ASTNode {}

class Assignment implements Statement {
    Expression target;    // OUTPUT["field"]
    Expression value;     // INPUT.source
    int lineNumber;
}

class ReturnStatement implements Statement {
    Expression value;
    int lineNumber;
}

class FunctionCall implements Statement {
    String name;
    List<Expression> args;
    int lineNumber;
}

class IfStatement implements Statement {
    Expression condition;
    List<Statement> thenBlock;
    List<Statement> elseBlock;
    int lineNumber;
}

// Expressions
interface Expression extends ASTNode {}

class DictAccess implements Expression {
    Expression object;     // OUTPUT
    Expression key;        // "field" or nested DictAccess
}

class AttrAccess implements Expression {
    Expression object;     // INPUT
    String attr;          // customerId
}

class StringLiteral implements Expression {
    String value;
}

class NumberLiteral implements Expression {
    String value;
}

class Identifier implements Expression {
    String name;
}

class BinaryOp implements Expression {
    Expression left;
    String operator;      // ==, !=, <, >, etc.
    Expression right;
}

class DictLiteral implements Expression {
    // For OUTPUT = {}
}
```

### 2. Parser

Create `src/main/java/com/grizzly/parser/GrizzlyParser.java`:

```java
public class GrizzlyParser {
    private final List<Token> tokens;
    private int position = 0;
    
    public Program parse(List<Token> tokens) {
        // Build AST from tokens
        List<FunctionDef> functions = new ArrayList<>();
        
        while (!isAtEnd()) {
            if (peek().getType() == TokenType.DEF) {
                functions.add(parseFunction());
            } else {
                advance(); // Skip comments, newlines, etc.
            }
        }
        
        return new Program(functions);
    }
    
    private FunctionDef parseFunction() {
        // def transform(INPUT):
        //     body...
    }
    
    private Statement parseStatement() {
        // Handles assignment, return, if, function call
    }
    
    private Expression parseExpression() {
        // Handles OUTPUT["field"], INPUT.value, etc.
    }
}
```

### 3. Interpreter

Create `src/main/java/com/grizzly/interpreter/`:

```java
public class GrizzlyInterpreter {
    private final Map<String, Object> builtins;
    
    public Object execute(Program program, Map<String, Object> input) {
        // Find "transform" function
        FunctionDef transform = findFunction(program, "transform");
        
        // Create execution context
        ExecutionContext context = new ExecutionContext();
        context.set("INPUT", input);
        
        // Execute function
        return executeFunction(transform, context, List.of(input));
    }
    
    private Object executeFunction(FunctionDef func, ExecutionContext ctx, List<Object> args) {
        // Execute function body
    }
    
    private Object executeStatement(Statement stmt, ExecutionContext ctx) {
        return switch (stmt) {
            case Assignment a -> executeAssignment(a, ctx);
            case ReturnStatement r -> evaluateExpression(r.value, ctx);
            case FunctionCall f -> executeFunctionCall(f, ctx);
            case IfStatement i -> executeIf(i, ctx);
        };
    }
    
    private Object evaluateExpression(Expression expr, ExecutionContext ctx) {
        return switch (expr) {
            case DictAccess d -> evaluateDictAccess(d, ctx);
            case AttrAccess a -> evaluateAttrAccess(a, ctx);
            case StringLiteral s -> s.value;
            case Identifier i -> ctx.get(i.name);
            // etc.
        };
    }
}
```

### 4. POJO Mapper

Create `src/main/java/com/grizzly/mapper/PojoMapper.java`:

```java
public class PojoMapper {
    private final ObjectMapper jackson = new ObjectMapper();
    
    public Map<String, Object> pojoToMap(Object pojo) {
        // POJO → Map using Jackson
        return jackson.convertValue(pojo, new TypeReference<>() {});
    }
    
    public <T> T mapToPojo(Map<String, Object> map, Class<T> clazz) {
        // Map → POJO using Jackson
        return jackson.convertValue(map, clazz);
    }
}
```

### 5. Main API

Create `src/main/java/com/grizzly/GrizzlyEngine.java`:

```java
public class GrizzlyEngine {
    private final PojoMapper mapper = new PojoMapper();
    private final Map<String, GrizzlyTemplate> cache = new ConcurrentHashMap<>();
    
    public <T> T transform(Object input, String templatePath, Class<T> outputClass) {
        // Get or compile template
        GrizzlyTemplate template = cache.computeIfAbsent(templatePath, this::compile);
        
        // Execute
        return template.execute(input, outputClass);
    }
    
    public GrizzlyTemplate compile(String templatePath) {
        // Read file
        String code = Files.readString(Path.of(templatePath));
        
        // Lex
        GrizzlyLexer lexer = new GrizzlyLexer(code);
        List<Token> tokens = lexer.tokenize();
        
        // Parse
        GrizzlyParser parser = new GrizzlyParser();
        Program program = parser.parse(tokens);
        
        // Return compiled template
        return new GrizzlyTemplate(program, mapper);
    }
}
```

### 6. GrizzlyTemplate

Create `src/main/java/com/grizzly/GrizzlyTemplate.java`:

```java
public class GrizzlyTemplate {
    private final Program program;
    private final PojoMapper mapper;
    private final GrizzlyInterpreter interpreter;
    
    public <T> T execute(Object input, Class<T> outputClass) {
        // Convert input POJO to Map
        Map<String, Object> inputMap = mapper.pojoToMap(input);
        
        // Execute template
        Map<String, Object> outputMap = (Map<String, Object>) interpreter.execute(program, inputMap);
        
        // Convert output Map to POJO
        return mapper.mapToPojo(outputMap, outputClass);
    }
}
```

## 🧪 Testing Strategy

### Test Your Python Template

```java
@Test
void shouldTransformSimpleTemplate() {
    String template = """
        def transform(INPUT):
            OUTPUT = {}
            OUTPUT["clientReference"] = INPUT.customerId
            return OUTPUT
        """;
    
    // Write to file
    Files.writeString(Path.of("test.py"), template);
    
    // Create input
    InputPojo input = new InputPojo();
    input.setCustomerId("CUST-001");
    
    // Execute
    GrizzlyEngine engine = new GrizzlyEngine();
    OutputPojo result = engine.transform(input, "test.py", OutputPojo.class);
    
    // Verify
    assertThat(result.getClientReference()).isEqualTo("CUST-001");
}
```

## 📦 Project Files

All files are in `grizzly-engine-phase1.tar.gz`:

```
grizzly-engine/
├── build.gradle                              ✅
├── settings.gradle                           ✅
├── README.md                                 ✅
├── src/main/java/com/grizzly/
│   ├── lexer/
│   │   ├── Token.java                        ✅
│   │   ├── TokenType.java                    ✅
│   │   └── GrizzlyLexer.java                 ✅
│   └── exception/
│       ├── GrizzlyParseException.java        ✅
│       └── GrizzlyExecutionException.java    ✅
└── src/test/java/com/grizzly/
    └── lexer/GrizzlyLexerTest.java           ✅
```

## 🚀 To Continue Development

1. Extract: `tar -xzf grizzly-engine-phase1.tar.gz`
2. Build: `cd grizzly-engine && ./gradlew build`
3. Run tests: `./gradlew test`
4. Create AST nodes (see above)
5. Build parser
6. Build interpreter
7. Test with real templates!

## 🎯 Milestones

- [x] **Phase 1**: Lexer + Infrastructure (DONE)
- [ ] **Phase 2**: Parser + AST
- [ ] **Phase 3**: Interpreter + Execution
- [ ] **Phase 4**: POJO Mapper + Main API
- [ ] **Phase 5**: Built-in Functions
- [ ] **Phase 6**: Module Support

## 📚 Resources

- **Lexer reference**: See `GrizzlyLexer.java` - handles all tokens
- **Token types**: See `TokenType.java` - all supported tokens
- **Error handling**: See exception classes - includes line numbers

---

**Ready to continue on GitHub Copilot!** 🐻

Just paste the AST node structure above and start building the parser.
