# Grizzly Engine

A lightweight Python-like template engine for JSON-to-JSON data transformation in Java.

**Requirements:** Java 17+

---

## Table of Contents

- [Quick Start](#quick-start)
- [MISMO Loan Example](#mismo-loan-example)
- [Safe Navigation Operators](#safe-navigation-operators)
- [Null Handling Modes](#null-handling-modes)
- [Access Tracking & Validation](#access-tracking--validation)
- [Template Syntax](#template-syntax)
- [API Reference](#api-reference)
- [Logging](#logging)
- [Architecture](#architecture)
- [Error Handling](#error-handling)

---

## Quick Start

### Installation

**Gradle:**
```gradle
dependencies {
    implementation 'com.grizzly:grizzly-engine:1.0.0'
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.grizzly</groupId>
    <artifactId>grizzly-engine</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

```java
import com.grizzly.format.json.JsonTemplate;

// Compile a template
JsonTemplate template = JsonTemplate.compile("""
    def transform(INPUT):
        OUTPUT = {}
        OUTPUT["fullName"] = INPUT["firstName"] + " " + INPUT["lastName"]
        OUTPUT["age"] = INPUT["age"]
        return OUTPUT
    """);

// Transform JSON
String input = "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"age\": 30}";
String output = template.transform(input);
// Output: {"fullName": "John Doe", "age": 30}
```

---

## MISMO Loan Example

A full **deal → loan → borrower, address, property, assets, credit** example with input schema, sample payloads (including nulls/arrays), Grizzly template, and output schema is in [docs/mismo-loan-example.md](docs/mismo-loan-example.md). It demonstrates safe navigation, loops, conditionals, and validation. A runnable test is in `MismoLoanExampleTest`.

---

## Safe Navigation Operators

GrizzlyEngine extends Python syntax with safe navigation operators (`?.` and `?[`) to handle null values gracefully in nested data structures.

### Test Cases for Safe Navigation, Null Handling & Access Tracking
```text
SafeNavigationTest, ValidationReportTest, AccessTrackerTest, GrizzlyLexerTest
```

### Problem: Null in Deep Property Chains

```python
# Standard access - crashes if any part is null
city = INPUT["deal"]["loan"]["address"]["city"]  # Throws exception if loan is null!
```

### Solution: Safe Navigation

```python
# Safe attribute access with ?.
city = INPUT?.deal?.loan?.address?.city  # Returns None if any part is null

# Safe dictionary access with ?[
city = INPUT?["deal"]?["loan"]?["address"]?["city"]  # Same behavior for dict access

# Mixed safe and regular access
city = INPUT["deal"]?.loan?.address?["city"]  # Regular access for known fields, safe for optional
```

### Operator Reference

| Operator | Description | On Null |
|----------|-------------|---------|
| `.` | Regular attribute access | Throws exception (in STRICT mode) or returns null |
| `?.` | Safe attribute access | Returns `None`, no exception |
| `[key]` | Regular dictionary access | Throws exception or returns null |
| `?[key]` | Safe dictionary access | Returns `None`, no exception |

---

## Null Handling Modes

Configure how the engine handles null values during property access.

### Mode Comparison

| Mode | Behavior | Use Case |
|------|----------|----------|
| **STRICT** | Throws exception on null access (unless `?.` used) | Development, testing |
| **SAFE** | Returns null, tracks all accesses | Production (recommended) |
| **SILENT** | Returns null, no tracking | High-performance batch processing |

### Configuration

```java
import com.grizzly.core.interpreter.InterpreterConfig;
import com.grizzly.core.validation.NullHandling;

// Option 1: Use preset configurations
InterpreterConfig dev = InterpreterConfig.development();     // STRICT mode
InterpreterConfig prod = InterpreterConfig.defaults();       // SAFE mode (default)
InterpreterConfig fast = InterpreterConfig.highPerformance(); // SILENT mode

// Option 2: Custom configuration with builder
InterpreterConfig config = InterpreterConfig.builder()
    .nullHandling(NullHandling.SAFE)
    .trackAccess(true)
    .maxLoopIterations(10_000)
    .maxRecursionDepth(100)
    .executionTimeout(Duration.ofSeconds(30))
    .build();

// Create engine with config
GrizzlyEngine engine = new GrizzlyEngine(config);
```

---

## Access Tracking & Validation

Track every property access during transformation to diagnose issues like:
- Why is a field null in the output?
- Was it because the input was null, or the key didn't exist?
- Which paths failed and which succeeded?

### Basic Usage

```java
import com.grizzly.format.json.JsonTemplate;
import com.grizzly.format.json.JsonTransformationResult;

JsonTemplate template = JsonTemplate.compile(templateCode);
JsonTransformationResult result = template.transformWithValidation(jsonInput);

// Get transformed output
String output = result.outputJson();

// Check validation report
if (result.hasPathErrors()) {
    System.err.println("Errors: " + result.validationReport().toJson());
}
```

### ValidationReport API

```java
ValidationReport report = result.validationReport();

// Check for specific error types
report.hasPathErrors();        // Null in path chain
report.hasKeyNotFoundErrors(); // Missing dictionary key
report.hasAnyErrors();         // Any error type
report.isClean();              // No errors at all

// Get detailed records
report.getPathErrors();        // List of broken path accesses
report.getKeyNotFoundErrors(); // List of missing key accesses
report.getExpectedNulls();     // Nulls that used ?. (expected)
report.getSuccessful();        // All successful accesses
report.getAllErrors();         // All error records
report.getAllRecords();        // All access records

// Get summary
Map<String, Integer> summary = report.getSummary();
// {total=15, successful=12, pathErrors=2, keyNotFound=1, ...}

// Grouping
Map<String, List<AccessRecord>> bySegment = report.groupByBrokenSegment();
Map<Integer, List<AccessRecord>> byLine = report.groupByLineNumber();

// Export as JSON
String json = report.toJson();
```

### Access Status Types

| Status | Meaning |
|--------|---------|
| `SUCCESS` | Path resolved to a value |
| `PATH_BROKEN` | Null encountered in path chain |
| `KEY_NOT_FOUND` | Dictionary key doesn't exist |
| `INDEX_OUT_OF_BOUNDS` | List index out of range |
| `VALUE_NULL` | Path resolved but value is null |
| `VALUE_EMPTY` | Path resolved but value is empty |
| `EXPECTED_NULL` | Used `?.` and got null (expected) |

---

## Template Syntax

Grizzly templates use Python-like syntax:

### Data Types

```python
# Numbers
count = 42
price = 19.99

# Strings
name = "John"
greeting = 'Hello'

# Booleans
active = True
disabled = False

# None
value = None

# Lists
items = [1, 2, 3]
mixed = ["a", 1, True]

# Dicts
person = {"name": "John", "age": 30}
```

### Control Flow

```python
# If/elif/else
if age >= 18:
    status = "adult"
elif age >= 13:
    status = "teen"
else:
    status = "child"

# For loops
for item in items:
    OUTPUT["names"].append(item["name"])

# Range
for i in range(5):
    print(i)  # 0, 1, 2, 3, 4

for i in range(2, 8, 2):
    print(i)  # 2, 4, 6

# Break/Continue
for item in items:
    if item["skip"]:
        continue
    if item["stop"]:
        break
    process(item)
```

### String Methods

```python
name.upper()           # "JOHN"
name.lower()           # "john"
text.strip()           # Remove whitespace
text.split(",")        # Split to list
text.replace("a", "b")
text.startswith("Hello")
text.endswith("!")
```

### List Methods

```python
items.append(value)
items.extend([1, 2, 3])
items.pop()
items.pop(0)
len(items)
```

### Dict Methods

```python
dict.keys()
dict.values()
dict.items()
dict.get("key", "default")
```

### Built-in Functions

```python
len(items)
str(123)
int("42")
float("3.14")
abs(-5)
min(1, 2, 3)
max(1, 2, 3)
sum([1, 2, 3])
round(3.14159, 2)
isinstance(value, "str")
```

### DateTime Functions

```python
now()                              # Current datetime
now("UTC")                         # With timezone
parseDate("2024-02-22", "yyyy-MM-dd")
formatDate(dt, "dd/MM/yyyy")
addDays(dt, 5)
addMonths(dt, 2)
addYears(dt, 1)
```

---

## API Reference

### JsonTemplate (Recommended Entry Point)

```java
import com.grizzly.format.json.JsonTemplate;
import com.grizzly.format.json.JsonTransformationResult;
import com.grizzly.core.interpreter.InterpreterConfig;

// Compile template
JsonTemplate template = JsonTemplate.compile(templateCode);
JsonTemplate template = JsonTemplate.compile(templateCode, config);

// Transform JSON string
String output = template.transform(jsonInput);
String output = template.transform(inputMap);

// Transform with validation
JsonTransformationResult result = template.transformWithValidation(jsonInput);
JsonTransformationResult result = template.transformWithValidation(inputMap);

// JsonTransformationResult methods
result.outputJson();         // String - transformed JSON
result.output();             // DictValue - internal representation
result.validationReport();   // ValidationReport - access tracking
result.executionTimeMs();    // long - execution time in milliseconds
result.hasPathErrors();      // boolean - quick check for errors
```

### GrizzlyEngine (Core API)

```java
import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;

// Create engine
GrizzlyEngine engine = new GrizzlyEngine();                  // Default config
GrizzlyEngine engine = new GrizzlyEngine(config);            // Custom config
GrizzlyEngine engine = new GrizzlyEngine(enableCaching);     // With/without cache
GrizzlyEngine engine = new GrizzlyEngine(enableCaching, config);

// Compile template
GrizzlyTemplate template = engine.compile(templateCode);

// GrizzlyTemplate methods
DictValue output = template.execute(inputDictValue);
Map<String, Object> output = template.executeRaw(inputMap);
TransformationResult result = template.executeWithValidation(inputDictValue);
TransformationResult result = template.executeWithValidation(inputMap);
```

### InterpreterConfig

```java
import com.grizzly.core.interpreter.InterpreterConfig;
import com.grizzly.core.validation.NullHandling;
import java.time.Duration;

// Preset configurations
InterpreterConfig.defaults();        // SAFE mode, tracking enabled
InterpreterConfig.development();     // STRICT mode, fail fast
InterpreterConfig.highPerformance(); // SILENT mode, no tracking
InterpreterConfig.unlimited();       // No limits (trusted templates)

// Builder
InterpreterConfig config = InterpreterConfig.builder()
    .nullHandling(NullHandling.SAFE)      // STRICT, SAFE, or SILENT
    .trackAccess(true)                    // Enable access tracking
    .maxLoopIterations(1_000_000)         // Max iterations per loop
    .maxRecursionDepth(1000)              // Max function recursion depth
    .executionTimeout(Duration.ofSeconds(30)) // Execution timeout
    .build();

// Query config
config.nullHandling();       // NullHandling enum
config.trackAccess();        // boolean
config.isTrackingEnabled();  // boolean (considers SILENT mode)
config.maxLoopIterations();  // int
config.maxRecursionDepth();  // int
config.executionTimeout();   // Duration
```

### ValidationReport

```java
import com.grizzly.core.validation.ValidationReport;
import com.grizzly.core.validation.AccessRecord;
import com.grizzly.core.validation.AccessStatus;

// Query methods
report.hasPathErrors();         // boolean
report.hasKeyNotFoundErrors();  // boolean
report.hasAnyErrors();          // boolean
report.hasAnyNulls();           // boolean
report.isClean();               // boolean - no errors

// Get records
report.getPathErrors();         // List<AccessRecord>
report.getKeyNotFoundErrors();  // List<AccessRecord>
report.getIndexErrors();        // List<AccessRecord>
report.getNullValues();         // List<AccessRecord>
report.getEmptyValues();        // List<AccessRecord>
report.getExpectedNulls();      // List<AccessRecord>
report.getSuccessful();         // List<AccessRecord>
report.getAllErrors();          // List<AccessRecord>
report.getAllRecords();         // List<AccessRecord>

// Summary & grouping
report.getSummary();            // Map<String, Integer>
report.groupByBrokenSegment();  // Map<String, List<AccessRecord>>
report.groupByLineNumber();     // Map<Integer, List<AccessRecord>>

// Output
report.toJson();                // String - JSON representation
report.toString();              // String - concise summary

// AccessRecord fields
record.fullPath();              // String - e.g., "INPUT.deal.loan.city"
record.status();                // AccessStatus enum
record.brokenAtSegment();       // String - segment where path broke
record.retrievedValue();        // Value - the retrieved value (if any)
record.lineNumber();            // int - line number in template
record.expectedNull();          // boolean - was ?. used?
record.isError();               // boolean - is this an error status?
record.isNull();                // boolean - is value null?
```

### ValueConverter

```java
import com.grizzly.core.types.ValueConverter;
import com.grizzly.core.types.DictValue;
import com.grizzly.core.types.Value;

// Java Map to DictValue
DictValue dict = ValueConverter.fromJavaMap(javaMap);

// Java object to Value
Value value = ValueConverter.fromJava(javaObject);

// Value to Java object
Object obj = ValueConverter.toJava(value);

// DictValue to Java Map
Map<String, Object> map = ValueConverter.toJavaMap(dictValue);
```

---

## Logging

Grizzly Engine uses SLF4J for logging. To see logs, add an SLF4J implementation to your project.

### Option 1: SLF4J Simple (quick setup)

```gradle
dependencies {
    implementation 'com.grizzly:grizzly-engine:1.0.0'
    runtimeOnly 'org.slf4j:slf4j-simple:2.0.11'
}
```

Configure via `src/main/resources/simplelogger.properties`:

```properties
org.slf4j.simpleLogger.defaultLogLevel=INFO
org.slf4j.simpleLogger.log.com.grizzly.core.logging=DEBUG
org.slf4j.simpleLogger.showDateTime=true
org.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss.SSS
```

Or set via JVM argument:
```bash
-Dorg.slf4j.simpleLogger.log.com.grizzly.core.logging=DEBUG
```

### Option 2: Logback (recommended for production)

```gradle
dependencies {
    implementation 'com.grizzly:grizzly-engine:1.0.0'
    implementation 'ch.qos.logback:logback-classic:1.4.14'
}
```

Configure `src/main/resources/logback.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.grizzly.core.logging" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### Option 3: Log4j2

```gradle
dependencies {
    implementation 'com.grizzly:grizzly-engine:1.0.0'
    implementation 'org.apache.logging.log4j:log4j-slf4j2-impl:2.22.1'
}
```

### Log Levels

| Level | What it shows |
|-------|---------------|
| `ERROR` | Execution errors only |
| `WARN` | Warnings and errors |
| `INFO` | Compilation/execution timing |
| `DEBUG` | Tokens, AST, function calls |
| `TRACE` | Variable reads/writes, detailed execution |

### No Logging Implementation

If you don't add any SLF4J implementation:
```
SLF4J: No SLF4J providers were found.
SLF4J: Defaulting to no-operation (NOP) logger implementation
```

The engine will still work, just without logs.

---

## Architecture

```
com.grizzly/
├── core/                    # Core engine (format-agnostic)
│   ├── GrizzlyEngine        # Compiles templates
│   ├── GrizzlyTemplate      # Executes with DictValue/Map
│   ├── lexer/               # Tokenization
│   ├── parser/              # AST generation
│   ├── interpreter/         # AST execution
│   │   └── InterpreterConfig # Configuration with safeguards
│   ├── types/               # Value hierarchy (StringValue, DictValue, etc.)
│   │   └── ValueConverter   # Java ↔ Value conversion
│   ├── validation/          # Access tracking & null handling
│   │   ├── NullHandling     # STRICT, SAFE, SILENT modes
│   │   ├── AccessTracker    # Records property accesses
│   │   ├── AccessRecord     # Single access event
│   │   ├── AccessStatus     # SUCCESS, PATH_BROKEN, etc.
│   │   ├── ValidationReport # Aggregates access records
│   │   └── TransformationResult # Output + validation report
│   ├── exception/           # Custom exceptions
│   └── logging/             # SLF4J logging
├── format/                  # Format handlers
│   └── json/
│       ├── JsonTemplate     # Main entry point for JSON
│       └── JsonTransformationResult # JSON output + validation
└── mapper/
    └── PojoMapper           # POJO ↔ Map conversion
```

---

## Error Handling

```java
import com.grizzly.core.exception.GrizzlyParseException;
import com.grizzly.core.exception.GrizzlyExecutionException;
import com.grizzly.format.FormatException;

try {
    String output = template.transform(input);
} catch (GrizzlyParseException e) {
    // Template syntax error
    System.err.println("Parse error at line " + e.getLine() + ": " + e.getMessage());
} catch (GrizzlyExecutionException e) {
    // Runtime error during transformation
    System.err.println("Execution error: " + e.getMessage());
} catch (FormatException e) {
    // JSON parsing/writing error
    System.err.println("Format error: " + e.getMessage());
}
```

---

## Python Compatibility Note

GrizzlyEngine uses **Python-like** syntax with some extensions for JSON transformations:

| Feature | Python | GrizzlyEngine |
|---------|--------|---------------|
| Basic syntax | ✓ | ✓ |
| Safe navigation (`?.`, `?[`) | ✗ | ✓ (extension) |
| Dict literals | ✓ | ✓ |
| List comprehensions | ✓ | ✗ |
| Classes | ✓ | ✗ |
| Imports | Limited | Partial |

The safe navigation operators (`?.`, `?[`) are **not standard Python** but are commonly used in languages like Kotlin, C#, and JavaScript. They are added to GrizzlyEngine for ergonomic null handling in JSON transformation use cases.

---

## License

MIT License
