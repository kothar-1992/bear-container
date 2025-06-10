package org.bearmod.container.utils;

import android.util.Log;

/**
 * Simple verifier to test the corrected 6-character license format
 */
public class LicenseFormatVerifier {
    private static final String TAG = "LicenseFormatVerifier";
    
    public static void verifyLicenseFormat() {
        Log.d(TAG, "=== Verifying 6-Character License Format ===");

        // Create formatter with null (now safe due to null checks)
        LicenseInputFormatter formatter = new LicenseInputFormatter(null);

        // Test 1: Format a 36-character license (6 segments of 6 characters each)
        String testLicense = "5dJo4e2Xq1IyxDtbLeuIQAsYNMIeA2LRgz95";
        String formatted = formatter.formatLicenseInput(testLicense);
        Log.d(TAG, "Input: " + testLicense);
        Log.d(TAG, "Formatted: " + formatted);
        Log.d(TAG, "Expected format: XXXXXX-XXXXXX-XXXXXX-XXXXXX-XXXXXX-XXXXXX");

        // Test 2: Validate the formatted license using static method
        boolean isValid = LicenseInputFormatter.isValidLicenseFormat(formatted);
        Log.d(TAG, "Is valid format: " + isValid);

        // Test 3: Test with the ACTUAL user's mixed-case license from screenshot
        String userLicense = "CUWOYJLRB1VUNY7G1WAEYL5H8SUL";
        String userFormatted = formatter.formatLicenseInput(userLicense);
        Log.d(TAG, "User Input: " + userLicense);
        Log.d(TAG, "User Formatted: " + userFormatted);

        boolean userValid = LicenseInputFormatter.isValidLicenseFormat(userFormatted);
        Log.d(TAG, "User License Is valid: " + userValid);

        // Test 4: Test with a real KeyAuth-style mixed case license
        String keyAuthLicense = "ABC123def456GHI789jkl012MNO345pqr678";
        String keyAuthFormatted = formatter.formatLicenseInput(keyAuthLicense);
        Log.d(TAG, "KeyAuth Input: " + keyAuthLicense);
        Log.d(TAG, "KeyAuth Formatted: " + keyAuthFormatted);

        boolean keyAuthValid = LicenseInputFormatter.isValidLicenseFormat(keyAuthFormatted);
        Log.d(TAG, "KeyAuth Is valid: " + keyAuthValid);

        // Test 5: Verify length constraints
        Log.d(TAG, "Max formatted length: " + (6 * 6 + 5) + " characters");
        Log.d(TAG, "Actual formatted length: " + keyAuthFormatted.length());

        // Test 6: Verify static validation methods work correctly with mixed case
        Log.d(TAG, "Testing static validation methods...");
        Log.d(TAG, "Valid mixed-case license test: " + LicenseInputFormatter.isValidLicenseFormat("ABC123-def456-GHI789-jkl012-MNO345-pqr678"));
        Log.d(TAG, "Valid uppercase license test: " + LicenseInputFormatter.isValidLicenseFormat("ABC123-DEF456-GHI789-JKL012-MNO345-PQR678"));
        Log.d(TAG, "Invalid license test: " + LicenseInputFormatter.isValidLicenseFormat("ABC12-DEF45-GHI78"));

        // Test 7: Test case preservation
        String mixedCaseInput = "AbC123dEf456";
        String mixedCaseFormatted = formatter.formatLicenseInput(mixedCaseInput);
        Log.d(TAG, "Case preservation test - Input: " + mixedCaseInput);
        Log.d(TAG, "Case preservation test - Output: " + mixedCaseFormatted);
        Log.d(TAG, "Case preserved correctly: " + mixedCaseFormatted.equals("AbC123-dEf456"));

        Log.d(TAG, "=== License Format Verification Complete ===");
    }
}
