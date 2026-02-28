package com.grizzly.types;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * A date and time value with timezone.
 * 
 * <p>Stores a specific moment in time (like "February 22, 2024 at 2:30 PM in New York").
 * 
 * <p><b>How to create:</b>
 * <pre>{@code
 * // Current time
 * now = now()
 * 
 * // From a string
 * date = parseDate("2024-02-22", "yyyy-MM-dd")
 * date = parseDate("22/02/2024", "dd/MM/yyyy")
 * 
 * // With timezone
 * utcTime = now("UTC")
 * nyTime = now("America/New_York")
 * }</pre>
 * 
 * <p><b>What you can do:</b>
 * <pre>{@code
 * // Add or subtract time
 * tomorrow = addDays(today, 1)
 * nextWeek = addDays(today, 7)
 * lastMonth = addMonths(today, -1)
 * 
 * // Format as string
 * formatted = formatDate(date, "dd/MM/yyyy")  // "22/02/2024"
 * formatted = formatDate(date, "yyyyMMdd")    // "20240222"
 * }</pre>
 */
public final class DateTimeValue implements Value {
    
    private final ZonedDateTime value;
    
    /**
     * Create a datetime value.
     * 
     * @param value The ZonedDateTime value
     */
    public DateTimeValue(ZonedDateTime value) {
        this.value = value;
    }
    
    /**
     * Get the underlying ZonedDateTime.
     * 
     * @return The datetime value
     */
    public ZonedDateTime getValue() {
        return value;
    }
    
    /**
     * Add days to this date.
     * 
     * <p>Use positive numbers to go forward, negative to go back.
     * <pre>{@code
     * tomorrow = date.addDays(1)
     * yesterday = date.addDays(-1)
     * nextWeek = date.addDays(7)
     * }</pre>
     * 
     * @param days How many days to add (negative = subtract)
     * @return New date with days added
     */
    public DateTimeValue addDays(long days) {
        return new DateTimeValue(value.plusDays(days));
    }
    
    /**
     * Add months to this date.
     * 
     * <p>Use positive to go forward, negative to go back.
     * <pre>{@code
     * nextMonth = date.addMonths(1)
     * lastYear = date.addMonths(-12)
     * }</pre>
     * 
     * @param months How many months to add (negative = subtract)
     * @return New date with months added
     */
    public DateTimeValue addMonths(long months) {
        return new DateTimeValue(value.plusMonths(months));
    }
    
    /**
     * Add years to this date.
     * 
     * <pre>{@code
     * nextYear = date.addYears(1)
     * tenYearsAgo = date.addYears(-10)
     * }</pre>
     * 
     * @param years How many years to add (negative = subtract)
     * @return New date with years added
     */
    public DateTimeValue addYears(long years) {
        return new DateTimeValue(value.plusYears(years));
    }
    
    /**
     * Add hours to this time.
     * 
     * <pre>{@code
     * twoHoursLater = time.addHours(2)
     * threeHoursEarlier = time.addHours(-3)
     * }</pre>
     * 
     * @param hours How many hours to add (negative = subtract)
     * @return New time with hours added
     */
    public DateTimeValue addHours(long hours) {
        return new DateTimeValue(value.plusHours(hours));
    }
    
    /**
     * Add minutes to this datetime.
     * 
     * @param minutes Number of minutes to add (can be negative)
     * @return New DateTimeValue with minutes added
     */
    public DateTimeValue addMinutes(long minutes) {
        return new DateTimeValue(value.plusMinutes(minutes));
    }
    
    /**
     * Format this datetime using a pattern.
     * 
     * <p><b>Common patterns:</b>
     * <ul>
     *   <li>yyyy-MM-dd → 2024-02-22</li>
     *   <li>dd/MM/yyyy → 22/02/2024</li>
     *   <li>yyyyMMdd → 20240222</li>
     *   <li>MM/dd/yyyy HH:mm:ss → 02/22/2024 14:30:00</li>
     * </ul>
     * 
     * @param pattern The format pattern
     * @return Formatted datetime string
     */
    public String format(String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return value.format(formatter);
    }
    
    /**
     * Convert to a different timezone.
     * 
     * @param timezone Target timezone (e.g., "America/New_York", "UTC", "Asia/Tokyo")
     * @return New DateTimeValue in target timezone
     */
    public DateTimeValue withTimezone(String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        return new DateTimeValue(value.withZoneSameInstant(zone));
    }
    
    /**
     * Get the year.
     * 
     * @return Year value
     */
    public int getYear() {
        return value.getYear();
    }
    
    /**
     * Get the month (1-12).
     * 
     * @return Month value
     */
    public int getMonth() {
        return value.getMonthValue();
    }
    
    /**
     * Get the day of month (1-31).
     * 
     * @return Day value
     */
    public int getDay() {
        return value.getDayOfMonth();
    }
    
    /**
     * Get the hour (0-23).
     * 
     * @return Hour value
     */
    public int getHour() {
        return value.getHour();
    }
    
    /**
     * Get the minute (0-59).
     * 
     * @return Minute value
     */
    public int getMinute() {
        return value.getMinute();
    }
    
    /**
     * Get the second (0-59).
     * 
     * @return Second value
     */
    public int getSecond() {
        return value.getSecond();
    }
    
    /**
     * Calculate days between this and another datetime.
     * 
     * @param other The other datetime
     * @return Number of days between (can be negative)
     */
    public long daysBetween(DateTimeValue other) {
        return ChronoUnit.DAYS.between(this.value, other.value);
    }
    
    @Override
    public String typeName() {
        return "datetime";
    }
    
    @Override
    public boolean isTruthy() {
        return true;
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DateTimeValue)) return false;
        DateTimeValue other = (DateTimeValue) obj;
        return value.equals(other.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
