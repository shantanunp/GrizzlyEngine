package com.grizzly;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Decimal (exact precision math) and Regex (pattern matching) features.
 * 
 * <p>These are critical for banking applications where float precision errors
 * are unacceptable and text validation is essential.
 */
@DisplayName("Decimal and Regex Tests")
public class DecimalAndRegexTest {
    
    private final GrizzlyEngine engine = new GrizzlyEngine();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DECIMAL TESTS - Exact Precision Money Calculations
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Decimal creation from string")
    public void testDecimalCreation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                amount = Decimal("1234.56")
                OUTPUT["amount"] = str(amount)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertEquals("1234.56", result.get("amount"));
    }
    
    @Test
    @DisplayName("Decimal addition - no precision loss")
    public void testDecimalAddition() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                # The classic float problem: 0.1 + 0.2 != 0.3
                # With Decimal, it works correctly!
                a = Decimal("0.1")
                b = Decimal("0.2")
                total = a + b
                OUTPUT["result"] = str(total)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        // With floats this would be "0.30000000000000004"
        // With Decimal it's exact
        assertEquals("0.3", result.get("result"));
    }
    
    @Test
    @DisplayName("Decimal multiplication - exact precision")
    public void testDecimalMultiplication() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                # Three times $0.30 should be exactly $0.90
                amount = Decimal("0.30")
                count = Decimal("3")
                total = amount * count
                OUTPUT["result"] = str(total)
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        // With floats: 0.30 * 3 = 0.8999999999999999
        // With Decimal: exact 0.90
        assertEquals("0.90", result.get("result"));
    }
    
    @Test
    @DisplayName("Decimal interest calculation")
    public void testDecimalInterestCalculation() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                principal = Decimal(INPUT.amount)
                rate = Decimal("0.05")
                years = Decimal("5")
                
                interest = principal * rate * years
                total = principal + interest
                
                OUTPUT["principal"] = str(principal)
                OUTPUT["interest"] = str(round(interest, 2))
                OUTPUT["total"] = str(round(total, 2))
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> input = Map.of("amount", "10000.00");
        Map<String, Object> result = compiled.executeRaw(input);
        
        assertEquals("10000.00", result.get("principal"));
        assertEquals("2500.00", result.get("interest"));
        assertEquals("12500.00", result.get("total"));
    }
    
    @Test
    @DisplayName("Decimal division with rounding")
    public void testDecimalDivision() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                total = Decimal("12000.00")
                months = Decimal("12")
                monthly = total / months
                OUTPUT["monthly"] = str(round(monthly, 2))
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertEquals("1000.00", result.get("monthly"));
    }
    
    @Test
    @DisplayName("Decimal comparison in if statements")
    public void testDecimalComparison() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                balance = Decimal(INPUT.balance)
                threshold = Decimal("1000.00")
                
                if balance >= threshold:
                    OUTPUT["tier"] = "premium"
                else:
                    OUTPUT["tier"] = "standard"
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        // Test premium tier
        Map<String, Object> result1 = compiled.executeRaw(Map.of("balance", "5000.00"));
        assertEquals("premium", result1.get("tier"));
        
        // Test standard tier
        Map<String, Object> result2 = compiled.executeRaw(Map.of("balance", "500.00"));
        assertEquals("standard", result2.get("tier"));
    }
    
    @Test
    @DisplayName("Decimal rounding to different precision")
    public void testDecimalRounding() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                amount = Decimal("1234.5678")
                
                OUTPUT["cents"] = str(round(amount, 2))
                OUTPUT["dollars"] = str(round(amount, 0))
                OUTPUT["precise"] = str(round(amount, 4))
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of());
        
        assertEquals("1234.57", result.get("cents"));
        assertEquals("1235", result.get("dollars"));
        assertEquals("1234.5678", result.get("precise"));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REGEX TESTS - Pattern Matching and Validation
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Regex match - SSN validation")
    public void testRegexMatchSSN() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                if re_match(r"^\\d{3}-\\d{2}-\\d{4}$", INPUT.ssn):
                    OUTPUT["validSSN"] = true
                else:
                    OUTPUT["validSSN"] = false
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        // Valid SSN
        Map<String, Object> result1 = compiled.executeRaw(Map.of("ssn", "123-45-6789"));
        assertEquals(true, result1.get("validSSN"));
        
        // Invalid SSN
        Map<String, Object> result2 = compiled.executeRaw(Map.of("ssn", "12-345-6789"));
        assertEquals(false, result2.get("validSSN"));
    }
    
    @Test
    @DisplayName("Regex match - Email validation")
    public void testRegexMatchEmail() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                if re_match(r"^[\\w\\.-]+@[\\w\\.-]+\\.[\\w]+$", INPUT.email):
                    OUTPUT["validEmail"] = true
                else:
                    OUTPUT["validEmail"] = false
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        
        // Valid email
        Map<String, Object> result1 = compiled.executeRaw(Map.of("email", "user@example.com"));
        assertEquals(true, result1.get("validEmail"));
        
        // Invalid email
        Map<String, Object> result2 = compiled.executeRaw(Map.of("email", "invalid@"));
        assertEquals(false, result2.get("validEmail"));
    }
    
    @Test
    @DisplayName("Regex sub - Remove dashes from SSN")
    public void testRegexSubRemoveDashes() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                cleanSSN = re_sub("-", "", INPUT.ssn)
                OUTPUT["ssn"] = cleanSSN
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of("ssn", "123-45-6789"));
        
        assertEquals("123456789", result.get("ssn"));
    }
    
    @Test
    @DisplayName("Regex sub - Remove all non-digits")
    public void testRegexSubDigitsOnly() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                digitsOnly = re_sub(r"\\D", "", INPUT.phone)
                OUTPUT["phone"] = digitsOnly
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> result = compiled.executeRaw(Map.of("phone", "(555) 123-4567"));
        
        assertEquals("5551234567", result.get("phone"));
    }
    
    @Test
    @DisplayName("Regex findall - Extract all emails")
    public void testRegexFindall() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                emails = re_findall(r"[\\w\\.-]+@[\\w\\.-]+\\.[\\w]+", INPUT.text)
                OUTPUT["emails"] = emails
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> input = Map.of(
            "text", "Contact: john@example.com or jane@test.org for info"
        );
        Map<String, Object> result = compiled.executeRaw(input);
        
        @SuppressWarnings("unchecked")
        List<String> emails = (List<String>) result.get("emails");
        assertEquals(2, emails.size());
        assertTrue(emails.contains("john@example.com"));
        assertTrue(emails.contains("jane@test.org"));
    }
    
    @Test
    @DisplayName("Regex split - Parse CSV")
    public void testRegexSplit() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                parts = re_split(",", INPUT.csv)
                OUTPUT["firstName"] = parts[0]
                OUTPUT["lastName"] = parts[1]
                OUTPUT["email"] = parts[2]
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> input = Map.of("csv", "John,Doe,john@example.com");
        Map<String, Object> result = compiled.executeRaw(input);
        
        assertEquals("John", result.get("firstName"));
        assertEquals("Doe", result.get("lastName"));
        assertEquals("john@example.com", result.get("email"));
    }
    
    @Test
    @DisplayName("Regex in for loop - Filter valid transactions")
    public void testRegexInForLoop() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                OUTPUT["validTransactions"] = []
                
                for txn in INPUT.transactions:
                    if re_match(r"^TXN-\\d{6}$", txn.id):
                        OUTPUT["validTransactions"].append(txn.id)
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> input = Map.of(
            "transactions", List.of(
                Map.of("id", "TXN-123456"),
                Map.of("id", "INVALID"),
                Map.of("id", "TXN-789012"),
                Map.of("id", "TXN-12")
            )
        );
        Map<String, Object> result = compiled.executeRaw(input);
        
        @SuppressWarnings("unchecked")
        List<String> valid = (List<String>) result.get("validTransactions");
        assertEquals(2, valid.size());
        assertEquals("TXN-123456", valid.get(0));
        assertEquals("TXN-789012", valid.get(1));
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMBINED TESTS - Decimal + Regex Together
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @DisplayName("Combined: Validate SSN and calculate loan")
    public void testDecimalAndRegexCombined() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                
                # Validate SSN
                if re_match(r"^\\d{3}-\\d{2}-\\d{4}$", INPUT.ssn):
                    OUTPUT["validSSN"] = true
                    OUTPUT["ssn"] = re_sub("-", "", INPUT.ssn)
                else:
                    OUTPUT["validSSN"] = false
                    OUTPUT["error"] = "Invalid SSN"
                    return OUTPUT
                
                # Calculate loan with exact precision
                principal = Decimal(INPUT.amount)
                rate = Decimal("0.05")
                years = Decimal(INPUT.years)
                
                interest = principal * rate * years
                total = principal + interest
                
                OUTPUT["principal"] = str(round(principal, 2))
                OUTPUT["interest"] = str(round(interest, 2))
                OUTPUT["total"] = str(round(total, 2))
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> input = Map.of(
            "ssn", "123-45-6789",
            "amount", "10000.00",
            "years", "5"
        );
        Map<String, Object> result = compiled.executeRaw(input);
        
        // Validate SSN processing
        assertEquals(true, result.get("validSSN"));
        assertEquals("123456789", result.get("ssn"));
        
        // Validate exact decimal calculations
        assertEquals("10000.00", result.get("principal"));
        assertEquals("2500.00", result.get("interest"));
        assertEquals("12500.00", result.get("total"));
    }
    
    @Test
    @DisplayName("Combined: Clean phone and calculate balance")
    public void testRegexCleanAndDecimalMath() {
        String template = """
            def transform(INPUT):
                OUTPUT = {}
                
                # Clean phone number
                cleanPhone = re_sub(r"\\D", "", INPUT.phone)
                OUTPUT["phone"] = cleanPhone
                
                # Calculate exact balance
                deposits = Decimal(INPUT.deposits)
                withdrawals = Decimal(INPUT.withdrawals)
                balance = deposits - withdrawals
                
                OUTPUT["balance"] = str(round(balance, 2))
                
                # Determine tier based on balance
                if balance >= Decimal("100000.00"):
                    OUTPUT["tier"] = "platinum"
                elif balance >= Decimal("50000.00"):
                    OUTPUT["tier"] = "gold"
                else:
                    OUTPUT["tier"] = "standard"
                
                return OUTPUT
            """;
        
        GrizzlyTemplate compiled = engine.compileFromString(template);
        Map<String, Object> input = Map.of(
            "phone", "(555) 123-4567",
            "deposits", "75000.50",
            "withdrawals", "10000.25"
        );
        Map<String, Object> result = compiled.executeRaw(input);
        
        assertEquals("5551234567", result.get("phone"));
        assertEquals("65000.25", result.get("balance"));
        assertEquals("gold", result.get("tier"));
    }
}
