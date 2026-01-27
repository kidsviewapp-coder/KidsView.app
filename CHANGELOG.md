# KidsView - Version Changelog

This file tracks all version changes, features, bug fixes, and improvements for the KidsView app.

**⚠️ CRITICAL: This file should NEVER be deleted and must be updated with every version change.**

---

## Version 1.0.0 (Build 10000) - Initial Release

**Release Date:** January 2026

### 🎉 Major Features

- **Dual Mode System**:
  - Kid Mode: Safe, child-friendly interface for watching approved videos
  - Parent Mode: Comprehensive parental controls protected by PIN/Password

- **Video Management**:
  - Search and add videos from YouTube
  - Custom video categories for organization
  - Video request system (kids can request videos to add/remove)
  - Per-user data isolation for complete privacy

- **Parental Controls**:
  - PIN (6 digits) or Password (8+ characters) authentication
  - Security questions for PIN/Password recovery
  - Daily time limits (0-60 minutes)
  - App lock feature (lock/unlock the app)
  - Time wallet system with rewarded ads for bonus time

- **Kids Mode Features**:
  - View only parent-approved videos
  - Request new videos to watch
  - Request removal of unwanted videos
  - Safe video player with auto-close functionality
  - No ads from KidsView (YouTube may show ads in embedded player)
  - Favorites system

- **Themes & Customization**:
  - Dark theme support
  - Premium themes (unlockable via rewarded ads)
  - Kid-friendly UI with smooth animations

- **Security & Privacy**:
  - Complete per-user data isolation
  - Encrypted local storage for authentication
  - Cloud storage for video data only
  - Comprehensive privacy policy
  - COPPA compliant

### 🔧 Technical Implementation

- **Architecture**: MVVM pattern with Jetpack Compose
- **Language**: Kotlin
- **Dependency Injection**: Hilt
- **Database**: Firebase Firestore (cloud), SharedPreferences (local)
- **Video Player**: YouTube Player API for Android
- **Ads**: Google AdMob (Banner, Interstitial, Rewarded)
- **APIs**: YouTube Data API v3, Firebase services

### 📱 Platform Support

- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 14 (API 36)

---

---

## Version 1.0.1 (Build 10001) - Reviewer Unlock Feature

**Release Date:** January 2026

### ✨ New Features

- **Google Play Reviewer Unlock System**:
  - Added session-only reviewer bypass for Google Play reviewers
  - Reviewer passkey/password: `791989` (works in both PIN and password modes)
  - When reviewer code is entered during initial setup, immediately skips setup flow
  - No authentication UI shown when reviewer mode is active
  - All parent-restricted features automatically unlocked for reviewers
  - Reviewer unlock is session-only (not persisted, resets on app restart)
  - No security questions required for reviewers
  - Reviewer code is never stored to disk

### 🔧 Technical Improvements

- **ReviewerUnlockManager**: New utility class for managing reviewer unlock state
- **InitialSetupDialog**: Enhanced with immediate reviewer code detection
- **ParentModeScreen**: Bypasses authentication UI when reviewer mode is active
- **PlayerScreen**: Bypasses YouTube controls unlock dialog for reviewers

### 🔒 Security

- Reviewer unlock is completely session-only (in-memory only)
- No reviewer credentials are persisted to SharedPreferences or database
- Reviewer unlock automatically resets on app restart
- Normal user security flow remains completely unchanged

---

## Version 1.0.2 (Build 10002) - Enhanced Watch-Time System with Firebase

**Release Date:** January 2026

### ✨ New Features

- **Enhanced Watch-Time System with Firebase Integration**:
  - Base daily watch-time: 1 hour (60 minutes) by default
  - Maximum effective watch-time: 2 hours (120 minutes) per day
  - Wallet system: Earn 15 minutes per rewarded ad (reduced from 30 minutes)
  - Wallet can accumulate up to 3 hours (180 minutes) independently
  - Manual wallet application: Parents can manually apply wallet time to increase daily watch-time
  - Automatic midnight reset: Daily watch-time, wallet, and applied time reset at 12:00 AM
  - Reset after usage: When user reaches 2 hours of used watch-time, they can watch 3 consecutive ads to reset daily watch-time to 2 hours
  - Firebase cloud sync: Watch-time data is synced across devices using Firebase Firestore
  - Backward compatibility: Existing users continue using local system until migration

- **Improved Time Selection UI**:
  - Quick selection dropdown menu for common time options (1 hour, 1.5 hours, 2 hours)
  - Custom minutes input button for manual entry
  - Automatic time adjustment when exceeding maximum limits
  - Real-time wallet time display with used time

### 🔧 Technical Improvements

- **WatchTimeManagerFirebase**: New Firebase-integrated watch-time management system
  - Atomic Firestore transactions for data consistency
  - Real-time sync across devices
  - Automatic midnight reset handling
  - Reset trigger detection (2 hours used time)
  - Wallet management with 15-minute increments
  - Applied time tracking and deduction

- **PreferencesManager**: Enhanced with Firebase migration support
  - Dual system support (local and Firebase)
  - Migration logic for seamless transition
  - Optimized wallet operations
  - Improved time limit validation

- **ParentSettingsScreen**: Enhanced time management UI
  - Dropdown menu for quick time selection
  - Custom minutes input dialog
  - Real-time wallet and used time display
  - Automatic time adjustment without user intervention
  - Improved error handling and user feedback

### 🐛 Bug Fixes

- Fixed wallet time not updating when time limit is reduced
- Fixed time limit validation to prevent applying more time than available
- Fixed base time reduction logic to correctly return only paid amount to wallet
- Fixed applied time deduction when wallet is empty during reset
- Fixed time picker options to match new maximum limits (2 hours)
- Fixed success messages to correctly reflect 15 minutes per ad (was showing 30 minutes)

### 🔒 Security & Privacy

- Watch-time data stored in Firebase Firestore with per-user isolation
- Atomic transactions prevent race conditions
- Local data remains encrypted and secure
- Migration preserves user privacy and data integrity

### 📊 Data Storage Updates

- **Firebase Firestore**: New collection `users/{userId}/watchTimeData/data` for watch-time sync
  - Stores: `baseTime`, `appliedExtraTime`, `walletTime`, `timeUsedToday`, `lastResetDate`, `resetAdCount`
  - Per-user data isolation maintained
  - Automatic midnight reset handled in cloud

---

**Last Updated:** January 2026  
**Current Version:** 1.0.2 (Build 10002)  
**Maintained By:** KidsView Development Team

