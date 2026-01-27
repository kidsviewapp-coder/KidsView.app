# Firebase Watch-Time System Integration Guide

## Overview

The `WatchTimeManagerFirebase` provides a Firebase-backed watch-time system with **full backward compatibility** for existing users. This guide explains how to integrate it into your KidsView app.

## Key Features

✅ **Backward Compatibility**: Existing users continue using local SharedPreferences  
✅ **Firebase Sync**: New/migrated users get cloud sync across devices  
✅ **Atomic Operations**: All Firebase updates use transactions to prevent race conditions  
✅ **Automatic Migration**: One-time migration preserves all user data  
✅ **Real-time Updates**: Firebase listeners enable cross-device synchronization  

## Architecture

### Dual System Approach

```
┌─────────────────────────────────────────────────────────┐
│              WatchTimeManagerFirebase                    │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────┐      ┌──────────────────┐        │
│  │  Local System    │      │  Firebase System │        │
│  │ (Backward Comp) │      │  (Cloud Sync)    │        │
│  │                  │      │                  │        │
│  │ SharedPreferences│      │  Firestore       │        │
│  └──────────────────┘      └──────────────────┘        │
│         │                           │                    │
│         └───────────┬───────────────┘                    │
│                     │                                    │
│            isMigratedToFirebase()                        │
│                     │                                    │
│         ┌───────────┴───────────┐                        │
│         │                       │                        │
│    false (Local)          true (Firebase)                │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## Integration Steps

### Step 1: Add Firebase Dependencies

Ensure you have Firebase Firestore in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
}
```

### Step 2: Initialize WatchTimeManagerFirebase

In your `Application` class or `MainActivity`:

```kotlin
class MainActivity : ComponentActivity() {
    private val watchTimeManager = WatchTimeManagerFirebase.getInstance(this)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check for midnight reset on app start
        lifecycleScope.launch {
            watchTimeManager.checkMidnightReset()
        }
    }
}
```

### Step 3: Migrate Users to Firebase (Optional)

Migrate users when they log in or opt-in:

```kotlin
// In your login/signup flow
lifecycleScope.launch {
    val success = watchTimeManager.migrateToFirebase()
    if (success) {
        // Show success message
        showMessage("Your data is now synced across devices!")
    } else {
        // Show error, user continues with local system
        showError("Sync failed. Using local storage.")
    }
}
```

### Step 4: Replace Existing Watch-Time Calls

Update your existing code to use `WatchTimeManagerFirebase`:

#### Before (Local Only):
```kotlin
// Old code
val preferencesManager = PreferencesManager.getInstance(context)
preferencesManager.addEarnedTimeToWallet()
```

#### After (Firebase-Compatible):
```kotlin
// New code - works for both local and Firebase users
val watchTimeManager = WatchTimeManagerFirebase.getInstance(context)
lifecycleScope.launch {
    watchTimeManager.watchAdForWallet()
}
```

### Step 5: Update Video Player Integration

```kotlin
class VideoPlayerViewModel : ViewModel() {
    private val watchTimeManager = WatchTimeManagerFirebase.getInstance(context)
    private var videoStartTime: Long = 0
    
    fun onVideoPlay() {
        viewModelScope.launch {
            // Check time limit
            if (watchTimeManager.isTimeLimitExceeded()) {
                _uiState.value = _uiState.value.copy(
                    isTimeLimitReached = true,
                    isVideoPlaying = false
                )
                return@launch
            }
            
            // Start timer
            videoStartTime = watchTimeManager.startTimer()
            _uiState.value = _uiState.value.copy(isVideoPlaying = true)
            
            // Periodic updates (every 10 seconds)
            startTimeTracking()
        }
    }
    
    private fun startTimeTracking() {
        viewModelScope.launch {
            while (_uiState.value.isVideoPlaying) {
                delay(10000) // 10 seconds
                watchTimeManager.addTimeUsed(10000)
                
                if (watchTimeManager.isTimeLimitExceeded()) {
                    onVideoPause()
                    break
                }
            }
        }
    }
    
    fun onVideoPause() {
        viewModelScope.launch {
            if (videoStartTime > 0) {
                watchTimeManager.stopTimer(videoStartTime)
                videoStartTime = 0
            }
            _uiState.value = _uiState.value.copy(isVideoPlaying = false)
        }
    }
}
```

### Step 6: Update UI Components

```kotlin
@Composable
fun TimeLimitDisplay() {
    val watchTimeManager = WatchTimeManagerFirebase.getInstance(LocalContext.current)
    var timeDisplay by remember { mutableStateOf("") }
    var isMigrated by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isMigrated = watchTimeManager.isMigratedToFirebase()
        while (true) {
            timeDisplay = watchTimeManager.getTimeDisplayString()
            delay(1000) // Update every second
        }
    }
    
    Column {
        Text(
            text = timeDisplay,
            style = MaterialTheme.typography.bodyLarge
        )
        if (isMigrated) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = "Synced to cloud"
            )
        }
    }
}
```

## Firebase Firestore Structure

### Collection: `users`
### Document: `{userId}`

```json
{
  "baseTime": 60,
  "appliedExtraTime": 30,
  "walletTime": 60,
  "timeUsedToday": 3600000,
  "lastResetDate": 1704067200000,
  "resetAdCount": 2,
  "resetAdStartTime": 1704067200000,
  "isMigratedToFirebase": true
}
```

## Backward Compatibility

### How It Works

1. **Migration Flag**: `isMigratedToFirebase` flag in SharedPreferences determines which system to use
2. **Default Behavior**: New users start with `isMigratedToFirebase = false` (local system)
3. **Migration**: When user logs in or opts-in, call `migrateToFirebase()`
4. **Dual Path**: All functions check `isMigratedToFirebase()` and route to appropriate system

### Existing Users

- Continue using local SharedPreferences
- No data loss
- No forced migration
- Can migrate anytime by calling `migrateToFirebase()`

### New Users

- Start with local system
- Can migrate to Firebase when they log in
- Get cloud sync benefits after migration

## Important Notes

### 1. Coroutines Required

All Firebase operations are `suspend` functions. Always use coroutines:

```kotlin
// ✅ Correct
lifecycleScope.launch {
    watchTimeManager.watchAdForWallet()
}

// ❌ Wrong - will not compile
watchTimeManager.watchAdForWallet() // Error: suspend function
```

### 2. Atomic Operations

Firebase operations use transactions to prevent race conditions:

```kotlin
// This is handled automatically - all updates are atomic
watchTimeManager.applyWalletTime(30) // Safe even with multiple devices
```

### 3. Real-time Sync

To enable real-time sync across devices, add a Firestore listener:

```kotlin
firestore.collection("users")
    .document(userId)
    .addSnapshotListener { snapshot, error ->
        // Update UI when data changes on another device
    }
```

### 4. Error Handling

Always handle potential Firebase errors:

```kotlin
lifecycleScope.launch {
    try {
        val success = watchTimeManager.watchAdForWallet()
        if (!success) {
            showError("Could not add to wallet")
        }
    } catch (e: Exception) {
        Log.e("WatchTime", "Error", e)
        showError("Network error. Please try again.")
    }
}
```

## Migration Strategy

### Option 1: Automatic Migration on Login

```kotlin
fun onUserLogin(userId: String) {
    lifecycleScope.launch {
        val watchTimeManager = WatchTimeManagerFirebase.getInstance(context)
        
        // Check if already migrated
        if (!watchTimeManager.isMigratedToFirebase()) {
            // Migrate user data to Firebase
            val success = watchTimeManager.migrateToFirebase()
            if (success) {
                showMessage("Your data is now synced!")
            }
        }
    }
}
```

### Option 2: Manual Migration (User Opt-in)

```kotlin
@Composable
fun SettingsScreen() {
    val watchTimeManager = WatchTimeManagerFirebase.getInstance(LocalContext.current)
    var isMigrated by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isMigrated = watchTimeManager.isMigratedToFirebase()
    }
    
    if (!isMigrated) {
        Button(onClick = {
            lifecycleScope.launch {
                val success = watchTimeManager.migrateToFirebase()
                if (success) {
                    isMigrated = true
                    showMessage("Sync enabled!")
                }
            }
        }) {
            Text("Enable Cloud Sync")
        }
    }
}
```

## Testing

### Test Local System (Backward Compatibility)

```kotlin
// Ensure user is NOT migrated
val watchTimeManager = WatchTimeManagerFirebase.getInstance(context)
assertFalse(watchTimeManager.isMigratedToFirebase())

// Test operations - should use SharedPreferences
lifecycleScope.launch {
    watchTimeManager.watchAdForWallet()
    val wallet = watchTimeManager.getWalletTime()
    assertEquals(30, wallet) // Should work with local system
}
```

### Test Firebase System

```kotlin
// Migrate user first
lifecycleScope.launch {
    watchTimeManager.migrateToFirebase()
    assertTrue(watchTimeManager.isMigratedToFirebase())
    
    // Test operations - should use Firestore
    watchTimeManager.watchAdForWallet()
    val wallet = watchTimeManager.getWalletTime()
    assertEquals(30, wallet) // Should work with Firebase
}
```

## Troubleshooting

### Issue: "User not logged in" error

**Solution**: Ensure Firebase Auth is initialized and user is logged in before calling Firebase operations.

```kotlin
if (FirebaseAuth.getInstance().currentUser != null) {
    // Safe to use Firebase operations
} else {
    // Use local system only
}
```

### Issue: Migration fails

**Solution**: Check Firebase permissions and network connectivity. Migration will fail gracefully and user continues with local system.

### Issue: Data not syncing across devices

**Solution**: Ensure you're listening to Firestore changes with `addSnapshotListener()` for real-time updates.

## Performance Considerations

1. **Batch Updates**: Time updates during video playback are batched (every 10 seconds) to reduce Firebase writes
2. **Transactions**: All critical operations use Firestore transactions for consistency
3. **Caching**: Consider caching frequently accessed values to reduce Firebase reads
4. **Offline Support**: Firestore supports offline persistence - enable it for better UX

## Security Rules

Ensure your Firestore security rules allow users to read/write only their own data:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      // Users can only read/write their own data
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Summary

- ✅ **Backward Compatible**: Existing users unaffected
- ✅ **Firebase Integration**: Cloud sync for migrated users
- ✅ **Atomic Operations**: Race condition prevention
- ✅ **Easy Migration**: One function call to migrate
- ✅ **Real-time Sync**: Cross-device synchronization
- ✅ **Error Handling**: Graceful fallbacks

The system automatically handles routing to the correct storage backend based on migration status, ensuring a smooth transition for all users.
