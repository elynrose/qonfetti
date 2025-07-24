# NFC Card Registration Setup Guide

## Overview
The Qonfetty app now supports NFC card registration for customers. This allows customers to use NFC cards at any store to earn and redeem points.

## Database Setup

### 1. Run the SQL Script
Execute the `nfc_cards_setup.sql` script in your Supabase database to create the necessary table and policies:

```sql
-- Run this in your Supabase SQL editor
-- The script creates:
-- - nfc_cards table
-- - Row Level Security policies
-- - Indexes for performance
-- - Validation triggers
```

### 2. Table Structure
The `nfc_cards` table contains:
- `id`: Unique identifier (UUID)
- `card_id`: The actual NFC card UID (unique)
- `member_id`: Links to customer's member_id
- `customer_id`: Links to customer's id
- `store_id`: Store where the card was registered
- `is_active`: Whether the card is currently active
- `created_at`: Registration timestamp
- `updated_at`: Last update timestamp

## App Features

### 1. Customer Detail Screen
- Tap any customer in the customer list to view details
- Shows customer information, points, and registered NFC cards
- Add button (+) to register new NFC cards

### 2. NFC Card Registration
- Manual entry of NFC card ID (for testing)
- Links the card to the customer's member_id
- Stores the registration in the database
- Card can be used at any store

### 3. NFC Card Management
- View all registered cards for a customer
- Deactivate cards (soft delete)
- See registration dates

## Usage Flow

### For Store Staff:
1. Navigate to Customers
2. Tap on a customer to view details
3. Tap the + button to register an NFC card
4. Enter the NFC card ID manually (or scan if NFC reader is implemented)
5. The card is now linked to the customer

### For Customers:
1. Present NFC card at any store
2. Staff can scan the card to identify the customer
3. Points can be earned/redeemed using the card
4. Card works across all stores in the system

## Security Features

### Row Level Security (RLS)
- Store owners can only view/register cards for their stores
- Cards are isolated by store ownership
- Secure access control through Supabase policies

### Validation
- Prevents duplicate active card registrations
- Validates customer exists and belongs to store
- Ensures member_id matches the customer

## Future Enhancements

### Planned Features:
1. **NFC Reader Integration**: Direct NFC card scanning
2. **Card Lookup API**: Find customer by scanning card
3. **Points Integration**: Automatic points earning/redeeming
4. **Card Transfer**: Move cards between customers
5. **Bulk Operations**: Register multiple cards

### Technical Improvements:
1. **Real-time Updates**: Live card status changes
2. **Offline Support**: Queue operations when offline
3. **Analytics**: Track card usage patterns
4. **Notifications**: Alert on card deactivation

## Testing

### Manual Testing:
1. Create a test customer
2. Register a test NFC card with a dummy ID
3. Verify the card appears in the customer's detail screen
4. Test deactivation functionality
5. Verify the card is removed from the list

### Sample NFC Card IDs:
- `test_card_001`
- `sample_nfc_123`
- `demo_card_456`

## Troubleshooting

### Common Issues:
1. **Card not appearing**: Check if card is active in database
2. **Registration fails**: Verify customer exists and has member_id
3. **Permission errors**: Ensure store ownership is correct
4. **Duplicate cards**: Check for existing active registrations

### Database Queries:
```sql
-- Check all NFC cards for a store
SELECT * FROM nfc_cards WHERE store_id = 'your_store_id';

-- Find customer by card ID
SELECT c.* FROM customers c 
JOIN nfc_cards n ON c.id = n.customer_id 
WHERE n.card_id = 'card_id_here' AND n.is_active = true;

-- Check inactive cards
SELECT * FROM nfc_cards WHERE is_active = false;
```

## API Endpoints

The app uses these Supabase endpoints:
- `POST /rest/v1/nfc_cards` - Register new card
- `GET /rest/v1/nfc_cards?card_id=eq.{id}` - Lookup card
- `GET /rest/v1/nfc_cards?customer_id=eq.{id}` - Get customer cards
- `PATCH /rest/v1/nfc_cards?card_id=eq.{id}` - Deactivate card

All endpoints require authentication and respect RLS policies. 