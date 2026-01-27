# Google Play Console Declarations - Version 1.0.2 (Build 10002)

**Release Date:** January 2026  
**Version:** 1.0.2  
**Version Code:** 10002

---

## 📋 Required Updates in Play Console

### 1. App Content → Data Safety Section

#### **Data Collection & Sharing**

**New Data Type Added:**
- **Watch-Time Data (Version 1.0.2+)**
  - **What data:** Base time, applied extra time, wallet time, time used today, last reset date, reset ad count
  - **Purpose:** Enable cloud sync of watch-time settings across devices, automatic midnight reset
  - **Collection:** Yes (for users on version 1.0.2+)
  - **Sharing:** No (stored in per-user Firebase collections, isolated by user ID)
  - **Location:** Firebase Firestore (`users/{userId}/watchTimeData/data`)
  - **Encryption:** Yes (Firebase Firestore encryption in transit and at rest)
  - **Deletion:** Users can delete app data, which removes all associated watch-time data

**Updated Data Types:**
- **App Activity (Time Usage Tracking)**
  - **Updated Purpose:** Now includes cloud sync for watch-time data (version 1.0.2+)
  - **Collection:** Yes (locally for all versions, cloud sync for 1.0.2+)
  - **Sharing:** No

#### **Data Security**

- **Data Encryption:** 
  - ✅ All data encrypted in transit (HTTPS/TLS)
  - ✅ Firebase Firestore data encrypted at rest
  - ✅ Local authentication data encrypted using Android Keystore

- **Data Deletion:**
  - Users can delete app data through Android Settings → Apps → KidsView → Clear Data
  - Firebase data can be deleted by uninstalling app (if user requests, we can delete from Firebase console)

---

### 2. App Content → Privacy Policy

**Action Required:**
- ✅ Update Privacy Policy URL to point to the updated privacy policy
- ✅ Ensure the privacy policy reflects:
  - New watch-time system (1 hour default, 2 hours max)
  - Wallet system (15 minutes per ad, up to 3 hours)
  - Firebase cloud sync for watch-time data
  - Automatic midnight reset
  - Reset after usage feature (3 consecutive ads)

**Privacy Policy Location:**
- Internal: `PRIVACY_POLICY_COMPLIANT.md`
- External: `PRIVACY_POLICY_COMPLIANT.html` (host this on your website)

---

### 3. App Content → Families Policy

**No Changes Required:**
- ✅ App still complies with Google Play Families Policy
- ✅ No ads shown in Kid Mode (KidsView ads only in Parent Mode)
- ✅ YouTube ads in embedded player are outside our control (disclosed in privacy policy)
- ✅ All ads are contextual and non-personalized
- ✅ Watch-time system is a parental control feature, not a monetization feature for children

---

### 4. App Content → Target Audience & Content

**No Changes Required:**
- ✅ Target audience remains: Parents managing children's video viewing
- ✅ Content rating remains appropriate
- ✅ No new sensitive content added

---

### 5. Pricing & Distribution

**No Changes Required:**
- ✅ App remains free
- ✅ No in-app purchases
- ✅ Monetization through ads (Parent Mode only)

---

### 6. Store Listing

**Recommended Updates:**

**What's New (Release Notes):**
```
Version 1.0.2 - Enhanced Watch-Time System

✨ New Features:
• Enhanced watch-time system with Firebase cloud sync
• Base daily watch-time: 1 hour (60 minutes)
• Maximum watch-time: 2 hours (120 minutes) per day
• Wallet system: Earn 15 minutes per rewarded ad
• Wallet can accumulate up to 3 hours independently
• Manual wallet application to increase daily watch-time
• Automatic midnight reset of watch-time and wallet
• Reset after usage: Watch 3 consecutive ads to reset when reaching 2 hours

🔧 Improvements:
• Quick time selection dropdown menu
• Custom minutes input for precise time settings
• Real-time wallet and used time display
• Automatic time adjustment when exceeding limits
• Improved time limit validation

🐛 Bug Fixes:
• Fixed wallet time updates when reducing time limit
• Fixed time limit validation
• Fixed applied time deduction logic
• Updated UI to reflect 15 minutes per ad (was showing 30 minutes)
```

**Short Description (if updating):**
- No changes required unless you want to mention "Cloud sync" feature

**Full Description (if updating):**
- Consider adding: "Watch-time settings sync across devices using secure Firebase cloud storage"

---

### 7. App Bundle → Release Management

**Release Information:**
- **Version Name:** 1.0.2
- **Version Code:** 10002
- **Release Type:** Production (or Alpha/Beta if testing first)

**Release Notes:**
- Use the "What's New" section above

---

### 8. App Bundle → Testing

**Testing Checklist:**
- ✅ Test watch-time system with Firebase sync
- ✅ Test wallet accumulation (15 minutes per ad)
- ✅ Test maximum limits (2 hours effective, 3 hours wallet)
- ✅ Test midnight reset functionality
- ✅ Test reset after usage (3 consecutive ads)
- ✅ Test backward compatibility (users on older versions)
- ✅ Test migration from local to Firebase system

---

### 9. App Bundle → Pre-launch Report

**Review Before Release:**
- ✅ Check for any new crashes or errors
- ✅ Verify Firebase rules are correctly configured
- ✅ Test on multiple devices and Android versions
- ✅ Verify privacy policy is accessible and up-to-date

---

### 10. App Bundle → Internal Testing / Closed Testing / Open Testing

**If Using Staged Rollout:**
- Start with Internal Testing (5-10% of users)
- Monitor Firebase console for any errors
- Check Crashlytics for new issues
- Gradually increase to 50%, then 100%

---

## 🔍 Key Points to Verify

### Firebase Configuration
- ✅ Firestore security rules updated for watch-time data
- ✅ Firebase project has sufficient quota for new data
- ✅ Crashlytics enabled and working
- ✅ Anonymous Authentication enabled

### Privacy Compliance
- ✅ Privacy policy updated and accessible
- ✅ Data Safety section reflects new watch-time data collection
- ✅ No new permissions required
- ✅ COPPA compliance maintained

### Functionality
- ✅ Watch-time system works correctly
- ✅ Wallet system works correctly
- ✅ Midnight reset works correctly
- ✅ Firebase sync works correctly
- ✅ Backward compatibility maintained

---

## 📝 Summary of Changes

### What Changed:
1. **Watch-Time System:**
   - Base time: 1 hour (was 30-60 minutes variable)
   - Maximum effective time: 2 hours (was 3 hours)
   - Wallet per ad: 15 minutes (was 30 minutes)
   - Wallet maximum: 3 hours (unchanged)

2. **Data Storage:**
   - New Firebase Firestore collection for watch-time data
   - Cloud sync across devices
   - Automatic midnight reset in cloud

3. **UI Improvements:**
   - Dropdown menu for quick time selection
   - Custom minutes input
   - Real-time wallet display

### What Stayed the Same:
- App structure and modes (Kid Mode / Parent Mode)
- Authentication system
- Video management
- Ad system (Parent Mode only)
- Privacy and security measures
- COPPA compliance

---

## ⚠️ Important Notes

1. **Backward Compatibility:**
   - Users on version 1.0.1 and earlier will continue using local storage
   - Migration to Firebase happens automatically on first launch of version 1.0.2+
   - No data loss during migration

2. **Firebase Costs:**
   - Monitor Firebase usage as watch-time data is now stored in cloud
   - Firestore read/write operations will increase
   - Consider setting up billing alerts

3. **Testing:**
   - Test migration from local to Firebase system
   - Test on devices with different Android versions
   - Test with users who have existing local data

4. **Rollout Strategy:**
   - Consider staged rollout (10% → 50% → 100%)
   - Monitor Firebase console for errors
   - Monitor Crashlytics for crashes
   - Monitor user feedback

---

## ✅ Pre-Release Checklist

- [ ] Version code updated to 10002
- [ ] Version name updated to 1.0.2
- [ ] Privacy policy updated (both .md and .html)
- [ ] Changelog updated
- [ ] Data Safety section updated in Play Console
- [ ] Release notes prepared
- [ ] Firebase rules updated and tested
- [ ] App tested on multiple devices
- [ ] Migration tested (local → Firebase)
- [ ] Backward compatibility verified
- [ ] No new crashes or errors
- [ ] Privacy policy URL updated in Play Console
- [ ] Ready for release

---

**Last Updated:** January 2026  
**Prepared By:** KidsView Development Team
