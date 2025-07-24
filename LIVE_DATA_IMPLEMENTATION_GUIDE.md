# Live Data Implementation Guide

## Overview

This guide explains the live data functionality that has been implemented in the Qonfetty app to provide real-time updates without requiring manual page refreshes.

## What Was Implemented

### 1. DataRefreshManager

A centralized manager that handles periodic data refresh and live updates across the app.

**Key Features:**
- **Periodic Refresh**: Automatically refreshes data at configurable intervals
- **Smart Refresh Triggers**: Refreshes data when specific events occur
- **Background Data Sync**: Keeps data fresh in the background
- **State Management**: Provides refresh state information to UI components

**Refresh Intervals:**
- Customers: 30 seconds
- Dashboard Activity: 10 seconds  
- Customer Points: 20 seconds

### 2. Live Data Integration

**ViewModels Updated:**
- `CustomerViewModel`: Now receives live customer data updates
- `DashboardViewModel`: Now receives live activity updates
- `CustomerDetailViewModel`: Now receives live points updates

**UI Components:**
- `LiveDataIndicator`: Visual indicator showing refresh status
- Added to Dashboard and Customer List screens

### 3. Smart Refresh Triggers

**Automatic Triggers:**
- NFC card scans trigger immediate activity refresh
- Login/logout starts/stops periodic refresh
- Points changes trigger reward updates

## How It Works

### 1. Initialization

```kotlin
// In MainActivity
globalDataRefreshManager = DataRefreshManager(api, sessionStorage)

// Start refresh when user logs in
if (isLoggedIn) {
    globalDataRefreshManager?.startPeriodicRefresh()
}
```

### 2. Data Flow

1. **DataRefreshManager** runs periodic jobs in the background
2. **ViewModels** observe the manager's StateFlows
3. **UI Components** automatically update when data changes
4. **Visual indicators** show refresh status to users

### 3. State Management

```kotlin
sealed class RefreshState {
    object Idle : RefreshState()
    object Refreshing : RefreshState()
    data class Success(val message: String) : RefreshState()
    data class Error(val message: String) : RefreshState()
}
```

## Benefits

### 1. Real-Time Updates
- No more manual pull-to-refresh needed
- Data stays current automatically
- Users see changes immediately

### 2. Better User Experience
- Visual feedback during updates
- Smooth transitions between data states
- No interruption to user workflow

### 3. Efficient Resource Usage
- Smart refresh intervals based on data type
- Background processing doesn't block UI
- Automatic cleanup when app is destroyed

### 4. Scalable Architecture
- Centralized data management
- Easy to add new data types
- Configurable refresh intervals

## Usage Examples

### 1. Viewing Live Customer Data

```kotlin
// CustomerViewModel automatically receives updates
val customers by viewModel.customers.collectAsStateWithLifecycle()

// UI automatically updates when new data arrives
LazyColumn {
    items(customers) { customer ->
        CustomerCard(customer = customer)
    }
}
```

### 2. Monitoring Refresh Status

```kotlin
// Show live indicator
val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

LiveDataIndicator(
    refreshState = refreshState,
    modifier = Modifier.align(Alignment.End)
)
```

### 3. Triggering Immediate Refresh

```kotlin
// After NFC scan
dataRefreshManager?.triggerRefresh(DataRefreshManager.DataType.ACTIVITY)
```

## Configuration

### Refresh Intervals

You can adjust the refresh intervals in `DataRefreshManager.kt`:

```kotlin
companion object {
    const val CUSTOMER_REFRESH_INTERVAL = 30_000L // 30 seconds
    const val DASHBOARD_REFRESH_INTERVAL = 15_000L // 15 seconds
    const val ACTIVITY_REFRESH_INTERVAL = 10_000L // 10 seconds
    const val POINTS_REFRESH_INTERVAL = 20_000L // 20 seconds
}
```

### Adding New Data Types

1. Add new StateFlow to DataRefreshManager
2. Create refresh method for the data type
3. Add to DataType enum
4. Update ViewModels to observe the new data

## Testing

The implementation includes comprehensive tests:

```kotlin
@Test
fun `test DataRefreshManager creation`() = runTest {
    val mockSupabaseApi = mock<SupabaseApi>()
    val mockSessionStorage = mock<SessionStorage>()
    val dataRefreshManager = DataRefreshManager(mockSupabaseApi, mockSessionStorage)
    assertNotNull(dataRefreshManager)
}
```

## Troubleshooting

### Common Issues

1. **Data not updating**: Check if user is logged in and refresh is started
2. **High battery usage**: Consider increasing refresh intervals
3. **Network errors**: Check authentication and network connectivity

### Debug Information

The implementation includes extensive logging:

```kotlin
Log.d("DataRefreshManager", "Refreshed ${customers.size} customers")
Log.d("DashboardViewModel", "Received live update: ${transactions.size} transactions")
```

## Future Enhancements

### Potential Improvements

1. **WebSocket Support**: Real-time updates from server
2. **Offline Support**: Cache data for offline viewing
3. **Smart Refresh**: Adjust intervals based on user activity
4. **Push Notifications**: Server-triggered updates

### Performance Optimizations

1. **Batch Updates**: Group multiple data changes
2. **Delta Updates**: Only refresh changed data
3. **Memory Management**: Limit cached data size

## Conclusion

The live data implementation provides a significant improvement to the user experience by eliminating the need for manual refreshes while maintaining efficient resource usage. The architecture is designed to be scalable and maintainable for future enhancements. 