package com.grizzly.lexer;

import com.grizzly.exception.GrizzlyParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lexer (Tokenizer) - Converts Python template text into tokens
 * 
 * Example:
 * Input:  "def transform(INPUT):"
 * Output: [DEF, IDENTIFIER("transform"), LPAREN, IDENTIFIER("INPUT"), RPAREN, COLON]
 * 
 * This is Step 1 of the compilation process:
 * Text → [Lexer] → Tokens → [Parser] → AST → [Interpreter] → Result
 */
public class GrizzlyLexer {
    
    // Python keywords we recognize
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("def", TokenType.DEF),
        Map.entry("if", TokenType.IF),
        Map.entry("elif", TokenType.ELIF),
        Map.entry("else", TokenType.ELSE),
        Map.entry("return", TokenType.RETURN),
        Map.entry("for", TokenType.FOR),
        Map.entry("in", TokenType.IN),
        Map.entry("break", TokenType.BREAK),
        Map.entry("continue", TokenType.CONTINUE),
        Map.entry("true", TokenType.TRUE),
        Map.entry("false", TokenType.FALSE)
    );
    
    // Constants
    private static final int SPACES_PER_INDENT = 4;
    private static final char EOF_CHAR = '\0';
    
    // ========== Lexer State Variables ==========
    
    /** 
     * The original source code being tokenized.
     * This never changes after construction.
     */
    private final String source;
    
    /** 
     * List of tokens generated during tokenization.
     * New tokens are added via addToken() as we scan the source.
     */
    private final List<Token> tokens = new ArrayList<>();
    
    /**
     * Current position in the entire input string (absolute index).
     * 
     * <p>This is the index into the {@code source} string, starting at 0 and
     * incrementing with each character read. It NEVER resets, even across newlines.
     * 
     * <p><b>Purpose:</b> Navigate through the input string character by character.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * Input: "def f():\n    x = 1"
     * position:  0123456789012345...
     * 
     * position = 0  → 'd' (first character)
     * position = 8  → '\n' (newline)
     * position = 13 → 'x' (continues counting)
     * }</pre>
     * 
     * <p><b>Used by:</b> {@link #currentChar()}, {@link #advance()}
     * 
     * @see #column
     */
    private int position = 0;
    
    /**
     * Current line number in the source code (1-indexed).
     * 
     * <p>Incremented each time we encounter a newline character ('\n').
     * Starts at 1 (first line) and increases throughout the file.
     * 
     * <p><b>Purpose:</b> Track which line we're on for error messages.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * def transform(INPUT):  // line = 1
     *     OUTPUT = {}        // line = 2
     *     return OUTPUT      // line = 3
     * }</pre>
     * 
     * <p><b>Error message example:</b>
     * <pre>{@code
     * throw new GrizzlyParseException(
     *     "Unterminated string at line " + line + ", column " + column
     * );
     * // Output: "Unterminated string at line 3, column 15"
     * }</pre>
     * 
     * <p><b>Used by:</b> Error messages, {@link Token} creation
     * 
     * @see #column
     * @see #handleNewline()
     */
    private int line = 1;
    
    /**
     * Current column number in the current line (1-indexed).
     * 
     * <p>Tracks the character position within the current line only.
     * Starts at 1 for each line and RESETS to 1 after every newline.
     * 
     * <p><b>Purpose:</b> Provide precise character location for error messages.
     * 
     * <p><b>Key Difference from position:</b>
     * <ul>
     *   <li>{@code position} = absolute index in entire file (never resets)</li>
     *   <li>{@code column} = position in current line (resets each line)</li>
     * </ul>
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * Line 1: def transform(INPUT):
     * Column: 123456789012345678901  (1-21)
     * 
     * Line 2:     OUTPUT = {}
     * Column: 123456789012345  (RESETS to 1!)
     * 
     * position at 'O' in OUTPUT = 26 (absolute)
     * column at 'O' in OUTPUT = 5 (in line 2)
     * }</pre>
     * 
     * <p><b>Detailed trace:</b>
     * <pre>{@code
     * Character  position  line  column
     * ─────────────────────────────────
     * 'd'        0         1     1
     * 'e'        1         1     2
     * 'f'        2         1     3
     * '\n'       8         1     9
     * ' '        9         2     1      ← column RESETS
     * ' '        10        2     2
     * 'O'        13        2     5
     * }</pre>
     * 
     * <p><b>Used by:</b> Error messages, {@link Token} creation
     * 
     * @see #position
     * @see #advance()
     * @see #handleNewline()
     */
    private int column = 1;
    
    /**
     * Current Python indentation level (0 = no indentation).
     * 
     * <p>Tracks how many indentation levels deep we currently are.
     * Python uses indentation to define code blocks (unlike Java's braces).
     * Each level represents 4 spaces (defined by {@link #SPACES_PER_INDENT}).
     * 
     * <p><b>Purpose:</b> Generate INDENT and DEDENT tokens for Python-like syntax.
     * 
     * <p><b>How it works:</b>
     * <ul>
     *   <li>When indentation increases → emit INDENT token, increment currentIndent</li>
     *   <li>When indentation decreases → emit DEDENT token, decrement currentIndent</li>
     *   <li>When indentation stays same → no token, keep currentIndent</li>
     * </ul>
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * def transform(INPUT):        # currentIndent = 0
     *     OUTPUT = {}              # 4 spaces → currentIndent = 1, emit INDENT
     *     if INPUT.x:              # 4 spaces → currentIndent = 1, no change
     *         OUTPUT["a"] = 1      # 8 spaces → currentIndent = 2, emit INDENT
     *     return OUTPUT            # 4 spaces → currentIndent = 1, emit DEDENT
     * }</pre>
     * 
     * <p><b>Token generation:</b>
     * <pre>{@code
     * // At "OUTPUT = {}" (4 spaces)
     * int spaces = 4;
     * int newLevel = spaces / 4 = 1;
     * 
     * while (newLevel > currentIndent) {  // 1 > 0 = true
     *     addToken(TokenType.INDENT);     // Emit INDENT
     *     currentIndent++;                // currentIndent = 1
     * }
     * }</pre>
     * 
     * <p><b>Why needed:</b> Parser uses INDENT/DEDENT tokens to know when
     * code blocks start and end (like { } in Java).
     * 
     * <p><b>Used by:</b> {@link #handleIndentation()}
     * 
     * @see #SPACES_PER_INDENT
     * @see TokenType#INDENT
     * @see TokenType#DEDENT
     */
    private int currentIndent = 0;
    
    public GrizzlyLexer(String source) {
        this.source = source;
    }
    
    /**
     * Main entry point - tokenize the entire source
     */
    public List<Token> tokenize() {
        while (!isAtEnd()) {
            tokenizeNext();
        }
        
        addFinalTokens();
        return tokens;
    }
    
    /**
     * Add DEDENT and EOF tokens at the end
     */
    private void addFinalTokens() {
        // Close any open indentation levels
        while (currentIndent > 0) {
            addToken(TokenType.DEDENT);
            currentIndent--;
        }
        
        addToken(TokenType.EOF);
    }
    
    /**
     * Process the next character(s) and create appropriate token
     */
    private void tokenizeNext() {
        char c = currentChar();
        
        // Try each token type in order
        if (isWhitespace(c)) {
            handleIndentation();
        } else if (c == '\n') {
            handleNewline();
        } else if (c == '#') {
            skipComment();
        } else if (isQuote(c)) {
            tokenizeString(c);
        } else if (Character.isDigit(c)) {
            tokenizeNumber();
        } else if (isIdentifierStart(c)) {
            tokenizeIdentifier();
        } else {
            tokenizeSymbol(c);
        }
    }
    
    // ========== Character Classification ==========
    
    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }
    
    private boolean isQuote(char c) {
        return c == '"' || c == '\'';
    }
    
    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }
    
    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    
    // ========== Token Creation Methods ==========
    
    /**
     * Handle Python-style indentation at the start of lines.
     * 
     * <p>Python uses indentation (spaces/tabs) to define code blocks, unlike Java's braces.
     * This method detects indentation changes and emits INDENT/DEDENT tokens accordingly.
     * 
     * <p><b>CRITICAL:</b> Only processes spaces at column 1 (start of line).
     * Spaces elsewhere in code are just formatting and are ignored.
     * 
     * <p><b>How it works:</b>
     * <ol>
     *   <li>Check if we're at the start of a line ({@code column == 1})</li>
     *   <li>If not at start, this is just a regular space → skip it</li>
     *   <li>If at start, count ALL leading spaces/tabs</li>
     *   <li>Calculate new indentation level (spaces ÷ 4)</li>
     *   <li>Emit INDENT tokens if indentation increased</li>
     *   <li>Emit DEDENT tokens if indentation decreased</li>
     * </ol>
     * 
     * <p><b>Example - Indentation at line start (column 1):</b>
     * <pre>{@code
     * def transform(INPUT):
     *     OUTPUT = {}              ← 4 spaces at column 1
     * ^^^^
     * These count as indentation
     * 
     * Flow:
     * 1. column = 1 → Process indentation
     * 2. countSpaces() → 4 spaces
     * 3. newIndentLevel = 4 / 4 = 1
     * 4. currentIndent was 0
     * 5. Emit INDENT token
     * 6. currentIndent = 1
     * }</pre>
     * 
     * <p><b>Example - Spaces in middle of line (column ≠ 1):</b>
     * <pre>{@code
     * OUTPUT["key"] = INPUT.value
     *       ^     ^       ^
     *       column=7,13,19 (NOT 1)
     * 
     * Flow for each space:
     * 1. column ≠ 1 → Not indentation
     * 2. advance() → Skip this space
     * 3. return → Done (no indentation processing)
     * }</pre>
     * 
     * <p><b>Visual trace:</b>
     * <pre>{@code
     * Code:
     * def f():
     *     x = 1
     * 
     * Line 2 processing:
     * Char  column  handleIndentation() action
     * ────────────────────────────────────────
     * ' '   1       ✓ Count indentation (column = 1)
     * ' '   2       ✗ Skip (column ≠ 1, just advance)
     * ' '   3       ✗ Skip (column ≠ 1, just advance)
     * ' '   4       ✗ Skip (column ≠ 1, just advance)
     * 'x'   5       Not a space
     * 
     * Result: 4 spaces counted → 1 indent level → emit INDENT
     * }</pre>
     * 
     * <p><b>Indentation level calculation:</b>
     * <pre>{@code
     * 0 spaces  → level 0 (no indentation)
     * 4 spaces  → level 1 (one indent)
     * 8 spaces  → level 2 (two indents)
     * 12 spaces → level 3 (three indents)
     * 
     * Formula: level = spaces / SPACES_PER_INDENT (4)
     * }</pre>
     * 
     * <p><b>Token emission examples:</b>
     * <pre>{@code
     * // Going deeper (indent increases)
     * def f():           # currentIndent = 0
     *     x = 1          # 4 spaces → level 1 → emit INDENT, currentIndent = 1
     *         y = 2      # 8 spaces → level 2 → emit INDENT, currentIndent = 2
     * 
     * // Going back (indent decreases)
     *         y = 2      # currentIndent = 2
     *     x = 3          # 4 spaces → level 1 → emit DEDENT, currentIndent = 1
     * z = 4              # 0 spaces → level 0 → emit DEDENT, currentIndent = 0
     * 
     * // Staying same (no change)
     *     x = 1          # currentIndent = 1
     *     y = 2          # 4 spaces → level 1 → no change, no token
     * }</pre>
     * 
     * <p><b>Why the column != 1 check is critical:</b>
     * <pre>{@code
     * WITHOUT the check:
     * OUTPUT["key"] = INPUT.value
     *       ^
     *       This space at column 7 would be treated as indentation!
     *       Would try to count it and emit DEDENT (completely wrong)
     * 
     * WITH the check:
     * OUTPUT["key"] = INPUT.value
     *       ^
     *       column = 7 ≠ 1 → advance() and return
     *       Just skip it, no harm done ✓
     * }</pre>
     * 
     * <p><b>Related processing:</b>
     * <ul>
     *   <li>Called by {@link #tokenizeNext()} when encountering spaces/tabs</li>
     *   <li>Uses {@link #countSpaces()} to count leading whitespace</li>
     *   <li>Updates {@link #currentIndent} to track depth</li>
     *   <li>Emits {@link TokenType#INDENT} when going deeper</li>
     *   <li>Emits {@link TokenType#DEDENT} when going back</li>
     * </ul>
     * 
     * <p><b>Important notes:</b>
     * <ul>
     *   <li>Only processes whitespace at {@code column == 1} (start of line)</li>
     *   <li>Tabs count as 4 spaces (see {@link #SPACES_PER_INDENT})</li>
     *   <li>Can emit multiple INDENT/DEDENT tokens in one call</li>
     *   <li>Regular spaces in code (column ≠ 1) are simply skipped</li>
     * </ul>
     * 
     * @see #column
     * @see #currentIndent
     * @see #countSpaces()
     * @see #SPACES_PER_INDENT
     * @see TokenType#INDENT
     * @see TokenType#DEDENT
     */
    private void handleIndentation() {
        // Only count indentation at start of line
        if (column != 1) {
            advance();
            return;
        }
        
        int spaces = countSpaces();
        int newIndentLevel = spaces / SPACES_PER_INDENT;
        
        // Emit INDENT tokens if we went deeper
        while (newIndentLevel > currentIndent) {
            addToken(TokenType.INDENT);
            currentIndent++;
        }
        
        // Emit DEDENT tokens if we went back
        while (newIndentLevel < currentIndent) {
            addToken(TokenType.DEDENT);
            currentIndent--;
        }
    }
    
    /**
     * Count spaces at current position (tab = 4 spaces)
     */
    private int countSpaces() {
        int spaces = 0;
        while (!isAtEnd() && isWhitespace(currentChar())) {
            spaces += (currentChar() == '\t') ? SPACES_PER_INDENT : 1;
            advance();
        }
        return spaces;
    }
    
    /**
     * Handle newline character
     */
    private void handleNewline() {
        addToken(TokenType.NEWLINE);
        advance();
        line++;
        column = 1;
    }
    
    /**
     * Skip comment (# to end of line)
     */
    private void skipComment() {
        while (!isAtEnd() && currentChar() != '\n') {
            advance();
        }
    }
    
    /**
     * Tokenize string literal ("hello" or 'world')
     */
    private void tokenizeString(char quote) {
        int startColumn = column;
        advance(); // Skip opening quote
        
        StringBuilder content = new StringBuilder();
        
        while (!isAtEnd() && currentChar() != quote) {
            if (currentChar() == '\\') {
                content.append(handleEscapeSequence());
            } else {
                content.append(currentChar());
                advance();
            }
        }
        
        if (isAtEnd()) {
            throw error("Unterminated string", startColumn);
        }
        
        advance(); // Skip closing quote
        addToken(TokenType.STRING, content.toString(), startColumn);
    }
    
    /**
     * Tokenize raw string literal (r"..." or r'...') - NO escape sequence processing!
     * 
     * <p>Raw strings are perfect for regex patterns where backslashes are common.
     * 
     * <p><b>Examples:</b>
     * <pre>{@code
     * r"\d{3}"         → "\d{3}" (backslash preserved)
     * r"[a-z]+"        → "[a-z]+"
     * r"\w+@\w+\.\w+"  → "\w+@\w+\.\w+" (all backslashes preserved)
     * 
     * // Compare with regular strings:
     * "\\d{3}"         → "\d{3}" (need double backslash)
     * r"\d{3}"         → "\d{3}" (single backslash works!)
     * }</pre>
     */
    private void tokenizeRawString(char quote) {
        int startColumn = column;
        advance(); // Skip opening quote
        
        StringBuilder content = new StringBuilder();
        
        while (!isAtEnd() && currentChar() != quote) {
            // Raw strings: NO escape sequence processing!
            // Everything between quotes is taken literally
            content.append(currentChar());
            advance();
        }
        
        if (isAtEnd()) {
            throw error("Unterminated raw string", startColumn);
        }
        
        advance(); // Skip closing quote
        addToken(TokenType.STRING, content.toString(), startColumn);
    }
    
    /**
     * Handle escape sequences in strings (\n, \t, \\, etc.)
     */
    private char handleEscapeSequence() {
        advance(); // Skip backslash
        if (isAtEnd()) return '\\';
        
        char escaped = currentChar();
        advance();
        
        return switch (escaped) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case '\\' -> '\\';
            case '"' -> '"';
            case '\'' -> '\'';
            default -> escaped;
        };
    }
    
    /**
     * Tokenize number (42 or 3.14)
     */
    private void tokenizeNumber() {
        int startColumn = column;
        StringBuilder number = new StringBuilder();
        
        while (!isAtEnd() && (Character.isDigit(currentChar()) || currentChar() == '.')) {
            number.append(currentChar());
            advance();
        }
        
        addToken(TokenType.NUMBER, number.toString(), startColumn);
    }
    
    /**
     * Tokenize identifier or keyword (transform, OUTPUT, def, if, etc.)
     * Also handles raw strings (r"..." for regex patterns)
     */
    private void tokenizeIdentifier() {
        int startColumn = column;
        StringBuilder identifier = new StringBuilder();
        
        while (!isAtEnd() && isIdentifierPart(currentChar())) {
            identifier.append(currentChar());
            advance();
        }
        
        String text = identifier.toString();
        
        // Check for raw string prefix: r"..." or r'...'
        if (text.equals("r") && !isAtEnd() && isQuote(currentChar())) {
            tokenizeRawString(currentChar());
            return;
        }
        
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type, text, startColumn);
    }
    
    /**
     * Tokenize symbols and operators (, ), {, }, ==, !=, etc.)
     */
    private void tokenizeSymbol(char c) {
        switch (c) {
            case '(' -> addTokenAndAdvance(TokenType.LPAREN);
            case ')' -> addTokenAndAdvance(TokenType.RPAREN);
            case '{' -> addTokenAndAdvance(TokenType.LBRACE);
            case '}' -> addTokenAndAdvance(TokenType.RBRACE);
            case '[' -> addTokenAndAdvance(TokenType.LBRACKET);
            case ']' -> addTokenAndAdvance(TokenType.RBRACKET);
            case ',' -> addTokenAndAdvance(TokenType.COMMA);
            case '.' -> addTokenAndAdvance(TokenType.DOT);
            case ':' -> addTokenAndAdvance(TokenType.COLON);
            case '+' -> addTokenAndAdvance(TokenType.PLUS);
            case '-' -> addTokenAndAdvance(TokenType.MINUS);
            case '%' -> addTokenAndAdvance(TokenType.PERCENT);
            case '*' -> tokenizeStar();      // * or **
            case '/' -> tokenizeSlash();     // / or //
            case '=' -> tokenizeEquals();
            case '!' -> tokenizeNotEquals();
            case '<' -> tokenizeLessThan();
            case '>' -> tokenizeGreaterThan();
            default -> throw error("Unexpected character '" + c + "'");
        }
    }
    
    /**
     * Tokenize = or ==
     */
    private void tokenizeEquals() {
        advance();
        if (currentChar() == '=') {
            addTokenAndAdvance(TokenType.EQ);
        } else {
            addToken(TokenType.ASSIGN);
        }
    }
    
    /**
     * Tokenize !=
     */
    private void tokenizeNotEquals() {
        advance();
        if (currentChar() == '=') {
            addTokenAndAdvance(TokenType.NE);
        } else {
            throw error("Expected '=' after '!'");
        }
    }
    
    /**
     * Tokenize < or <=
     */
    private void tokenizeLessThan() {
        advance();
        if (currentChar() == '=') {
            addTokenAndAdvance(TokenType.LE);
        } else {
            addToken(TokenType.LT);
        }
    }
    
    /**
     * Tokenize > or >=
     */
    private void tokenizeGreaterThan() {
        advance();
        if (currentChar() == '=') {
            addTokenAndAdvance(TokenType.GE);
        } else {
            addToken(TokenType.GT);
        }
    }
    
    /**
     * Tokenize * or **
     */
    private void tokenizeStar() {
        advance();
        if (currentChar() == '*') {
            addTokenAndAdvance(TokenType.DOUBLESTAR);
        } else {
            addToken(TokenType.STAR);
        }
    }
    
    /**
     * Tokenize / or //
     */
    private void tokenizeSlash() {
        advance();
        if (currentChar() == '/') {
            addTokenAndAdvance(TokenType.DOUBLESLASH);
        } else {
            addToken(TokenType.SLASH);
        }
    }
    
    // ========== Token Addition Helpers ==========
    
    private void addToken(TokenType type) {
        tokens.add(new Token(type, line, column));
    }
    
    private void addToken(TokenType type, String value, int startColumn) {
        tokens.add(new Token(type, value, line, startColumn));
    }
    
    private void addTokenAndAdvance(TokenType type) {
        addToken(type);
        advance();
    }
    
    // ========== Character Navigation ==========
    
    private char currentChar() {
        return isAtEnd() ? EOF_CHAR : source.charAt(position);
    }
    
    private void advance() {
        if (!isAtEnd()) {
            position++;
            column++;
        }
    }
    
    private boolean isAtEnd() {
        return position >= source.length();
    }
    
    // ========== Error Handling ==========
    
    private GrizzlyParseException error(String message) {
        return new GrizzlyParseException(message + " at " + line + ":" + column);
    }
    
    private GrizzlyParseException error(String message, int col) {
        return new GrizzlyParseException(message + " at " + line + ":" + col);
    }
}
