package org.bearmod.container.activity;


import static com.topjohnwu.superuser.internal.Utils.context;
import static org.bearmod.container.activity.SplashActivity.mahyong;
import static org.bearmod.container.server.ApiServer.URLJSON;
import static org.bearmod.container.server.ApiServer.getOwner;
import static org.bearmod.container.server.ApiServer.getTelegram;
import static org.bearmod.container.server.ApiServer.mainURL;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.cardview.widget.CardView;

import net_62v.external.MetaActivationManager;
import org.bearmod.container.BuildConfig;
import org.bearmod.container.Component.DownC;
import org.bearmod.container.Component.Prefs;
import org.bearmod.container.R;
import org.bearmod.container.utils.ActivityCompat;
import org.bearmod.container.utils.FLog;
import org.bearmod.container.KeyAuthAPIManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Locale;

import org.bearmod.container.utils.LanguageItem;
import org.bearmod.container.utils.LanguageSpinnerAdapter;
import org.bearmod.container.utils.myTools;
import org.bearmod.container.utils.AuthTestHelper;
import org.bearmod.container.utils.LicenseInputFormatter;
import org.bearmod.container.utils.LicenseFormatVerifier;
import org.json.JSONObject;
public class LoginActivity extends ActivityCompat {

    // Removed native library loading - using KeyAuth API instead
    private myTools m;
    private static final String QUESTION = "Q: %s";
    public static int REQUEST_OVERLAY_PERMISSION = 5469;
    private static final String USER = "USER";
    private static final String PASS = "PASS";
    public static String USERKEY, PASSKEY;
    LinearLayoutCompat btnSignIn;
    private Prefs prefs;
    private boolean isCardExpanded = false;

    private Spinner languageSpinner;
    private LicenseInputFormatter licenseFormatter;
    private EditText licenseInput;
    private ImageView pasteOrCut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLightStatusBar(this);
        m = new myTools(this);
        setContentView(R.layout.activity_login);
        if (!mahyong){
            finish();
            finishActivity(1);
        }
        initializeLanguageSpinner();

        // Initialize KeyAuth early to avoid session issues
        initializeKeyAuth();

        // Run authentication system tests in debug mode
        if (BuildConfig.DEBUG) {
            AuthTestHelper.runAllTests(this);
            // Verify 6-character license format (safe for production)
            LicenseFormatVerifier.verifyLicenseFormat();
        }

        initDesign();
        OverlayPermision();

    }

    /**
     * Initialize KeyAuth early to establish session and check for stored licenses
     */
    private void initializeKeyAuth() {
        // Initialize license manager with context
        KeyAuthAPIManager.initializeWithContext(this);

        if (!KeyAuthAPIManager.isInitialized()) {
            KeyAuthAPIManager.init(new KeyAuthAPIManager.InitCallback() {
                @Override
                public void onInitSuccess() {
                    Log.d("LoginActivity", "KeyAuth initialized successfully");
                    // Check for stored valid license after initialization
                    checkStoredLicense();
                }

                @Override
                public void onInitError(String error) {
                    Log.e("LoginActivity", "KeyAuth initialization failed: " + error);

                    // Handle session-related initialization errors
                    if (error.toLowerCase().contains("session") ||
                        error.toLowerCase().contains("use latest code") ||
                        error.toLowerCase().contains("only have app opened")) {

                        Log.w("LoginActivity", "Session-related init error, clearing session and retrying...");

                        // Clear session and try once more
                        KeyAuthAPIManager.clearSession();

                        // Retry initialization after a short delay
                        final KeyAuthAPIManager.InitCallback retryCallback = this;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!KeyAuthAPIManager.isInitialized()) {
                                KeyAuthAPIManager.init(retryCallback); // Retry with same callback
                            }
                        }, 1000);

                        return;
                    }

                    // Don't show error dialog here, let the login attempt handle it
                }
            });
        } else {
            // Already initialized, check for stored license
            checkStoredLicense();
        }
    }

    /**
     * Check for stored valid license and auto-login if available
     */
    private void checkStoredLicense() {
        KeyAuthAPIManager.checkStoredLicense(new KeyAuthAPIManager.LicenseCheckCallback() {
            @Override
            public void onValidLicense(JSONObject userInfo) {
                runOnUiThread(() -> {
                    Log.d("LoginActivity", "Valid stored license found, auto-logging in");
                    Toast.makeText(LoginActivity.this, "Welcome back! Auto-logging in...", Toast.LENGTH_SHORT).show();

                    // Auto-login with stored license
                    new DownC(LoginActivity.this).execute(URLJSON(), mainURL());
                    if(!Shell.rootAccess()) {
                        try {
                            MetaActivationManager.activateSdk((new Object() {
                                int t;
                                public String toString() {
                                    byte[] buf = new byte[20];
                                    t = -890156797;
                                    buf[0] = (byte) (t >>> 8);
                                    t = 1128011527;
                                    buf[1] = (byte) (t >>> 24);
                                    t = 911245763;
                                    buf[2] = (byte) (t >>> 14);
                                    t = -1249206879;
                                    buf[3] = (byte) (t >>> 11);
                                    t = -634720750;
                                    buf[4] = (byte) (t >>> 15);
                                    t = -1593699817;
                                    buf[5] = (byte) (t >>> 3);
                                    t = 1632999894;
                                    buf[6] = (byte) (t >>> 12);
                                    t = 1067074683;
                                    buf[7] = (byte) (t >>> 11);
                                    t = -1409628443;
                                    buf[8] = (byte) (t >>> 5);
                                    t = 823877935;
                                    buf[9] = (byte) (t >>> 15);
                                    t = -1705352434;
                                    buf[10] = (byte) (t >>> 11);
                                    t = -324639709;
                                    buf[11] = (byte) (t >>> 9);
                                    t = -1518002790;
                                    buf[12] = (byte) (t >>> 20);
                                    t = 589873750;
                                    buf[13] = (byte) (t >>> 10);
                                    t = -1493830540;
                                    buf[14] = (byte) (t >>> 4);
                                    t = 1632763232;
                                    buf[15] = (byte) (t >>> 18);
                                    t = 1316187076;
                                    buf[16] = (byte) (t >>> 17);
                                    t = -108566584;
                                    buf[17] = (byte) (t >>> 19);
                                    t = -1616344701;
                                    buf[18] = (byte) (t >>> 15);
                                    t = 695584914;
                                    buf[19] = (byte) (t >>> 23);
                                    return new String(buf);
                                }
                            }.toString()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onInvalidLicense() {
                Log.d("LoginActivity", "No valid stored license found, user needs to login");
                // Continue with normal login flow
            }

            @Override
            public void onError(String error) {
                Log.e("LoginActivity", "Error checking stored license: " + error);
                // Continue with normal login flow
            }
        });
    }

    public void initDesign(){

        prefs = Prefs.with(this);
        final Context m_Context = this;
        final EditText textUsername = findViewById(R.id.textUsername);
        final EditText textPassword = findViewById(R.id.textPassword);

        // Initialize enhanced license input
        licenseInput = findViewById(R.id.licenseInput);
        licenseFormatter = new LicenseInputFormatter(licenseInput);

        // Setup license validation listener
        licenseFormatter.setValidationListener(new LicenseInputFormatter.LicenseValidationListener() {
            @Override
            public void onValidLicense(String license) {
                // License format is valid, update legacy fields for compatibility
                textUsername.setText(license);
                textPassword.setText(license);
                // Update paste/cut icon
                pasteOrCut.setImageResource(R.drawable.ic_close);
                // Visual feedback could be added here (e.g., green border)
            }

            @Override
            public void onInvalidLicense(String license) {
                // Show error feedback for invalid license
                if (license.length() >= 41) { // Only show error for complete but invalid licenses (6-char segments)
                    Toast.makeText(m_Context, getString(R.string.license_invalid), Toast.LENGTH_SHORT).show();
                    // Auto-clear after a delay
                    licenseInput.postDelayed(() -> {
                        licenseFormatter.clearLicense();
                        Toast.makeText(m_Context, getString(R.string.license_cleared), Toast.LENGTH_SHORT).show();
                        licenseInput.requestFocus();
                    }, 2000);
                }
            }

            @Override
            public void onLicenseCleared() {
                // Clear legacy fields when license is cleared
                textUsername.setText("");
                textPassword.setText("");
                // Update paste/cut icon
                pasteOrCut.setImageResource(R.drawable.ic_paste);
            }
        });

        pasteOrCut = findViewById(R.id.paste);

        // Load saved license and format it properly
        String savedLicense = prefs.read(USER, "");
        if (!savedLicense.isEmpty()) {
            licenseFormatter.setLicenseText(savedLicense);
        }

        // Set the correct icon during initialization
        if (!licenseInput.getText().toString().isEmpty()) {
            pasteOrCut.setImageResource(R.drawable.ic_close); // Show cut icon if text is filled
        } else {
            pasteOrCut.setImageResource(R.drawable.ic_paste); // Show paste icon if text is empty
        }
        btnSignIn = findViewById(R.id.loginBtn);
        btnSignIn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String licenseKey = licenseFormatter.getFormattedLicense().trim();

                        if (!licenseKey.isEmpty()) {
                            // Validate license format before proceeding
                            if (LicenseInputFormatter.isValidLicenseFormat(licenseKey)) {
                                // Save the formatted license
                                prefs.write(USER, licenseKey);
                                prefs.write(PASS, licenseKey);

                                // Use the formatted license for authentication
                                Login(LoginActivity.this, licenseKey);
                                USERKEY = licenseKey;
                                PASSKEY = licenseKey;
                            } else {
                                // Show error for invalid license format
                                Toast.makeText(m_Context, getString(R.string.license_invalid), Toast.LENGTH_LONG).show();
                                licenseInput.requestFocus();
                                return;
                            }
                        } else {
                            // Show error for empty license
                            Toast.makeText(m_Context, "Please enter a valid license key", Toast.LENGTH_SHORT).show();
                            licenseInput.requestFocus();
                            return;
                        }

                        if (textUsername.getText().toString().isEmpty()
                                && textPassword.getText().toString().isEmpty()) {
                            textUsername.setError(getString(R.string.please_enter_username));
                            textPassword.setError(getString(R.string.please_enter_password));
                        }
                        if (textUsername.getText().toString().isEmpty()) {
                            textUsername.setError(getString(R.string.please_enter_username));
                        }
                        if (textPassword.getText().toString().isEmpty()) {
                            textPassword.setError(getString(R.string.please_enter_password));
                        }
                    }
                });


        pasteOrCut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if license is already present
                if (!licenseInput.getText().toString().isEmpty()) {
                    // License is already present, perform "cut" action
                    licenseFormatter.clearLicense();

                    // Switch to paste icon
                    pasteOrCut.setImageResource(R.drawable.ic_paste);

                    Toast.makeText(m_Context, getString(R.string.license_cleared), Toast.LENGTH_SHORT).show();
                } else {
                    // Perform enhanced "paste" action
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                    if (clipboardManager != null && clipboardManager.getPrimaryClip() != null) {
                        ClipData clipData = clipboardManager.getPrimaryClip();
                        if (clipData.getItemCount() > 0) {
                            CharSequence pastedText = clipData.getItemAt(0).getText();
                            if (pastedText != null && pastedText.length() > 5) {
                                // Clean and format the pasted license
                                String cleanedLicense = licenseFormatter.cleanPastedLicense(pastedText.toString());

                                if (!cleanedLicense.isEmpty()) {
                                    licenseFormatter.setLicenseText(cleanedLicense);

                                    // Switch to cut icon
                                    pasteOrCut.setImageResource(R.drawable.ic_close);

                                    // Show success feedback
                                    Toast.makeText(m_Context, getString(R.string.license_paste_success), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(m_Context, getString(R.string.license_invalid), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(m_Context, getString(R.string.please_copy_licence_and_paste), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(m_Context, getString(R.string.clipboard_empty), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });


        LinearLayoutCompat getKey = findViewById(R.id.telegram);
        getKey.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(getOwner()));
                        startActivity(intent);
                    }
                });
        LinearLayoutCompat store = findViewById(R.id.store);
        store.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(getTelegram()));
                        startActivity(intent);
                    }
                });

    }

    private void initializeLanguageSpinner() {
        languageSpinner = findViewById(R.id.splang);

        // Create language options with display names
        ArrayList<LanguageItem> languageList = new ArrayList<>();
        languageList.add(new LanguageItem("🇮🇳 English", "en"));
        languageList.add(new LanguageItem("🇨🇳 中文", "zh"));
        languageList.add(new LanguageItem("🇸🇦 العربية", "ar"));
        languageList.add(new LanguageItem("🇷🇺 Русский", "ru"));

        // Create adapter with a nice looking dropdown
        LanguageSpinnerAdapter adapter = new LanguageSpinnerAdapter(this,
                android.R.layout.simple_spinner_item,
                languageList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        // Get the saved language or system default
        String currentLang = m.getSt("myKey", "mapLang", null);
        if (currentLang == null) {
            // If no saved language, use system's default language
            currentLang = Locale.getDefault().getLanguage();
        }

        // Set initial selection based on saved or system default language
        for (int i = 0; i < languageList.size(); i++) {
            if (languageList.get(i).getCode().equals(currentLang)) {
                languageSpinner.setSelection(i); // Set the default selection
                break;
            }
        }

        // Set selection listener for language change
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean isInitialSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Skip the first automatic selection
                if (isInitialSelection) {
                    isInitialSelection = false;
                    return;
                }

                LanguageItem selectedLanguage = (LanguageItem) parent.getItemAtPosition(position);
                String langCode = selectedLanguage.getCode();

                // If the selected language is different, change the language
                if (!langCode.equals(m.getSt("myKey", "mapLang", "en"))) {
                    m.setLocale(LoginActivity.this, langCode); // Update locale based on selection
                    m.setSt("myKey", "mapLang", langCode); // Save the selected language
                    recreate(); // Recreate activity to apply language change
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        languageSpinner.setBackground(getDrawable(R.drawable.custom_spinner_background));
    }


    private void setLightStatusBar(Activity activity) {
        activity.getWindow().setStatusBarColor(Color.parseColor("#FFFFFF"));
        activity.getWindow().setNavigationBarColor(Color.parseColor("#FFFFFF"));
    }

    public void OverlayPermision() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                builder.setMessage(R.string.please_allow_permision_floating);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface p1, int p2) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        }
    }




    private static void Login(final LoginActivity m_Context, final String userKey) {
        LayoutInflater inflater = LayoutInflater.from(m_Context);
        View viewloading = inflater.inflate(R.layout.animation_login, null);
        AlertDialog dialogloading =
                new AlertDialog.Builder(m_Context, 5)
                        .setView(viewloading)
                        .setCancelable(false)
                        .create();
        dialogloading.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogloading.show();

        final Handler loginHandler =
                new Handler() {
                    @SuppressLint("HandlerLeak")
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == 0) {
                            new DownC(m_Context).execute(URLJSON(), mainURL());
                            if(!Shell.rootAccess()) {
                                try {
                                    MetaActivationManager.activateSdk((new Object() {
   int t;
   public String toString() {
      byte[] buf = new byte[20];
      t = -890156797;
      buf[0] = (byte) (t >>> 8);
      t = 1128011527;
      buf[1] = (byte) (t >>> 24);
      t = 911245763;
      buf[2] = (byte) (t >>> 14);
      t = -1249206879;
      buf[3] = (byte) (t >>> 11);
      t = -634720750;
      buf[4] = (byte) (t >>> 15);
      t = -1593699817;
      buf[5] = (byte) (t >>> 3);
      t = 1632999894;
      buf[6] = (byte) (t >>> 12);
      t = 1067074683;
      buf[7] = (byte) (t >>> 11);
      t = -1409628443;
      buf[8] = (byte) (t >>> 5);
      t = 823877935;
      buf[9] = (byte) (t >>> 15);
      t = -1705352434;
      buf[10] = (byte) (t >>> 11);
      t = -324639709;
      buf[11] = (byte) (t >>> 9);
      t = -1518002790;
      buf[12] = (byte) (t >>> 20);
      t = 589873750;
      buf[13] = (byte) (t >>> 10);
      t = -1493830540;
      buf[14] = (byte) (t >>> 4);
      t = 1632763232;
      buf[15] = (byte) (t >>> 18);
      t = 1316187076;
      buf[16] = (byte) (t >>> 17);
      t = -108566584;
      buf[17] = (byte) (t >>> 19);
      t = -1616344701;
      buf[18] = (byte) (t >>> 15);
      t = 695584914;
      buf[19] = (byte) (t >>> 23);
      return new String(buf);
   }
}.toString()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            Toast.makeText(m_Context, "Login Successfully", Toast.LENGTH_SHORT).show();
                        } else if (msg.what == 1) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(m_Context, 5);
                            builder.setTitle(m_Context.getString(R.string.erorserver));
                            builder.setMessage(msg.obj.toString());
                            builder.setCancelable(false);
                            builder.setPositiveButton(
                                    "OK",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                           /// System.exit(0);
                                        }
                                    });
                            builder.show();
                        }
                        dialogloading.dismiss();
                    }
                };

        // ✅ Block login if session is still active elsewhere
        if (KeyAuthAPIManager.isSessionActiveElsewhere()) {
            Log.w("LoginActivity", "Session active elsewhere - blocking login");
            Message msg = new Message();
            msg.what = 1;
            msg.obj = "Another instance is already running. Please close other instances and try again.";
            loginHandler.sendMessage(msg);
            return;
        }

        // ✅ Call KeyAuth.init() before login
        if (!KeyAuthAPIManager.isInitialized()) {
            KeyAuthAPIManager.init(new KeyAuthAPIManager.InitCallback() {
                @Override
                public void onInitSuccess() {
                    // KeyAuth initialized successfully, now validate license
                    validateLicenseAfterInit(userKey, loginHandler);
                }

                @Override
                public void onInitError(String error) {
                    // Handle session-related initialization errors
                    if (error.toLowerCase().contains("session") ||
                        error.toLowerCase().contains("use latest code")) {

                        Log.w("LoginActivity", "Session-related init error during login, clearing session and retrying...");

                        // Clear session and try once more
                        KeyAuthAPIManager.clearSession();

                        // Retry initialization after a short delay
                        final KeyAuthAPIManager.InitCallback retryCallback = this;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!KeyAuthAPIManager.isInitialized()) {
                                KeyAuthAPIManager.init(retryCallback); // Retry with same callback
                            } else {
                                // If retry succeeds, proceed with license validation
                                validateLicenseAfterInit(userKey, loginHandler);
                            }
                        }, 1000);

                        return;
                    }

                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = "KeyAuth initialization failed: " + error;
                    loginHandler.sendMessage(msg);
                }
            });
        } else {
            // Already initialized, proceed with license validation
            validateLicenseAfterInit(userKey, loginHandler);
        }
    }

    /**
     * Helper method to validate license after KeyAuth initialization with HWID binding
     */
    private static void validateLicenseAfterInit(String userKey, Handler loginHandler) {
        // Use the new HWID-enabled validation method
        KeyAuthAPIManager.validateLicenseWithHWID(userKey, new KeyAuthAPIManager.AuthCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        Log.d("LoginActivity", "License validation successful with HWID binding");
                        loginHandler.sendEmptyMessage(0);
                    } else {
                        String message = response.optString("message", "Authentication failed");
                        Log.w("LoginActivity", "License validation failed: " + message);

                        // Check if it's a HWID-related error
                        if (message.toLowerCase().contains("hwid") || message.toLowerCase().contains("hardware")) {
                            message = "Device not authorized. This license is bound to a different device.";
                        }

                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = message;
                        loginHandler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    Log.e("LoginActivity", "Error parsing license validation response", e);
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = "Error parsing response: " + e.getMessage();
                    loginHandler.sendMessage(msg);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("LoginActivity", "License validation error: " + error);

                // Handle session-related errors by resetting session
                if (error.toLowerCase().contains("session not found") ||
                    error.toLowerCase().contains("session id not provided") ||
                    error.toLowerCase().contains("use latest code")) {

                    Log.w("LoginActivity", "Session error detected, resetting session...");

                    // Clear any existing session
                    KeyAuthAPIManager.resetSession();

                    // Show user-friendly message for session errors
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = "Session expired. Please restart the app and try again.";
                    loginHandler.sendMessage(msg);
                    return;
                }

                // Provide more user-friendly error messages
                String userMessage = error;
                if (error.toLowerCase().contains("network") || error.toLowerCase().contains("connection")) {
                    userMessage = "Network error. Please check your internet connection and try again.";
                }

                Message msg = new Message();
                msg.what = 1;
                msg.obj = userMessage;
                loginHandler.sendMessage(msg);
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            OverlayPermision();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            InstllUnknownApp();
        } else if (requestCode == REQUEST_MANAGE_UNKNOWN_APP_SOURCES) {
            if (!isPermissionGaranted()) {
                takeFilePermissions();
            }
        }
    }

    /**
     * Add logout functionality to clear stored license
     */
    public void logout() {
        KeyAuthAPIManager.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Clear saved credentials
        prefs.write(USER, "");
        prefs.write(PASS, "");

        // Restart the activity to show login screen
        recreate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        KeyAuthAPIManager.onAppPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        KeyAuthAPIManager.onAppResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Only reset session, don't clear stored license unless explicitly logging out
        // This allows the app to remember valid licenses across restarts
        KeyAuthAPIManager.resetSession();
    }

    // Removed native method - using KeyAuth API instead
}
