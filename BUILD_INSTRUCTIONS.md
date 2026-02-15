# Grizzly Engine - Build Instructions

## ✅ Lombok Removed - Pure Java!

All classes use plain Java - no Lombok, works with **JDK 21-25**.

---

## Quick Start (No Build Tool Needed)

### Option 1: Manual Compilation

```bash
# Extract
tar -xzf grizzly-engine-final-no-lombok.tar.gz
cd grizzly-engine

# Download Jackson JARs (if not already available)
wget https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.16.1/jackson-databind-2.16.1.jar
wget https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.16.1/jackson-core-2.16.1.jar
wget https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.16.1/jackson-annotations-2.16.1.jar

# Compile
javac -d build/classes \
  -cp "jackson-databind-2.16.1.jar:jackson-core-2.16.1.jar:jackson-annotations-2.16.1.jar" \
  $(find src/main/java -name "*.java")

# Create JAR
jar cf grizzly-engine.jar -C build/classes .

echo "✅ grizzly-engine.jar created!"
```

### Option 2: Using Maven

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.grizzly</groupId>
    <artifactId>grizzly-engine</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.16.1</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>2.16.1</version>
        </dependency>
    </dependencies>
</project>
```

Then:

```bash
mvn clean package
# JAR will be in target/grizzly-engine-1.0.0.jar
```

---

## Quick Test

Create `Test.java`:

```java
import com.grizzly.GrizzlyEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws Exception {
        // Create template
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["result"] = "Hello from Grizzly!"
                OUTPUT["input_was"] = INPUT.value
                return OUTPUT
            """;
        
        Path tempFile = Files.createTempFile("test", ".py");
        Files.writeString(tempFile, template);
        
        // Create input
        Map<String, Object> input = new HashMap<>();
        input.put("value", "test123");
        
        // Transform!
        GrizzlyEngine engine = new GrizzlyEngine();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = engine.transform(
            input,
            tempFile.toString(),
            Map.class
        );
        
        System.out.println("✅ SUCCESS!");
        System.out.println("Result: " + result);
        // Output: {result=Hello from Grizzly!, input_was=test123}
    }
}
```

Compile and run:

```bash
javac -cp ".:grizzly-engine.jar:jackson-*.jar" Test.java
java -cp ".:grizzly-engine.jar:jackson-*.jar" Test
```

---

## Project Structure

```
grizzly-engine/
├── build.gradle                 # Gradle build (optional)
├── src/main/java/com/grizzly/
│   ├── GrizzlyEngine.java       # Main API
│   ├── GrizzlyTemplate.java     # Compiled template
│   ├── lexer/
│   │   ├── Token.java           # Plain Java ✅
│   │   ├── TokenType.java
│   │   └── GrizzlyLexer.java
│   ├── parser/
│   │   ├── GrizzlyParser.java
│   │   └── ast/                 # All plain Java ✅
│   │       ├── ASTNode.java
│   │       ├── Program.java
│   │       ├── FunctionDef.java
│   │       ├── Assignment.java
│   │       ├── ReturnStatement.java
│   │       ├── FunctionCall.java
│   │       ├── IfStatement.java
│   │       ├── Identifier.java
│   │       ├── StringLiteral.java
│   │       ├── DictAccess.java
│   │       ├── AttrAccess.java
│   │       ├── BinaryOp.java
│   │       └── DictLiteral.java
│   ├── interpreter/
│   │   ├── GrizzlyInterpreter.java
│   │   └── ExecutionContext.java
│   ├── mapper/
│   │   └── PojoMapper.java
│   └── exception/
│       ├── GrizzlyParseException.java
│       └── GrizzlyExecutionException.java
└── src/test/java/              # Plain Java POJOs ✅
```

---

## Verified Lombok-Free!

```bash
# Check - should return 0
grep -r "lombok\|@Data\|@Value" src/ | wc -l
# Output: 0 ✅
```

---

## Dependencies

**Required:**
- Jackson Databind 2.16.1
- Jackson Core 2.16.1
- Jackson Annotations 2.16.1
- Jackson JSR310 2.16.1 (for date/time support)

**Total size:** ~1.5MB

---

## Usage Example

```java
// 1. Create engine
GrizzlyEngine engine = new GrizzlyEngine();

// 2. Write template
String template = """
    def transform(INPUT):
        OUTPUT = {}
        OUTPUT["customerId"] = INPUT.id
        OUTPUT["name"] = INPUT.firstName
        return OUTPUT
    """;

Files.writeString(Path.of("transform.py"), template);

// 3. Create input
Customer customer = new Customer("C123", "John");

// 4. Transform!
CustomerDTO result = engine.transform(
    customer,
    "transform.py",
    CustomerDTO.class
);

System.out.println(result.getCustomerId()); // C123
System.out.println(result.getName());       // John
```

---

## Troubleshooting

### ClassNotFoundException: jackson

Download Jackson JARs from Maven Central:
- https://repo1.maven.org/maven2/com/fasterxml/jackson/core/

### JDK Version

Requires JDK 21+ (uses text blocks `"""`)

To check:
```bash
java -version
# Should show 21 or higher
```

---

**You're all set!** 🐻

No Lombok, pure Java, works with any JDK 21-25!
