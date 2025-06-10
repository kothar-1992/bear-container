package org.bearmod.container.utils;

import android.util.Log;

/**
 * Simple test runner for LicenseInputFormatter to verify functionality
 */
public class LicenseFormatterTestRunner {
    private static final String TAG = "LicenseFormatterTest";
    
    public static void runTests() {
        Log.d(TAG, "Starting LicenseInputFormatter tests...");
        
        testFormatLicenseInput();
        testValidLicenseFormat();
        testCleanPastedLicense();
        
        Log.d(TAG, "All tests completed!");
    }
    
    private static void testFormatLicenseInput() {
        Log.d(TAG, "Testing formatLicenseInput...");
        
        LicenseInputFormatter formatter = new LicenseInputFormatter(null);
        
        // Test basic formatting with 6-character segments
        String input = "5dJo4e2Xq1IyxDtbLeuIQAsYNMIeA2LRgz95ABC";
        String result = formatter.formatLicenseInput(input);
        String expected = "5DJO4E-2XQ1IY-XDTBLE-UIQASY-NMIEA2-LRGZ95";
        
        if (expected.equals(result)) {
            Log.d(TAG, "✓ Basic formatting test passed");
        } else {
            Log.e(TAG, "✗ Basic formatting test failed. Expected: " + expected + ", Got: " + result);
        }
        
        // Test with existing hyphens
        String inputWithHyphens = "5dJo4e-2Xq1Iy-xDtbLe-uIQAsY-NMIeA2-LRgz95ABC";
        String resultWithHyphens = formatter.formatLicenseInput(inputWithHyphens);
        
        if (expected.equals(resultWithHyphens)) {
            Log.d(TAG, "✓ Hyphen handling test passed");
        } else {
            Log.e(TAG, "✗ Hyphen handling test failed. Expected: " + expected + ", Got: " + resultWithHyphens);
        }
        
        // Test partial input
        String partialInput = "5dJo4e2X";
        String partialResult = formatter.formatLicenseInput(partialInput);
        String expectedPartial = "5DJO4E-2X";
        
        if (expectedPartial.equals(partialResult)) {
            Log.d(TAG, "✓ Partial input test passed");
        } else {
            Log.e(TAG, "✗ Partial input test failed. Expected: " + expectedPartial + ", Got: " + partialResult);
        }
    }
    
    private static void testValidLicenseFormat() {
        Log.d(TAG, "Testing isValidLicenseFormat...");
        
        // Test valid license format (6-character segments)
        if (LicenseInputFormatter.isValidLicenseFormat("5DJO4E-2XQ1IY-XDTBLE-UIQASY-NMIEA2-LRGZ95")) {
            Log.d(TAG, "✓ Valid license format test passed");
        } else {
            Log.e(TAG, "✗ Valid license format test failed");
        }

        // Test invalid formats
        if (!LicenseInputFormatter.isValidLicenseFormat("5DJO4E-2XQ1IY-XDTBLE-UIQASY-NMIEA2")) {
            Log.d(TAG, "✓ Invalid license format test passed");
        } else {
            Log.e(TAG, "✗ Invalid license format test failed");
        }

        if (!LicenseInputFormatter.isValidLicenseFormat("5DJO4E2XQ1IYXDTBLEUIQASYNMIEA2LRGZ95")) {
            Log.d(TAG, "✓ No hyphens format test passed");
        } else {
            Log.e(TAG, "✗ No hyphens format test failed");
        }
    }
    
    private static void testCleanPastedLicense() {
        Log.d(TAG, "Testing cleanPastedLicense...");
        
        LicenseInputFormatter formatter = new LicenseInputFormatter(null);
        
        // Test cleaning pasted license with spaces (6-character segments)
        String pastedWithSpaces = "  5dJo4e-2Xq1Iy-xDtbLe-uIQAsY-NMIeA2-LRgz95ABC  ";
        String resultCleaned = formatter.cleanPastedLicense(pastedWithSpaces);
        String expectedCleaned = "5DJO4E-2XQ1IY-XDTBLE-UIQASY-NMIEA2-LRGZ95";
        
        if (expectedCleaned.equals(resultCleaned)) {
            Log.d(TAG, "✓ Clean pasted license test passed");
        } else {
            Log.e(TAG, "✗ Clean pasted license test failed. Expected: " + expectedCleaned + ", Got: " + resultCleaned);
        }
        
        // Test short pasted text
        String shortPasted = "short";
        String resultShort = formatter.cleanPastedLicense(shortPasted);
        
        if ("".equals(resultShort)) {
            Log.d(TAG, "✓ Short pasted text test passed");
        } else {
            Log.e(TAG, "✗ Short pasted text test failed. Expected empty string, Got: " + resultShort);
        }
    }
}
