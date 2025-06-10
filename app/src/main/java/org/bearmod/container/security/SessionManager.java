package org.bearmod.container.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.UUID;

/**
 * Session manager to handle session conflicts and prevent multiple app instances
 */
public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREFS_NAME = "session_prefs";
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_SESSION_TIMESTAMP = "session_timestamp";
    private static final String KEY_APP_INSTANCE_ID = "app_instance_id";
    
    private static SessionManager instance;
    private SharedPreferences prefs;
    private Context context;
    private String currentInstanceId;
    private String currentSessionId;
    
    private SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.currentInstanceId = UUID.randomUUID().toString();
        Log.d(TAG, "SessionManager initialized with instance ID: " + currentInstanceId.substring(0, 8) + "...");
    }
    
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }
    
    /**
     * Check if another instance of the app is running
     */
    public boolean isAnotherInstanceRunning() {
        try {
            String storedInstanceId = prefs.getString(KEY_APP_INSTANCE_ID, null);
            long storedTimestamp = prefs.getLong(KEY_SESSION_TIMESTAMP, 0);
            long currentTime = System.currentTimeMillis();
            
            // If no stored instance or timestamp is older than 30 seconds, consider it safe
            if (storedInstanceId == null || (currentTime - storedTimestamp) > 30000) {
                return false;
            }
            
            // If stored instance ID is different from current, another instance might be running
            return !currentInstanceId.equals(storedInstanceId);
        } catch (Exception e) {
            Log.e(TAG, "Error checking instance status", e);
            return false;
        }
    }
    
    /**
     * Register current app instance
     */
    public void registerInstance() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_APP_INSTANCE_ID, currentInstanceId);
            editor.putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis());
            editor.apply();
            
            Log.d(TAG, "App instance registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register instance", e);
        }
    }
    
    /**
     * Update session heartbeat to indicate app is still active
     */
    public void updateHeartbeat() {
        try {
            prefs.edit().putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to update heartbeat", e);
        }
    }
    
    /**
     * Create a new session ID
     */
    public String createNewSession() {
        // Create a truly unique session ID with timestamp to avoid conflicts
        long timestamp = System.currentTimeMillis();
        String uniquePart = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        currentSessionId = (uniquePart + Long.toHexString(timestamp)).substring(0, 16);

        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_SESSION_ID, currentSessionId);
            editor.putLong(KEY_SESSION_TIMESTAMP, timestamp);
            editor.apply();
            Log.d(TAG, "New unique session created: " + currentSessionId.substring(0, 8) + "...");
        } catch (Exception e) {
            Log.e(TAG, "Failed to store session ID", e);
        }

        return currentSessionId;
    }
    
    /**
     * Get current session ID
     */
    public String getCurrentSessionId() {
        if (currentSessionId == null) {
            currentSessionId = prefs.getString(KEY_SESSION_ID, null);
        }
        return currentSessionId;
    }
    
    /**
     * Clear session data
     */
    public void clearSession() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_SESSION_ID);
            editor.remove(KEY_SESSION_TIMESTAMP);
            editor.remove(KEY_APP_INSTANCE_ID);
            editor.apply();
            
            currentSessionId = null;
            Log.d(TAG, "Session cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear session", e);
        }
    }
    
    /**
     * Force take over session (for handling session conflicts)
     */
    public void forceSessionTakeover() {
        try {
            // Clear any existing session data
            clearSession();
            
            // Register current instance
            registerInstance();
            
            // Create new session
            createNewSession();
            
            Log.d(TAG, "Session takeover completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to takeover session", e);
        }
    }
    
    /**
     * Check if current session is valid
     */
    public boolean isSessionValid() {
        try {
            String storedSessionId = prefs.getString(KEY_SESSION_ID, null);
            String storedInstanceId = prefs.getString(KEY_APP_INSTANCE_ID, null);
            
            return storedSessionId != null && 
                   currentInstanceId.equals(storedInstanceId) &&
                   storedSessionId.equals(currentSessionId);
        } catch (Exception e) {
            Log.e(TAG, "Error validating session", e);
            return false;
        }
    }
    
    /**
     * Get current instance ID
     */
    public String getCurrentInstanceId() {
        return currentInstanceId;
    }
    
    /**
     * Handle app pause (update timestamp)
     */
    public void onAppPause() {
        updateHeartbeat();
    }
    
    /**
     * Handle app resume (check for conflicts)
     */
    public void onAppResume() {
        updateHeartbeat();
        
        // Check if session is still valid
        if (!isSessionValid()) {
            Log.w(TAG, "Session validation failed on resume, creating new session");
            forceSessionTakeover();
        }
    }
    
    /**
     * Handle app termination
     */
    public void onAppTerminate() {
        // Don't clear session data on normal termination
        // This allows the app to resume with the same session
        updateHeartbeat();
        Log.d(TAG, "App terminating, session preserved");
    }

    /**
     * Recover from session errors by creating a fresh session
     */
    public void recoverFromSessionError() {
        try {
            Log.w(TAG, "Recovering from session error...");

            // Clear all session data completely
            clearSession();

            // Wait a moment to ensure cleanup
            try {
                Thread.sleep(1000); // Increased wait time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Create completely fresh instance and session with timestamp
            currentInstanceId = UUID.randomUUID().toString().replace("-", "");
            registerInstance();

            // Force create a new unique session
            createNewSession();

            Log.d(TAG, "Session recovery completed successfully with new instance: " +
                  currentInstanceId.substring(0, 8) + "...");
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover from session error", e);
        }
    }
}
