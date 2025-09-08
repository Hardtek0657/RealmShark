package tomato.gui.stats.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for common formatting operations used across stats GUI components.
 * Consolidates number formatting, time formatting, and string building patterns.
 */
public class FormatUtils {
    
    private static final DateTimeFormatter TIME_SHORT_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_FULL_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
    private static final SimpleDateFormat SESSION_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Formats a number with appropriate precision - integers without decimals, decimals with full precision.
     */
    public static String formatNumber(double number) {
        if (number == (long) number) {
            // Integer value - display without decimals
            return String.format("%d", (long) number);
        } else {
            // Decimal value - display with full precision
            return String.valueOf(number);
        }
    }
    
    /**
     * Formats fame per hour values with 2 decimal places for readability.
     */
    public static String formatFamePerHour(double famePerHour) {
        return String.format("%.2f", famePerHour);
    }
    
    /**
     * Formats a time value with 1 decimal place.
     */
    public static String formatTimeValue(double value) {
        return String.format("%.1f", value);
    }
    
    /**
     * Gets current time in short format (HH:mm:ss).
     */
    public static String getTimeShort() {
        LocalDateTime dateTime = LocalDateTime.now();
        return TIME_SHORT_FORMAT.format(dateTime);
    }
    
    /**
     * Gets current time in full format (yyyy/MM/dd-HH:mm:ss).
     */
    public static String getTimeFull() {
        LocalDateTime dateTime = LocalDateTime.now();
        return TIME_FULL_FORMAT.format(dateTime);
    }
    
    /**
     * Formats a timestamp for session display.
     */
    public static String formatSessionTime(long timestamp) {
        return SESSION_TIME_FORMAT.format(new java.util.Date(timestamp));
    }
    
    /**
     * Formats time duration in milliseconds to a readable format.
     */
    public static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else {
            return String.format("%02d:%02d", minutes, seconds % 60);
        }
    }
    
    /**
     * Formats time span for display (e.g., "2.5 min", "1.2 h").
     */
    public static String formatTimeSpan(long timeMs) {
        double minutes = timeMs / 60000.0;
        if (minutes < 60) {
            return String.format("%.1f min", minutes);
        } else {
            double hours = minutes / 60.0;
            return String.format("%.1f h", hours);
        }
    }
}