# 🔐 Keystore Setup Guide for Bear-Container

## 🎯 Overview

This guide helps you set up your own signing keystore to replace the removed `Yadav.jks` development keystore.

## 🚨 Security Status

✅ **COMPLETED:**
- Removed insecure `Yadav.jks` keystore (not your signature)
- Removed `signing.properties` with plain text credentials
- Updated `.gitignore` to prevent future keystore commits
- Configured signing to use environment variables for CI/CD

## 📋 Setup Steps

### 1. ✅ Your Keystore is Ready

Your BearOwner keystore is already available at:
```
C:\Users\BearOwner\BearOwner.jks
```

### 2. Configure Local Development

The `signing.properties` file has been created with your keystore path.
**You need to update the passwords:**

```bash
# Edit signing.properties and replace the placeholder passwords:
storeFile=C:\\Users\\BearOwner\\BearOwner.jks
storePassword=YOUR_ACTUAL_KEYSTORE_PASSWORD
keyAlias=BearOwner
keyPassword=YOUR_ACTUAL_ALIAS_PASSWORD
```

### 3. Update KeyAuth Hash

```bash
# Get your certificate hash for KeyAuth registration
keytool -list -v -keystore BearOwner.jks -alias BearOwner

# Copy the SHA1 fingerprint (without colons)
# Update the hash in KeyAuthAPIManager.java line 30
```

### 4. GitHub Actions Setup

The `SIGNING_KEY` environment variable is already configured in GitHub secrets.
You need to:

1. Encode your keystore to base64:
   ```bash
   base64 -i BearOwner.jks | tr -d '\n' | pbcopy
   ```

2. Update GitHub secrets with:
   - `SIGNING_KEY` - Base64 encoded keystore
   - `SIGNING_KEY_PASSWORD` - Keystore password
   - `SIGNING_KEY_ALIAS` - Key alias (BearOwner)
   - `SIGNING_ALIAS_PASSWORD` - Alias password

## 🔧 Current Configuration

### Signing Behavior:
- **Production builds**: Uses GitHub environment variables
- **Debug builds**: Uses Android debug keystore
- **Local release builds**: Uses your `signing.properties`

### KeyAuth Configuration:
- **Debug builds**: Uses `DEBUG_HASH_PLACEHOLDER`
- **Release builds**: Uses production hash `0fcf16068e3c343f85d1abfb761c5609`

## ⚠️ Important Notes

1. **Never commit** your keystore or signing.properties to git
2. **Keep backups** of your keystore in a secure location
3. **Use strong passwords** for both keystore and key alias
4. **Update KeyAuth hash** to match your certificate
5. **Test locally** before pushing to production

## 🔍 Verification

After setup, verify:
- [ ] App builds successfully with your keystore
- [ ] KeyAuth authentication works with your certificate hash
- [ ] GitHub Actions can build and sign the app
- [ ] No sensitive files are committed to git

## 🆘 Troubleshooting

**Build fails with signing error:**
- Check `signing.properties` file exists and has correct paths
- Verify keystore passwords are correct

**KeyAuth authentication fails:**
- Ensure the hash in `KeyAuthAPIManager.java` matches your certificate
- Register the new hash with KeyAuth if needed

**GitHub Actions fails:**
- Verify all environment variables are set correctly
- Check the base64 encoding of your keystore is valid
