package com.grizzly.types;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Represents a datetime value with timezone support.
 * 
 * <p><b>Usage examples:</b>
 * <pre>{@code
 * // Get current time
 * now = now()
 * 
 * // Parse from string
 * date = parseDate("2024-02-22", "yyyy-MM-dd")
 * 
 * // Add/subtract time
 * tomorrow = addDays(now, 1)
 * nextMonth = addMonths(now, 1)
 * 
 * // Format output
 * formatted = formatDate(tomorrow, "dd/MM/yyyy")
 * }</pre>
 * 
 * @author Grizzly Engine
 */
public class DateTimeValue {
    
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
     * Add days to this datetime.
     * 
     * @param days Number of days to add (can be negative)
     * @return New DateTimeValue with days added
     */
    public DateTimeValue addDays(long days) {
        return new DateTimeValue(value.plusDays(days));
    }
    
    /**
     * Add months to this datetime.
     * 
     * @param months Number of months to add (can be negative)
     * @return New DateTimeValue with months added
     */
    public DateTimeValue addMonths(long months) {
        return new DateTimeValue(value.plusMonths(months));
    }
    
    /**
     * Add years to this datetime.
     * 
     * @param years Number of years to add (can be negative)
     * @return New DateTimeValue with years added
     */
    public DateTimeValue addYears(long years) {
        return new DateTimeValue(value.plusYears(years));
    }
    
    /**
     * Add hours to this datetime.
     * 
     * @param hours Number of hours to add (can be negative)
     * @return New DateTimeValue with hours added
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
