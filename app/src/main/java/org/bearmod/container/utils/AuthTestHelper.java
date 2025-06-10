package org.bearmod.container.utils;

import android.content.Context;
import android.util.Log;

import org.bearmod.container.KeyAuthAPIManager;
import org.bearmod.container.security.SecureLicenseManager;
import org.bearmod.container.security.SessionManager;
import org.json.JSONObject;

/**
 * Helper class for testing authentication functionality
 */
public class AuthTestHelper {
    private static final String TAG = "AuthTestHelper";
    
    /**
     * Test HWID generation and consistency
     */
    public static void testHWIDGeneration(Context context) {
        Log.d(TAG, "=== Testing HWID Generation ===");
        
        SecureLicenseManager licenseManager = SecureLicenseManager.getInstance(context);
        
        // Generate HWID multiple times to ensure consistency
        String hwid1 = licenseManager.generateHWID();
        String hwid2 = licenseManager.generateHWID();
        String hwid3 = licenseManager.generateHWID();
        
        Log.d(TAG, "HWID 1: " + hwid1);
        Log.d(TAG, "HWID 2: " + hwid2);
        Log.d(TAG, "HWID 3: " + hwid3);
        
        if (hwid1.equals(hwid2) && hwid2.equals(hwid3)) {
            Log.d(TAG, "✓ HWID generation is consistent");
        } else {
            Log.e(TAG, "✗ HWID generation is inconsistent");
        }
        
        if (hwid1.length() == 32) {
            Log.d(TAG, "✓ HWID has correct length (32 characters)");
        } else {
            Log.e(TAG, "✗ HWID has incorrect length: " + hwid1.length());
        }
    }
    
    /**
     * Test session management functionality
     */
    public static void testSessionManagement(Context context) {
        Log.d(TAG, "=== Testing Session Management ===");
        
        SessionManager sessionManager = SessionManager.getInstance(context);
        
        // Test instance registration
        sessionManager.registerInstance();
        Log.d(TAG, "Instance registered: " + sessionManager.getCurrentInstanceId().substring(0, 8) + "...");
        
        // Test session creation
        String sessionId = sessionManager.createNewSession();
        Log.d(TAG, "Session created: " + sessionId);
        
        // Test session validation
        boolean isValid = sessionManager.isSessionValid();
        Log.d(TAG, "Session valid: " + isValid);
        
        if (isValid) {
            Log.d(TAG, "✓ Session management working correctly");
        } else {
            Log.e(TAG, "✗ Session management has issues");
        }
        
        // Test heartbeat
        sessionManager.updateHeartbeat();
        Log.d(TAG, "Heartbeat updated");
        
        // Test conflict detection
        boolean hasConflict = sessionManager.isAnotherInstanceRunning();
        Log.d(TAG, "Another instance running: " + hasConflict);
    }
    
    /**
     * Test license storage functionality
     */
    public static void testLicenseStorage(Context context) {
        Log.d(TAG, "=== Testing License Storage ===");
        
        SecureLicenseManager licenseManager = SecureLicenseManager.getInstance(context);
        
        // Test storing a dummy license
        try {
            JSONObject dummyUserInfo = new JSONObject();
            dummyUserInfo.put("username", "test_user");
            dummyUserInfo.put("subscription", "premium");
            dummyUserInfo.put("expiry", System.currentTimeMillis() / 1000 + 86400); // 24 hours from now
            
            String testLicense = "TEST-LICENSE-KEY-12345";
            long expiry = System.currentTimeMillis() + 86400000; // 24 hours from now
            
            licenseManager.storeLicense(testLicense, dummyUserInfo, expiry);
            Log.d(TAG, "Test license stored");
            
            // Test retrieving the license
            String retrievedLicense = licenseManager.getStoredLicense();
            if (testLicense.equals(retrievedLicense)) {
                Log.d(TAG, "✓ License storage and retrieval working");
            } else {
                Log.e(TAG, "✗ License storage/retrieval failed");
            }
            
            // Test license validation
            boolean isValid = licenseManager.isStoredLicenseValid();
            Log.d(TAG, "Stored license valid: " + isValid);
            
            if (isValid) {
                Log.d(TAG, "✓ License validation working");
            } else {
                Log.e(TAG, "✗ License validation failed");
            }
            
            // Test user info retrieval
            JSONObject retrievedUserInfo = licenseManager.getStoredUserInfo();
            if (retrievedUserInfo != null) {
                Log.d(TAG, "✓ User info retrieval working");
                Log.d(TAG, "Retrieved user info: " + retrievedUserInfo.toString());
            } else {
                Log.e(TAG, "✗ User info retrieval failed");
            }
            
            // Clean up test data
            licenseManager.clearStoredLicense();
            Log.d(TAG, "Test license cleared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing license storage", e);
        }
    }
    
    /**
     * Test KeyAuth integration
     */
    public static void testKeyAuthIntegration(Context context) {
        Log.d(TAG, "=== Testing KeyAuth Integration ===");
        
        // Initialize KeyAuth with context
        KeyAuthAPIManager.initializeWithContext(context);
        Log.d(TAG, "KeyAuth initialized with context");
        
        // Test HWID retrieval
        String hwid = KeyAuthAPIManager.getCurrentHWID();
        if (hwid != null && !hwid.isEmpty()) {
            Log.d(TAG, "✓ HWID retrieval working: " + hwid.substring(0, 8) + "...");
        } else {
            Log.e(TAG, "✗ HWID retrieval failed");
        }
        
        // Test stored license checking
        KeyAuthAPIManager.checkStoredLicense(new KeyAuthAPIManager.LicenseCheckCallback() {
            @Override
            public void onValidLicense(JSONObject userInfo) {
                Log.d(TAG, "✓ Valid stored license found");
                if (userInfo != null) {
                    Log.d(TAG, "User info: " + userInfo.toString());
                }
            }
            
            @Override
            public void onInvalidLicense() {
                Log.d(TAG, "No valid stored license (expected for new installation)");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking stored license: " + error);
            }
        });
    }
    
    /**
     * Run all tests
     */
    public static void runAllTests(Context context) {
        Log.d(TAG, "=== Starting Authentication System Tests ===");
        
        testHWIDGeneration(context);
        testSessionManagement(context);
        testLicenseStorage(context);
        testKeyAuthIntegration(context);
        
        Log.d(TAG, "=== Authentication System Tests Completed ===");
    }
    
    /**
     * Test license expiry handling
     */
    public static void testLicenseExpiry(Context context) {
        Log.d(TAG, "=== Testing License Expiry ===");
        
        SecureLicenseManager licenseManager = SecureLicenseManager.getInstance(context);
        
        try {
            // Store an expired license
            JSONObject expiredUserInfo = new JSONObject();
            expiredUserInfo.put("username", "expired_user");
            expiredUserInfo.put("subscription", "expired");
            
            String expiredLicense = "EXPIRED-LICENSE-KEY";
            long pastExpiry = System.currentTimeMillis() - 86400000; // 24 hours ago
            
            licenseManager.storeLicense(expiredLicense, expiredUserInfo, pastExpiry);
            Log.d(TAG, "Expired license stored");
            
            // Test validation of expired license
            boolean isValid = licenseManager.isStoredLicenseValid();
            if (!isValid) {
                Log.d(TAG, "✓ Expired license correctly detected as invalid");
            } else {
                Log.e(TAG, "✗ Expired license incorrectly validated as valid");
            }
            
            // Verify license was cleared
            String retrievedLicense = licenseManager.getStoredLicense();
            if (retrievedLicense == null || retrievedLicense.isEmpty()) {
                Log.d(TAG, "✓ Expired license automatically cleared");
            } else {
                Log.e(TAG, "✗ Expired license not cleared");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing license expiry", e);
        }
    }
}
