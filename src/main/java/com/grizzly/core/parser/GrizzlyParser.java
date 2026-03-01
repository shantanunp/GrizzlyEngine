package com.grizzly.core.parser;

import com.grizzly.core.exception.GrizzlyParseException;
import com.grizzly.core.lexer.Token;
import com.grizzly.core.lexer.TokenType;
import com.grizzly.core.logging.GrizzlyLogger;
import com.grizzly.core.parser.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>Parser - Step 2 of the Compilation Pipeline</h1>
 * 
 * <p>The Parser takes the flat list of tokens from the Lexer and builds a hierarchical
 * tree structure called an <b>Abstract Syntax Tree (AST)</b>. This tree represents
 * the grammatical structure of your code.
 * 
 * <h2>What is Parsing?</h2>
 * 
 * <p>Parsing is like understanding the grammar of a sentence. The lexer gives us "words"
 * (tokens), and the parser figures out how they relate to form "sentences" (statements).
 * 
 * <pre>{@code
 * English:
 *   Sentence: "The quick brown fox jumps over the lazy dog"
 *   Grammar:  [Subject] [Verb] [Object]
 *   Tree:     Sentence
 *               ├── Subject: "The quick brown fox"
 *               ├── Verb: "jumps over"
 *               └── Object: "the lazy dog"
 * 
 * Code:
 *   Statement: "OUTPUT["name"] = INPUT.firstName"
 *   Grammar:   [Target] [Operator] [Value]
 *   Tree:      Assignment
 *                ├── target: DictAccess(OUTPUT, "name")
 *                ├── operator: =
 *                └── value: AttrAccess(INPUT, "firstName")
 * }</pre>
 * 
 * <h2>The Compilation Pipeline</h2>
 * 
 * <pre>{@code
 * ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
 * │   Source    │     │   Tokens    │     │    AST      │     │   Result    │
 * │   Code      │────▶│   (List)    │────▶│   (Tree)    │────▶│   (Map)     │
 * │             │     │             │     │             │     │             │
 * └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
 *        │                  │                   │                   │
 *     LEXER              PARSER            INTERPRETER          OUTPUT
 *                     (this class)
 * }</pre>
 * 
 * <h2>What is an AST (Abstract Syntax Tree)?</h2>
 * 
 * <p>An AST is a tree where each node represents a part of your code. It's "abstract"
 * because it ignores unimportant details (like whitespace) and focuses on the meaning.
 * 
 * <pre>{@code
 * Code:
 * def transform(INPUT):
 *     OUTPUT = {}
 *     OUTPUT["name"] = INPUT.firstName
 *     return OUTPUT
 * 
 * AST:
 * Program
 *   └── FunctionDef
 *         ├── name: "transform"
 *         ├── parameters: ["INPUT"]
 *         └── body:
 *               ├── Assignment
 *               │     ├── target: Identifier("OUTPUT")
 *               │     └── value: DictLiteral({})
 *               │
 *               ├── Assignment
 *               │     ├── target: DictAccess
 *               │     │             ├── object: Identifier("OUTPUT")
 *               │     │             └── key: StringLiteral("name")
 *               │     └── value: AttrAccess
 *               │                   ├── object: Identifier("INPUT")
 *               │                   └── attribute: "firstName"
 *               │
 *               └── ReturnStatement
 *                     └── value: Identifier("OUTPUT")
 * }</pre>
 * 
 * <h2>AST Node Types</h2>
 * 
 * <h3>Statements (things that DO something):</h3>
 * <ul>
 *   <li>{@link FunctionDef} - Function definition: {@code def name(params):}</li>
 *   <li>{@link Assignment} - Variable assignment: {@code x = value}</li>
 *   <li>{@link IfStatement} - Conditional: {@code if condition:}</li>
 *   <li>{@link ForLoop} - Loop: {@code for item in items:}</li>
 *   <li>{@link ReturnStatement} - Return value: {@code return value}</li>
 * </ul>
 * 
 * <h3>Expressions (things that PRODUCE a value):</h3>
 * <ul>
 *   <li>{@link Identifier} - Variable name: {@code x, INPUT, OUTPUT}</li>
 *   <li>{@link StringLiteral} - String: {@code "hello"}</li>
 *   <li>{@link NumberLiteral} - Number: {@code 42, 3.14}</li>
 *   <li>{@link BinaryOp} - Operations: {@code a + b, x == y}</li>
 *   <li>{@link AttrAccess} - Dot access: {@code INPUT.name}</li>
 *   <li>{@link DictAccess} - Bracket access: {@code OUTPUT["key"]}</li>
 *   <li>{@link MethodCall} - Method call: {@code list.append(item)}</li>
 * </ul>
 * 
 * <h2>Operator Precedence</h2>
 * 
 * <p>The parser respects operator precedence (like PEMDAS in math):
 * 
 * <pre>{@code
 * Precedence (lowest to highest):
 * 1. or              (logical OR)
 * 2. and             (logical AND)
 * 3. not             (logical NOT)
 * 4. ==, !=, <, >    (comparisons)
 * 5. +, -            (addition/subtraction)
 * 6. *, /, //, %     (multiplication/division)
 * 7. **              (exponentiation)
 * 
 * Example: 2 + 3 * 4 → BinaryOp(2, "+", BinaryOp(3, "*", 4))
 * Because * has higher precedence than +
 * }</pre>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // First, tokenize the source
 * GrizzlyLexer lexer = new GrizzlyLexer(sourceCode);
 * List<Token> tokens = lexer.tokenize();
 * 
 * // Then parse tokens into AST
 * GrizzlyParser parser = new GrizzlyParser(tokens);
 * Program program = parser.parse();
 * 
 * // The program contains functions
 * FunctionDef transform = program.findFunction("transform");
 * System.out.println("Parameters: " + transform.parameters());
 * }</pre>
 * 
 * <h2>Debugging</h2>
 * 
 * <p>Enable logging to see the parsing process:
 * 
 * <pre>{@code
 * GrizzlyLogger.setLevel(GrizzlyLogger.LogLevel.DEBUG);
 * Program program = parser.parse();
 * 
 * // Output shows AST tree structure:
 * // [DEBUG] [PARSER     ] === AST ===
 * // [DEBUG] [PARSER     ] Program
 * // [DEBUG] [PARSER     ]   └── FunctionDef: transform(INPUT)
 * // [DEBUG] [PARSER     ]         ├── Assignment: OUTPUT = DictLiteral{}
 * // ...
 * }</pre>
 * 
 * <h2>Grammar Rules (Simplified)</h2>
 * 
 * <p>The parser implements these grammar rules:
 * 
 * <pre>{@code
 * program     → function*
 * function    → "def" IDENTIFIER "(" params ")" ":" block
 * block       → INDENT statement+ DEDENT
 * statement   → assignment | return | if | for | expression
 * assignment  → expression "=" expression
 * expression  → or_expr
 * or_expr     → and_expr ("or" and_expr)*
 * and_expr    → not_expr ("and" not_expr)*
 * not_expr    → "not" not_expr | comparison
 * comparison  → addition (("==" | "!=" | "<" | ">") addition)*
 * addition    → multiplication (("+" | "-") multiplication)*
 * multiplication → power (("*" | "/" | "%") power)*
 * power       → primary ("**" power)?
 * primary     → literal | identifier | "(" expression ")" | access
 * }</pre>
 * 
 * @see GrizzlyLexer The previous step: converts source to tokens
 * @see com.grizzly.interpreter.GrizzlyInterpreter The next step: executes the AST
 * @see Program The root AST node containing all functions
 */
public class GrizzlyParser {
    
    private final List<Token> tokens;
    private int position = 0;
    
    public GrizzlyParser(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    /**
     * Parse all tokens into a Program (the root AST node).
     * 
     * <p>This method reads through all tokens and constructs the complete
     * Abstract Syntax Tree. It expects to find:
     * <ul>
     *   <li>Zero or more import statements</li>
     *   <li>One or more function definitions (at least 'transform')</li>
     * </ul>
     * 
     * <h3>Example:</h3>
     * <pre>{@code
     * // Parse tokens into AST
     * GrizzlyParser parser = new GrizzlyParser(tokens);
     * Program program = parser.parse();
     * 
     * // Access the transform function
     * FunctionDef transform = program.findFunction("transform");
     * }</pre>
     * 
     * @return Program containing all parsed functions and imports
     * @throws GrizzlyParseException if no functions are found or syntax is invalid
     */
    public Program parse() {
        GrizzlyLogger.info("PARSER", "Starting parsing (" + tokens.size() + " tokens)");
        
        List<ImportStatement> imports = new ArrayList<>();
        List<FunctionDef> functions = new ArrayList<>();
        
        // Skip initial newlines and comments
        skipNewlines();
        
        // Parse all top-level statements (imports and functions)
        while (!isAtEnd()) {
            if (peek().type() == TokenType.IMPORT) {
                ImportStatement imp = parseImportStatement();
                imports.add(imp);
                GrizzlyLogger.debug("PARSER", "Parsed import: " + imp.moduleName());
                skipNewlines();
            } else if (peek().type() == TokenType.DEF) {
                FunctionDef func = parseFunction();
                functions.add(func);
                GrizzlyLogger.debug("PARSER", "Parsed function: " + func.name() + 
                    "(" + String.join(", ", func.params()) + ") with " + 
                    func.body().size() + " statements");
                skipNewlines(); // Skip blank lines between functions
            } else if (peek().type() == TokenType.NEWLINE || peek().type() == TokenType.COMMENT) {
                advance();
            } else if (peek().type() == TokenType.EOF) {
                break;
            } else {
                advance(); // Skip unknown tokens for now
            }
        }
        
        if (functions.isEmpty()) {
            throw new GrizzlyParseException("No functions found in template");
        }
        
        Program program = new Program(imports, functions);
        
        GrizzlyLogger.info("PARSER", "Parsing complete (" + functions.size() + " functions)");
        GrizzlyLogger.logAST(program);
        
        return program;
    }
    
    /**
     * Parse an import statement
     * Example: import re
     */
    private ImportStatement parseImportStatement() {
        int lineNumber = peek().line();
        expect(TokenType.IMPORT, "Expected 'import'");
        String moduleName = expect(TokenType.IDENTIFIER, "Expected module name").value();
        return new ImportStatement(moduleName, lineNumber);
    }
    
    /**
     * Parse a function definition
     * Example: def transform(INPUT):
     */
    private FunctionDef parseFunction() {
        int lineNumber = peek().line();
        
        // Consume 'def'
        expect(TokenType.DEF, "Expected 'def'");
        
        // Get function name
        String name = expect(TokenType.IDENTIFIER, "Expected function name").value();
        
        // Parse parameters
        expect(TokenType.LPAREN, "Expected '('");
        List<String> params = new ArrayList<>();
        
        if (peek().type() != TokenType.RPAREN) {
            do {
                params.add(expect(TokenType.IDENTIFIER, "Expected parameter name").value());
                if (peek().type() == TokenType.COMMA) {
                    advance();
                }
            } while (peek().type() != TokenType.RPAREN);
        }
        
        expect(TokenType.RPAREN, "Expected ')'");
        expect(TokenType.COLON, "Expected ':' after function signature");
        skipNewlines();
        
        // Skip INDENT if present (flexible for multiple functions)
        if (peek().type() == TokenType.INDENT) {
            advance();
        }
        
        // Parse function body
        List<Statement> body = parseBlock();
        
        if (body.isEmpty()) {
            throw new GrizzlyParseException(
                "Function body cannot be empty at line " + lineNumber,
                lineNumber,
                1
            );
        }
        
        return new FunctionDef(name, params, body, lineNumber);
    }
    
    /**
     * Parse an indented block of statements
     */
    private List<Statement> parseBlock() {
        List<Statement> statements = new ArrayList<>();
        
        while (!isAtEnd()) {
            TokenType type = peek().type();
            
            // Stop at next function definition or EOF
            if (type == TokenType.DEF || type == TokenType.EOF) {
                break;
            }
            
            // Skip newlines and dedents
            if (type == TokenType.NEWLINE || type == TokenType.DEDENT || type == TokenType.INDENT) {
                advance();
                continue;
            }
            
            statements.add(parseStatement());
            skipNewlines();
        }
        
        return statements;
    }
    
    /**
     * Parse a single statement
     */
    /**
     * Parse a single statement.
     * 
     * <p>Handles all statement types: return, if, for, break, continue, assignments, function calls.
     */
    private Statement parseStatement() {
        int lineNumber = peek().line();
        
        // Import statement
        if (peek().type() == TokenType.IMPORT) {
            advance(); // Skip 'import'
            String moduleName = expect(TokenType.IDENTIFIER, "Expected module name after 'import'").value();
            return new ImportStatement(moduleName, lineNumber);
        }
        
        // Return statement
        if (peek().type() == TokenType.RETURN) {
            advance();
            Expression value = parseExpression();
            return new ReturnStatement(value, lineNumber);
        }
        
        // Break statement
        if (peek().type() == TokenType.BREAK) {
            advance();
            return new BreakStatement(lineNumber);
        }
        
        // Continue statement
        if (peek().type() == TokenType.CONTINUE) {
            advance();
            return new ContinueStatement(lineNumber);
        }
        
        // If statement
        if (peek().type() == TokenType.IF) {
            return parseIfStatement();
        }
        
        // For loop
        if (peek().type() == TokenType.FOR) {
            return parseForLoop();
        }
        
        // Check for function call (identifier followed by '(')
        if (peek().type() == TokenType.IDENTIFIER) {
            Token identToken = peek();
            int savePos = position;
            advance();
            
            if (peek().type() == TokenType.LPAREN) {
                // It's a function call
                String functionName = identToken.value();
                advance(); // Skip '('
                
                List<Expression> args = new ArrayList<>();
                if (peek().type() != TokenType.RPAREN) {
                    do {
                        args.add(parseExpression());
                        if (peek().type() == TokenType.COMMA) {
                            advance();
                        }
                    } while (peek().type() != TokenType.RPAREN);
                }
                
                expect(TokenType.RPAREN, "Expected ')'");
                return new FunctionCall(functionName, args, lineNumber);
            } else {
                // Not a function call, restore position
                position = savePos;
            }
        }
        
        // Try to parse as expression (might be assignment or method call)
        Expression expr = parseExpression();
        
        // Check if it's a method call expression (like list.append())
        if (expr instanceof MethodCall) {
            // Wrap it in an ExpressionStatement
            return new ExpressionStatement(expr, lineNumber);
        }
        
        // Check if it's an assignment
        if (peek().type() == TokenType.ASSIGN) {
            advance();
            Expression value = parseExpression();
            return new Assignment(expr, value, lineNumber);
        }
        
        throw new GrizzlyParseException("Expected statement at line " + lineNumber);
    }
    
    /**
     * Parse if statement
     */
    /**
     * Parse if/elif/else statement with support for multiple elif branches.
     * 
     * <p><b>Syntax:</b>
     * <pre>{@code
     * if condition:
     *     statements
     * elif condition:
     *     statements
     * elif condition:
     *     statements
     * else:
     *     statements
     * }</pre>
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * if x < 0:
     *     status = "negative"
     * elif x == 0:
     *     status = "zero"
     * else:
     *     status = "positive"
     * }</pre>
     * 
     * @return IfStatement with all branches
     */
    private IfStatement parseIfStatement() {
        int lineNumber = peek().line();
        
        expect(TokenType.IF, "Expected 'if'");
        Expression condition = parseExpression();
        expect(TokenType.COLON, "Expected ':' after if condition");
        skipNewlines();
        
        // Skip INDENT if present (flexible)
        if (peek().type() == TokenType.INDENT) {
            advance();
        }
        
        List<Statement> thenBlock = parseIfBlock();
        
        if (thenBlock.isEmpty()) {
            throw new GrizzlyParseException(
                "'if' block cannot be empty at line " + lineNumber,
                lineNumber,
                1
            );
        }
        
        // Consume DEDENT after if block
        if (peek().type() == TokenType.DEDENT) {
            advance();
        }
        
        // Skip newlines (but NOT dedents/indents) to find elif/else
        skipNewlines();
        
        // Parse elif branches (can be multiple)
        List<IfStatement.ElifBranch> elifBranches = new ArrayList<>();
        while (peek().type() == TokenType.ELIF) {
            int elifLine = peek().line();
            advance(); // consume elif
            
            Expression elifCondition = parseExpression();
            expect(TokenType.COLON, "Expected ':' after elif condition");
            skipNewlines();
            
            // Skip INDENT if present
            if (peek().type() == TokenType.INDENT) {
                advance();
            }
            
            List<Statement> elifBlock = parseIfBlock();
            
            if (elifBlock.isEmpty()) {
                throw new GrizzlyParseException(
                    "'elif' block cannot be empty at line " + elifLine,
                    elifLine,
                    1
                );
            }
            
            elifBranches.add(new IfStatement.ElifBranch(elifCondition, elifBlock));
            
            // Consume DEDENT after elif block
            if (peek().type() == TokenType.DEDENT) {
                advance();
            }
            
            // Skip newlines (but NOT dedents) to find next elif/else
            skipNewlines();
        }
        
        // Parse else block (optional)
        List<Statement> elseBlock = null;
        if (peek().type() == TokenType.ELSE) {
            int elseLine = peek().line();
            advance();
            expect(TokenType.COLON, "Expected ':' after else");
            skipNewlines();
            
            // Skip INDENT if present (flexible)
            if (peek().type() == TokenType.INDENT) {
                advance();
            }
            
            elseBlock = parseIfBlock();
            
            if (elseBlock.isEmpty()) {
                throw new GrizzlyParseException(
                    "'else' block cannot be empty at line " + elseLine,
                    elseLine,
                    1
                );
            }
            
            // Consume DEDENT after else block
            if (peek().type() == TokenType.DEDENT) {
                advance();
            }
        }
        
        return new IfStatement(condition, thenBlock, elifBranches, elseBlock, lineNumber);
    }
    
    /**
     * Parse an indented block for if/elif/else (stops at ELIF, ELSE, DEDENT, DEF, or EOF).
     * Does NOT consume the DEDENT - caller must handle it.
     */
    private List<Statement> parseIfBlock() {
        return parseIndentedBlock(TokenType.ELIF, TokenType.ELSE);
    }
    
    /**
     * Parse an indented block for for loops (stops at DEDENT, DEF, or EOF).
     * Does NOT consume the DEDENT - caller must handle it.
     */
    private List<Statement> parseForBlock() {
        return parseIndentedBlock();
    }
    
    /**
     * Parse an indented block of statements until a terminator token is found.
     * 
     * @param extraTerminators Additional token types that should terminate the block
     * @return List of parsed statements
     */
    private List<Statement> parseIndentedBlock(TokenType... extraTerminators) {
        List<Statement> statements = new ArrayList<>();
        
        while (!isAtEnd()) {
            TokenType type = peek().type();
            
            // Stop at standard terminators
            if (type == TokenType.DEDENT || type == TokenType.EOF || type == TokenType.DEF) {
                break;
            }
            
            // Stop at extra terminators
            for (TokenType terminator : extraTerminators) {
                if (type == terminator) {
                    return statements;
                }
            }
            
            // Skip newlines and indents
            if (type == TokenType.NEWLINE || type == TokenType.INDENT) {
                advance();
                continue;
            }
            
            statements.add(parseStatement());
            skipNewlines();
        }
        
        return statements;
    }
    
    /**
     * Parse a for loop statement: {@code for item in items:}
     */
    private ForLoop parseForLoop() {
        int lineNumber = peek().line();
        
        expect(TokenType.FOR, "Expected 'for'");
        String variable = expect(TokenType.IDENTIFIER, "Expected variable name").value();
        expect(TokenType.IN, "Expected 'in'");
        Expression iterable = parseExpression();
        expect(TokenType.COLON, "Expected ':' after for statement");
        skipNewlines();
        
        if (peek().type() == TokenType.INDENT) {
            advance();
        }
        
        List<Statement> body = parseForBlock();
        
        if (body.isEmpty()) {
            throw new GrizzlyParseException(
                "'for' loop body cannot be empty at line " + lineNumber,
                lineNumber,
                1
            );
        }
        
        if (peek().type() == TokenType.DEDENT) {
            advance();
        }
        
        return new ForLoop(variable, iterable, body, lineNumber);
    }
    
    /**
     * Parse an expression with proper operator precedence.
     * 
     * <p>Precedence (lowest to highest):
     * <ol>
     *   <li>or (logical)</li>
     *   <li>and (logical)</li>
     *   <li>not (unary logical)</li>
     *   <li>Comparison: ==, !=, <, >, <=, >=, in, not in</li>
     *   <li>Addition/Subtraction: +, -</li>
     *   <li>Multiplication/Division: *, /, //, %</li>
     *   <li>Power: **</li>
     * </ol>
     * 
     * @return Expression AST node with proper precedence
     */
    private Expression parseExpression() {
        return parseOr();
    }
    
    private Expression parseOr() {
        Expression left = parseAnd();
        
        while (peek().type() == TokenType.OR) {
            advance();
            Expression right = parseAnd();
            left = new BinaryOp(left, "or", right);
        }
        
        return left;
    }
    
    private Expression parseAnd() {
        Expression left = parseNot();
        
        while (peek().type() == TokenType.AND) {
            advance();
            Expression right = parseNot();
            left = new BinaryOp(left, "and", right);
        }
        
        return left;
    }
    
    private Expression parseNot() {
        if (peek().type() == TokenType.NOT) {
            advance();
            Expression expr = parseNot();
            return new BinaryOp(new BooleanLiteral(true), "not", expr);
        }
        return parseComparison();
    }
    
    private Expression parseComparison() {
        Expression left = parseAddition();
        
        while (true) {
            TokenType op = peek().type();
            if (op == TokenType.EQ || op == TokenType.NE || 
                op == TokenType.LT || op == TokenType.GT ||
                op == TokenType.LE || op == TokenType.GE) {
                advance();
                Expression right = parseAddition();
                left = new BinaryOp(left, tokenTypeToOp(op), right);
            } else if (op == TokenType.IN) {
                advance();
                Expression right = parseAddition();
                left = new BinaryOp(left, "in", right);
            } else if (op == TokenType.NOT && peekNext().type() == TokenType.IN) {
                advance(); // consume 'not'
                advance(); // consume 'in'
                Expression right = parseAddition();
                left = new BinaryOp(left, "not in", right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private Expression parseAddition() {
        Expression left = parseMultiplication();
        
        while (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
            TokenType op = peek().type();
            advance();
            Expression right = parseMultiplication();
            left = new BinaryOp(left, tokenTypeToOp(op), right);
        }
        
        return left;
    }
    
    private Expression parseMultiplication() {
        Expression left = parsePower();
        
        while (true) {
            TokenType op = peek().type();
            if (op == TokenType.STAR || op == TokenType.SLASH || 
                op == TokenType.DOUBLESLASH || op == TokenType.PERCENT) {
                advance();
                Expression right = parsePower();
                left = new BinaryOp(left, tokenTypeToOp(op), right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private Expression parsePower() {
        Expression left = parsePrimary();
        
        // ** is right-associative: 2 ** 3 ** 2 = 2 ** (3 ** 2) = 512
        if (peek().type() == TokenType.DOUBLESTAR) {
            advance();
            Expression right = parsePower();  // Recursive for right-associativity
            return new BinaryOp(left, "**", right);
        }
        
        return left;
    }
    
    private String tokenTypeToOp(TokenType type) {
        return switch (type) {
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case DOUBLESLASH -> "//";
            case PERCENT -> "%";
            case DOUBLESTAR -> "**";
            case EQ -> "==";
            case NE -> "!=";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            default -> throw new GrizzlyParseException("Unknown operator: " + type);
        };
    }
    
    /**
     * Parse primary expressions (identifiers, literals, dict/attr access)
     */
    /**
     * Parse the most basic expressions and chain them together.
     * 
     * <p><b>What's a "primary" expression?</b> The simplest building blocks:
     * <ul>
     *   <li>Literals: {@code "hello"}, {@code 42}, {@code []}, {@code {}}</li>
     *   <li>Variables: {@code x}, {@code INPUT}, {@code OUTPUT}</li>
     * </ul>
     * 
     * <p><b>The magic: Chaining!</b> After getting a basic expression, we can chain more:
     * <pre>{@code
     * INPUT.user.address.city
     * ^^^^^  dot   dot    dot
     * Start  |     |      |
     *    Attribute chains keep going!
     * 
     * OUTPUT["users"][0].name
     * ^^^^^^   [  ]  [ ] dot
     * Start  Dict List Attr
     *        access access access
     * }</pre>
     * 
     * <p><b>Example 1 - Simple identifier:</b>
     * <pre>{@code
     * x
     * 
     * Parse: See IDENTIFIER "x"
     * Return: Identifier("x")
     * Done!
     * }</pre>
     * 
     * <p><b>Example 2 - Attribute access:</b>
     * <pre>{@code
     * INPUT.name
     * 
     * Parse:
     * 1. See IDENTIFIER "INPUT" → create Identifier("INPUT")
     * 2. See DOT → continue chaining
     * 3. See IDENTIFIER "name" → wrap in AttrAccess
     * 4. No more dots/brackets → done
     * 
     * Return: AttrAccess(Identifier("INPUT"), "name")
     * }</pre>
     * 
     * <p><b>Example 3 - Dict access:</b>
     * <pre>{@code
     * OUTPUT["key"]
     * 
     * Parse:
     * 1. See IDENTIFIER "OUTPUT" → Identifier("OUTPUT")
     * 2. See LBRACKET → continue chaining
     * 3. Parse "key" expression → StringLiteral("key")
     * 4. See RBRACKET → close bracket
     * 5. Wrap in DictAccess
     * 
     * Return: DictAccess(Identifier("OUTPUT"), StringLiteral("key"))
     * }</pre>
     * 
     * <p><b>Example 4 - Method call:</b>
     * <pre>{@code
     * items.append(value)
     * 
     * Parse:
     * 1. See IDENTIFIER "items" → Identifier("items")
     * 2. See DOT → continue chaining
     * 3. See IDENTIFIER "append"
     * 4. See LPAREN → this is a method call!
     * 5. Parse arguments: value
     * 6. See RPAREN → close
     * 7. Wrap in MethodCall
     * 
     * Return: MethodCall(Identifier("items"), "append", [Identifier("value")])
     * }</pre>
     * 
     * <p><b>Example 5 - Complex chaining:</b>
     * <pre>{@code
     * INPUT.users[0].name.upper()
     * 
     * Parse steps:
     * 1. INPUT → Identifier("INPUT")
     * 2. .users → AttrAccess(prev, "users")
     * 3. [0] → DictAccess(prev, NumberLiteral("0"))
     * 4. .name → AttrAccess(prev, "name")
     * 5. .upper() → MethodCall(prev, "upper", [])
     * 
     * Each step wraps the previous result!
     * }</pre>
     * 
     * <p><b>The loop:</b> Keep chaining while we see:
     * <ul>
     *   <li>{@code [} → Dict/list access</li>
     *   <li>{@code .} → Attribute or method</li>
     *   <li>{@code (} → Function call</li>
     * </ul>
     * 
     * @return Expression (could be Identifier, AttrAccess, DictAccess, MethodCall, etc.)
     */
    private Expression parsePrimary() {
        Token token = peek();
        
        // Unary minus (negative numbers): -5, -10
        if (token.type() == TokenType.MINUS) {
            advance();
            Expression expr = parsePrimary(); // Get the number after minus
            // Wrap in binary op: 0 - expr
            return new BinaryOp(new NumberLiteral(0), "-", expr);
        }
        
        // Boolean literals: True, False
        if (token.type() == TokenType.TRUE) {
            advance();
            return new BooleanLiteral(true);
        }
        
        if (token.type() == TokenType.FALSE) {
            advance();
            return new BooleanLiteral(false);
        }
        
        // None literal
        if (token.type() == TokenType.NONE) {
            advance();
            return new NullLiteral();
        }
        
        // String literal
        if (token.type() == TokenType.STRING) {
            advance();
            return new StringLiteral(token.value());
        }
        
        // Number literal
        if (token.type() == TokenType.NUMBER) {
            advance();
            String value = token.value();
            try {
                // Parse as integer if no decimal point
                if (!value.contains(".")) {
                    return new NumberLiteral(Integer.parseInt(value));
                } else {
                    return new NumberLiteral(Double.parseDouble(value));
                }
            } catch (NumberFormatException e) {
                throw new GrizzlyParseException(
                    "Invalid number format: " + value,
                    token.line(),
                    token.column()
                );
            }
        }
        
        // Dict literal: {} or {"key": value, ...}
        if (token.type() == TokenType.LBRACE) {
            return parseDictLiteral();
        }
        
        // List literal: [] or [1, 2, 3]
        if (token.type() == TokenType.LBRACKET) {
            return parseListLiteral();
        }
        
        // Identifier (variable name)
        if (token.type() == TokenType.IDENTIFIER) {
            String name = token.value();
            advance();
            Expression expr = new Identifier(name);
            
            // Check for dict access, list access, attribute access, or method call
            // Also handles safe navigation: ?. and ?[
            while (true) {
                TokenType tokenType = peek().type();
                
                if (tokenType == TokenType.LBRACKET || tokenType == TokenType.SAFE_LBRACKET) {
                    // Dict/list access: obj["key"] or obj?["key"]
                    boolean safe = (tokenType == TokenType.SAFE_LBRACKET);
                    advance();
                    Expression key = parseExpression();
                    expect(TokenType.RBRACKET, "Expected ']'");
                    
                    expr = new DictAccess(expr, key, safe);
                    
                } else if (tokenType == TokenType.DOT || tokenType == TokenType.SAFE_DOT) {
                    // Attribute access or method call: obj.attr or obj?.attr
                    boolean safe = (tokenType == TokenType.SAFE_DOT);
                    advance();
                    String attr = expect(TokenType.IDENTIFIER, "Expected attribute name").value();
                    
                    // Check if it's a method call
                    if (peek().type() == TokenType.LPAREN) {
                        advance(); // Skip '('
                        List<Expression> args = new ArrayList<>();
                        
                        if (peek().type() != TokenType.RPAREN) {
                            do {
                                args.add(parseExpression());
                                if (peek().type() == TokenType.COMMA) {
                                    advance();
                                }
                            } while (peek().type() != TokenType.RPAREN);
                        }
                        
                        expect(TokenType.RPAREN, "Expected ')'");
                        expr = new MethodCall(expr, attr, args);
                    } else {
                        // Attribute access (with safe flag)
                        expr = new AttrAccess(expr, attr, safe);
                    }
                    
                } else if (tokenType == TokenType.LPAREN) {
                    // Function call expression: len(items), helper(x)
                    advance(); // Skip '('
                    List<Expression> args = new ArrayList<>();
                    
                    if (peek().type() != TokenType.RPAREN) {
                        do {
                            args.add(parseExpression());
                            if (peek().type() == TokenType.COMMA) {
                                advance();
                            }
                        } while (peek().type() != TokenType.RPAREN);
                    }
                    
                    expect(TokenType.RPAREN, "Expected ')'");
                    
                    // Return as FunctionCallExpression
                    return new FunctionCallExpression(name, args);
                } else {
                    break;
                }
            }
            
            return expr;
        }
        
        throw new GrizzlyParseException("Unexpected token: " + token, token.line(), token.column());
    }
    
    /**
     * Parse a list literal expression: {@code []} or {@code [1, 2, 3]}.
     * 
     * <p>List literals create new list objects with zero or more initial elements.
     * 
     * <p><b>Examples:</b>
     * <pre>{@code
     * items = []                           // Empty list
     * numbers = [1, 2, 3]                  // List with literals
     * ids = [INPUT.id1, INPUT.id2]         // List with expressions
     * mixed = [1, "hello", INPUT.value]    // Mixed types
     * }</pre>
     * 
     * <p><b>Token sequence:</b>
     * LBRACKET → [Expression → COMMA → Expression → ...] → RBRACKET
     * 
     * @return ListLiteral AST node containing list of element expressions
     * @throws GrizzlyParseException if syntax is invalid
     */
    private ListLiteral parseListLiteral() {
        expect(TokenType.LBRACKET, "Expected '['");
        
        List<Expression> elements = new ArrayList<>();
        
        if (peek().type() != TokenType.RBRACKET) {
            do {
                elements.add(parseExpression());
                if (peek().type() == TokenType.COMMA) {
                    advance();
                }
            } while (peek().type() != TokenType.RBRACKET);
        }
        
        expect(TokenType.RBRACKET, "Expected ']'");
        
        return new ListLiteral(elements);
    }
    
    /**
     * Parse a dict literal: {} or {"key": value, ...}
     */
    private DictLiteral parseDictLiteral() {
        expect(TokenType.LBRACE, "Expected '{'");
        
        List<DictLiteral.Entry> entries = new ArrayList<>();
        
        if (peek().type() != TokenType.RBRACE) {
            do {
                Expression key = parseExpression();
                expect(TokenType.COLON, "Expected ':' after dict key");
                Expression value = parseExpression();
                entries.add(new DictLiteral.Entry(key, value));
                
                if (peek().type() == TokenType.COMMA) {
                    advance();
                }
            } while (peek().type() != TokenType.RBRACE);
        }
        
        expect(TokenType.RBRACE, "Expected '}'");
        
        return new DictLiteral(entries);
    }
    
    // === Helper methods ===
    
    private Token peek() {
        if (position >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // Return EOF
        }
        return tokens.get(position);
    }
    
    private Token peekNext() {
        if (position + 1 >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // Return EOF
        }
        return tokens.get(position + 1);
    }
    
    private Token advance() {
        if (!isAtEnd()) position++;
        return tokens.get(position - 1);
    }
    
    private boolean isAtEnd() {
        if (position >= tokens.size()) return true;
        return tokens.get(position).type() == TokenType.EOF;
    }
    
    private Token expect(TokenType type, String message) {
        if (peek().type() != type) {
            throw new GrizzlyParseException(
                message + ", got " + peek().type(), 
                peek().line(), 
                peek().column()
            );
        }
        return advance();
    }
    
    private void skipNewlines() {
        while (!isAtEnd() && peek().type() == TokenType.NEWLINE) {
            advance();
        }
    }
}
