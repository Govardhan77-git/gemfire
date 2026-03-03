package com.gemfire.demo.util;

/**
 * Utility class for null-safe operations on primitive wrapper types.
 *
 * <p>Centralizes the strategy for handling null Integer and Boolean values
 * sent in user payloads, following the DRY principle.
 *
 * <p>Rationale: When clients omit or explicitly null a field, Java's auto-unboxing
 * would throw a NullPointerException if we try to assign to a primitive.
 * These helpers provide a safe fallback without scattering ternary operators
 * throughout the codebase.
 */
public final class NullSafeUtil {

    private NullSafeUtil() {
        // Utility class — no instances
    }

    /**
     * Returns the value of a nullable Integer, or a default if null/empty.
     *
     * <pre>
     *   defaultInt(null, 0)  → 0
     *   defaultInt(5, 0)     → 5
     *   defaultInt(-1, 0)    → -1  (negative is preserved, not treated as empty)
     * </pre>
     *
     * @param value    the nullable Integer from user payload
     * @param fallback the default value to use when value is null
     * @return the resolved int value
     */
    public static int defaultInt(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    /**
     * Returns the value of a nullable Boolean, or a default if null.
     *
     * <pre>
     *   defaultBool(null, false)  → false
     *   defaultBool(true, false)  → true
     *   defaultBool(false, true)  → false  (explicit false is preserved)
     * </pre>
     *
     * @param value    the nullable Boolean from user payload
     * @param fallback the default value to use when value is null
     * @return the resolved boolean value
     */
    public static boolean defaultBool(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }

    /**
     * Returns 0 for null or negative integers (e.g., stock count cannot be negative).
     *
     * @param value the nullable Integer
     * @return max(0, value) or 0 if null
     */
    public static int nonNegativeInt(Integer value) {
        return value != null ? Math.max(0, value) : 0;
    }

    /**
     * Checks if a String is null or blank (empty/whitespace-only).
     *
     * @param value the string to check
     * @return true if null or blank
     */
    public static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }
}
