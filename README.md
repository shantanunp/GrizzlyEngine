# Grizzly Engine

A lightweight Python-like template engine for JSON-to-JSON data transformation in Java.

**Requirements:** Java 21+

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

### Using Core API (for advanced usage)

```java
import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;

GrizzlyEngine engine = new GrizzlyEngine();
GrizzlyTemplate template = engine.compile(templateCode);

// With Java Map
Map<String, Object> input = Map.of("firstName", "John", "lastName", "Doe");
Map<String, Object> output = template.executeRaw(input);

// With type-safe DictValue
DictValue input = ValueConverter.fromJavaMap(inputMap);
DictValue output = template.execute(input);
```

---

## Safe Navigation Operators

GrizzlyEngine extends Python syntax with safe navigation operators (`?.` and `?[`) to handle null values gracefully in nested data structures.

### Problem: Null in Deep Property Chains

```python
# Standard access - crashes if any part is null
city = INPUT["deal"]["loan"]["address"]["city"]  # NullPointerException if loan is null!
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

### How It Works

| Operator | Description | On Null |
|----------|-------------|---------|
| `.` | Regular attribute access | Throws exception (in STRICT mode) or returns null |
| `?.` | Safe attribute access | Returns `None`, no exception |
| `[key]` | Regular dictionary access | Throws exception or returns null |
| `?[key]` | Safe dictionary access | Returns `None`, no exception |

---

## Null Handling Modes

Configure how the engine handles null values during property access:

```java
import com.grizzly.core.interpreter.InterpreterConfig;
import com.grizzly.core.validation.NullHandling;

InterpreterConfig config = InterpreterConfig.builder()
    .nullHandling(NullHandling.SAFE)  // STRICT, SAFE, or SILENT
    .build();

GrizzlyEngine engine = new GrizzlyEngine(config);
```

### Mode Comparison

| Mode | Behavior | Use Case |
|------|----------|----------|
| **STRICT** | Throws exception on null access (unless `?.` used) | Development, testing |
| **SAFE** | Returns null, tracks all accesses | Production (recommended) |
| **SILENT** | Returns null, no tracking | High-performance batch processing |

### Quick Configuration

```java
// Development - fail fast on null
InterpreterConfig dev = InterpreterConfig.development();

// Production - safe with tracking (default)
InterpreterConfig prod = InterpreterConfig.defaults();

// High performance - no tracking
InterpreterConfig fast = InterpreterConfig.highPerformance();
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
if (result.validationReport().hasAnyErrors()) {
    System.err.println("Errors: " + result.validationReport().toJson());
}
```

### Validation Report API

```java
ValidationReport report = result.validationReport();

// Check for specific error types
report.hasPathErrors();       // Null in path chain
report.hasKeyNotFoundErrors(); // Missing dictionary key
report.hasAnyErrors();        // Any error type

// Get detailed records
report.getPathErrors();       // List of broken path accesses
report.getKeyNotFoundErrors(); // List of missing key accesses
report.getExpectedNulls();    // Nulls that used ?. (expected)
report.getSuccessful();       // All successful accesses

// Get summary
Map<String, Integer> summary = report.getSummary();
// {total=15, successful=12, pathErrors=2, keyNotFound=1, ...}

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

## Logging

Grizzly Engine uses SLF4J for logging. To see logs, add an SLF4J implementation to your project.

### Option 1: SLF4J Simple (quick setup)

```gradle
dependencies {
    implementation 'com.grizzly:grizzly-engine:1.0.0'
    runtimeOnly 'org.slf4j:slf4j-simple:2.0.11'
}
```

Configure via system property or `simplelogger.properties`:

```properties
# src/main/resources/simplelogger.properties
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

    <!-- Grizzly Engine logging -->
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

If you don't add any SLF4J implementation, you'll see:
```
SLF4J: No SLF4J providers were found.
SLF4J: Defaulting to no-operation (NOP) logger implementation
```

The engine will still work, just without logs.

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
name.upper()      # "JOHN"
name.lower()      # "john"
text.strip()      # Remove whitespace
text.split(",")   # Split to list
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

## Architecture

```
com.grizzly/
├── core/                    # Core engine (format-agnostic)
│   ├── GrizzlyEngine        # Compiles templates
│   ├── GrizzlyTemplate      # Executes with DictValue/Map
│   ├── lexer/               # Tokenization
│   ├── parser/              # AST generation
│   ├── interpreter/         # AST execution
│   ├── types/               # Value hierarchy (StringValue, DictValue, etc.)
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
│   ├── FormatReader         # Interface for reading formats
│   ├── FormatWriter         # Interface for writing formats
│   ├── FormatRegistry       # Central registry
│   └── json/
│       ├── JsonReader       # JSON → DictValue
│       ├── JsonWriter       # DictValue → JSON
│       ├── JsonTemplate     # Convenience wrapper
│       └── JsonTransformationResult # JSON output + validation
└── mapper/
    └── PojoMapper           # POJO ↔ Map conversion
```

---

## Production Safeguards

The engine includes built-in safeguards:

```java
import com.grizzly.core.interpreter.InterpreterConfig;

InterpreterConfig config = InterpreterConfig.builder()
    .maxLoopIterations(10_000)    // Prevent infinite loops
    .maxRecursionDepth(100)       // Prevent stack overflow
    .executionTimeout(Duration.ofSeconds(30))  // Timeout
    .nullHandling(NullHandling.SAFE)  // Never crash on null
    .trackAccess(true)            // Enable validation reports
    .build();

GrizzlyEngine engine = new GrizzlyEngine(config);
```

---

## Error Handling

```java
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
