package org.bearmod.container;

import android.content.Context;
import android.util.Log;

import org.bearmod.container.security.SecureLicenseManager;
import org.bearmod.container.security.SessionManager;
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

    // Build-variant specific configuration
    private static final String appname = BuildConfig.DEBUG ?
        "org.bearmod.container.dev" : "org.bearmod.container"; // App name
    private static final String hash = BuildConfig.DEBUG ?
        "4f9b15598f6e8bdf07ca39e9914cd3e9" : "4f9b15598f6e8bdf07ca39e9914cd3e9"; // Application hash for KeyAuth (BearOwner.jks)

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
     * Initialize KeyAuth application - MUST be called before any other operations
     */
    public static void init(InitCallback callback) {
        new Thread(() -> {
            try {
                // Clear any existing session first to prevent conflicts
                sessionId = null;
                isInitialized = false;
                encKey = null;

                // Generate a fresh session ID
                sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                Log.d(TAG, "Generated fresh session ID: " + sessionId.substring(0, 8) + "...");

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

                Log.d(TAG, "KeyAuth init request with hash: " + hash.substring(0, 8) + "...");
                Log.d(TAG, "Full request data: " + postData);

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

                    String responseBody = sb.toString();
                    Log.d(TAG, "KeyAuth response: " + responseBody);

                    JSONObject json = new JSONObject(responseBody);
                    boolean success = json.optBoolean("success", false);

                    if (success) {
                        isInitialized = true;
                        encKey = json.optString("enckey", "");
                        Log.d(TAG, "KeyAuth initialized successfully");
                        callback.onInitSuccess();
                    } else {
                        String message = json.optString("message", "Initialization failed");
                        Log.e(TAG, "KeyAuth initialization failed: " + message);

                        // Handle specific session errors
                        if (message.toLowerCase().contains("session") ||
                            message.toLowerCase().contains("use latest code") ||
                            message.toLowerCase().contains("only have app opened")) {
                            Log.w(TAG, "Session conflict detected, clearing session state");
                            // Reset everything for next attempt
                            sessionId = null;
                            isInitialized = false;
                            encKey = null;
                        }

                        callback.onInitError(message);
                    }
                } else {
                    String errorMsg = "Server returned error: " + responseCode;
                    Log.e(TAG, errorMsg);
                    callback.onInitError(errorMsg);
                }
            } catch (Exception e) {
                Log.e(TAG, "KeyAuth initialization failed", e);
                e.printStackTrace();
                callback.onInitError("Exception: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Clear session state - useful for handling session conflicts
     */
    public static void clearSession() {
        Log.d(TAG, "Clearing KeyAuth session state");
        sessionId = null;
        isInitialized = false;
        encKey = null;
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
                    JSONObject json = new JSONObject(sb.toString());

                    // If validation successful, store license securely
                    boolean success = json.optBoolean("success", false);
                    if (success) {
                        JSONObject info = json.optJSONObject("info");
                        if (info != null) {
                            long expiry = info.optLong("expiry", 0) * 1000; // Convert to milliseconds
                            licenseManager.storeLicense(license, info, expiry);
                            Log.d(TAG, "License validated and stored successfully");
                        }
                    }

                    callback.onSuccess(json);
                } else {
                    callback.onError("Server returned error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "License validation failed", e);
                callback.onError("Exception: " + e.getMessage());
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
        sessionId = null;
        isInitialized = false;
        encKey = null;

        if (sessionManager != null) {
            sessionManager.recoverFromSessionError();
        }

        Log.d(TAG, "KeyAuth session reset with recovery");
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
