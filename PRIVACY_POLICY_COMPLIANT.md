# Privacy Policy
## KidsView – Parental Control App

**Last Updated:** January 2026  
**App Version:** 1.0.2 (Build 10002)  
**Application ID:** why.xee.kidsview  
**Platform:** Android

---

## 1. INTRODUCTION

KidsView ("we", "our", "us") is a parental control application designed to provide a safe video viewing experience for children. This Privacy Policy explains how we collect, use, store, and protect information when you use our app.

**By using KidsView, you acknowledge that you have read and understood this Privacy Policy. If you do not agree with this policy, please do not use our app.**

This app is designed to comply with:
- Children's Online Privacy Protection Act (COPPA)
- Google Play Families Policy
- YouTube API Services Terms of Service
- General Data Protection Regulation (GDPR) principles

---

## 2. APP STRUCTURE AND MODES

KidsView operates in two distinct modes with different privacy implications:

### 2.1 Kid Mode
- **Purpose:** Child-facing interface for viewing approved videos
- **Ads:** KidsView does not display its own ads in Kid Mode. YouTube may display ads within the embedded player according to YouTube's policies, which are outside KidsView's control.
- **Video Content:** Only individual YouTube videos are shown. Playlists are never displayed or accessible in Kid Mode.
- **Player Safety:** 
  - KidsView uses the officially supported MINIMAL player style provided by the YouTube Player API
  - KidsView does not modify or obscure YouTube-provided UI elements beyond options officially supported by the API
  - When a video finishes, the player automatically closes and returns to the video list
  - The player automatically closes when playback ends, which limits interaction with suggested videos
- **External Links:** Kid Mode does not intentionally include external links or navigation controls. The YouTube player automatically closes when videos end.
- **Data Collection:** Minimal, as described in Section 3.1

### 2.2 Parent Mode
- **Purpose:** Management interface for parents to approve content and manage settings
- **Access:** Protected by PIN or password authentication
- **Ads:** Contextual, non-personalized ads may be displayed (see Section 5)
- **External Links:** May redirect to YouTube app in certain error scenarios (see Section 4.2)
- **Data Collection:** As described in Section 3.2

---

## 3. INFORMATION WE COLLECT

### 3.1 Data Collected in Kid Mode

We collect minimal information necessary for app functionality:

**Video Requests:**
- When children request videos to be added, we store only the video name (e.g., "Peppa Pig Episode 1")
- This information is not personally identifiable and cannot be used to identify a child
- Video requests are stored in Firebase Firestore (cloud storage) to allow parents to review and approve them

**App Usage Data:**
- Time spent watching videos (stored locally on device only)
- Daily time limit tracking (stored locally on device only)
- Video viewing preferences (stored locally on device only)

**Device Information:**
- Basic device information (device type, Android OS version) for compatibility purposes
- This information is collected automatically and used only for app optimization

**What We Do NOT Collect in Kid Mode:**
- Personal information (names, email addresses, phone numbers)
- Location data
- Photos or files from the device
- Contact information
- YouTube account information
- Biometric data
- Audio or video recordings

### 3.2 Data Collected in Parent Mode

**Authentication Data (Stored Locally Only):**
- PIN (6 digits) or Password (8+ characters) for parent access
- Security questions and answers (3 questions) for PIN/Password recovery
- All authentication data is encrypted and stored locally on your device only

**App Preferences (Stored Locally or in Firebase):**
- Selected theme preference
- Time limit settings (daily watch time limit, default: 1 hour/60 minutes, maximum: 2 hours/120 minutes)
- Time usage tracking
- App lock status
- Unlocked themes
- Watch-time wallet data (timestamp and minutes from rewarded ads, 15 minutes per ad)
  - For users on version 1.0.2+: Stored in Firebase Firestore for cloud sync across devices
  - For users on older versions: Stored locally on device only
  - Wallet can accumulate up to 3 hours (180 minutes) independently
  - Applied wallet time (manually applied to increase daily watch-time)

**Video Management Data (Stored in Cloud - Firebase Firestore):**
- Videos saved by parents to approved lists
- Video categories created by parents
- Video request status (pending, approved, rejected)
- Rejection messages (optional messages when parents reject video requests)
- **Important:** All video and category data is stored in per-user collections (`users/{userId}/videos` and `users/{userId}/categories`). Each user has their own private, isolated data collection. Users can only access their own videos and categories. This ensures complete privacy and data isolation between users.

**Search Activity (Parent Mode Only):**
- When parents search for videos using YouTube Data API, search queries are sent to YouTube
- We do not store or log search queries
- Search results are temporarily cached locally for performance

### 3.3 Data Collected by Third-Party Services

**Firebase Services:**
- **Firestore:** Stores video requests, parent-selected videos, and watch-time data in per-user collections (see Section 3.1 and 3.2)
  - Each user's videos are stored in their own private collection: `users/{userId}/videos`
  - Each user's categories are stored in their own private collection: `users/{userId}/categories`
  - Watch-time data (for version 1.0.2+ users): Stored in `users/{userId}/watchTimeData/data`
    - Includes: base time, applied extra time, wallet time, time used today, last reset date, reset ad count
    - Enables cloud sync across devices
    - Automatic midnight reset handled in cloud
    - Complete data isolation: Users can only access their own data
  - Video requests are stored in a global collection but are associated with the user's anonymous ID
  - Complete data isolation: Users can only access their own data
- **Crashlytics:** Collects crash reports and error logs for app improvement
  - Includes device information (model, OS version)
  - Includes app version and build number
  - Includes anonymous user ID (Firebase Anonymous Auth UID)
  - Does not include personal information
- **Anonymous Authentication:** Creates an anonymous user ID for associating data
  - This ID is not linked to any personal information
  - Used for associating video requests, crash reports, and accessing per-user data collections
  - Each user gets a unique, permanent anonymous ID that ensures data isolation

**Google AdMob (Parent Mode Only):**
- Advertising ID (may be requested for ad delivery, including non-personalized ads, in accordance with Google Play policies)
- Ad interaction data (impressions, clicks)
- Device information for ad delivery
- All ads are contextual and non-personalized (see Section 5)

**YouTube Services:**
- When using YouTube Player API, YouTube may collect data according to their privacy policy
- When using YouTube Data API for search, YouTube processes search queries
- We do not collect or store YouTube account information
- We do not access YouTube user data

---

## 4. YOUTUBE INTEGRATION

### 4.1 YouTube Player API

KidsView uses the official YouTube Player API to embed and play videos within the app.

**How It Works:**
- Videos are played using YouTube's embedded player
- **Only Individual Videos:** The app only displays individual YouTube videos. Playlists are never shown or accessible.
- **YouTube Data API:** All video searches use `type=video` parameter only, ensuring only individual videos are returned
- KidsView does not modify, track, or intercept YouTube playback behavior
- KidsView does not control YouTube ads that may appear in the embedded player

**Kid Mode Player Safety:**
- **MINIMAL Player Style:** KidsView uses the officially supported MINIMAL player style provided by the YouTube Player API
- **Auto-Close on Video End:** When a video finishes playing in Kid Mode, the player automatically closes and returns to the video list
- **No Browsing:** The player automatically closes when playback ends, which limits interaction with suggested videos
- **No Playlist UI:** Playlists are never loaded or displayed. The player automatically closes at video completion
- **Compliance:** This implementation ensures compliance with Google Play Families Policy requirements
- **YouTube API Compliance:** KidsView does not modify or obscure YouTube-provided UI elements beyond options officially supported by the YouTube Player API. The player automatically closes when playback ends.

**Parent Mode Player:**
- Normal player style with controls
- Player stays open after video ends (normal behavior)
- User can interact normally with player controls

**YouTube UI Elements:**
- In Kid Mode: KidsView uses the officially supported MINIMAL player style. The player automatically closes when playback ends, which limits interaction with suggested videos
- In Parent Mode: Standard player controls are available
- YouTube branding may appear (required by YouTube API Terms of Service)
- Interaction with related or suggested videos is limited in Kid Mode through automatic player closure when playback ends

**YouTube Ads:**
- YouTube may display ads within the embedded player according to YouTube's policies
- KidsView does not remove, filter, or control YouTube ads
- YouTube ads are separate from KidsView's own ads (which only appear in Parent Mode)

### 4.2 Error Handling and Redirects

**In Parent Mode:**
- If a video cannot be played in the embedded player (e.g., due to embedding restrictions), the app may redirect users to the official YouTube app
- This redirect only occurs in Parent Mode, never in Kid Mode
- Before redirecting, an interstitial ad may be shown (Parent Mode only)
- The redirect opens the YouTube app with the video URL

**In Kid Mode:**
- KidsView does not intentionally trigger external redirects
- If a video cannot be played due to embedding restrictions, the app returns to the previous screen without opening external apps
- **Player Auto-Close:** When videos finish playing, the player automatically closes and returns to the video list, preventing any interaction with suggested videos
- **No Playlist Access:** Playlists are never displayed or accessible in Kid Mode
- **MINIMAL Player:** The player uses MINIMAL style with limited controls suitable for child safety to prevent browsing of suggested content

### 4.3 YouTube Data API

**Usage:**
- Used in Parent Mode only for searching YouTube videos
- **Video-Only Search:** All searches use `type=video` parameter, ensuring only individual videos are returned
- **No Playlists:** Playlists are never fetched, displayed, or accessible in the app
- Search queries are sent to YouTube's servers
- Search results are returned and displayed in the app
- We do not store or log search queries

**Content Filtering:**
- The app filters out any playlist items that might be returned in API responses
- Only individual videos with valid video IDs are processed and displayed
- Multiple layers of filtering ensure no playlists can appear in the app

**Data Collection:**
- We do not collect YouTube account information
- We do not access YouTube user data
- We do not store YouTube search history

**YouTube API Services Terms:**
- Our use of YouTube Data API and YouTube Player API is subject to YouTube API Services Terms of Service
- Users must also comply with YouTube Terms of Service when viewing videos

---

## 5. ADVERTISING (PARENT MODE ONLY)

### 5.1 Ad Display

**Where Ads Appear:**
- KidsView ads are displayed ONLY in Parent Mode
- KidsView does not display its own ads in Kid Mode. YouTube may display ads within the embedded player according to YouTube's policies, which are outside KidsView's control

**Ad Types:**
- **Banner Ads:** Displayed at the bottom of Parent Mode screens
- **Interstitial Ads:** Full-screen ads shown between screens (minimum 60 seconds between ads)
- **Rewarded Ads:** Optional ads that parents can watch to unlock premium themes or add bonus watch time to wallet (15 minutes per ad, can accumulate up to 3 hours)

### 5.2 Ad Characteristics

**Contextual and Non-Personalized:**
- All ads are contextual (based on app content, not user behavior)
- All ads are non-personalized (not based on user interests or browsing history)
- Ads comply with Google AdMob Families Policy
- Ads are suitable for family audiences

**Advertising ID:**
- The app may request the AD_ID permission when required by Google AdMob for ad delivery, including non-personalized ads, in accordance with Google Play policies
- Advertising ID is used by AdMob for ad delivery
- We do not use Advertising ID for tracking or profiling users

**Ad Data Collection:**
- AdMob may collect device information, ad interaction data, and Advertising ID
- This data is collected by Google AdMob, not by KidsView
- AdMob's data collection is governed by Google's Privacy Policy

### 5.3 Ad Frequency and Cooldown

- Interstitial ads have a minimum cooldown period of 60 seconds between displays
- This ensures reasonable spacing between ads and complies with AdMob policies
- Banner ads refresh automatically but do not interrupt user experience

---

## 6. DATA STORAGE

### 6.1 Local Storage (Device)

The following data is stored locally on your device using Android SharedPreferences:

**Authentication Data:**
- Parent PIN or password (encrypted)
- Security questions and answers (encrypted)

**App Settings:**
- Theme preferences
- Time limit settings
- Time usage tracking
- Bonus time data (timestamp and minutes from rewarded ads, expires after 8 hours)
- App lock status
- Unlocked themes
- Rejection read status

**Ad Cooldown Data:**
- Last ad display timestamp (for enforcing 60-second cooldown)

**Cache Data:**
- Temporarily cached YouTube search results (for performance)
- Video thumbnails (cached locally)

**Data Deletion:**
- All local data can be deleted by uninstalling the app
- Local data is not synced to cloud or other devices

### 6.2 Cloud Storage (Firebase Firestore)

The following data is stored in Firebase Firestore (Google's cloud database):

**Video Requests Collection (`video_requests`):**
- Request ID (UUID)
- Request type (ADD or DELETE)
- Video name (provided by child)
- Video ID (if applicable)
- Optional message from child
- Timestamp
- Status (PENDING, APPROVED, REJECTED)
- Rejection message (if rejected)
- Associated with anonymous Firebase user ID

**Per-User Video Collections (`users/{userId}/videos`):**
- Each user has their own private video collection
- Video ID
- Video title
- Thumbnail URL
- Duration
- Channel name
- Timestamp
- Quality setting
- Category ID (if assigned to a category)
- **Data Isolation:** Users can only access their own videos. Complete privacy and isolation between users.

**Per-User Category Collections (`users/{userId}/categories`):**
- Each user has their own private category collection
- Category ID
- Category name
- Description
- Icon and color preferences
- Video count
- Timestamp
- **Data Isolation:** Users can only access their own categories. Complete privacy and isolation between users.

**Data Security:**
- All data transmission to Firestore is encrypted using HTTPS
- Data is stored in Google's secure cloud infrastructure
- Access is controlled through Firebase security rules
- **Per-User Isolation:** Firebase security rules enforce that users can only access their own data collections
- Data is associated with an anonymous Firebase user ID (not linked to personal information)
- Each user's data is completely isolated from other users' data

**Data Retention:**
- Video requests are retained until deleted by parents
- Parent-selected videos are retained until deleted by parents (stored in user's private collection)
- Categories are retained until deleted by parents (stored in user's private collection)
- Parents can delete any data through the app interface
- When a user deletes their data, only their own data is deleted (other users' data remains unaffected)

---

## 7. CRASH REPORTING (FIREBASE CRASHLYTICS)

### 7.1 What Is Collected

Firebase Crashlytics collects the following information when the app crashes or encounters errors:

**Technical Information:**
- Device model and manufacturer
- Android OS version
- App version and build number
- Stack traces and error messages
- Log messages (non-sensitive only)

**User Identification:**
- Anonymous Firebase user ID (not linked to personal information)
- This ID is the same anonymous ID used for Firestore data association

**What Is NOT Collected:**
- Personal information (names, emails, phone numbers)
- Location data
- Video content or search queries
- Authentication credentials (PINs, passwords)

### 7.2 Purpose

Crash reporting is used solely for:
- Identifying and fixing app bugs
- Improving app stability
- Understanding technical issues

### 7.3 Data Handling

- Crash reports are sent to Google's Firebase Crashlytics service
- Data is processed according to Google's Privacy Policy
- We do not use crash reports for advertising or marketing purposes

---

## 8. PERMISSIONS

### 8.1 Required Permissions

**INTERNET:**
- Required for: API calls, ad loading, video playback, cloud data sync
- Used for: YouTube API, AdMob, Firebase services

**ACCESS_NETWORK_STATE:**
- Required for: Checking internet connectivity
- Used for: Determining when to show cached content vs. online content

**AD_ID (Android 13+):**
- Used for: Ad delivery by Google AdMob, including non-personalized ads, in accordance with Google Play policies
- Used for: Displaying contextual, non-personalized ads in Parent Mode

### 8.2 Permissions NOT Requested

We do NOT request the following permissions:
- Location (GPS, network location)
- Camera
- Microphone
- Contacts
- Storage (read/write external storage)
- Phone/SMS
- Calendar
- Biometric authentication (fingerprint, face recognition)

---

## 9. CHILDREN'S PRIVACY (COPPA COMPLIANCE)

### 9.1 COPPA Compliance Statement

KidsView is designed to comply with the Children's Online Privacy Protection Act (COPPA). We do not knowingly collect personal information from children under 13.

**Kid Mode Protections:**
- No ads displayed by KidsView (YouTube may display ads within the embedded player according to YouTube's policies)
- No intentional external links or redirects initiated by KidsView
- No personal information collection
- No behavioral tracking
- No user accounts or profiles
- **No Playlists:** Only individual videos are shown. Playlists are never displayed or accessible.
- **Player Safety:** KidsView uses the officially supported MINIMAL player style. Videos auto-close when finished, which limits access to suggested videos.

**Parental Controls:**
- Parents must approve all videos before children can watch them
- Parents control time limits and app settings
- Parents can review and delete all data
- Parent Mode is protected by authentication

### 9.2 Parental Consent

By using Parent Mode features (including video approval and settings management), parents provide consent for:
- Collection of video request data (video names only, not personal information)
- Storage of parent-selected videos in cloud
- Display of contextual, non-personalized ads in Parent Mode
- Crash reporting for app improvement

### 9.3 Data Minimization

We collect only the minimum information necessary for app functionality:
- Video names (not personal information)
- App preferences (stored locally)
- Technical data for app improvement (crash reports)

We do NOT collect:
- Names, email addresses, or phone numbers
- Location data
- Photos or files
- Contact information
- YouTube account information

---

## 10. THIRD-PARTY SERVICES AND THEIR POLICIES

### 10.1 Google Services

**Firebase (Firestore, Crashlytics, Anonymous Auth):**
- Privacy Policy: https://firebase.google.com/support/privacy
- Data Processing: Governed by Google's Privacy Policy

**Google AdMob:**
- Privacy Policy: https://support.google.com/admob/answer/6128543
- Families Policy: Compliant with AdMob Families Self-Certified Program
- Ad Delivery: Contextual, non-personalized ads only

**YouTube (Player API and Data API):**
- Privacy Policy: https://policies.google.com/privacy
- Terms of Service: https://www.youtube.com/static?template=terms
- API Services Terms: https://developers.google.com/youtube/terms/api-services-terms-of-service

### 10.2 Your Rights Regarding Third-Party Services

When using KidsView, you are also subject to:
- Google's Privacy Policy (for Firebase, AdMob, YouTube services)
- YouTube Terms of Service (when viewing YouTube videos)
- AdMob policies (for ads displayed in Parent Mode)

We recommend reviewing these policies to understand how third-party services handle your data.

---

## 11. DATA SECURITY

### 11.1 Security Measures

**Local Data:**
- Authentication data (PINs, passwords) is stored using Android's secure storage
- SharedPreferences data is stored in app-private storage
- No local data is accessible to other apps

**Cloud Data:**
- All data transmission is encrypted using HTTPS
- Data is stored in Google's secure cloud infrastructure (Firebase)
- Access is controlled through Firebase security rules
- Anonymous authentication is used (no personal credentials stored)

**Network Security:**
- All API calls use HTTPS encryption
- No data is transmitted over unencrypted connections

### 11.2 Limitations

While we implement security measures, no method of transmission or storage is 100% secure. We cannot guarantee absolute security of your data.

---

## 12. DATA RETENTION AND DELETION

### 12.1 Data Retention

**Local Data:**
- Retained until app is uninstalled or data is manually cleared
- Time usage data resets daily (based on time limit settings)

**Cloud Data:**
- Video requests: Retained until deleted by parents
- Parent-selected videos: Retained until deleted by parents
- Crash reports: Retained by Firebase according to their retention policy (typically 90 days)

### 12.2 Data Deletion

**How to Delete Data:**
- **Local Data:** Uninstall the app or clear app data through Android settings
- **Video Requests:** Delete individual requests through Parent Mode interface
- **Parent-Selected Videos:** Delete individual videos through Parent Mode interface
- **All Cloud Data:** Parents can request deletion directly from the app settings or by contacting us via email

**Deletion Requests:**
To request deletion of all data associated with your app installation:
- **In-App:** Use the data deletion option in Parent Mode settings (if available)
- **Email:** Contact us at kidsview.app@gmail.com
- Include: Request for data deletion
- We will delete all associated data within 30 days

---

## 13. YOUR RIGHTS

### 13.1 Parent Rights

As a parent using KidsView, you have the right to:

**Access:**
- View all video requests made by your child
- View all videos you have approved
- Review app settings and preferences

**Deletion:**
- Delete individual video requests
- Delete individual approved videos
- Request deletion of all data (see Section 12.2)

**Control:**
- Approve or reject video requests
- Set time limits
- Lock or unlock the app
- Manage video categories

**Opt-Out:**
- Disable ads by not using Parent Mode (ads only appear in Parent Mode)
- Note: Some features may not work if data collection is disabled

### 13.2 Exercising Your Rights

To exercise any of these rights:
- Use the app's built-in features (for most actions)
- Email us at: kidsview.app@gmail.com
- We will respond within 30 days

---

## 14. INTERNATIONAL DATA TRANSFERS

Data collected by KidsView may be processed and stored in:
- United States (Google Firebase servers)
- Other countries where Google operates data centers

Data transfers are governed by:
- Google's data processing agreements
- Standard contractual clauses (where applicable)
- Applicable data protection laws

---

## 15. CHANGES TO THIS PRIVACY POLICY

We may update this Privacy Policy from time to time to reflect:
- Changes in app functionality
- Changes in data collection practices
- Legal or regulatory requirements
- Third-party service updates

**Notification of Changes:**
- Significant changes will be announced in the app
- Updated policy will be posted with a new "Last Updated" date
- Continued use of the app after changes constitutes acceptance

**Review Policy:**
- We recommend reviewing this policy periodically
- Current version is always available in the app and at our privacy policy URL

---

## 16. CONTACT US

If you have questions, concerns, or requests regarding this Privacy Policy or our data practices:

**Email:** kidsview.app@gmail.com

**Play Store:** You can also provide feedback and rate the app through the Google Play Store. Use the "Rate Our App" option in Parent Mode settings to open the Play Store rating page.

**Response Time:** We aim to respond within 30 days

**Privacy Concerns:** We take privacy seriously and will address all concerns promptly

---

## 17. ACKNOWLEDGMENT

By using KidsView, you acknowledge that:
- ✅ You have read and understood this Privacy Policy
- ✅ You consent to the data practices described herein
- ✅ You are a parent or legal guardian (for Parent Mode access)
- ✅ You understand that Kid Mode is designed for children and is COPPA-compliant
- ✅ You understand that YouTube services are subject to YouTube's own policies
- ✅ You understand that ads in Parent Mode are provided by Google AdMob

---

## 18. EFFECTIVE DATE

This Privacy Policy is effective as of **January 1, 2026** and applies to all users of KidsView version 1.0.0 (Build 10000) and later versions.

---

## 19. ADDITIONAL INFORMATION

### 19.1 App Information

- **App Name:** KidsView
- **Version:** 1.0.0 (Build 10000)
- **Platform:** Android
- **Minimum Android Version:** Android 7.0 (API 24)
- **Target Android Version:** Android 14 (API 36)

### 19.2 Developer Information

- **Developer:** KidsView Development Team
- **Contact:** kidsview.app@gmail.com

---

**END OF PRIVACY POLICY**

---

*This Privacy Policy reflects the data collection, storage, and usage practices of KidsView as of January 1, 2026.*

*For the most current version of this policy, please refer to the in-app Privacy Policy screen or visit our privacy policy URL provided in the app.*

