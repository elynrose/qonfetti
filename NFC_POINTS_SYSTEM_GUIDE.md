# NFC Points System Guide

## Overview

The NFC Points System is a comprehensive solution that reads customer member IDs from NFC cards, manages points across stores, and handles rewards. This system enables customers to earn points by simply tapping their NFC card at any store location.

## System Architecture

### Core Components

1. **NfcPointsManager**: Main business logic for processing NFC cards
2. **NfcPointsViewModel**: UI state management and user interactions
3. **NfcScanResultScreen**: UI for displaying scan results and rewards
4. **NfcManager**: Low-level NFC card reading/writing operations

## How It Works

### 1. **NFC Card Reading Process**
```kotlin
// Read member ID from NFC card
val memberIdResult = nfcManager.readMemberIdFromCard(tag)
val memberId = memberIdResult.getOrNull()
```

### 2. **Customer Lookup**
```kotlin
// Find customer by member ID in database
val customerResult = findCustomerByMemberId(memberId, authToken)
val customer = customerResult.getOrNull()
```

### 3. **Points Management**
```kotlin
// Check existing points for this store
val existingPoints = getCustomerPoints(customerId, storeId, authToken)

// Update or create points record
if (existingPoints != null) {
    incrementCustomerPoints(customerId, storeId, existingPoints.points + 1, authToken)
} else {
    createCustomerPoints(customerId, storeId, 1, authToken)
}
```

### 4. **Rewards Processing**
```kotlin
// Check for claimable rewards
val rewards = checkClaimableRewards(storeId, newPoints, authToken)
```

## Database Schema

### Required Tables

#### 1. **customers** (existing)
```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT NOT NULL,
    address TEXT,
    member_id TEXT UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### 2. **customer_points** (existing)
```sql
CREATE TABLE customer_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    store_id UUID REFERENCES stores(id),
    points INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(customer_id, store_id)
);
```

#### 3. **rewards** (new)
```sql
CREATE TABLE rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    points_required INTEGER NOT NULL,
    store_id UUID REFERENCES stores(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### 4. **reward_claims** (new)
```sql
CREATE TABLE reward_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    reward_id UUID REFERENCES rewards(id),
    store_id UUID REFERENCES stores(id),
    claimed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_claimed BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## API Endpoints

### Customer Management
- `GET /rest/v1/customers?member_id=eq.{memberId}` - Find customer by member ID
- `GET /rest/v1/customers?id=eq.{customerId}` - Get customer by ID

### Points Management
- `GET /rest/v1/customer_points?customer_id=eq.{customerId}&store_id=eq.{storeId}` - Get customer points
- `POST /rest/v1/customer_points` - Create new points record
- `PATCH /rest/v1/customer_points?customer_id=eq.{customerId}&store_id=eq.{storeId}` - Update points

### Rewards Management
- `GET /rest/v1/rewards?store_id=eq.{storeId}&points_required=lte.{points}&is_active=eq.true` - Get claimable rewards
- `POST /rest/v1/reward_claims` - Claim a reward

## Usage Examples

### Basic NFC Processing
```kotlin
// Initialize the points manager
val nfcPointsManager = NfcPointsManager(supabaseApi, sessionStorage, nfcManager)

// Process NFC card
val result = nfcPointsManager.processNfcCard(tag)

result.fold(
    onSuccess = { processingResult ->
        println("Customer: ${processingResult.customer.name}")
        println("Points: ${processingResult.currentPoints}")
        println("Rewards: ${processingResult.claimableRewards.size}")
    },
    onFailure = { exception ->
        println("Error: ${exception.message}")
    }
)
```

### UI Integration
```kotlin
// In your Compose screen
val viewModel: NfcPointsViewModel = viewModel {
    NfcPointsViewModel(supabaseApi, sessionStorage, nfcManager)
}

val uiState by viewModel.uiState.collectAsStateWithLifecycle()
val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()

// Process NFC card when tag is discovered
LaunchedEffect(nfcTag) {
    nfcTag?.let { tag ->
        viewModel.processNfcCard(tag)
    }
}

// Display results
when (uiState) {
    is NfcPointsUiState.Success -> {
        scanResult?.let { result ->
            NfcScanResultScreen(
                result = result,
                onBack = { /* navigate back */ },
                onClaimReward = { reward ->
                    viewModel.claimReward(reward)
                }
            )
        }
    }
    is NfcPointsUiState.Error -> {
        // Show error message
    }
    is NfcPointsUiState.Processing -> {
        // Show loading indicator
    }
}
```

## Error Handling

### Common Error Scenarios

1. **NFC Card Not Found**
   - Error: "No member ID found on NFC card"
   - Solution: Ensure card is properly formatted with member ID

2. **Customer Not Found**
   - Error: "Customer not found with member ID: {memberId}"
   - Solution: Verify customer exists in database with correct member ID

3. **Authentication Failed**
   - Error: "Not authenticated or store not found"
   - Solution: Check user login status and store assignment

4. **Database Errors**
   - Error: Various HTTP status codes
   - Solution: Check database connectivity and permissions

### Error Recovery
```kotlin
// The system automatically handles most errors
result.fold(
    onSuccess = { /* handle success */ },
    onFailure = { exception ->
        when {
            exception.message?.contains("401") == true -> {
                // Redirect to login
                redirectToLogin()
            }
            exception.message?.contains("Customer not found") == true -> {
                // Show customer not found message
                showCustomerNotFoundMessage()
            }
            else -> {
                // Show generic error
                showErrorMessage(exception.message)
            }
        }
    }
)
```

## Security Considerations

### Data Protection
- **Member IDs**: Stored securely on NFC cards
- **Authentication**: All API calls require valid auth tokens
- **Store Scoping**: All data is scoped to the logged-in store
- **Row Level Security**: Database policies enforce access control

### Validation
- **Member ID Format**: Validate member ID format before processing
- **Points Validation**: Ensure points are positive integers
- **Store Validation**: Verify store exists and user has access

## Testing

### Unit Tests
```kotlin
@Test
fun testNfcProcessing() {
    // Test successful NFC processing
    val mockTag = mock<Tag>()
    val result = nfcPointsManager.processNfcCard(mockTag)
    assertTrue(result.isSuccess)
}

@Test
fun testCustomerNotFound() {
    // Test customer not found scenario
    val result = nfcPointsManager.processNfcCard(mockTag)
    assertTrue(result.isFailure)
    assertEquals("Customer not found", result.exceptionOrNull()?.message)
}
```

### Integration Tests
```kotlin
@Test
fun testEndToEndNfcFlow() {
    // Test complete NFC flow from card read to points update
    // 1. Read NFC card
    // 2. Find customer
    // 3. Update points
    // 4. Check rewards
    // 5. Verify database state
}
```

## Performance Optimization

### Caching
- **Customer Data**: Cache frequently accessed customer information
- **Rewards**: Cache rewards list for current store
- **Points**: Cache current points for active customers

### Batch Operations
- **Multiple Cards**: Process multiple NFC cards in batch
- **Bulk Updates**: Update multiple customer points simultaneously

## Monitoring and Analytics

### Key Metrics
- **NFC Scan Success Rate**: Track successful vs failed scans
- **Points Distribution**: Monitor points earned across customers
- **Reward Claims**: Track reward claim frequency and types
- **Error Rates**: Monitor system errors and failure points

### Logging
```kotlin
Log.d("NfcPointsManager", "Processing NFC card for member ID: $memberId")
Log.d("NfcPointsManager", "Customer found: ${customer.name}")
Log.d("NfcPointsManager", "Points updated: ${newPoints.points}")
Log.d("NfcPointsManager", "Rewards found: ${rewards.size}")
```

## Deployment Checklist

### Pre-deployment
- [ ] Database tables created with proper indexes
- [ ] Row Level Security policies configured
- [ ] API endpoints tested and working
- [ ] NFC permissions added to AndroidManifest.xml
- [ ] Error handling implemented and tested
- [ ] UI components integrated and tested

### Post-deployment
- [ ] Monitor error logs for issues
- [ ] Verify NFC card reading works correctly
- [ ] Test points accumulation and rewards
- [ ] Validate store-scoped data isolation
- [ ] Performance testing with multiple concurrent users

## Support and Troubleshooting

### Common Issues
1. **NFC not working**: Check device NFC settings and permissions
2. **Points not updating**: Verify database connectivity and auth tokens
3. **Rewards not showing**: Check rewards table and points thresholds
4. **Customer not found**: Verify member ID format and database records

### Debug Tools
- **Logs**: Check Android logs for detailed error information
- **Database**: Query tables directly to verify data integrity
- **Network**: Monitor API calls and responses
- **NFC**: Test NFC card reading with different cards

---

**Note**: This system provides a complete NFC-based loyalty solution that can be easily integrated into existing store management applications. 