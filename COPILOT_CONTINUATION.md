# Grizzly Engine - GitHub Copilot Continuation Guide

## ✅ Current Status

**All tests passing!** The engine is fully functional with modern JDK 21 features.

## 🏗️ Architecture

```
Grizzly Engine
├── Lexer (Tokenization)
│   └── Python template → Tokens
├── Parser (AST Generation)
│   └── Tokens → Abstract Syntax Tree (Records)
├── Interpreter (Execution)
│   └── AST + Input POJO → Output POJO
└── Mapper (POJO ↔ Map)
    └── Jackson-based conversion
```

## 📋 Modern Java Features Used

- ✅ **Records** (JDK 16) - All AST classes
- ✅ **Pattern Matching for Switch** (JDK 21) - Interpreter
- ✅ **Text Blocks** (JDK 15) - Tests
- ✅ **Switch Expressions** (JDK 14) - Throughout

## 🎯 Potential Enhancements

### 1. Built-in Functions
**Goal:** Add Python-like string manipulation

```python
# Example usage in templates:
OUTPUT["name"] = upper(INPUT.firstName)
OUTPUT["ssn"] = mask(INPUT.ssn, "XXX-XX-####")
OUTPUT["date"] = format_date(INPUT.birthDate, "yyyy-MM-dd")
```

**Implementation:**
- Add `BuiltinFunctions.java` in `interpreter` package
- Register functions in `GrizzlyInterpreter`
- Handle function calls in `evaluateExpression`

**Suggested functions:**
- `upper(str)`, `lower(str)`, `trim(str)`
- `format_date(date, pattern)`
- `mask(str, pattern)` - for SSN/credit cards
- `concat(str1, str2, ...)`
- `substring(str, start, end)`

### 2. Number Literal Support
**Current:** Numbers parsed as StringLiteral
**Better:** Create NumberLiteral record

```java
public record NumberLiteral(Number value) implements Expression {}
```

Then update parser and interpreter to handle it properly.

### 3. List/Array Support
**Goal:** Handle Python lists

```python
OUTPUT["items"] = INPUT.products[0]
OUTPUT["first"] = INPUT.names[0]
```

**Implementation:**
- Add `ListAccess(Expression list, Expression index)` record
- Update parser to handle `[index]` on identifiers
- Update interpreter to handle List types

### 4. For Loops (Advanced)
**Goal:** Iterate over collections

```python
for item in INPUT.items:
    OUTPUT["items"].append(transform_item(item))
```

**Implementation:**
- Add `ForStatement` record
- Add `ListLiteral` for `[]`
- Handle iteration in interpreter

### 5. Math Operations
**Goal:** Support +, -, *, /

```python
OUTPUT["total"] = INPUT.price * INPUT.quantity
OUTPUT["discount"] = INPUT.total * 0.1
```

**Implementation:**
- Extend `BinaryOp` to handle math operators
- Add math token types to lexer
- Implement arithmetic in interpreter

### 6. String Concatenation
**Goal:** Join strings

```python
OUTPUT["fullName"] = INPUT.firstName + " " + INPUT.lastName
```

Same as math operations but for strings.

### 7. Null/None Handling
**Goal:** Handle missing values

```python
OUTPUT["optional"] = INPUT.field if INPUT.field else "default"
```

**Implementation:**
- Add ternary operator support
- Add null checking in interpreter

### 8. Lambda/Inline Functions
**Goal:** Small transformations

```python
OUTPUT["emails"] = map(lambda x: x.lower(), INPUT.emails)
```

Advanced - requires significant work.

### 9. Import/Module System
**Goal:** Reusable transformations

```python
from common_mappings import format_address

OUTPUT["address"] = format_address(INPUT.address)
```

**Implementation:**
- Template registry
- Module loading mechanism
- Function resolution across files

### 10. Error Messages
**Goal:** Better debugging

Current errors are good but could show:
- Template snippet where error occurred
- Variable values at error time
- Stack trace through template functions

## 🔧 Quick Wins

### Easy (1-2 hours):
1. Add upper/lower/trim built-in functions
2. Create NumberLiteral record
3. Better error messages with context

### Medium (3-5 hours):
4. Math operations (+, -, *, /)
5. String concatenation
6. List access `items[0]`

### Hard (1-2 days):
7. For loops
8. Module system
9. Lambda expressions

## 📝 Code Style Guidelines

1. **Use Records** for immutable data
2. **Pattern matching switch** for type checks
3. **Text blocks** for multi-line strings
4. **Descriptive names** - `evaluateExpression` not `evalExpr`
5. **Javadoc** on public methods
6. **Tests first** - TDD approach

## 🧪 Testing Strategy

For each new feature:
1. Add unit test in appropriate test file
2. Add integration test in `GrizzlyEngineTest`
3. Test edge cases
4. Test error conditions

## 🐛 Known Limitations

1. **No type checking** - Runtime errors only
2. **Simple indentation** - Relies on INDENT/DEDENT tokens
3. **No imports** - Single-file templates only
4. **No classes** - Only functions
5. **No try/catch** - No exception handling in templates

## 📚 Resources

- **Parser:** `GrizzlyParser.java` - Recursive descent
- **Interpreter:** `GrizzlyInterpreter.java` - Tree-walking
- **AST:** `parser/ast/` - All record classes
- **Tests:** `src/test/java/` - Comprehensive suite

## 🚀 Getting Started with Copilot

1. **Read this file** to understand architecture
2. **Pick an enhancement** from the list above
3. **Write test first** - describe what you want
4. **Let Copilot suggest** implementation
5. **Run tests** - `./gradlew test`
6. **Iterate** until tests pass

## 💡 Copilot Prompts to Try

```
// Add upper() built-in function
// Test: upper("hello") should return "HELLO"

// Add number literal support
// Test: 42 should parse as NumberLiteral(42)

// Add math operations
// Test: 5 + 3 should evaluate to 8

// Add list access
// Test: items[0] should get first item
```

## 📞 Support

Issues or questions? Check:
- Test files for examples
- MODERN_FEATURES.md for current capabilities
- Interpreter code for execution logic

---

**Happy coding with Copilot!** 🐻✨
