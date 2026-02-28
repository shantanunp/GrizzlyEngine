package com.grizzly.types;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A decimal number with exact precision for financial calculations.
 * 
 * <p>Avoids the precision errors that come with regular floating point numbers.
 * 
 * <p><b>The Problem with Regular Numbers:</b>
 * <pre>{@code
 * // Regular float/double has precision errors!
 * double result = 0.1 + 0.2;  // 0.30000000000000004 ❌
 * 
 * // Three times 0.30 should be 0.90
 * double amount = 0.30 * 3;   // 0.8999999999999999 ❌
 * }</pre>
 * 
 * <p><b>The Solution - DecimalValue:</b>
 * <pre>{@code
 * // Exact precision!
 * DecimalValue result = new DecimalValue("0.1").add(new DecimalValue("0.2"));
 * // result = 0.3 ✓
 * 
 * // Money calculations work correctly
 * DecimalValue amount = new DecimalValue("0.30").multiply(new DecimalValue("3"));
 * // amount = 0.90 ✓
 * }</pre>
 * 
 * <p><b>Usage in templates:</b>
 * <pre>{@code
 * // Create from string (always use strings!)
 * amount = Decimal("1234.56")
 * 
 * // Math operations
 * total = principal + interest
 * remaining = balance - withdrawal
 * interest = principal * rate
 * payment = total / months
 * 
 * // Round to 2 decimal places
 * rounded = round(amount, 2)
 * }</pre>
 */
public class DecimalValue {
    
    private final BigDecimal value;
    
    /**
     * Create a decimal from a string.
     * 
     * <p><b>Always use strings to create DecimalValue!</b>
     * 
     * <pre>{@code
     * // GOOD - exact precision
     * DecimalValue amount = new DecimalValue("1234.56");
     * 
     * // BAD - defeats the purpose (still uses double internally)
     * DecimalValue bad = new DecimalValue(1234.56);  // Don't do this!
     * }</pre>
     * 
     * @param value String representation of the decimal number
     */
    public DecimalValue(String value) {
        this.value = new BigDecimal(value);
    }
    
    /**
     * Create a decimal from a BigDecimal.
     * 
     * @param value The BigDecimal value
     */
    public DecimalValue(BigDecimal value) {
        this.value = value;
    }
    
    /**
     * Create a decimal from an integer.
     * 
     * <p>Safe because integers have exact representation.
     * 
     * @param value The integer value
     */
    public DecimalValue(int value) {
        this.value = BigDecimal.valueOf(value);
    }
    
    /**
     * Get the underlying BigDecimal.
     * 
     * @return The decimal value as BigDecimal
     */
    public BigDecimal getValue() {
        return value;
    }
    
    /**
     * Add another decimal to this one.
     * 
     * <pre>{@code
     * DecimalValue principal = new DecimalValue("10000.00");
     * DecimalValue interest = new DecimalValue("250.50");
     * DecimalValue total = principal.add(interest);  // 10250.50
     * }</pre>
     * 
     * @param other The decimal to add
     * @return New decimal with the sum
     */
    public DecimalValue add(DecimalValue other) {
        return new DecimalValue(this.value.add(other.value));
    }
    
    /**
     * Subtract another decimal from this one.
     * 
     * <pre>{@code
     * DecimalValue balance = new DecimalValue("5000.00");
     * DecimalValue withdrawal = new DecimalValue("750.25");
     * DecimalValue remaining = balance.subtract(withdrawal);  // 4249.75
     * }</pre>
     * 
     * @param other The decimal to subtract
     * @return New decimal with the difference
     */
    public DecimalValue subtract(DecimalValue other) {
        return new DecimalValue(this.value.subtract(other.value));
    }
    
    /**
     * Multiply this decimal by another.
     * 
     * <pre>{@code
     * DecimalValue principal = new DecimalValue("10000.00");
     * DecimalValue rate = new DecimalValue("0.05");  // 5%
     * DecimalValue interest = principal.multiply(rate);  // 500.00
     * }</pre>
     * 
     * @param other The decimal to multiply by
     * @return New decimal with the product
     */
    public DecimalValue multiply(DecimalValue other) {
        return new DecimalValue(this.value.multiply(other.value));
    }
    
    /**
     * Divide this decimal by another.
     * 
     * <p>Uses 10 decimal places precision by default.
     * 
     * <pre>{@code
     * DecimalValue total = new DecimalValue("12000.00");
     * DecimalValue months = new DecimalValue("12");
     * DecimalValue monthly = total.divide(months);  // 1000.00
     * }</pre>
     * 
     * @param other The decimal to divide by
     * @return New decimal with the quotient
     * @throws ArithmeticException if dividing by zero
     */
    public DecimalValue divide(DecimalValue other) {
        return new DecimalValue(this.value.divide(other.value, 10, RoundingMode.HALF_UP));
    }
    
    /**
     * Round this decimal to a specific number of decimal places.
     * 
     * <p>Uses banker's rounding (HALF_UP): .5 rounds up.
     * 
     * <pre>{@code
     * DecimalValue amount = new DecimalValue("1234.5678");
     * 
     * DecimalValue cents = amount.round(2);    // 1234.57
     * DecimalValue dollars = amount.round(0);  // 1235
     * DecimalValue precise = amount.round(4);  // 1234.5678
     * }</pre>
     * 
     * @param decimalPlaces Number of decimal places to keep
     * @return New decimal rounded to specified places
     */
    public DecimalValue round(int decimalPlaces) {
        return new DecimalValue(this.value.setScale(decimalPlaces, RoundingMode.HALF_UP));
    }
    
    /**
     * Compare this decimal to another for equality.
     * 
     * <p>Uses value comparison, ignoring scale differences.
     * So 2.0 equals 2.00.
     * 
     * @param obj Object to compare with
     * @return true if values are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DecimalValue)) return false;
        DecimalValue other = (DecimalValue) obj;
        return this.value.compareTo(other.value) == 0;
    }
    
    /**
     * Hash code based on value.
     * 
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }
    
    /**
     * Compare this decimal to another.
     * 
     * <pre>{@code
     * DecimalValue balance = new DecimalValue("5000.00");
     * DecimalValue threshold = new DecimalValue("1000.00");
     * 
     * int result = balance.compareTo(threshold);
     * // result > 0 (balance is greater)
     * }</pre>
     * 
     * @param other Decimal to compare with
     * @return -1 if less, 0 if equal, 1 if greater
     */
    public int compareTo(DecimalValue other) {
        return this.value.compareTo(other.value);
    }
    
    /**
     * Check if this decimal is greater than another.
     * 
     * @param other Decimal to compare with
     * @return true if this > other
     */
    public boolean greaterThan(DecimalValue other) {
        return this.value.compareTo(other.value) > 0;
    }
    
    /**
     * Check if this decimal is less than another.
     * 
     * @param other Decimal to compare with
     * @return true if this < other
     */
    public boolean lessThan(DecimalValue other) {
        return this.value.compareTo(other.value) < 0;
    }
    
    /**
     * Check if this decimal is greater than or equal to another.
     * 
     * @param other Decimal to compare with
     * @return true if this >= other
     */
    public boolean greaterThanOrEqual(DecimalValue other) {
        return this.value.compareTo(other.value) >= 0;
    }
    
    /**
     * Check if this decimal is less than or equal to another.
     * 
     * @param other Decimal to compare with
     * @return true if this <= other
     */
    public boolean lessThanOrEqual(DecimalValue other) {
        return this.value.compareTo(other.value) <= 0;
    }
    
    /**
     * Convert to string representation.
     * 
     * <p>Use this when outputting to JSON.
     * 
     * <pre>{@code
     * DecimalValue amount = new DecimalValue("1234.56");
     * String output = amount.toString();  // "1234.56"
     * }</pre>
     * 
     * @return String representation of the decimal
     */
    @Override
    public String toString() {
        return value.toPlainString();
    }
    
    /**
     * Convert to double (for cases where exact precision isn't needed).
     * 
     * <p><b>Warning:</b> This may lose precision!
     * 
     * @return Double representation (may have precision loss)
     */
    public double toDouble() {
        return value.doubleValue();
    }
    
    /**
     * Convert to integer (truncates decimal part).
     * 
     * @return Integer representation (decimal part removed)
     */
    public int toInt() {
        return value.intValue();
    }
}
