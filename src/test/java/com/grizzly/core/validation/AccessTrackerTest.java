package com.grizzly.core.validation;

import com.grizzly.core.types.NullValue;
import com.grizzly.core.types.StringValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AccessTracker Tests")
class AccessTrackerTest {
    
    private AccessTracker tracker;
    
    @BeforeEach
    void setUp() {
        tracker = new AccessTracker(true);
    }
    
    @Nested
    @DisplayName("Record Methods")
    class RecordMethodsTests {
        
        @Test
        @DisplayName("recordSuccess creates SUCCESS record")
        void recordSuccessCreatesSuccessRecord() {
            tracker.recordSuccess("INPUT.name", new StringValue("John"), 5);
            
            assertThat(tracker.size()).isEqualTo(1);
            
            AccessRecord record = tracker.getRecords().get(0);
            assertThat(record.status()).isEqualTo(AccessStatus.SUCCESS);
            assertThat(record.fullPath()).isEqualTo("INPUT.name");
            assertThat(record.lineNumber()).isEqualTo(5);
        }
        
        @Test
        @DisplayName("recordSuccess with NullValue creates VALUE_NULL record")
        void recordSuccessWithNullValueCreatesValueNullRecord() {
            tracker.recordSuccess("INPUT.middleName", NullValue.INSTANCE, 6);
            
            AccessRecord record = tracker.getRecords().get(0);
            assertThat(record.status()).isEqualTo(AccessStatus.VALUE_NULL);
        }
        
        @Test
        @DisplayName("recordPathBroken creates PATH_BROKEN record")
        void recordPathBrokenCreatesPathBrokenRecord() {
            tracker.recordPathBroken("INPUT.deal.loan.city", "loan", 10, false);
            
            AccessRecord record = tracker.getRecords().get(0);
            assertThat(record.status()).isEqualTo(AccessStatus.PATH_BROKEN);
            assertThat(record.brokenAtSegment()).isEqualTo("loan");
            assertThat(record.expectedNull()).isFalse();
        }
        
        @Test
        @DisplayName("recordPathBroken with expectedNull creates EXPECTED_NULL record")
        void recordPathBrokenWithExpectedNullCreatesExpectedNullRecord() {
            tracker.recordPathBroken("INPUT?.deal?.loan?.city", "loan", 10, true);
            
            AccessRecord record = tracker.getRecords().get(0);
            assertThat(record.status()).isEqualTo(AccessStatus.EXPECTED_NULL);
            assertThat(record.expectedNull()).isTrue();
        }
        
        @Test
        @DisplayName("recordKeyNotFound creates KEY_NOT_FOUND record")
        void recordKeyNotFoundCreatesKeyNotFoundRecord() {
            tracker.recordKeyNotFound("INPUT.missing", "missing", 15, false);
            
            AccessRecord record = tracker.getRecords().get(0);
            assertThat(record.status()).isEqualTo(AccessStatus.KEY_NOT_FOUND);
            assertThat(record.brokenAtSegment()).isEqualTo("missing");
        }
        
        @Test
        @DisplayName("recordIndexOutOfBounds creates INDEX_OUT_OF_BOUNDS record")
        void recordIndexOutOfBoundsCreatesIndexOutOfBoundsRecord() {
            tracker.recordIndexOutOfBounds("INPUT.items[10]", 10, 3, 20, false);
            
            AccessRecord record = tracker.getRecords().get(0);
            assertThat(record.status()).isEqualTo(AccessStatus.INDEX_OUT_OF_BOUNDS);
        }
    }
    
    @Nested
    @DisplayName("Disabled Tracker")
    class DisabledTrackerTests {
        
        @Test
        @DisplayName("disabled tracker does not record")
        void disabledTrackerDoesNotRecord() {
            AccessTracker disabled = AccessTracker.disabled();
            
            disabled.recordSuccess("INPUT.name", new StringValue("John"), 5);
            disabled.recordPathBroken("INPUT.x", "x", 10, false);
            
            assertThat(disabled.size()).isEqualTo(0);
            assertThat(disabled.isEnabled()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Report Generation")
    class ReportGenerationTests {
        
        @Test
        @DisplayName("generateReport creates ValidationReport")
        void generateReportCreatesValidationReport() {
            tracker.recordSuccess("INPUT.a", new StringValue("A"), 1);
            tracker.recordPathBroken("INPUT.b.c", "b", 2, false);
            tracker.recordPathBroken("INPUT.d.e", "d", 3, true);
            
            ValidationReport report = tracker.generateReport();
            
            assertThat(report).isNotNull();
            assertThat(report.getSuccessful()).hasSize(1);
            assertThat(report.getPathErrors()).hasSize(1);
            assertThat(report.getExpectedNulls()).hasSize(1);
        }
    }
    
    @Nested
    @DisplayName("Clear and Size")
    class ClearAndSizeTests {
        
        @Test
        @DisplayName("clear removes all records")
        void clearRemovesAllRecords() {
            tracker.recordSuccess("INPUT.a", new StringValue("A"), 1);
            tracker.recordSuccess("INPUT.b", new StringValue("B"), 2);
            
            assertThat(tracker.size()).isEqualTo(2);
            
            tracker.clear();
            
            assertThat(tracker.size()).isEqualTo(0);
        }
    }
}
