package p2pblockchain.utils;

import java.time.Instant;

public class TimeUtils {
    
    /**
     * Get the current timestamp as an Instant.
     * 
     * @return Current Instant
     */
    public static Instant getNowAsInstant() {
        return Instant.now();
    }

    /**
     * Get the current timestamp as a long (milliseconds since epoch).
     * 
     * @return Current timestamp in milliseconds
     */
    public static long getNowAsLong() {
        return getNowAsInstant().toEpochMilli();
    }

    /**
     * Convert an ISO 8601 date string to an Instant.
     * 
     * @param isoDateString ISO 8601 formatted date string
     * @return Corresponding Instant
     */
    public static Instant isoStringToInstant(String isoDateString) {
        return Instant.parse(isoDateString);
    }
    
    /**
     * Convert an ISO 8601 date string to a long timestamp.
     * 
     * @param isoDateString ISO 8601 formatted date string
     * @return Corresponding timestamp in milliseconds
     */
    public static long isoStringToLong(String isoDateString) {
        return isoStringToInstant(isoDateString).toEpochMilli();
    }

    /**
     * Convert (year, month, date, hour, minute, second) to an Instant.
     * 
     * @param year   Year component
     * @param month  Month component (1-12)
     * @param date   Date component (1-31)
     * @param hour   Hour component (0-23)
     * @param minute Minute component (0-59)
     * @param second Second component (0-59)
     * 
     * @return Corresponding Instant
     */
    public static Instant digitsToInstant(int year, int month, int date, int hour, int minute, int second) {
        String isoString = Integer.toString(year) + "-" + Integer.toString(month) + "-" + Integer.toString(date) +
                        "T" + Integer.toString(hour) + ":" + Integer.toString(minute) + ":" + Integer.toString(second) + ".00Z";
        return isoStringToInstant(isoString);
    }

    /**
     * Convert (year, month, date, hour, minute, second) to a long timestamp.
     * 
     * @param year   Year component
     * @param month  Month component (1-12)
     * @param date   Date component (1-31)
     * @param hour   Hour component (0-23)
     * @param minute Minute component (0-59)
     * @param second Second component (0-59)
     * 
     * @return Corresponding timestamp in milliseconds
     */
    public static long digitsToLongTimestamp(int year, int month, int date, int hour, int minute, int second) {
        return digitsToInstant(year, month, date, hour, minute, second).toEpochMilli();
    }

    /**
     * Convert a long timestamp to an Instant.
     * 
     * @param timestamp Timestamp in milliseconds
     * @return Corresponding Instant
     */
    public static Instant longTimestampToInstant(long timestamp) {
        return Instant.ofEpochMilli(timestamp);
    }

    /**
     * Convert an Instant to a long timestamp.
     * 
     * @param thisInstant Input Instant
     * @return Corresponding timestamp in milliseconds
     */
    public static long instantToLongTimestamp(Instant thisInstant) {
        return thisInstant.toEpochMilli();
    }
}