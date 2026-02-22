# 🐻 Grizzly Engine - Python-Compliant Data Transformation Engine

## Overview

Grizzly Engine is a production-ready data transformation engine that implements a Python subset for mapping data between different schemas. Built specifically for banking domain applications (MISMO schema transformations), it provides full support for Python Sections 3 and 4 plus comprehensive DateTime operations with timezone support.

---

## ✅ Features Implemented

### Python Section 3: Data Types

#### 3.1.1. Numbers
Full numeric support with proper type handling:

**Operators:**
- Arithmetic: `+`, `-`, `*`, `/`, `//` (floor division), `%` (modulo), `**` (power)
- Comparison: `==`, `!=`, `<`, `>`, `<=`, `>=`
- Operator precedence follows Python rules

**Number Types:**
- Integers: `42`, `0`, `-5`
- Floats: `3.14`, `-0.5`, `2.0`
- Automatic type conversion in operations

**Examples:**
```python
# Arithmetic
result = 10 + 5 * 2        # 20 (precedence: * before +)
quotient = 17 // 5         # 3 (floor division)
remainder = 17 % 5         # 2 (modulo)
power = 2 ** 8             # 256

# Comparison
is_adult = age >= 18       # True/False
is_zero = value == 0       # Numeric equality with type coercion
```

#### 3.1.2. Text (Strings)
String operations and methods:

**Operations:**
- Concatenation: `"Hello" + " " + "World"`
- Repetition: `"Ha" * 3` → `"HaHaHa"`

**Methods:**
- `str.upper()` - Convert to uppercase
- `str.lower()` - Convert to lowercase
- `str.strip()` - Remove leading/trailing whitespace

**Examples:**
```python
full_name = first_name + " " + last_name
greeting = "Hello! " * 3
uppercase = name.upper()
cleaned = "  data  ".strip()  # "data"
```

#### 3.1.3. Lists
List operations and methods:

**Operations:**
- Concatenation: `[1, 2] + [3, 4]` → `[1, 2, 3, 4]`
- Repetition: `[0] * 3` → `[0, 0, 0]`
- Indexing: `items[0]`, `items[-1]` (negative indexing supported)

**Methods:**
- `list.append(item)` - Add single element
- `list.extend(items)` - Add multiple elements
- `len(list)` - Get list length

**Examples:**
```python
numbers = [1, 2, 3]
numbers.append(4)           # [1, 2, 3, 4]
numbers.extend([5, 6])      # [1, 2, 3, 4, 5, 6]
first = numbers[0]          # 1
last = numbers[-1]          # 6
count = len(numbers)        # 6
```

---

### Python Section 4: Control Flow

#### 4.1. if Statements
Full if/elif/else support with multiple branches:

**Syntax:**
```python
if condition:
    statements
elif another_condition:
    statements
else:
    statements
```

**Features:**
- Multiple `elif` branches supported
- Optional `else` clause
- Nested if statements
- Boolean expressions with `and`, `or`, `not`

**Examples:**
```python
# Simple if/else
if age >= 18:
    status = "adult"
else:
    status = "minor"

# Multiple elif branches
if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
elif score >= 70:
    grade = "C"
else:
    grade = "F"

# Nested conditions
if customer_type == "premium":
    if balance > 100000:
        tier = "platinum"
    else:
        tier = "gold"
else:
    tier = "standard"
```

#### 4.2. for Statements
Iteration over lists and ranges:

**Syntax:**
```python
for variable in iterable:
    statements
```

**Features:**
- Iterate over lists, strings, and ranges
- Access to loop variable
- Nested loops supported
- Works with break/continue

**Examples:**
```python
# Iterate over list
for customer in INPUT.customers:
    OUTPUT["names"].append(customer.name)

# Iterate over range
for i in range(5):
    OUTPUT["numbers"].append(i)  # [0, 1, 2, 3, 4]

# Nested loops
for i in range(3):
    for j in range(2):
        OUTPUT.append(i * 10 + j)  # [0, 1, 10, 11, 20, 21]
```

#### 4.3. The range() Function
Generate sequences of numbers:

**Signatures:**
1. `range(stop)` - From 0 to stop (exclusive)
2. `range(start, stop)` - From start to stop (exclusive)
3. `range(start, stop, step)` - With custom step

**Examples:**
```python
range(5)           # [0, 1, 2, 3, 4]
range(2, 7)        # [2, 3, 4, 5, 6]
range(0, 10, 2)    # [0, 2, 4, 6, 8]
range(10, 0, -1)   # [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
range(5, 0, -2)    # [5, 3, 1]
```

**Use Cases:**
```python
# Generate indices
for i in range(len(items)):
    process(items[i])

# Count backwards
for countdown in range(10, 0, -1):
    OUTPUT.append(countdown)

# Skip elements
for i in range(0, 100, 10):
    OUTPUT.append(i)  # [0, 10, 20, ..., 90]
```

#### 4.4. break and continue Statements
Loop control flow:

**break** - Exit the loop immediately
**continue** - Skip to next iteration

**Examples:**
```python
# break - stop when condition met
for item in items:
    if item.value > 1000:
        break  # Exit loop
    OUTPUT.append(item)

# continue - skip certain items
for number in range(10):
    if number % 2 == 0:
        continue  # Skip even numbers
    OUTPUT.append(number)  # [1, 3, 5, 7, 9]

# Combined in nested loops
for i in range(5):
    if i == 3:
        break  # Exit outer loop
    for j in range(5):
        if j == 2:
            continue  # Skip j=2, continue inner loop
        OUTPUT.append(i * 10 + j)
```

---

### DateTime Support with Timezones

Comprehensive date and time operations with full timezone support:

#### Core Functions

**1. now() - Current DateTime**
```python
# System timezone
current_time = now()

# Specific timezone
utc_time = now("UTC")
ny_time = now("America/New_York")
tokyo_time = now("Asia/Tokyo")
```

**2. parseDate() - Parse String to DateTime**
```python
# Basic date parsing
date = parseDate("2024-02-22", "yyyy-MM-dd")

# Different formats
date = parseDate("22/02/2024", "dd/MM/yyyy")
date = parseDate("20240222", "yyyyMMdd")

# With timezone
date = parseDate("2024-02-22", "yyyy-MM-dd", "UTC")
```

**3. formatDate() - Format DateTime to String**
```python
# Format to different patterns
formatted = formatDate(datetime, "yyyy-MM-dd")      # "2024-02-22"
formatted = formatDate(datetime, "dd/MM/yyyy")      # "22/02/2024"
formatted = formatDate(datetime, "yyyyMMdd")        # "20240222"
formatted = formatDate(datetime, "yyyy-MM-dd HH:mm") # "2024-02-22 14:30"
```

#### Date Arithmetic

**Add/Subtract Time Units:**
```python
# Add days
tomorrow = addDays(now(), 1)
next_week = addDays(now(), 7)
yesterday = addDays(now(), -1)

# Add months
next_month = addMonths(now(), 1)
last_year_month = addMonths(now(), -12)

# Add years
next_year = addYears(now(), 1)

# Add hours/minutes
later = addHours(now(), 2)
soon = addMinutes(now(), 30)
```

#### Real-World Examples

**Date Format Conversion:**
```python
def transform(INPUT):
    OUTPUT = {}
    
    # Convert yyyyMMdd to dd/MM/yyyy
    input_date = parseDate(INPUT.transaction_date, "yyyyMMdd")
    OUTPUT["formatted_date"] = formatDate(input_date, "dd/MM/yyyy")
    
    return OUTPUT

# Input: {"transaction_date": "20240222"}
# Output: {"formatted_date": "22/02/2024"}
```

**Settlement Date Calculation:**
```python
def transform(INPUT):
    OUTPUT = {}
    
    # Parse trade date and add settlement days
    trade_date = parseDate(INPUT.trade_date, "yyyyMMdd")
    settlement_date = addDays(trade_date, INPUT.settlement_days)
    
    OUTPUT["settlement_date"] = formatDate(settlement_date, "yyyy-MM-dd")
    
    return OUTPUT

# Input: {"trade_date": "20240220", "settlement_days": 3}
# Output: {"settlement_date": "2024-02-23"}
```

**Age Calculation:**
```python
def transform(INPUT):
    OUTPUT = {}
    
    birth_date = parseDate(INPUT.date_of_birth, "yyyy-MM-dd")
    current = now()
    
    # Calculate age (simplified)
    age_years = current.year - birth_date.year
    
    OUTPUT["age"] = age_years
    OUTPUT["is_adult"] = age_years >= 18
    
    return OUTPUT
```

**Timezone Conversion:**
```python
def transform(INPUT):
    OUTPUT = {}
    
    # Parse UTC time and convert to local timezone
    utc_time = parseDate(INPUT.utc_timestamp, "yyyy-MM-dd HH:mm:ss", "UTC")
    local_time = utc_time.withTimezone("America/New_York")
    
    OUTPUT["local_time"] = formatDate(local_time, "yyyy-MM-dd HH:mm:ss")
    
    return OUTPUT
```

#### Supported Date Formats

Common pattern letters:
- `yyyy` - 4-digit year (2024)
- `yy` - 2-digit year (24)
- `MM` - 2-digit month (02)
- `dd` - 2-digit day (22)
- `HH` - 2-digit hour (14)
- `mm` - 2-digit minute (30)
- `ss` - 2-digit second (45)

Examples:
- `"yyyy-MM-dd"` → "2024-02-22"
- `"dd/MM/yyyy"` → "22/02/2024"
- `"yyyyMMdd"` → "20240222"
- `"yyyy-MM-dd HH:mm:ss"` → "2024-02-22 14:30:45"

#### Timezone Support

Supports all standard timezone IDs:
- `"UTC"` - Coordinated Universal Time
- `"America/New_York"` - Eastern Time (US)
- `"America/Chicago"` - Central Time (US)
- `"America/Los_Angeles"` - Pacific Time (US)
- `"Europe/London"` - British Time
- `"Asia/Tokyo"` - Japan Time
- `"Australia/Sydney"` - Australian Eastern Time

---

## 🏗️ Architecture

### Clean Separation of Concerns

```
┌─────────────────────────────────────────────┐
│  GrizzlyEngine (Entry Point)                │
└─────────────────┬───────────────────────────┘
                  │
    ┌─────────────┴─────────────┐
    │                           │
    ▼                           ▼
┌─────────┐              ┌────────────┐
│ Lexer   │────tokens───>│  Parser    │
└─────────┘              └──────┬─────┘
                                │
                                │ AST
                                ▼
                         ┌──────────────┐
                         │ Interpreter  │
                         └──────────────┘
```

### Components

**1. Lexer (GrizzlyLexer.java)**
- Tokenizes source code
- Handles Python indentation (INDENT/DEDENT tokens)
- Recognizes keywords, operators, literals
- Proper NUMBER token support (not strings)

**2. Parser (GrizzlyParser.java)**
- Builds Abstract Syntax Tree (AST)
- Proper DEDENT consumption in compound statements
- Creates NumberLiteral AST nodes (no string hacks)
- Handles nested structures correctly

**3. AST Nodes (parser/ast/)**
- `NumberLiteral` - Numeric values (Integer/Double)
- `StringLiteral` - Text values
- `IfStatement` - Conditional logic with elif/else
- `ForLoop` - Iteration statements
- `BreakStatement`, `ContinueStatement` - Loop control
- And more...

**4. Interpreter (GrizzlyInterpreter.java)**
- Executes AST
- Variable scoping with ExecutionContext
- Built-in functions (len, range, now, parseDate, etc.)
- Exception-based control flow for break/continue
- Numeric type handling with proper equality

**5. DateTime Support (types/DateTimeValue.java)**
- Immutable wrapper around ZonedDateTime
- Full timezone support
- Date arithmetic operations
- Format/parse with any pattern

---

## 📖 Usage Examples

### Basic Transformation
```python
def transform(INPUT):
    OUTPUT = {}
    OUTPUT["full_name"] = INPUT.first_name + " " + INPUT.last_name
    OUTPUT["email"] = INPUT.email.lower()
    return OUTPUT
```

### Conditional Logic
```python
def transform(INPUT):
    OUTPUT = {}
    
    # Calculate tier based on balance
    if INPUT.balance >= 100000:
        OUTPUT["tier"] = "platinum"
    elif INPUT.balance >= 50000:
        OUTPUT["tier"] = "gold"
    elif INPUT.balance >= 10000:
        OUTPUT["tier"] = "silver"
    else:
        OUTPUT["tier"] = "bronze"
    
    return OUTPUT
```

### List Processing
```python
def transform(INPUT):
    OUTPUT = {}
    OUTPUT["valid_transactions"] = []
    
    for transaction in INPUT.transactions:
        # Skip invalid transactions
        if transaction.amount <= 0:
            continue
        
        # Stop after 100 transactions
        if len(OUTPUT["valid_transactions"]) >= 100:
            break
        
        OUTPUT["valid_transactions"].append({
            "id": transaction.id,
            "amount": transaction.amount
        })
    
    return OUTPUT
```

### Date Transformation
```python
def transform(INPUT):
    OUTPUT = {}
    
    # Parse various date formats
    birth_date = parseDate(INPUT.dob, "yyyyMMdd")
    
    # Calculate dates
    age_years = (now().year - birth_date.year)
    retirement_date = addYears(birth_date, 65)
    
    # Format for output
    OUTPUT["birth_date"] = formatDate(birth_date, "dd/MM/yyyy")
    OUTPUT["retirement_date"] = formatDate(retirement_date, "yyyy-MM-dd")
    OUTPUT["age"] = age_years
    
    return OUTPUT
```

### Complex Business Logic
```python
def transform(INPUT):
    OUTPUT = {}
    OUTPUT["loan_applications"] = []
    
    for applicant in INPUT.applicants:
        # Check eligibility
        age = (now().year - parseDate(applicant.dob, "yyyy-MM-dd").year)
        
        if age < 18:
            continue  # Skip minors
        
        # Calculate loan amount based on income and credit score
        if applicant.credit_score >= 750:
            max_loan = applicant.annual_income * 5
        elif applicant.credit_score >= 650:
            max_loan = applicant.annual_income * 3
        else:
            max_loan = applicant.annual_income * 2
        
        # Add to output
        OUTPUT["loan_applications"].append({
            "applicant_id": applicant.id,
            "max_loan_amount": max_loan,
            "approved": applicant.credit_score >= 600
        })
    
    return OUTPUT
```

---

## 🚀 Getting Started

### Installation
```bash
cd ~/Workspace/vscode/GrizzlyEngine
tar -xzf grizzly-PRODUCTION-CLEAN.tar.gz --strip-components=1
./gradlew build
```

### Quick Start
```java
// Create engine
GrizzlyEngine engine = new GrizzlyEngine();

// Compile template
String template = """
    def transform(INPUT):
        OUTPUT = {}
        OUTPUT["result"] = INPUT.value * 2
        return OUTPUT
    """;
GrizzlyTemplate compiled = engine.compileFromString(template);

// Execute
Map<String, Object> input = Map.of("value", 42);
Map<String, Object> output = compiled.executeRaw(input);

System.out.println(output.get("result")); // 84
```

### With POJOs
```java
// Define POJOs
record Customer(String name, int age) {}
record CustomerDTO(String name, String ageGroup) {}

// Execute with type conversion
Customer input = new Customer("Alice", 25);
CustomerDTO output = compiled.execute(input, CustomerDTO.class);
```

---

## ✅ Test Coverage

**51 Tests - All Passing**

### Section 3 Tests (28 tests)
- Number operations and precedence
- String concatenation and methods
- List operations and methods
- Negative indexing
- Type handling

### Section 4 Tests (13 tests)
- if/elif/else statements (3 tests)
- range() function (5 tests)
- break statement (2 tests)
- continue statement (2 tests)
- Combined break/continue (1 test)

### DateTime Tests (10 tests)
- now() with and without timezone
- parseDate() with various formats
- formatDate() to different patterns
- Date arithmetic (days, months, years)
- Real-world date conversion pipelines
- Complex date mapping scenarios

---

## 🎯 Production Ready

### Code Quality
✅ **Zero Hacks** - Clean implementation throughout  
✅ **100% JavaDoc Coverage** - Every method documented  
✅ **Proper AST Design** - NumberLiteral, not string conversion  
✅ **Clean Parser** - Correct DEDENT handling  
✅ **Production Error Messages** - Clear errors with line numbers

### Performance
✅ **Efficient Parsing** - Single-pass lexer and parser  
✅ **Fast Execution** - Direct AST interpretation  
✅ **Minimal Allocations** - Reusable contexts

### Reliability
✅ **All Tests Passing** - 51/51 tests green  
✅ **Type Safety** - Proper numeric types  
✅ **Exception Handling** - Comprehensive error coverage

---

## 📚 API Reference

### Built-in Functions

| Function | Description | Example |
|----------|-------------|---------|
| `len(list)` | Get list length | `len([1,2,3])` → 3 |
| `range(stop)` | Generate sequence | `range(5)` → [0,1,2,3,4] |
| `range(start, stop)` | Range with start | `range(2,5)` → [2,3,4] |
| `range(start, stop, step)` | Range with step | `range(0,10,2)` → [0,2,4,6,8] |
| `now()` | Current datetime | `now()` |
| `now(timezone)` | Current in timezone | `now("UTC")` |
| `parseDate(str, fmt)` | Parse date string | `parseDate("20240222", "yyyyMMdd")` |
| `parseDate(str, fmt, tz)` | Parse with timezone | `parseDate("2024-02-22", "yyyy-MM-dd", "UTC")` |
| `formatDate(dt, fmt)` | Format datetime | `formatDate(dt, "dd/MM/yyyy")` |
| `addDays(dt, days)` | Add days | `addDays(now(), 5)` |
| `addMonths(dt, months)` | Add months | `addMonths(now(), 2)` |
| `addYears(dt, years)` | Add years | `addYears(now(), 1)` |
| `addHours(dt, hours)` | Add hours | `addHours(now(), 3)` |
| `addMinutes(dt, mins)` | Add minutes | `addMinutes(now(), 30)` |

### String Methods

| Method | Description | Example |
|--------|-------------|---------|
| `str.upper()` | Uppercase | `"hello".upper()` → "HELLO" |
| `str.lower()` | Lowercase | `"HELLO".lower()` → "hello" |
| `str.strip()` | Trim whitespace | `" hi ".strip()` → "hi" |

### List Methods

| Method | Description | Example |
|--------|-------------|---------|
| `list.append(item)` | Add element | `[1,2].append(3)` → [1,2,3] |
| `list.extend(items)` | Add multiple | `[1].extend([2,3])` → [1,2,3] |

---

## 🔍 Implementation Details

### Number Literals
- Lexer creates NUMBER tokens
- Parser creates `NumberLiteral(Integer/Double)` AST nodes
- Interpreter returns actual Number objects
- **No string-to-number conversion hacks**

### DEDENT Token Handling
- Compound statements (if/for) consume their own DEDENTs
- Block parsers stop AT DEDENT (don't consume it)
- parseIfStatement only skips NEWLINES when looking for elif/else
- **No aggressive DEDENT consumption**

### Break/Continue
- Implemented as exceptions (BreakException, ContinueException)
- Caught by for loop with labeled break/continue
- Pass through if statements transparently
- **Clean exception-based control flow**

### DateTime
- Immutable DateTimeValue wraps ZonedDateTime
- Handles both LocalDate and LocalDateTime parsing
- Full timezone support via ZoneId
- **Production-grade date handling**

---

## 🎓 Advanced Features

### Nested Loops
```python
for i in range(3):
    for j in range(5):
        if j == 2:
            break  # Breaks inner loop only
        OUTPUT.append(i * 10 + j)
```

### Complex Conditionals
```python
if (age >= 18 and credit_score >= 700) or has_cosigner:
    if income > 50000:
        approved = True
```

### Date Pipelines
```python
# Parse → Calculate → Format pipeline
trade_date = parseDate(INPUT.trade_date, "yyyyMMdd")
settlement = addDays(trade_date, 3)
OUTPUT["settlement"] = formatDate(settlement, "dd/MM/yyyy")
```

---

## 🐛 Error Handling

Clear, actionable error messages:

```
Error: 'for' loop body cannot be empty at line 5
       Suggestion: Add at least one statement in the loop body

Error: parseDate() requires 2 or 3 arguments: (dateString, format, [timezone])
       Got: parseDate("2024-02-22")
       
Error: Failed to parse date '2024-02-22' with format 'yyyyMMdd'
       Did you mean 'yyyy-MM-dd'?
```

---

## 📝 License

Production-ready code with comprehensive documentation.

---

**Built for Banking Domain MISMO Schema Transformations** 🏦🐻