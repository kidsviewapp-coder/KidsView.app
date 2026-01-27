# Unity Ads Mediation Integration Guide
## KidsView Android App - Parent Mode Only

**Last Updated:** January 2025  
**App Version:** 1.0.2 (Build 10002)  
**AdMob SDK Version:** 24.8.0  
**Unity Ads Mediation Adapter:** 4.16.0.1

---

## ⚠️ IMPORTANT: Unity Ads Bidding Only

**Unity Ads no longer supports waterfall mediation.** As of January 31, 2026, waterfall mediation for Unity Ads is deprecated and no longer available. Unity Ads must be configured as a **bidding partner** in AdMob mediation.

---

## Overview

Unity Ads has been integrated into KidsView through **AdMob Mediation Bidding** for Parent Mode only. This integration allows AdMob to automatically include Unity Ads in real-time bidding auctions when serving ads in Parent Mode, while keeping Kids Mode completely ad-free.

### Key Points:
- ✅ **Kids Mode remains completely ad-free** - No code changes needed
- ✅ **Parent Mode automatically includes Unity Ads** - Through AdMob mediation
- ✅ **Existing ad logic unchanged** - All ad loading/showing methods work as before
- ✅ **COPPA-compliant** - Existing RequestConfiguration applies to Unity Ads
- ✅ **No manifest changes required** - Unity Ads works through AdMob

---

## 1. Gradle Dependencies

The Unity Ads mediation adapter has been added to `app/build.gradle.kts`:

```kotlin
// Unity Ads Mediation Adapter (for AdMob mediation)
// Unity Ads will be served through AdMob mediation in Parent Mode only
// Version 4.16.0.1+ required for full bidding support with all ad formats
implementation("com.google.ads.mediation:unity:4.16.0.1")
```

**Dependency Details:**
- **Adapter Version:** 4.16.0.1 (required for bidding support - waterfall is deprecated)
- **AdMob SDK:** 24.8.0 (already in use, supports bidding)
- **Minimum Android API:** 23 (app targets API 24+)
- **Mediation Type:** **Bidding only** (waterfall no longer supported for Unity Ads)
- **Compatibility:** Fully compatible with existing AdMob setup

---

## 2. AndroidManifest.xml

**No changes required!** Unity Ads mediation works through AdMob, so the existing manifest configuration is sufficient:

- ✅ `INTERNET` permission (already present)
- ✅ `ACCESS_NETWORK_STATE` permission (already present)
- ✅ `AD_ID` permission (already present for Android 13+)
- ✅ AdMob App ID metadata (already configured)

---

## 3. Code Implementation

### Current Implementation Status

**No code changes are required!** The existing ad loading logic automatically includes Unity Ads through AdMob mediation.

#### How It Works:

1. **AdMob Initialization** (`AdManager.init()`):
   - Sets COPPA-compliant RequestConfiguration
   - Initializes MobileAds SDK
   - Unity Ads adapter is automatically initialized as part of mediation

2. **Ad Loading** (Parent Mode only):
   ```java
   // Existing code - no changes needed
   AdManager.getInstance().loadInterstitialAd(context, isParentMode);
   AdManager.getInstance().loadRewardedAd(context, isParentMode);
   AdManager.getInstance().createBannerAd(context, isParentMode);
   ```
   - When `isParentMode = true`, AdMob requests ads
   - AdMob mediation automatically includes Unity Ads in real-time bidding auctions
   - Unity Ads will serve if it wins the auction (highest eCPM)

3. **Kids Mode Protection**:
   ```java
   // All ad methods check isParentMode first
   if (!isParentMode) {
       Log.d(TAG, "Ad load skipped - not in parent mode");
       return; // Early return - no ad requests
   }
   ```
   - ✅ Kids Mode never calls ad loading methods
   - ✅ No ad requests reach AdMob/mediation layer
   - ✅ Unity Ads adapter is never triggered in Kids Mode

### COPPA Compliance

The existing `RequestConfiguration` in `AdManager.init()` applies to Unity Ads:

```java
RequestConfiguration.Builder requestConfigBuilder = new RequestConfiguration.Builder()
    .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
    .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G);
```

**This configuration:**
- ✅ Applies to all mediation partners, including Unity Ads
- ✅ Ensures child-directed treatment
- ✅ Restricts ads to G-rated content
- ✅ Complies with COPPA requirements

---

## 4. AdMob Mediation Dashboard Configuration

### ⚠️ CRITICAL: Use Bidding, Not Waterfall

**Unity Ads must be configured as a bidding partner, not waterfall.** Waterfall mediation for Unity Ads is no longer supported.

### Step 1: Access AdMob Mediation

1. Sign in to [AdMob Console](https://apps.admob.com/)
2. Navigate to **Mediation** → **Mediation groups**
3. Select your mediation group (or create one if needed)

### Step 2: Add Unity Ads as Bidding Partner

1. Click **Add ad source** or **Edit** your existing mediation group
2. Search for **Unity Ads**
3. **IMPORTANT:** Select **Bidding** (not waterfall) when adding Unity Ads
4. Click **Add** to include Unity Ads as a bidding partner

### Step 3: Configure Unity Ads Placement IDs

For each ad format (Banner, Interstitial, Rewarded), you need to:

1. **Get Unity Ads Placement IDs:**
   - Sign in to [Unity Ads Dashboard](https://operate.dashboard.unity3d.com/)
   - Navigate to your project
   - Go to **Monetization** → **Placements**
   - Copy the Placement IDs for:
     - Banner ads
     - Interstitial ads
     - Rewarded video ads

2. **Add Placement IDs in AdMob:**
   - In AdMob Mediation, select Unity Ads
   - For each ad format, enter the corresponding Unity Ads Placement ID
   - Save the configuration

### Step 4: Bidding Configuration

**How Bidding Works:**
- All bidding partners (including Unity Ads) participate in real-time auctions
- AdMob automatically selects the highest eCPM bidder
- Unity Ads competes with AdMob and other bidding partners in each auction
- No manual ordering needed - bidding is automatic and optimized

**Note:** With bidding, Unity Ads can win auctions even if AdMob has fill, as long as Unity Ads offers a higher eCPM. This provides better revenue optimization than waterfall.

### Step 5: Privacy & Compliance Settings

1. In AdMob, go to **Privacy & messaging**
2. Add **Unity Ads** to your ad partners list for:
   - GDPR compliance (if applicable)
   - US state privacy regulations (if applicable)
3. Configure non-personalized ads if needed (already handled by RequestConfiguration)

---

## 5. Testing Unity Ads Integration

### Verify Adapter Initialization

After app launch, check Logcat for adapter initialization:

```
AdManager: AdMob SDK initialized - Status: [com.google.android.gms.ads, com.unity3d.ads]
AdManager: Adapter: com.unity3d.ads - State: READY, Description: ...
```

**Note:** The adapter will show as "READY" when properly configured for bidding.

### Test in Parent Mode

1. Switch to **Parent Mode** in the app
2. Navigate to screens with ads (banner, interstitial, rewarded)
3. Check Logcat for Unity Ads serving (if it wins the bidding auction):
   ```
   AdManager: ✅ Interstitial ad loaded successfully
   AdManager: Adapter: com.unity3d.ads - Serving ad (bidding)
   ```

**Note:** Unity Ads may not serve every time - it only serves when it wins the bidding auction (highest eCPM).

### Verify Kids Mode Protection

1. Switch to **Kids Mode**
2. Verify no ad requests are made:
   ```
   AdManager: Ad load skipped - not in parent mode
   ```
3. Confirm no Unity Ads adapter activity in Logcat

---

## 6. Troubleshooting

### Unity Ads Not Serving

**Possible Causes:**
1. **Configured as waterfall instead of bidding:**
   - ⚠️ **CRITICAL:** Unity Ads must be configured as **bidding partner**, not waterfall
   - Waterfall mediation for Unity Ads is deprecated and will not work
   - Verify in AdMob dashboard that Unity Ads is set up as bidding

2. **Placement IDs not configured in AdMob:**
   - Verify Placement IDs are added in AdMob Mediation dashboard
   - Check that Placement IDs match your Unity Ads project

3. **Unity Ads account not set up:**
   - Ensure Unity Ads account is active
   - Verify app is registered in Unity Ads dashboard

4. **AdMob or other partners winning auctions:**
   - Unity Ads only serves when it wins the bidding auction (highest eCPM)
   - This is normal bidding behavior - not every ad request will result in Unity Ads serving

**Solution:**
- Check AdMob Mediation reports for Unity Ads fill rate
- Verify Placement IDs are correct
- Ensure Unity Ads account is properly configured

### Adapter Not Initializing

**Check:**
1. Verify dependency is added: `com.google.ads.mediation:unity:4.16.0.1`
2. Sync Gradle and rebuild project
3. Check Logcat for initialization errors

**Solution:**
- Clean and rebuild project
- Verify internet connection (adapter downloads on first run)
- Check AdMob App ID is correct in manifest

### Ads Not Showing in Parent Mode

**Verify:**
1. App is in Parent Mode (`isParentMode = true`)
2. `BuildConfig.ADS_ENABLED = true` (not in alpha build)
3. AdMob SDK is initialized (check Logcat)

**Solution:**
- Check `AdManager.init()` is called in `MainActivity.onCreate()`
- Verify Parent Mode detection logic
- Check BuildConfig flags for current build type

---

## 7. Build Configuration

### Debug Build
- Uses test AdMob IDs
- Unity Ads will not serve (test mode)
- Adapter still initializes for testing

### Alpha Build
- `ADS_ENABLED = false`
- No ad SDK initialization
- Unity Ads adapter not loaded
- Perfect for Play Store alpha testing

### Release Build
- Uses production AdMob IDs
- Unity Ads serves through mediation
- Real ads and real revenue

---

## 8. Privacy & Compliance

### COPPA Compliance
- ✅ RequestConfiguration sets child-directed treatment
- ✅ Applies to Unity Ads through mediation
- ✅ G-rated content only

### GDPR Compliance
- ✅ Add Unity Ads to Privacy & messaging partners in AdMob
- ✅ Non-personalized ads configured via RequestConfiguration

### Kids Mode Protection
- ✅ No ads in Kids Mode (code-level protection)
- ✅ No ad requests reach mediation layer
- ✅ Unity Ads adapter never triggered in Kids Mode

---

## 9. Monitoring & Analytics

### AdMob Mediation Reports

1. Go to **AdMob Console** → **Mediation** → **Reports**
2. View Unity Ads performance:
   - Fill rate
   - eCPM
   - Revenue
   - Impressions

### Unity Ads Dashboard

1. Sign in to [Unity Ads Dashboard](https://operate.dashboard.unity3d.com/)
2. View ad performance and revenue
3. Monitor placement performance

---

## 10. Summary

### What Changed:
- ✅ Added Unity Ads mediation adapter dependency
- ✅ No code changes required
- ✅ No manifest changes required

### What Stays the Same:
- ✅ Kids Mode remains ad-free
- ✅ Parent Mode ad logic unchanged
- ✅ COPPA compliance maintained
- ✅ Existing ad loading/showing methods work as before

### Next Steps:
1. ✅ **Configure Unity Ads as BIDDING partner** (not waterfall) in AdMob Mediation dashboard
2. ✅ Add Unity Ads Placement IDs for bidding
3. ✅ Test ads in Parent Mode
4. ✅ Verify Kids Mode remains ad-free
5. ✅ Monitor bidding performance in AdMob reports

---

## Support & Resources

- **AdMob Mediation Unity Ads Guide:** https://developers.google.com/admob/android/mediation/unity
- **Unity Ads Documentation:** https://docs.unity.com/ads/
- **AdMob Support:** https://support.google.com/admob

---

**Integration Complete!** Unity Ads will now automatically participate in AdMob bidding auctions in Parent Mode only, while Kids Mode remains completely ad-free.

**Remember:** Unity Ads must be configured as a **bidding partner** in AdMob, not waterfall. Waterfall mediation for Unity Ads is no longer supported.
