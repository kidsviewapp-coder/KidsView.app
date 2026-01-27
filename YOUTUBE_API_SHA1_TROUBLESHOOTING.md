# YouTube Data API V3 - SHA1 Fingerprint Troubleshooting Guide

## Problem
After updating SHA1 fingerprints for alpha and release builds in Google Cloud Console, the app shows "invalid API key" errors and stops displaying videos.

## Root Cause
When you add SHA1 fingerprints to an API key with Android app restrictions, the API key will ONLY work from Android apps that:
1. Have the exact package name specified
2. Are signed with a certificate that matches one of the listed SHA1 fingerprints

If these don't match exactly, Google will reject the request with a 403 "invalid API key" error.

## Common Issues

### 1. Missing SHA1 for Debug Build
- If you only added alpha and release SHA1s, debug builds will fail
- **Solution**: Add debug SHA1 fingerprint as well (or test with alpha/release builds only)

### 2. Google Play App Signing
- If you use Google Play App Signing, Google re-signs your app with their certificate
- Your local release SHA1 won't match the Google Play SHA1
- **Solution**: Add the Google Play SHA1 from Play Console → Release → Setup → App integrity → App signing key certificate

### 3. Package Name Mismatch
- Package name must match exactly: `why.xee.kidsview`
- **Solution**: Double-check package name in API key restrictions matches exactly

### 4. API Key Restriction Type Conflict
- If API key has both Android restrictions AND HTTP referrer/IP restrictions, it will fail
- **Solution**: Use ONLY Android app restrictions for mobile apps

### 5. Propagation Delay
- Changes can take a few minutes to propagate (usually 1-5 minutes)
- **Solution**: Wait a few minutes and test again

## Step-by-Step Fix

### Step 1: Get Correct SHA1 Fingerprints

#### For Debug Build:
```bash
cd android
./gradlew signingReport
```
Look for `Variant: debug` and copy the SHA1 value.

#### For Release/Alpha Build (without Google Play App Signing):
```bash
keytool -list -v -keystore path/to/your/keystore.jks -alias your_alias
```
Enter your keystore password and copy the SHA1 value.

#### For Release Build (with Google Play App Signing):
1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Go to **Release** → **Setup** → **App integrity**
4. Under **App signing key certificate**, copy the **SHA-1 certificate fingerprint**

### Step 2: Update API Key Restrictions in Google Cloud Console

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to **APIs & Services** → **Credentials**
3. Click on your YouTube Data API key
4. Under **Application restrictions**, select **Android apps**
5. Click **Add an item** for each build variant:
   
   For **Debug**:
   - Package name: `why.xee.kidsview`
   - SHA-1 certificate fingerprint: `[your debug SHA1]`
   
   For **Alpha**:
   - Package name: `why.xee.kidsview`
   - SHA-1 certificate fingerprint: `[your alpha SHA1]`
   
   For **Release** (if using Google Play App Signing):
   - Package name: `why.xee.kidsview`
   - SHA-1 certificate fingerprint: `[Google Play SHA1 from Play Console]`
   
   For **Release** (if NOT using Google Play App Signing):
   - Package name: `why.xee.kidsview`
   - SHA-1 certificate fingerprint: `[your release keystore SHA1]`

6. **IMPORTANT**: Remove any HTTP referrer or IP address restrictions (these conflict with Android restrictions)
7. Click **Save**

### Step 3: Wait for Propagation
- Wait 2-5 minutes for changes to propagate
- Clear app cache/data or reinstall the app
- Test the API again

### Step 4: Verify Configuration

Check that:
- ✅ Package name matches exactly: `why.xee.kidsview`
- ✅ SHA1 fingerprints are correct (no extra spaces, correct format)
- ✅ All build variants you test have their SHA1 added
- ✅ API restrictions are set to "Android apps" (not HTTP referrer or IP)
- ✅ API key restrictions are enabled (not "None")
- ✅ YouTube Data API v3 is enabled for the project

## Quick Checklist

- [ ] Debug SHA1 added (if testing debug builds)
- [ ] Alpha SHA1 added (matches your alpha keystore)
- [ ] Release SHA1 added (Google Play SHA1 if using Play App Signing, or your keystore SHA1)
- [ ] Package name is exactly: `why.xee.kidsview`
- [ ] Application restrictions = "Android apps" (not HTTP referrer)
- [ ] No conflicting restrictions (HTTP referrer/IP restrictions removed)
- [ ] Waited 2-5 minutes after saving
- [ ] Cleared app cache/data or reinstalled app

## Testing

1. **Test with Debug Build:**
   ```bash
   ./gradlew installDebug
   ```

2. **Test with Alpha Build:**
   ```bash
   ./gradlew installAlpha
   ```

3. **Test with Release Build:**
   ```bash
   ./gradlew installRelease
   ```

## Additional Notes

- **Multiple API Keys**: Your app supports multiple API keys via `youtube.api.keys` in `local.properties`. Make sure ALL keys have the correct SHA1 restrictions configured.

- **Key Rotation**: If you're using multiple keys for rotation (configured in `local.properties`), ensure each key has the same SHA1 restrictions.

- **Firebase vs YouTube API**: The API key in `google-services.json` is for Firebase services, not YouTube API. YouTube API key is configured in `local.properties`.

## Still Not Working?

1. **Check API Key Restrictions:**
   - Temporarily set restrictions to "None" to test if the key itself works
   - If it works with "None", the issue is with SHA1/package name configuration
   - Remember to set restrictions back after testing!

2. **Verify API Key in App:**
   - Check `local.properties` file contains correct API key
   - Rebuild the app after changing `local.properties`
   - Check logs for the actual API key being used

3. **Check YouTube Data API v3 Status:**
   - Ensure YouTube Data API v3 is enabled in Google Cloud Console
   - Check if API quota is exceeded
   - Verify billing is enabled (if required)

4. **Check Logs:**
   - Look for 403 errors in logcat
   - Error message might indicate if it's a restriction issue

## Example: Getting SHA1 for Your Project

```bash
# For debug (works on Windows PowerShell)
cd "D:\Android_Dev\KidsVIew  Stable Version 2.2.1"
.\gradlew.bat signingReport

# Look for output like:
# Variant: debug
# Config: debug
# Store: C:\Users\...\.android\debug.keystore
# Alias: AndroidDebugKey
# SHA1: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD
```

Copy the SHA1 value (the one starting with AA:BB:CC...) and add it to your API key restrictions.

