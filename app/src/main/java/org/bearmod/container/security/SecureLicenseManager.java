package org.bearmod.container.security;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * Secure license manager with HWID binding and encrypted storage
 */
public class SecureLicenseManager {
    private static final String TAG = "SecureLicenseManager";
    private static final String PREFS_NAME = "secure_license_prefs";
    private static final String KEY_LICENSE = "license_key";
    private static final String KEY_HWID = "hardware_id";
    private static final String KEY_EXPIRY = "license_expiry";
    private static final String KEY_USER_INFO = "user_info";
    private static final String KEY_LAST_VALIDATION = "last_validation";
    
    private static SecureLicenseManager instance;
    private SharedPreferences encryptedPrefs;
    private Context context;
    private String currentHWID;
    
    private SecureLicenseManager(Context context) {
        this.context = context.getApplicationContext();
        initializeEncryptedPrefs();
        this.currentHWID = generateHWID();
    }
    
    public static synchronized SecureLicenseManager getInstance(Context context) {
        if (instance == null) {
            instance = new SecureLicenseManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize encrypted shared preferences
     */
    private void initializeEncryptedPrefs() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
                    
            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            
            Log.d(TAG, "Encrypted preferences initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize encrypted preferences", e);
            // Fallback to regular SharedPreferences (less secure but functional)
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }
    
    /**
     * Generate unique Hardware ID for device binding
     */
    @SuppressLint("HardwareIds")
    public String generateHWID() {
        try {
            StringBuilder hwid = new StringBuilder();
            
            // Android ID (most reliable)
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId != null && !androidId.equals("9774d56d682e549c")) { // Avoid known bad Android ID
                hwid.append(androidId);
            }
            
            // Device information
            hwid.append(Build.MANUFACTURER);
            hwid.append(Build.MODEL);
            hwid.append(Build.DEVICE);
            hwid.append(Build.PRODUCT);
            
            // Build information for additional uniqueness
            hwid.append(Build.BOARD);
            hwid.append(Build.BRAND);
            
            // Create MD5 hash of the combined string
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(hwid.toString().getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String finalHWID = hexString.toString().toUpperCase();
            Log.d(TAG, "Generated HWID: " + finalHWID.substring(0, 8) + "...");
            return finalHWID;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate HWID", e);
            // Fallback to UUID based on device info
            String fallback = UUID.nameUUIDFromBytes((Build.MANUFACTURER + Build.MODEL + Build.DEVICE).getBytes()).toString();
            return fallback.replace("-", "").toUpperCase();
        }
    }
    
    /**
     * Store license information securely
     */
    public void storeLicense(String licenseKey, JSONObject userInfo, long expiryTimestamp) {
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            editor.putString(KEY_LICENSE, licenseKey);
            editor.putString(KEY_HWID, currentHWID);
            editor.putLong(KEY_EXPIRY, expiryTimestamp);
            editor.putString(KEY_USER_INFO, userInfo.toString());
            editor.putLong(KEY_LAST_VALIDATION, System.currentTimeMillis());
            editor.apply();
            
            Log.d(TAG, "License stored successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to store license", e);
        }
    }
    
    /**
     * Retrieve stored license key
     */
    public String getStoredLicense() {
        try {
            return encryptedPrefs.getString(KEY_LICENSE, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve license", e);
            return null;
        }
    }
    
    /**
     * Check if stored license is valid for current device
     */
    public boolean isStoredLicenseValid() {
        try {
            String storedLicense = getStoredLicense();
            if (storedLicense == null || storedLicense.isEmpty()) {
                return false;
            }
            
            // Check HWID binding
            String storedHWID = encryptedPrefs.getString(KEY_HWID, null);
            if (!currentHWID.equals(storedHWID)) {
                Log.w(TAG, "HWID mismatch - license bound to different device");
                clearStoredLicense(); // Clear invalid license
                return false;
            }
            
            // Check expiry
            long expiryTime = encryptedPrefs.getLong(KEY_EXPIRY, 0);
            if (expiryTime > 0 && System.currentTimeMillis() > expiryTime) {
                Log.w(TAG, "Stored license has expired");
                clearStoredLicense(); // Clear expired license
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to validate stored license", e);
            return false;
        }
    }
    
    /**
     * Clear stored license data
     */
    public void clearStoredLicense() {
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            editor.remove(KEY_LICENSE);
            editor.remove(KEY_HWID);
            editor.remove(KEY_EXPIRY);
            editor.remove(KEY_USER_INFO);
            editor.remove(KEY_LAST_VALIDATION);
            editor.apply();
            
            Log.d(TAG, "Stored license cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear license", e);
        }
    }
    
    /**
     * Get current device HWID
     */
    public String getCurrentHWID() {
        return currentHWID;
    }

    /**
     * ✅ Refresh HWID - handles reinstall/device change scenarios
     */
    public void refreshHWID() {
        try {
            String newHWID = generateHWID();
            if (!newHWID.equals(currentHWID)) {
                Log.w(TAG, "HWID changed - device reinstall or change detected");
                Log.d(TAG, "Old HWID: " + (currentHWID != null ? currentHWID.substring(0, 8) + "..." : "null"));
                Log.d(TAG, "New HWID: " + newHWID.substring(0, 8) + "...");

                // Clear any stored license as it's bound to old HWID
                clearStoredLicense();
                currentHWID = newHWID;

                Log.d(TAG, "HWID refreshed and stored license cleared");
            } else {
                Log.d(TAG, "HWID unchanged - device consistent");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to refresh HWID", e);
        }
    }
    
    /**
     * Get stored user information
     */
    public JSONObject getStoredUserInfo() {
        try {
            String userInfoStr = encryptedPrefs.getString(KEY_USER_INFO, null);
            if (userInfoStr != null) {
                return new JSONObject(userInfoStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve user info", e);
        }
        return null;
    }
    
    /**
     * Update last validation timestamp
     */
    public void updateLastValidation() {
        try {
            encryptedPrefs.edit().putLong(KEY_LAST_VALIDATION, System.currentTimeMillis()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to update last validation", e);
        }
    }
    
    /**
     * Check if license needs revalidation (every 24 hours)
     */
    public boolean needsRevalidation() {
        try {
            long lastValidation = encryptedPrefs.getLong(KEY_LAST_VALIDATION, 0);
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastValidation;
            
            // Revalidate every 24 hours
            return timeDiff > (24 * 60 * 60 * 1000);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check revalidation", e);
            return true; // Default to requiring revalidation
        }
    }
}
