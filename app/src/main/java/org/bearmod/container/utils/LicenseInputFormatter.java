package org.bearmod.container.utils;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;
import java.util.regex.Pattern;

/**
 * Enhanced License Input Formatter for KeyAuth license keys
 * Provides real-time formatting, validation, and auto-correction
 */
public class LicenseInputFormatter {
    
    private static final String TAG = "LicenseInputFormatter";
    
    // Simple license validation - accept any reasonable input
    private static final int MAX_LICENSE_LENGTH = 200; // Very generous limit
    
    private EditText editText;
    private boolean isFormatting = false;
    private LicenseValidationListener validationListener;
    
    public interface LicenseValidationListener {
        void onValidLicense(String license);
        void onInvalidLicense(String license);
        void onLicenseCleared();
    }
    
    public LicenseInputFormatter(EditText editText) {
        this.editText = editText;
        // Only setup TextWatcher if EditText is not null (for testing purposes)
        if (editText != null) {
            setupTextWatcher();
        }
    }
    
    public void setValidationListener(LicenseValidationListener listener) {
        this.validationListener = listener;
    }
    
    /**
     * Setup text watcher for real-time formatting and validation
     */
    private void setupTextWatcher() {
        if (editText == null) {
            return; // Skip TextWatcher setup if EditText is null
        }
        editText.addTextChangedListener(new TextWatcher() {
            private String previousText = "";
            private int cursorPosition = 0;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isFormatting && editText != null) {
                    previousText = s.toString();
                    cursorPosition = editText.getSelectionStart();
                }
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Real-time validation during typing
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting || editText == null) return;

                String input = s.toString();
                String formatted = formatLicenseInput(input);

                if (!input.equals(formatted)) {
                    isFormatting = true;

                    // Calculate new cursor position
                    int newCursorPos = calculateNewCursorPosition(previousText, formatted, cursorPosition);

                    s.replace(0, s.length(), formatted);

                    // Set cursor position safely
                    if (newCursorPos >= 0 && newCursorPos <= formatted.length()) {
                        editText.setSelection(newCursorPos);
                    }

                    isFormatting = false;
                }

                // Validate the formatted input
                validateLicense(formatted);
            }
        });
    }
    
    /**
     * Format license input - accept any input, no complex formatting
     */
    public String formatLicenseInput(String input) {
        if (input == null) return "";

        // Just trim whitespace and limit length - accept any characters
        String cleaned = input.trim();

        // Limit to maximum length
        if (cleaned.length() > MAX_LICENSE_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LICENSE_LENGTH);
        }

        return cleaned;
    }
    
    /**
     * Calculate new cursor position after formatting
     */
    private int calculateNewCursorPosition(String oldText, String newText, int oldCursor) {
        if (oldCursor <= 0) return 0;
        if (oldCursor >= newText.length()) return newText.length();
        
        // Count characters before cursor position (excluding hyphens)
        int charCount = 0;
        for (int i = 0; i < oldCursor && i < oldText.length(); i++) {
            if (oldText.charAt(i) != '-') {
                charCount++;
            }
        }
        
        // Find position in new text for the same character count
        int newPos = 0;
        int currentCharCount = 0;
        for (int i = 0; i < newText.length(); i++) {
            if (newText.charAt(i) != '-') {
                currentCharCount++;
                if (currentCharCount > charCount) {
                    break;
                }
            }
            newPos = i + 1;
        }
        
        return Math.min(newPos, newText.length());
    }
    
    /**
     * Validate license format and notify listener - simple validation
     */
    private void validateLicense(String license) {
        if (validationListener == null) return;

        if (license.isEmpty()) {
            validationListener.onLicenseCleared();
        } else if (license.length() >= 5) {
            // Accept any license with at least 5 characters
            validationListener.onValidLicense(license);
        }
    }
    
    /**
     * Check if license matches valid KeyAuth format - simple validation
     */
    public static boolean isValidLicenseFormat(String license) {
        return license != null && license.trim().length() > 5; // Just check minimum length
    }
    
    /**
     * Check if license is a valid partial input - accept anything
     */
    public static boolean isValidPartialLicense(String license) {
        return license != null && license.length() <= MAX_LICENSE_LENGTH;
    }
    
    /**
     * Clean and format pasted license text - accept any input
     */
    public String cleanPastedLicense(String pastedText) {
        if (pastedText == null) return "";

        // Just trim and return - no complex validation
        return pastedText.trim();
    }
    
    /**
     * Set license text programmatically (e.g., from clipboard)
     */
    public void setLicenseText(String license) {
        if (editText == null) {
            return; // Skip if EditText is null
        }
        String formatted = formatLicenseInput(license);
        isFormatting = true;
        editText.setText(formatted);
        editText.setSelection(formatted.length());
        isFormatting = false;
        validateLicense(formatted);
    }
    
    /**
     * Clear license input
     */
    public void clearLicense() {
        if (editText == null) {
            return; // Skip if EditText is null
        }
        isFormatting = true;
        editText.setText("");
        isFormatting = false;
        if (validationListener != null) {
            validationListener.onLicenseCleared();
        }
    }
    
    /**
     * Get current license without formatting
     */
    public String getRawLicense() {
        if (editText == null) {
            return ""; // Return empty string if EditText is null
        }
        return editText.getText().toString().replaceAll("-", "");
    }

    /**
     * Get current formatted license
     */
    public String getFormattedLicense() {
        if (editText == null) {
            return ""; // Return empty string if EditText is null
        }
        return editText.getText().toString();
    }

    /**
     * Create input filter for license field (preserves case sensitivity)
     */
    public static InputFilter createLicenseInputFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                     Spanned dest, int dstart, int dend) {

                // Allow only alphanumeric characters and hyphens (PRESERVE case)
                StringBuilder filtered = new StringBuilder();
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (Character.isLetterOrDigit(c) || c == '-') {
                        filtered.append(c); // Keep original case
                    }
                }

                // Check if the result would exceed maximum length
                String currentText = dest.toString();
                String newText = currentText.substring(0, dstart) +
                               filtered.toString() +
                               currentText.substring(dend);

                if (newText.length() > MAX_LICENSE_LENGTH) {
                    return "";
                }

                return filtered.toString();
            }
        };
    }
}
