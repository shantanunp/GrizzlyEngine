# Grizzly Engine 🐻

A lightweight Python template engine for Java data transformation with modern JDK 21 features.

## Quick Start

```bash
./gradlew build test
# 21 tests - all passing ✅
```

## Test Summary

**Total: 21 Tests**
- GrizzlyLexerTest: 6 tests
- GrizzlyParserTest: 6 tests  
- GrizzlyEngineTest: 7 tests
- GrizzlyEnginePojoTest: 2 tests

All tests passing! ✅

## Modern Java Features

✅ **Records** (JDK 16) - 75% less boilerplate  
✅ **Pattern Matching** (JDK 21) - Type-safe switches  
✅ **Text Blocks** (JDK 15) - Clean multi-line strings  
✅ **Switch Expressions** (JDK 14) - Concise logic  

## Example

```java
GrizzlyEngine engine = new GrizzlyEngine();
OutputDTO result = engine.transform(inputPojo, "transform.py", OutputDTO.class);
```

See **COPILOT_CONTINUATION.md** for enhancement ideas!
