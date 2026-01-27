# Free User Workflow - KidsView App

## Overview
KidsView is a **free app with ad-supported premium features**. Free users can access all core features and unlock premium features by watching rewarded ads.

---

## 1. Initial Setup (First Launch)

### Step 1: Launch the App
- User opens KidsView for the first time
- **Initial Setup Dialog** appears (cannot be dismissed)

### Step 2: Choose Authentication Method
User must choose between:
- **PIN Mode**: 6-digit numeric PIN
- **Password Mode**: 8+ characters with at least one number and one special character (@#$%&*!)

### Step 3: Set PIN or Password
- **PIN**: Enter 6 digits (cannot be weak PINs like 123456, 000000, etc.)
- **Password**: Enter password meeting requirements:
  - Minimum 8 characters
  - At least one numeric digit (0-9)
  - At least one special character (@#$%&*!)

### Step 4: Confirm PIN/Password
- Re-enter the same PIN/Password to confirm
- If mismatch, error message appears

### Step 5: Set Security Questions (Optional but Recommended)
- User sets 3 security questions and answers
- Used for password recovery if PIN/Password is forgotten
- Questions are selected from predefined list

### Step 6: Setup Complete
- App navigates to main screen
- User can now access **Kids Mode** and **Parent Mode**

---

## 2. Daily Usage Flow

### Accessing the App
1. **Launch App** → User sees **Kids Mode** or **Parent Mode** selection screen
2. **Enter PIN/Password** → Required to access Parent Mode
3. **Kids Mode** → No authentication needed (if app is not locked)

### Kids Mode Features (Always Free)
- ✅ Browse videos by country (Pakistan, India, etc.)
- ✅ Filter by category (Educational, Comedy, Action, etc.)
- ✅ Watch videos
- ✅ Search functionality
- ✅ Time limit tracking (if enabled by parent)

### Parent Mode Features (Free Access)
- ✅ View settings
- ✅ Set time limits (1-180 minutes)
- ✅ Change PIN/Password
- ✅ View time usage statistics
- ✅ Manage security questions
- ✅ Contact developer
- ✅ Rate app
- ✅ View privacy policy

---

## 3. Ad-Gated Premium Features

Free users can unlock premium features by **watching rewarded ads**. Each feature requires watching a full ad to completion.

### Feature 1: Screen Lock (24-Hour Access)
**What it does**: Prevents kids from exiting the app without parent PIN/Password

**How to unlock**:
1. Go to **Parent Settings** → **Screen Lock** section
2. Tap **"Enable Feature with Ad (Disables in 24h)"** button
3. **Watch rewarded ad** to completion
4. Feature is enabled for **24 hours**
5. After 24 hours, feature expires and requires another ad to re-enable

**After unlocking**:
- Toggle the **Screen Lock switch** ON to lock the app
- **Each time you toggle ON**, you must watch a rewarded ad
- Toggling OFF does not require an ad

**Note**: If ad fails to load or user doesn't complete it, feature remains locked.

---

### Feature 2: Premium Themes
**What it does**: Unlocks custom app themes (beyond the free "System" theme)

**How to unlock**:
1. Go to **Parent Settings** → **Premium Themes** section
2. Browse available themes (locked themes show 🔒 icon and "Watch ad" text)
3. Tap on a **locked theme**
4. **Watch rewarded ad** to completion
5. Theme is permanently unlocked for that user

**After unlocking**:
- Theme can be selected and applied immediately
- Theme remains unlocked permanently (no expiration)
- User can unlock multiple themes by watching ads for each

**Note**: "System" theme is always free and does not require an ad.

---

### Feature 3: Earn Time to Wallet
**What it does**: Adds 15 minutes to the "Earned Time Wallet" (does not auto-apply to daily limit)

**How to earn**:
1. Go to **Parent Settings** → **Time Limit** section
2. Scroll to **"Earn +15 minutes to wallet (Watch ad)"** button
3. Tap the button
4. **Watch rewarded ad** to completion
5. 15 minutes are added to the wallet

**Using earned time**:
- Earned time is stored in a **wallet** (does not automatically apply)
- User must manually apply wallet time to increase daily watch-time
- Wallet can accumulate up to **3 hours (180 minutes)** independently
- Maximum effective watch-time (base + applied wallet) is **2 hours (120 minutes)**
- Applied wallet time is deducted from wallet
- Wallet time can accumulate (watch multiple ads to build up wallet)

**Example**:
- Base limit: 60 minutes (1 hour)
- Wallet: 45 minutes (from 3 ads)
- User applies 60 minutes → Daily limit becomes 120 minutes (2 hours max)
- Wallet remaining: 0 minutes (45 minutes applied, 15 minutes came from base increase)

---

## 4. Time Limit System

### Setting Time Limits
1. Go to **Parent Settings** → **Time Limit** section
2. Toggle **"Enable Time Limit"** switch ON
   - **Note**: Toggling ON/OFF shows an **interstitial ad** (with cooldown)
3. Tap the time field or edit icon
4. Set daily limit using:
   - **Quick selection dropdown**: Choose from 1 hour (default), 1.5 hours, or 2 hours (maximum)
   - **Custom minutes input**: Enter specific minutes (60-120 minutes)
5. Time limit is saved

### How Time Tracking Works
- Timer **only counts when videos are playing** in Kids Mode
- Time resets daily at **12:00 AM (midnight)**
- Display shows: **"Used today: HH:MM / HH:MM | Wallet: X min"** (used / effective limit / wallet balance)

### Effective Time Limit
The effective limit includes:
- **Base limit** (default: 1 hour/60 minutes, can be set up to 2 hours/120 minutes)
- **Applied wallet time** (manually applied from wallet, up to 2 hours total effective time)

**Maximum Limits**:
- **Base time**: 1 hour (60 minutes) default, up to 2 hours (120 minutes) maximum
- **Effective time** (base + applied): Maximum 2 hours (120 minutes)
- **Wallet**: Can accumulate up to 3 hours (180 minutes) independently

**Example**:
- Base limit: 60 minutes (1 hour)
- Applied wallet time: 60 minutes
- **Effective limit**: 120 minutes (2 hours)
- Wallet remaining: 30 minutes

### Resetting Daily Time
**Normal Reset (Before Reaching 2 Hours)**:
1. Go to **Parent Settings** → **Time Limit** section
2. Tap **"Reset Time Used Today"** button
3. **Watch rewarded ad** to completion
4. Time used today is reset to 0
5. **Deduction logic**:
   - If wallet has 60+ minutes → Deducts 60 minutes from wallet
   - If wallet is empty or has <60 minutes → Deducts from applied earned time in kids mode
   - Time limit in kids mode is updated accordingly

**Reset After Usage (When Reaching 2 Hours)**:
- When user reaches **2 hours of used watch-time**, a message appears: "Watch 3 ads consecutively to reset your daily watch-time"
- User must watch **3 consecutive rewarded ads** (fully watched, no skips)
- After 3 consecutive ads → Daily watch-time resets to **2 hours (120 minutes)**
- Wallet remains unchanged (unless manually applied)
- Reset ad count resets at midnight

---

## 5. Support Development (Optional)

### Watch Ads to Support
1. Go to **Parent Settings** → **Support Us** section (top of screen)
2. Tap **"Support Development"** button
3. **Watch rewarded ad** to completion
4. No reward is granted (purely to support development)
5. Can be done multiple times (with cooldown)

---

## 6. Ad Types in the App

### Rewarded Ads (User-Initiated)
- **When shown**: User taps button to unlock feature or earn reward
- **User must watch**: Full ad to completion
- **Reward**: Feature unlocked or time added to wallet
- **If skipped**: No reward granted

**Features using rewarded ads**:
- Screen Lock (enable feature)
- Screen Lock (toggle ON)
- Premium Themes (unlock theme)
- Earn +15 minutes to wallet
- Reset Time Used Today
- Reset After Usage (3 consecutive ads when reaching 2 hours)

### Interstitial Ads (Automatic)
- **When shown**: Automatically between screens/actions
- **User can skip**: After a few seconds
- **No reward**: Just displays ad

**Triggers**:
- Toggling Time Limit ON/OFF (with cooldown)
- Navigating between screens (occasionally)

### Banner Ads (Always Visible)
- **Where shown**: Bottom of screen in various screens
- **Always visible**: No interaction required
- **No reward**: Just displays ad

---

## 7. Important Notes for Free Users

### ✅ What's Always Free
- All video content
- Kids Mode browsing and playback
- Parent Mode settings access
- Time limit management
- PIN/Password change
- Security questions
- System theme
- Basic app functionality

### 🔒 What Requires Ads
- Screen Lock feature (24-hour access per ad)
- Premium Themes (permanent unlock per theme)
- Earn time to wallet (15 minutes per ad)
- Reset daily time (requires ad)
- Reset after usage (3 consecutive ads when reaching 2 hours)

### ⏱️ Time Limits
- **Base daily limit**: 1 hour (60 minutes) default, up to 2 hours (120 minutes) maximum
- **Maximum effective time**: 2 hours (120 minutes) per day
- **Wallet maximum**: 3 hours (180 minutes) can be accumulated
- Timer only runs when videos are playing
- Resets at midnight automatically (time, wallet, and applied time all reset)
- Can earn extra time via ads (15 minutes per ad, stored in wallet)
- Wallet must be manually applied to increase daily watch-time

### 🔄 Ad Loading
- Ads may take a few seconds to load
- If ad doesn't load, user may need to retry
- App shows loading indicators while ad is loading
- Ad must be watched to completion to receive reward

### 💡 Tips
- Build up wallet time by watching multiple ads (15 minutes per ad, up to 3 hours)
- Apply wallet time when needed (doesn't auto-apply)
- Maximum effective watch-time is 2 hours (base + applied wallet)
- When you reach 2 hours of used time, watch 3 consecutive ads to reset
- Screen Lock expires after 24 hours (watch ad again to re-enable)
- Premium Themes are permanent once unlocked
- Support development by watching ads in "Support Us" section
- All time data (time used, wallet, applied time) resets at midnight automatically

---

## 8. Troubleshooting

### Ad Not Loading
- Check internet connection
- Wait a few seconds and try again
- Ads may take 5-10 seconds to load

### Feature Not Unlocking
- Ensure you watched the ad to completion
- Do not close the ad early
- Try again if ad was skipped

### Time Limit Not Working
- Ensure "Enable Time Limit" is ON
- Timer only counts during video playback
- Check if time has reset at midnight

### Forgot PIN/Password
- Use security questions to recover
- If security questions not set, may need to reinstall app (data will be lost)

---

## Summary

**KidsView is a free app** where:
- ✅ Core features are always free
- 🔒 Premium features unlock via rewarded ads
- ⏱️ Time limits help manage screen time (1 hour default, 2 hours max)
- 💰 Earn extra time by watching ads (15 minutes per ad, up to 3 hours in wallet)
- 🎨 Unlock themes permanently with ads
- 🔐 Screen Lock available with 24-hour access per ad
- ☁️ Watch-time settings sync across devices (version 1.0.2+)

**No subscription required** - all features accessible through watching ads!
