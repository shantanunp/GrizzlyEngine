package com.grizzly.core.validation;

import com.grizzly.core.GrizzlyEngine;
import com.grizzly.core.GrizzlyTemplate;
import com.grizzly.core.interpreter.InterpreterConfig;
import com.grizzly.format.json.JsonTemplate;
import com.grizzly.format.json.JsonTransformationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Safe Navigation Operator Tests")
class SafeNavigationTest {
    
    @Nested
    @DisplayName("Safe Dot Operator (?.)")
    class SafeDotOperatorTests {
        
        @Test
        @DisplayName("?. returns null when intermediate value is null")
        void safeDotReturnsNullWhenIntermediateIsNull() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT?.deal?.loan?.address?.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            
            String input = """
                {"deal": {"loan": null}}
                """;
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"city\" : null");
        }
        
        @Test
        @DisplayName("?. works through entire chain when all values exist")
        void safeDotWorksWhenValuesExist() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT?.deal?.loan?.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            
            String input = """
                {"deal": {"loan": {"city": "New York"}}}
                """;
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"city\" : \"New York\"");
        }
        
        @Test
        @DisplayName("?. handles missing root key")
        void safeDotHandlesMissingRootKey() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["value"] = INPUT?.missing?.key
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            
            String input = "{}";
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"value\" : null");
        }
    }
    
    @Nested
    @DisplayName("Safe Bracket Operator (?[)")
    class SafeBracketOperatorTests {
        
        @Test
        @DisplayName("?[ returns null when object is null")
        void safeBracketReturnsNullWhenObjectIsNull() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["item"] = INPUT?["items"]?[0]
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            
            String input = """
                {"items": null}
                """;
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"item\" : null");
        }
        
        @Test
        @DisplayName("?[ works when all values exist")
        void safeBracketWorksWhenValuesExist() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["item"] = INPUT?["items"]?[0]
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            
            String input = """
                {"items": ["first", "second"]}
                """;
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"item\" : \"first\"");
        }
        
        @Test
        @DisplayName("Mixed ?. and ?[ in same expression")
        void mixedSafeNavigationOperators() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["name"] = INPUT?.customers?[0]?.name
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            
            String input = """
                {"customers": [{"name": "John"}]}
                """;
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"name\" : \"John\"");
        }
    }
    
    @Nested
    @DisplayName("STRICT Mode")
    class StrictModeTests {
        
        @Test
        @DisplayName("STRICT mode throws on null without ?.")
        void strictModeThrowsOnNullWithoutSafeNav() {
            InterpreterConfig config = InterpreterConfig.builder()
                .nullHandling(NullHandling.STRICT)
                .build();
            
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT.deal.loan.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template, config);
            
            String input = """
                {"deal": {"loan": null}}
                """;
            
            assertThatThrownBy(() -> jsonTemplate.transform(input))
                .hasMessageContaining("null");
        }
        
        @Test
        @DisplayName("STRICT mode allows ?. for safe navigation")
        void strictModeAllowsSafeNav() {
            InterpreterConfig config = InterpreterConfig.builder()
                .nullHandling(NullHandling.STRICT)
                .build();
            
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT?.deal?.loan?.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template, config);
            
            String input = """
                {"deal": {"loan": null}}
                """;
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"city\" : null");
        }
    }
    
    @Nested
    @DisplayName("SAFE Mode (Default)")
    class SafeModeTests {
        
        @Test
        @DisplayName("SAFE mode returns null without crashing")
        void safeModeReturnsNullWithoutCrashing() {
            InterpreterConfig config = InterpreterConfig.builder()
                .nullHandling(NullHandling.SAFE)
                .build();
            
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT.deal.loan.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template, config);
            
            String input = """
                {"deal": {"loan": null}}
                """;
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"city\" : null");
        }
        
        @Test
        @DisplayName("SAFE mode tracks broken paths")
        void safeModeTracksBrokenPaths() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT.deal.loan.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            
            String input = """
                {"deal": {"loan": null}}
                """;
            
            JsonTransformationResult result = jsonTemplate.transformWithValidation(input);
            
            assertThat(result.hasPathErrors()).isTrue();
            assertThat(result.validationReport().getPathErrors()).isNotEmpty();
        }
    }
    
    @Nested
    @DisplayName("SILENT Mode")
    class SilentModeTests {
        
        @Test
        @DisplayName("SILENT mode returns null without tracking")
        void silentModeReturnsNullWithoutTracking() {
            InterpreterConfig config = InterpreterConfig.builder()
                .nullHandling(NullHandling.SILENT)
                .build();
            
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT.deal.loan.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template, config);
            
            String input = """
                {"deal": {"loan": null}}
                """;
            
            String output = jsonTemplate.transform(input);
            assertThat(output).contains("\"city\" : null");
        }
    }
}
