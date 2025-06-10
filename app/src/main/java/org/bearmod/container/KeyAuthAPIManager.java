package org.bearmod.container;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.bearmod.container.security.SecureLicenseManager;
import org.bearmod.container.security.SessionManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Enhanced KeyAuth API Manager with HWID binding and persistent license storage
 */
public class KeyAuthAPIManager {
    private static final String TAG = "KeyAuthAPIManager";

    private static final String ownerid = "yLoA9zcOEF"; // Account ID
    private static final String version = "1.3"; // Application version

    // Unified configuration for both debug and release builds
    private static final String appname = "org.bearmod.container"; // Single app name for all builds
    private static final String hash = "4f9b15598f6e8bdf07ca39e9914cd3e9"; // Application hash for KeyAuth (BearOwner.jks)

    private static final String baseUrl = "https://keyauth.win/api/1.3/";

    // Session management
    private static String sessionId = null;
    private static boolean isInitialized = false;
    private static String encKey = null;
    private static Context appContext;
    private static SecureLicenseManager licenseManager;
    private static SessionManager sessionManager;

    public interface AuthCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public interface InitCallback {
        void onInitSuccess();
        void onInitError(String error);
    }

    public interface LicenseCheckCallback {
        void onValidLicense(JSONObject userInfo);
        void onInvalidLicense();
        void onError(String error);
    }

    /**
     * Initialize with context for license management and session handling
     */
    public static void initializeWithContext(Context context) {
        appContext = context.getApplicationContext();
        licenseManager = SecureLicenseManager.getInstance(appContext);
        sessionManager = SessionManager.getInstance(appContext);

        // Reset state for clean initialization (following Bear-Loader pattern)
        sessionId = null;
        isInitialized = false;
        encKey = null;
    }

    /**
     * Check for stored valid license and auto-validate
     */
    public static void checkStoredLicense(LicenseCheckCallback callback) {
        if (licenseManager == null) {
            callback.onError("License manager not initialized. Call initializeWithContext() first.");
            return;
        }

        new Thread(() -> {
            try {
                if (licenseManager.isStoredLicenseValid()) {
                    String storedLicense = licenseManager.getStoredLicense();

                    // Check if we need to revalidate with server
                    if (licenseManager.needsRevalidation()) {
                        // Revalidate with server
                        validateLicenseWithHWID(storedLicense, new AuthCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                boolean success = response.optBoolean("success", false);
                                if (success) {
                                    // Update validation timestamp
                                    licenseManager.updateLastValidation();

                                    // Extract user info and expiry
                                    JSONObject info = response.optJSONObject("info");
                                    if (info != null) {
                                        // Store updated license info
                                        long expiry = info.optLong("expiry", 0) * 1000; // Convert to milliseconds
                                        licenseManager.storeLicense(storedLicense, info, expiry);
                                        callback.onValidLicense(info);
                                    } else {
                                        callback.onValidLicense(licenseManager.getStoredUserInfo());
                                    }
                                } else {
                                    // License no longer valid, clear it
                                    licenseManager.clearStoredLicense();
                                    callback.onInvalidLicense();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                // Network error, but license is still locally valid
                                callback.onValidLicense(licenseManager.getStoredUserInfo());
                            }
                        });
                    } else {
                        // License is valid and doesn't need revalidation
                        callback.onValidLicense(licenseManager.getStoredUserInfo());
                    }
                } else {
                    callback.onInvalidLicense();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking stored license", e);
                callback.onError("Exception: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Initialize KeyAuth application with retry logic - MUST be called before any other operations
     */
    public static void init(InitCallback callback) {
        initWithRetry(callback, 0);
    }

    /**
     * Initialize KeyAuth with retry mechanism and enhanced session conflict handling
     */
    private static void initWithRetry(InitCallback callback, int attemptCount) {
        final int MAX_RETRIES = 5; // Increased retries for session conflicts
        final int BASE_RETRY_DELAY_MS = 2000; // Base delay
        final int RETRY_DELAY_MS = BASE_RETRY_DELAY_MS + (attemptCount * 1000); // Progressive delay

        new Thread(() -> {
            try {
                Log.d(TAG, "🔄 KeyAuth initialization attempt " + (attemptCount + 1) + "/" + (MAX_RETRIES + 1));

                // Enhanced session cleanup for conflict resolution
                performEnhancedSessionCleanup(attemptCount);

                // Progressive delay to handle session conflicts
                if (attemptCount > 0) {
                    Log.d(TAG, "⏳ Waiting " + RETRY_DELAY_MS + "ms before retry (progressive delay for session conflicts)...");
                    Thread.sleep(RETRY_DELAY_MS);
                }

                // Generate a truly unique session ID with timestamp
                long timestamp = System.currentTimeMillis();
                String uniquePart = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                sessionId = (uniquePart + Long.toHexString(timestamp)).substring(0, 16);
                Log.d(TAG, "🆔 Generated unique session ID: " + sessionId.substring(0, 8) + "... (attempt " + (attemptCount + 1) + ")");

                // Enhanced configuration validation
                if (!validateKeyAuthConfiguration()) {
                    String configError = buildConfigurationErrorMessage();
                    Log.e(TAG, "❌ KeyAuth configuration validation failed: " + configError);
                    callback.onInitError(configError);
                    return;
                }

                Log.d(TAG, "✅ KeyAuth configuration validated successfully");

                URL url = new URL(baseUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("User-Agent", "KeyAuth");
                conn.setConnectTimeout(15000); // 15 second timeout
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);

                // Build post data (following Bear-Loader pattern)
                String postData = "type=init" +
                        "&ver=" + version +
                        "&name=" + appname +
                        "&ownerid=" + ownerid +
                        "&hash=" + hash +
                        "&sessionid=" + sessionId;

                Log.d(TAG, "KeyAuth init request:");
                Log.d(TAG, "  App name: " + appname);
                Log.d(TAG, "  Hash: " + hash);
                Log.d(TAG, "  Owner ID: " + ownerid);
                Log.d(TAG, "  Version: " + version);
                Log.d(TAG, "  Session ID: " + sessionId.substring(0, 8) + "...");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "KeyAuth response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream())) {
                        while (scanner.hasNextLine()) {
                            sb.append(scanner.nextLine());
                        }
                    }

                    String responseBody = sb.toString().trim();
                    Log.d(TAG, "KeyAuth raw response: " + responseBody);

                    // ✅ FIX 1: Handle plain string responses BEFORE parsing as JSON
                    if (!responseBody.startsWith("{") && !responseBody.startsWith("[")) {
                        Log.e(TAG, "🚨 KeyAuth returned non-JSON response: " + responseBody);

                        if (responseBody.equalsIgnoreCase("KeyAuth_Invalid")) {
                            String configError = buildConfigurationErrorMessage();
                            callback.onInitError("❌ KeyAuth configuration is invalid.\n\n" + configError);
                            return;
                        }

                        if (handleSessionConflict(responseBody)) {
                            if (attemptCount < MAX_RETRIES) {
                                Log.d(TAG, "🔄 Retrying after non-JSON session conflict in " + RETRY_DELAY_MS + "ms...");
                                new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    initWithRetry(callback, attemptCount + 1), RETRY_DELAY_MS);
                                return;
                            } else {
                                callback.onInitError("❌ Persistent session conflict: " + responseBody);
                                return;
                            }
                        }

                        callback.onInitError("Unexpected response from KeyAuth server: " + responseBody);
                        return;
                    }

                    // ✅ SAFE TO PARSE JSON NOW
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        boolean success = json.optBoolean("success", false);

                        if (success) {
                            isInitialized = true;
                            encKey = json.optString("enckey", "");
                            Log.d(TAG, "✅ KeyAuth initialized successfully");

                            // Register successful session with session manager
                            if (sessionManager != null) {
                                sessionManager.registerInstance();
                                sessionManager.updateHeartbeat();
                            }

                            // ✅ CRITICAL: Always check for license on success
                            if (licenseManager != null) {
                                String storedLicense = licenseManager.getStoredLicense();
                                if (storedLicense != null && !storedLicense.isEmpty()) {
                                    Log.d(TAG, "🔐 Found stored license, validating after init...");
                                    validateLicenseWithHWID(storedLicense, new AuthCallback() {
                                        @Override
                                        public void onSuccess(JSONObject licenseResponse) {
                                            Log.d(TAG, "✅ License automatically revalidated post-init");
                                            // Update license manager with fresh validation
                                            boolean licenseSuccess = licenseResponse.optBoolean("success", false);
                                            if (licenseSuccess) {
                                                JSONObject info = licenseResponse.optJSONObject("info");
                                                if (info != null) {
                                                    long expiry = info.optLong("expiry", 0) * 1000;
                                                    licenseManager.storeLicense(storedLicense, info, expiry);
                                                    licenseManager.updateLastValidation();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Log.e(TAG, "❌ License validation failed post-init: " + error);

                                            // ✅ Handle session conflicts in license validation
                                            if (error.toLowerCase().contains("session") ||
                                                error.toLowerCase().contains("not found")) {
                                                Log.w(TAG, "🚨 Session conflict in license validation, clearing session");
                                                resetSession();
                                                licenseManager.clearStoredLicense(); // Avoid looping
                                            } else {
                                                licenseManager.clearStoredLicense();
                                            }
                                        }
                                    });
                                } else {
                                    Log.d(TAG, "ℹ️ No stored license found after init");
                                }
                            }

                            callback.onInitSuccess();
                        } else {
                            String message = json.optString("message", "Unknown error");
                            callback.onInitError("❌ KeyAuth initialization failed: " + message);
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "❌ Failed to parse JSON: " + e.getMessage());
                        callback.onInitError("Invalid JSON response from server: " + responseBody);
                    }
                } else {
                    String errorMsg = "Server returned HTTP error: " + responseCode;
                    Log.e(TAG, errorMsg);

                    // Retry on server errors
                    if (attemptCount < MAX_RETRIES) {
                        Log.w(TAG, "Retrying KeyAuth initialization in " + RETRY_DELAY_MS + "ms...");
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        initWithRetry(callback, attemptCount + 1);
                    } else {
                        callback.onInitError(errorMsg + " (after " + (MAX_RETRIES + 1) + " attempts)");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "KeyAuth initialization failed (attempt " + (attemptCount + 1) + ")", e);
                e.printStackTrace();

                String errorMessage = "Exception: " + e.getMessage();

                // Retry on network/connection errors
                if (attemptCount < MAX_RETRIES && isRetryableException(e)) {
                    Log.w(TAG, "Retrying KeyAuth initialization due to: " + e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    initWithRetry(callback, attemptCount + 1);
                } else {
                    if (attemptCount >= MAX_RETRIES) {
                        errorMessage += " (after " + (MAX_RETRIES + 1) + " attempts)";
                    }
                    callback.onInitError(errorMessage);
                }
            }
        }).start();
    }

    /**
     * Check if an exception is retryable
     */
    private static boolean isRetryableException(Exception e) {
        return e instanceof java.net.SocketTimeoutException ||
               e instanceof java.net.ConnectException ||
               e instanceof java.net.UnknownHostException ||
               e instanceof java.io.IOException;
    }

    /**
     * Validate KeyAuth configuration before initialization
     */
    public static boolean validateConfiguration() {
        Log.d(TAG, "Validating KeyAuth configuration...");

        boolean isValid = true;
        StringBuilder issues = new StringBuilder();

        // Check hash
        if (hash == null || hash.isEmpty()) {
            issues.append("- Hash is null or empty\n");
            isValid = false;
        } else if (hash.equals("DEBUG_HASH_PLACEHOLDER") || hash.equals("DEVELOPMENT_HASH_PLACEHOLDER")) {
            issues.append("- Hash is still a placeholder: ").append(hash).append("\n");
            isValid = false;
        } else if (hash.length() != 32) {
            issues.append("- Hash length is incorrect (expected 32 chars, got ").append(hash.length()).append(")\n");
            isValid = false;
        }

        // Check app name
        if (appname == null || appname.isEmpty()) {
            issues.append("- App name is null or empty\n");
            isValid = false;
        }

        // Check owner ID
        if (ownerid == null || ownerid.isEmpty()) {
            issues.append("- Owner ID is null or empty\n");
            isValid = false;
        }

        // Check version
        if (version == null || version.isEmpty()) {
            issues.append("- Version is null or empty\n");
            isValid = false;
        }

        if (isValid) {
            Log.d(TAG, "✅ KeyAuth configuration is valid");
            Log.d(TAG, "  App: " + appname);
            Log.d(TAG, "  Hash: " + hash);
            Log.d(TAG, "  Owner: " + ownerid);
            Log.d(TAG, "  Version: " + version);
        } else {
            Log.e(TAG, "❌ KeyAuth configuration issues found:");
            Log.e(TAG, issues.toString());
        }

        return isValid;
    }

    /**
     * Clear session state - useful for handling session conflicts
     */
    public static void clearSession() {
        Log.d(TAG, "Clearing KeyAuth session state");
        sessionId = null;
        isInitialized = false;
        encKey = null;

        // Force garbage collection to ensure session cleanup
        System.gc();

        Log.d(TAG, "Session data cleared and garbage collected");
    }

    /**
     * Enhanced session cleanup for conflict resolution
     */
    private static void performEnhancedSessionCleanup(int attemptCount) {
        Log.d(TAG, "🧹 Performing enhanced session cleanup (attempt " + (attemptCount + 1) + ")");

        // Clear all session state
        clearSession();

        // Clear session manager state if available
        if (sessionManager != null) {
            sessionManager.clearSession();
            sessionManager.forceSessionTakeover();
        }

        // Force garbage collection to ensure cleanup
        System.gc();

        // Additional cleanup for higher retry attempts
        if (attemptCount > 1) {
            Log.d(TAG, "🔧 Performing deep session cleanup for retry attempt " + (attemptCount + 1));
            // Reset all static state
            sessionId = null;
            isInitialized = false;
            encKey = null;
        }

        Log.d(TAG, "✅ Enhanced session cleanup completed");
    }

    /**
     * Validate KeyAuth configuration before initialization
     */
    private static boolean validateKeyAuthConfiguration() {
        // Check hash configuration
        if (hash == null || hash.isEmpty() || hash.equals("DEBUG_HASH_PLACEHOLDER")) {
            Log.e(TAG, "❌ Invalid hash configuration: " + hash);
            return false;
        }

        // Check app name configuration
        if (appname == null || appname.isEmpty()) {
            Log.e(TAG, "❌ Invalid app name configuration: " + appname);
            return false;
        }

        // Check owner ID configuration
        if (ownerid == null || ownerid.isEmpty()) {
            Log.e(TAG, "❌ Invalid owner ID configuration: " + ownerid);
            return false;
        }

        Log.d(TAG, "✅ KeyAuth configuration validation passed");
        Log.d(TAG, "  📱 App name: " + appname);
        Log.d(TAG, "  🔑 Hash: " + hash);
        Log.d(TAG, "  👤 Owner ID: " + ownerid);

        return true;
    }

    /**
     * Build detailed configuration error message
     */
    private static String buildConfigurationErrorMessage() {
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("❌ KeyAuth Configuration Error\n\n");
        errorMsg.append("CRITICAL: Please verify in your KeyAuth dashboard:\n\n");

        errorMsg.append("1. 🔑 HASH REGISTRATION:\n");
        errorMsg.append("   Current hash: ").append(hash).append("\n");
        errorMsg.append("   ⚠️  Ensure this hash is REGISTERED (not marked for deletion)\n");
        errorMsg.append("   ⚠️  Check that hash matches your BearOwner.jks keystore\n\n");

        errorMsg.append("2. 📱 APP NAME REGISTRATION:\n");
        errorMsg.append("   Current app name: ").append(appname).append("\n");
        errorMsg.append("   ⚠️  Ensure '").append(appname).append("' exists in your KeyAuth dashboard\n");
        errorMsg.append("   ⚠️  This single app name is used for all build variants\n\n");

        errorMsg.append("3. 👤 OWNER ID VERIFICATION:\n");
        errorMsg.append("   Current owner ID: ").append(ownerid).append("\n");
        errorMsg.append("   ⚠️  Verify this matches your KeyAuth account settings\n\n");

        errorMsg.append("4. 🔧 DASHBOARD ACTIONS REQUIRED:\n");
        errorMsg.append("   • Cancel any hash deletion operations\n");
        errorMsg.append("   • Register app name 'org.bearmod.container' in your application\n");
        errorMsg.append("   • Verify certificate hash matches BearOwner.jks keystore\n");
        errorMsg.append("   • Check application status is active\n\n");

        errorMsg.append("Visit https://keyauth.cc/app/ to fix these issues.");

        return errorMsg.toString();
    }

    /**
     * Enhanced session conflict detection and immediate resolution
     */
    private static boolean handleSessionConflict(String response) {
        if (response != null && (
            response.contains("Session not found") ||
            response.contains("Use latest code") ||
            response.contains("only have app opened 1 at a time") ||
            response.contains("session_unauthed") ||
            response.contains("KeyAuth_Invalid") ||
            response.contains("session conflict") ||
            response.contains("multiple instances"))) {

            Log.w(TAG, "🚨 Session conflict detected: " + response);

            // ✅ CRITICAL: Force complete session reset
            forceCompleteSessionReset();

            Log.d(TAG, "✅ Session conflict resolved, ready for immediate retry");
            return true;
        }
        return false;
    }

    /**
     * Force complete session reset for conflict resolution
     */
    private static void forceCompleteSessionReset() {
        Log.d(TAG, "🔄 Forcing complete session reset...");

        // Clear all session state
        sessionId = null;
        isInitialized = false;
        encKey = null;

        // Clear session manager state
        if (sessionManager != null) {
            sessionManager.clearSession();
            sessionManager.forceSessionTakeover();
        }

        // Clear any stored license that might be causing conflicts
        if (licenseManager != null) {
            licenseManager.clearStoredLicense();
            Log.d(TAG, "🧹 Cleared stored license to prevent session loops");
        }

        // Force garbage collection
        System.gc();

        // Generate completely new session ID with timestamp
        long timestamp = System.currentTimeMillis();
        String uniquePart = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        sessionId = (uniquePart + Long.toHexString(timestamp)).substring(0, 16);

        Log.d(TAG, "🆔 Generated fresh session ID: " + sessionId.substring(0, 8) + "...");
        Log.d(TAG, "✅ Complete session reset finished");
    }

    /**
     * Login with username and password - requires initialization first
     */
    public static void login(String username, String password, AuthCallback callback) {
        if (!isInitialized || sessionId == null) {
            callback.onError("Session ID not provided, this is required. Please initialize KeyAuth first.");
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(baseUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("User-Agent", "KeyAuth");
                conn.setDoOutput(true);

                String postData = "type=login" +
                        "&username=" + username +
                        "&pass=" + password +
                        "&sessionid=" + sessionId +
                        "&name=" + appname +
                        "&ownerid=" + ownerid;

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream())) {
                        while (scanner.hasNextLine()) {
                            sb.append(scanner.nextLine());
                        }
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    callback.onSuccess(json);
                } else {
                    callback.onError("Server returned error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Login failed", e);
                callback.onError("Exception: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Validate license key with HWID binding - requires initialization first
     */
    public static void validateLicenseWithHWID(String license, AuthCallback callback) {
        if (!isInitialized || sessionId == null) {
            callback.onError("Session ID not provided, this is required. Please initialize KeyAuth first.");
            return;
        }

        if (licenseManager == null) {
            callback.onError("License manager not initialized. Call initializeWithContext() first.");
            return;
        }

        new Thread(() -> {
            try {
                String hwid = licenseManager.getCurrentHWID();

                URL url = new URL(baseUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("User-Agent", "KeyAuth");
                conn.setDoOutput(true);

                String postData = "type=license" +
                        "&key=" + license +
                        "&hwid=" + hwid +
                        "&sessionid=" + sessionId +
                        "&name=" + appname +
                        "&ownerid=" + ownerid;

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream())) {
                        while (scanner.hasNextLine()) {
                            sb.append(scanner.nextLine());
                        }
                    }
                    String responseBody = sb.toString().trim();
                    Log.d(TAG, "License validation response: " + responseBody);

                    // ✅ Check for session conflicts in license validation
                    if (handleSessionConflict(responseBody)) {
                        Log.w(TAG, "🚨 Session conflict in license validation, triggering reset");
                        callback.onError("Session conflict detected: " + responseBody);
                        return;
                    }

                    JSONObject json = new JSONObject(responseBody);

                    // If validation successful, store license securely
                    boolean success = json.optBoolean("success", false);
                    if (success) {
                        JSONObject info = json.optJSONObject("info");
                        if (info != null) {
                            long expiry = info.optLong("expiry", 0) * 1000; // Convert to milliseconds
                            licenseManager.storeLicense(license, info, expiry);
                            Log.d(TAG, "✅ License validated and stored successfully");
                        }
                    } else {
                        String message = json.optString("message", "License validation failed");
                        Log.e(TAG, "❌ License validation failed: " + message);

                        // ✅ Handle session conflicts in JSON response
                        if (handleSessionConflict(message)) {
                            callback.onError("Session conflict in license validation: " + message);
                            return;
                        }
                    }

                    callback.onSuccess(json);
                } else {
                    callback.onError("Server returned error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "License validation failed", e);

                // ✅ Handle session conflicts in exceptions
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.toLowerCase().contains("session") ||
                                        errorMsg.toLowerCase().contains("not found"))) {
                    Log.w(TAG, "🚨 Session conflict in exception, resetting session");
                    resetSession();
                    if (licenseManager != null) {
                        licenseManager.clearStoredLicense(); // Avoid looping
                    }
                }

                callback.onError("Exception: " + errorMsg);
            }
        }).start();
    }

    /**
     * Validate license key (legacy method for backward compatibility)
     */
    public static void validateLicense(String license, AuthCallback callback) {
        validateLicenseWithHWID(license, callback);
    }

    /**
     * Check if KeyAuth is initialized
     */
    public static boolean isInitialized() {
        return isInitialized && sessionId != null;
    }

    /**
     * ✅ Block login if session is still active elsewhere
     */
    public static boolean isSessionActiveElsewhere() {
        if (sessionManager != null) {
            return sessionManager.isAnotherInstanceRunning();
        }
        return false;
    }

    /**
     * Get current session ID
     */
    public static String getSessionId() {
        return sessionId;
    }

    /**
     * Reset session - call this when logging out or on app restart
     */
    public static void resetSession() {
        Log.d(TAG, "🔄 Resetting KeyAuth session...");

        sessionId = null;
        isInitialized = false;
        encKey = null;

        if (sessionManager != null) {
            sessionManager.clearSession();
            sessionManager.recoverFromSessionError();
        }

        // ✅ Force garbage collection to ensure cleanup
        System.gc();

        Log.d(TAG, "✅ KeyAuth session reset completed");
    }

    /**
     * Force complete session and license reset for conflict resolution
     */
    public static void forceCompleteReset() {
        Log.d(TAG, "🔄 Forcing complete KeyAuth reset...");

        // Reset session
        resetSession();

        // Clear stored license
        if (licenseManager != null) {
            licenseManager.clearStoredLicense();
            Log.d(TAG, "🧹 Cleared stored license");
        }

        // Clear session manager
        if (sessionManager != null) {
            sessionManager.forceSessionTakeover();
        }

        Log.d(TAG, "✅ Complete KeyAuth reset finished");
    }

    /**
     * Clear stored license and reset session
     */
    public static void logout() {
        if (licenseManager != null) {
            licenseManager.clearStoredLicense();
        }

        if (sessionManager != null) {
            sessionManager.clearSession();
        }

        sessionId = null;
        isInitialized = false;
        encKey = null;

        Log.d(TAG, "License cleared and session reset");
    }

    /**
     * Generate detailed diagnostic information for troubleshooting
     */
    public static String generateDiagnosticInfo() {
        StringBuilder diagnostic = new StringBuilder();
        diagnostic.append("🔍 KeyAuth Diagnostic Information\n");
        diagnostic.append("=====================================\n\n");

        diagnostic.append("📱 Application Configuration:\n");
        diagnostic.append("  App Name: ").append(appname != null ? appname : "NULL").append("\n");
        diagnostic.append("  Hash: ").append(hash != null ? hash : "NULL").append("\n");
        diagnostic.append("  Owner ID: ").append(ownerid != null ? ownerid : "NULL").append("\n");
        diagnostic.append("  Version: ").append(version != null ? version : "NULL").append("\n");
        diagnostic.append("  Base URL: ").append(baseUrl).append("\n\n");

        diagnostic.append("🔗 Session Information:\n");
        diagnostic.append("  Initialized: ").append(isInitialized).append("\n");
        diagnostic.append("  Session ID: ").append(sessionId != null ? sessionId.substring(0, Math.min(8, sessionId.length())) + "..." : "NULL").append("\n");
        diagnostic.append("  Encryption Key: ").append(encKey != null ? "SET" : "NULL").append("\n\n");

        if (sessionManager != null) {
            diagnostic.append("📊 Session Manager Status:\n");
            diagnostic.append("  Current Session: ").append(sessionManager.getCurrentSessionId() != null ? "ACTIVE" : "INACTIVE").append("\n");
            diagnostic.append("  Session Valid: ").append(sessionManager.isSessionValid()).append("\n");
            diagnostic.append("  Another Instance Running: ").append(sessionManager.isAnotherInstanceRunning()).append("\n\n");
        }

        if (licenseManager != null) {
            diagnostic.append("🎫 License Manager Status:\n");
            diagnostic.append("  Has Stored License: ").append(licenseManager.isStoredLicenseValid()).append("\n");
            diagnostic.append("  Needs Revalidation: ").append(licenseManager.needsRevalidation()).append("\n\n");
        }

        diagnostic.append("⚠️  Common Issues to Check:\n");
        diagnostic.append("  1. Hash '").append(hash).append("' is registered in KeyAuth dashboard\n");
        diagnostic.append("  2. App name '").append(appname).append("' exists in KeyAuth dashboard\n");
        diagnostic.append("  3. No hash deletion operations pending\n");
        diagnostic.append("  4. Certificate matches BearOwner.jks keystore\n");
        diagnostic.append("  5. Single package name used for all build variants\n");
        diagnostic.append("  6. No other app instances running\n\n");

        diagnostic.append("🔧 Dashboard URL: https://keyauth.cc/app/\n");

        return diagnostic.toString();
    }

    /**
     * Test KeyAuth configuration without full initialization
     */
    public static void testConfiguration(InitCallback callback) {
        Log.d(TAG, "🧪 Testing KeyAuth configuration...");

        // Validate configuration first
        if (!validateKeyAuthConfiguration()) {
            callback.onInitError(buildConfigurationErrorMessage());
            return;
        }

        new Thread(() -> {
            try {
                // Generate test session ID
                String testSessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

                URL url = new URL(baseUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("User-Agent", "KeyAuth");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000); // 10 second timeout
                conn.setReadTimeout(10000);

                String postData = "type=init" +
                        "&ver=" + version +
                        "&name=" + appname +
                        "&ownerid=" + ownerid +
                        "&hash=" + hash +
                        "&sessionid=" + testSessionId;

                Log.d(TAG, "🧪 Test request parameters:");
                Log.d(TAG, "  App name: " + appname);
                Log.d(TAG, "  Hash: " + hash);
                Log.d(TAG, "  Owner ID: " + ownerid);
                Log.d(TAG, "  Test Session ID: " + testSessionId.substring(0, 8) + "...");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "🧪 Test response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream())) {
                        while (scanner.hasNextLine()) {
                            sb.append(scanner.nextLine());
                        }
                    }

                    String responseBody = sb.toString().trim();
                    Log.d(TAG, "🧪 Test response: " + responseBody);

                    if (responseBody.startsWith("{")) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            boolean success = json.optBoolean("success", false);

                            if (success) {
                                Log.d(TAG, "✅ Configuration test PASSED - KeyAuth dashboard is properly configured");
                                callback.onInitSuccess();
                            } else {
                                String message = json.optString("message", "Test failed");
                                Log.e(TAG, "❌ Configuration test FAILED: " + message);
                                callback.onInitError("Configuration test failed: " + message);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "❌ Test response JSON parsing failed: " + e.getMessage());
                            callback.onInitError("Invalid JSON in test response: " + responseBody);
                        }
                    } else {
                        Log.e(TAG, "❌ Test returned non-JSON response: " + responseBody);
                        if (responseBody.equals("KeyAuth_Invalid")) {
                            callback.onInitError(buildConfigurationErrorMessage());
                        } else {
                            callback.onInitError("Test failed with response: " + responseBody);
                        }
                    }
                } else {
                    String errorMsg = "Test failed with HTTP error: " + responseCode;
                    Log.e(TAG, "❌ " + errorMsg);
                    callback.onInitError(errorMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Configuration test exception", e);
                callback.onInitError("Test exception: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Handle app pause
     */
    public static void onAppPause() {
        if (sessionManager != null) {
            sessionManager.onAppPause();
        }
    }

    /**
     * Handle app resume
     */
    public static void onAppResume() {
        if (sessionManager != null) {
            sessionManager.onAppResume();
        }
    }

    /**
     * Get current device HWID
     */
    public static String getCurrentHWID() {
        if (licenseManager != null) {
            return licenseManager.getCurrentHWID();
        }
        return null;
    }

    /**
     * Utility method to generate MD5 hash (if needed for additional security)
     */
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "MD5 generation failed", e);
            return "";
        }
    }
}
