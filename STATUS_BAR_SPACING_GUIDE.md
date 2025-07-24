# Status Bar Spacing Guide

## Overview
This guide explains how to prevent layout overlap with the mobile status bar across all screens in the Qonfetty app.

## Problem
Mobile devices have a status bar at the top that displays time, battery, signal, etc. Without proper spacing, app content can overlap this status bar, making it unreadable and creating a poor user experience.

## Solution
Use the `StatusBarSpacer()` composable at the top of your main content Column to ensure proper spacing below the status bar.

## Implementation

### 1. Import the StatusBarSpacer
```kotlin
import com.example.qonfetty.ui.theme.StatusBarSpacer
```

### 2. Add to Your Screen Layout
```kotlin
@Composable
fun YourScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Add this at the top of your main Column
        StatusBarSpacer()
        
        // Your screen content here
        // Header, content, etc.
    }
}
```

## Current Implementation

The `StatusBarSpacer()` composable is defined in:
```
app/src/main/java/com/example/qonfetty/ui/theme/StatusBarSpacer.kt
```

It provides a consistent 48dp spacing that works across different device sizes and orientations.

## Screens Already Updated

The following screens have been updated to use `StatusBarSpacer()`:

- ✅ **DashboardScreen** - Main dashboard with NFC scanning
- ✅ **StoreSettingsScreen** - Store configuration form
- ✅ **CustomerListScreen** - Customer management list
- ✅ **RewardsScreen** - Rewards management
- ✅ **CustomerDetailScreen** - Individual customer details
- ✅ **NfcTestScreen** - NFC testing interface

## Best Practices

### 1. Always Use StatusBarSpacer
- Add `StatusBarSpacer()` at the top of every screen's main Column
- This ensures consistent spacing across all screens

### 2. Import the Component
- Always import `com.example.qonfetty.ui.theme.StatusBarSpacer`
- This keeps the implementation centralized and maintainable

### 3. Placement
- Place `StatusBarSpacer()` as the first child of your main Column
- This ensures it's always at the top, regardless of other content

### 4. Don't Use Manual Spacers
- Avoid using `Spacer(modifier = Modifier.height(48.dp))` directly
- Use `StatusBarSpacer()` instead for consistency

## Example Template

```kotlin
@Composable
fun NewScreen(
    viewModel: YourViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ✅ Always add this first
        StatusBarSpacer()
        
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Screen Title",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Your screen content here
        // ...
    }
}
```

## Troubleshooting

### If Content Still Overlaps
1. Ensure `StatusBarSpacer()` is the first child of your main Column
2. Check that you're not using any negative margins or padding
3. Verify the import is correct: `com.example.qonfetty.ui.theme.StatusBarSpacer`

### For Different Screen Types
- **Full-screen dialogs**: May need additional spacing
- **Bottom sheets**: Usually don't need StatusBarSpacer
- **Overlays**: Consider the status bar in your positioning

## Future Considerations

- The 48dp spacing works for most devices, but for very large screens, consider using a percentage-based approach
- For landscape orientation, the status bar is typically hidden, but the spacing is still good for consistency
- Consider using `WindowInsets` for more dynamic spacing if needed in the future

## Maintenance

- When adding new screens, always include `StatusBarSpacer()`
- If you see any screens without proper spacing, update them to use this component
- Keep this guide updated as new patterns emerge 