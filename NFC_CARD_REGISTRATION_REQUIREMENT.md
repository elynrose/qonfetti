# NFC Card Registration Requirement

## Overview

**NFC cards must now be registered to a customer before points can be awarded.** This ensures data integrity and prevents unauthorized point accumulation.

## How It Works

### Before (Old Behavior):
1. Scan NFC card → Read member ID
2. Find customer by member ID
3. **Award points immediately**
4. Register card (optional)

### After (New Behavior):
1. Scan NFC card → Read member ID
2. Find customer by member ID
3. **Check if card is registered to this customer**
4. If not registered → Register card first, then award points
5. If already registered → Award points directly

## Security Benefits

✅ **Prevents unauthorized point accumulation** - Cards must be properly linked to customers
✅ **Ensures data integrity** - Points are only awarded to registered cards
✅ **Prevents card sharing** - Each card is tied to a specific customer
✅ **Audit trail** - All point transactions are linked to registered cards

## Expected Log Messages

### When scanning an unregistered card:
```
D NfcPointsManager: Found customer: James Whitcomb (ID: c242ccb4-e041-44b9-a9c1-c5c0a7052ed8)
D NfcPointsManager: Checking if NFC card is registered to customer: 2047583972 -> c242ccb4-e041-44b9-a9c1-c5c0a7052ed8
D NfcPointsManager: NFC card registration check: false (customer: null, active: false)
D NfcPointsManager: NFC card not registered to customer. Registering card first...
D NfcPointsManager: Registering new NFC card: 2047583972 for customer: c242ccb4-e041-44b9-a9c1-c5c0a7052ed8
D NfcPointsManager: NFC card registered successfully, proceeding with points
D NfcPointsManager: Updated customer points to: 9
```

### When scanning a registered card:
```
D NfcPointsManager: Found customer: James Whitcomb (ID: c242ccb4-e041-44b9-a9c1-c5c0a7052ed8)
D NfcPointsManager: Checking if NFC card is registered to customer: 2047583972 -> c242ccb4-e041-44b9-a9c1-c5c0a7052ed8
D NfcPointsManager: NFC card registration check: true (customer: c242ccb4-e041-44b9-a9c1-c5c0a7052ed8, active: true)
D NfcPointsManager: NFC card already registered to customer, proceeding with points
D NfcPointsManager: Updated customer points to: 10
```

## Testing Scenarios

### Scenario 1: First-time card scan
1. Scan a new NFC card
2. **Expected**: Card gets registered, then points awarded
3. **Result**: Customer gets 1 point, card appears in their NFC Cards section

### Scenario 2: Registered card scan
1. Scan a card that's already registered to a customer
2. **Expected**: Points awarded directly
3. **Result**: Customer gets 1 point, no duplicate registration

### Scenario 3: Unlinked card scan
1. Unlink a card from a customer (using the unlink button)
2. Scan the same card again
3. **Expected**: Card gets re-registered, then points awarded
4. **Result**: Customer gets 1 point, card reappears in their NFC Cards section

### Scenario 4: Wrong customer card scan
1. Register a card to Customer A
2. Try to scan the same card for Customer B
3. **Expected**: Error - card already registered to different customer
4. **Result**: No points awarded, error message shown

## Error Handling

### Card Registration Failure
If card registration fails, points are **not awarded**:
```
E NfcPointsManager: Failed to register NFC card: Database error
E NfcPointsManager: NFC card must be registered before awarding points
```

### Card Already Registered to Different Customer
If a card is already registered to a different customer:
```
D NfcPointsManager: NFC card registration check: true (customer: different-customer-id, active: true)
E NfcPointsManager: NFC card already registered to different customer
```

## Business Logic

### Point Awarding Rules:
1. **Card must be registered** to the customer before points are awarded
2. **One card per customer** - cards cannot be shared between customers
3. **Active cards only** - inactive cards must be reactivated before use
4. **Automatic registration** - new cards are automatically registered on first scan

### Card Management:
1. **Registration**: Automatic on first scan
2. **Unlinking**: Manual via customer detail page
3. **Re-registration**: Automatic when unlinked card is scanned again
4. **Status tracking**: Cards can be active/inactive

## Implementation Details

### Key Functions:
- `checkNfcCardRegistration()` - Verifies if card is registered to customer
- `registerNfcCardIfNeeded()` - Registers card if not already registered
- `processNfcCard()` - Main processing function with new validation

### Database Operations:
- **Check**: Query nfc_cards table for card_id and customer_id match
- **Register**: Insert new record in nfc_cards table
- **Update**: Modify existing card record if needed

This ensures a secure, auditable points system where every transaction is properly linked to a registered NFC card. 