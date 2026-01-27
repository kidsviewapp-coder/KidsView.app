# Google Play Console Reviewer Instructions

## App Overview
**KidsView** is a child-safe video streaming app with parental controls, time limits, and content filtering.

## Reviewer Access Instructions

### Initial Setup
1. **Launch the app** on your test device
2. On the **first launch**, you will see the **Parent Setup** screen
3. When prompted to set a **PIN or Password**, enter: **`791989`**
4. The app will automatically skip the setup process and grant full access

### What to Test

#### ✅ Core Features (All Unlocked in Reviewer Mode)
- **Kids Mode**: Browse and watch videos from curated content
- **Parent Mode**: Access settings, time limits, and parental controls
- **Time Management**: Set daily time limits (1-3 hours)
- **Screen Lock**: Lock the app with a timer
- **App Lock**: Lock the app with password/PIN
- **Premium Themes**: Change app appearance
- **Earned Time Wallet**: Earn time by watching ads (automatically granted in reviewer mode)

#### ✅ Parental Controls
- Time limit settings (1-180 minutes)
- Password/PIN change
- Security questions setup
- App lock toggle
- Screen lock feature

#### ✅ Content Features
- Video browsing by country (Pakistan, India, etc.)
- Category filtering (Educational, Comedy, Action, etc.)
- Video playback with child-friendly controls
- Search functionality

### Important Notes for Reviewers

1. **Ads**: In reviewer mode, all ads (rewarded, interstitial, banner) are automatically skipped. Features that normally require watching ads will unlock immediately.

2. **Time Limits**: You can test time limit functionality, but time restrictions are bypassed in reviewer mode.

3. **Authentication**: The reviewer PIN (`791989`) works for both PIN mode and Password mode.

4. **Session-Based**: Reviewer mode is active only for the current app session. Restarting the app will require entering the PIN again.

### Testing Checklist

- [ ] App launches successfully
- [ ] Reviewer PIN grants access on first launch
- [ ] Kids Mode displays content correctly
- [ ] Parent Mode settings are accessible
- [ ] Time limit features work
- [ ] Video playback functions properly
- [ ] No crashes or errors during normal use
- [ ] Content filtering works as expected
- [ ] Parental controls function correctly

### Troubleshooting

**If the reviewer PIN doesn't work:**
- Ensure you're entering it during the **initial setup** (first launch)
- The PIN is: `791989` (numeric only)
- If you've already completed setup, uninstall and reinstall the app to access the setup screen again

**If you encounter any issues:**
- Please note the specific screen/feature where the issue occurred
- Check if the issue persists after restarting the app
- All features should be accessible without watching ads in reviewer mode

### Contact
If you have any questions or encounter issues during review, please contact us through the Play Console developer contact form.

---

**Version**: 1.0.1 (Build 10001)  
**Package Name**: `why.xee.kidsview`  
**Minimum SDK**: 24  
**Target SDK**: 36


