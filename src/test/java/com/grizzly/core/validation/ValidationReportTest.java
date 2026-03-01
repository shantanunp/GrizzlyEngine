package com.grizzly.core.validation;

import com.grizzly.format.json.JsonTemplate;
import com.grizzly.format.json.JsonTransformationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ValidationReport Tests")
class ValidationReportTest {
    
    @Nested
    @DisplayName("Path Errors Detection")
    class PathErrorsTests {
        
        @Test
        @DisplayName("detects broken path and identifies broken segment")
        void detectsBrokenPathAndSegment() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT.deal.loan.address.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            String input = """
                {"deal": {"loan": null}}
                """;
            
            JsonTransformationResult result = jsonTemplate.transformWithValidation(input);
            ValidationReport report = result.validationReport();
            
            assertThat(report.hasPathErrors()).isTrue();
            
            List<AccessRecord> pathErrors = report.getPathErrors();
            assertThat(pathErrors).isNotEmpty();
            
            AccessRecord error = pathErrors.get(0);
            assertThat(error.status()).isEqualTo(AccessStatus.PATH_BROKEN);
            assertThat(error.fullPath()).contains("loan");
        }
        
        @Test
        @DisplayName("reports multiple broken paths")
        void reportsMultipleBrokenPaths() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT.deal.loan.city
                    OUTPUT["state"] = INPUT.deal.loan.state
                    OUTPUT["zip"] = INPUT.deal.loan.zip
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            String input = """
                {"deal": {"loan": null}}
                """;
            
            JsonTransformationResult result = jsonTemplate.transformWithValidation(input);
            ValidationReport report = result.validationReport();
            
            assertThat(report.getPathErrors().size()).isGreaterThanOrEqualTo(3);
        }
    }
    
    @Nested
    @DisplayName("Expected Nulls (via ?.)")
    class ExpectedNullsTests {
        
        @Test
        @DisplayName("marks ?. accesses as expected nulls")
        void marksSafeNavigationAsExpectedNull() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["city"] = INPUT?.deal?.loan?.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            String input = """
                {"deal": {"loan": null}}
                """;
            
            JsonTransformationResult result = jsonTemplate.transformWithValidation(input);
            ValidationReport report = result.validationReport();
            
            assertThat(report.hasPathErrors()).isFalse();
            assertThat(report.getExpectedNulls()).isNotEmpty();
            
            AccessRecord expectedNull = report.getExpectedNulls().get(0);
            assertThat(expectedNull.status()).isEqualTo(AccessStatus.EXPECTED_NULL);
            assertThat(expectedNull.expectedNull()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Successful Accesses")
    class SuccessfulAccessesTests {
        
        @Test
        @DisplayName("tracks successful accesses")
        void tracksSuccessfulAccesses() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["name"] = INPUT.customer.name
                    OUTPUT["age"] = INPUT.customer.age
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            String input = """
                {"customer": {"name": "John", "age": 30}}
                """;
            
            JsonTransformationResult result = jsonTemplate.transformWithValidation(input);
            ValidationReport report = result.validationReport();
            
            assertThat(report.isClean()).isTrue();
            assertThat(report.hasAnyErrors()).isFalse();
            assertThat(report.getSuccessful()).isNotEmpty();
        }
    }
    
    @Nested
    @DisplayName("Report Summary")
    class ReportSummaryTests {
        
        @Test
        @DisplayName("provides accurate summary counts")
        void providesAccurateSummaryCounts() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["name"] = INPUT.customer.name
                    OUTPUT["city"] = INPUT.customer.address.city
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            String input = """
                {"customer": {"name": "John", "address": null}}
                """;
            
            JsonTransformationResult result = jsonTemplate.transformWithValidation(input);
            ValidationReport report = result.validationReport();
            
            Map<String, Integer> summary = report.getSummary();
            
            assertThat(summary.get("total")).isGreaterThan(0);
            assertThat(summary.get("pathErrors")).isGreaterThanOrEqualTo(1);
        }
        
        @Test
        @DisplayName("toJson produces valid JSON")
        void toJsonProducesValidJson() {
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
            String json = result.validationReport().toJson();
            
            assertThat(json).contains("\"summary\"");
            assertThat(json).contains("\"pathErrors\"");
            assertThat(json).startsWith("{");
            assertThat(json).endsWith("}");
        }
    }
    
    @Nested
    @DisplayName("Grouping")
    class GroupingTests {
        
        @Test
        @DisplayName("groups errors by broken segment")
        void groupsErrorsByBrokenSegment() {
            String template = """
                def transform(INPUT):
                    OUTPUT = {}
                    OUTPUT["a"] = INPUT.deal.loan.a
                    OUTPUT["b"] = INPUT.deal.loan.b
                    OUTPUT["c"] = INPUT.other.c
                    return OUTPUT
                """;
            
            JsonTemplate jsonTemplate = JsonTemplate.compile(template);
            String input = """
                {"deal": {"loan": null}, "other": null}
                """;
            
            JsonTransformationResult result = jsonTemplate.transformWithValidation(input);
            ValidationReport report = result.validationReport();
            
            Map<String, List<AccessRecord>> grouped = report.groupByBrokenSegment();
            
            assertThat(grouped).isNotEmpty();
        }
    }
}
