# Customer Management System

## Overview

The customer management system provides comprehensive CRUD operations for managing customers across multiple stores. Each customer has a unique global `member_id` tied to their email, allowing them to be shared across different stores while maintaining separate point balances.

## Database Schema

### Tables

#### `customers`
- `id` (UUID, Primary Key) - Unique customer identifier
- `name` (TEXT, Required) - Customer's full name
- `email` (TEXT, Required, Unique) - Customer's email address
- `phone` (TEXT, Required) - Customer's phone number
- `address` (TEXT, Optional) - Customer's address (supports Google Maps autocomplete)
- `member_id` (TEXT, Unique) - Global member identifier (auto-generated from email hash)
- `created_at` (TIMESTAMP) - Record creation timestamp
- `updated_at` (TIMESTAMP) - Record last update timestamp

#### `customer_points`
- `id` (UUID, Primary Key) - Unique points record identifier
- `customer_id` (UUID, Foreign Key) - References customers.id
- `store_id` (UUID, Foreign Key) - References stores.id
- `points` (INTEGER, Default: 0) - Customer's points balance for this store
- `created_at` (TIMESTAMP) - Record creation timestamp
- `updated_at` (TIMESTAMP) - Record last update timestamp

## Setup Instructions

### 1. Database Setup

Run the SQL script in your Supabase SQL Editor:

```sql
-- Run the contents of database/customer_setup.sql
```

This script creates:
- Customer and customer_points tables
- Proper indexes for performance
- Row Level Security (RLS) policies
- Automatic triggers for member_id generation and timestamp updates

### 2. App Integration

The customer management system is already integrated into the app. After setup:

1. **Login** to your store account
2. **Tap "Manage Customers"** from the dashboard
3. **Start adding customers** with the full CRUD interface

## API Functions

### Core Customer Operations

#### 1. Fetch All Customers
```kotlin
suspend fun getCustomers(storeId: String, authToken: String): Result<List<CustomerWithPoints>>
```
- **Purpose**: Retrieves all customers for the current store with their point balances
- **Returns**: List of customers with their points for the store
- **Scoped**: Only returns customers associated with the specified store

#### 2. Fetch Single Customer
```kotlin
suspend fun getCustomer(customerId: String, storeId: String, authToken: String): Result<CustomerWithPoints?>
```
- **Purpose**: Retrieves a specific customer by ID for the current store
- **Returns**: Customer with points if found in the store, null otherwise
- **Scoped**: Only returns customer if they exist in the specified store

#### 3. Create Customer
```kotlin
suspend fun createCustomer(
    request: CreateCustomerRequest,
    storeId: String,
    authToken: String
): Result<CustomerWithPoints>
```
- **Purpose**: Creates a new customer or adds existing customer to store
- **Logic**:
  - Checks if customer exists by email
  - If exists: uses existing customer record
  - If new: creates customer with auto-generated member_id
  - Adds customer to store with 0 points
- **Returns**: Customer with points for the store

#### 4. Update Customer
```kotlin
suspend fun updateCustomer(
    customerId: String,
    request: UpdateCustomerRequest,
    storeId: String,
    authToken: String
): Result<CustomerWithPoints?>
```
- **Purpose**: Updates customer information
- **Scoped**: Only updates customers that exist in the specified store
- **Returns**: Updated customer with points

#### 5. Delete Customer
```kotlin
suspend fun deleteCustomer(customerId: String, storeId: String, authToken: String): Result<Boolean>
```
- **Purpose**: Removes customer from store (deletes customer_points entry)
- **Note**: Does not delete the customer globally, only removes from this store
- **Returns**: True if successfully removed, false if not found

#### 6. Search Customers
```kotlin
suspend fun searchCustomers(
    query: String,
    storeId: String,
    authToken: String
): Result<List<CustomerWithPoints>>
```
- **Purpose**: Searches customers by name or email
- **Scoped**: Only searches within customers of the specified store
- **Returns**: Filtered list of customers with points

## Data Models

### Customer
```kotlin
data class Customer(
    val id: String? = null,
    val name: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val memberId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
```

### CustomerWithPoints
```kotlin
data class CustomerWithPoints(
    val customer: Customer,
    val points: Int = 0
)
```

### Request Models
```kotlin
data class CreateCustomerRequest(
    val name: String,
    val email: String,
    val phone: String,
    val address: String? = null
)

data class UpdateCustomerRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null
)
```

## Key Features

### 1. Global Customer Management
- **Unique member_id**: Each customer gets a unique global identifier based on email hash
- **Cross-store sharing**: Customers can exist in multiple stores with separate point balances
- **Email-based linking**: If a customer already exists (by email), they're automatically linked to the new store

### 2. Store-Scoped Operations
- **Security**: All operations are scoped to the logged-in store owner
- **Isolation**: Store owners can only see and manage their own customers
- **Points tracking**: Each store maintains separate point balances for customers

### 3. Address Support
- **Google Maps integration**: Address field supports Google Maps autocomplete
- **Optional field**: Address is not required for customer creation
- **Flexible storage**: Address stored as text for maximum compatibility

### 4. Search and Filter
- **Real-time search**: Search by customer name or email
- **Store-scoped**: Search only within customers of the current store
- **Case-insensitive**: Search works regardless of case

## Usage Examples

### Creating a Customer
```kotlin
val request = CreateCustomerRequest(
    name = "John Doe",
    email = "john@example.com",
    phone = "+1234567890",
    address = "123 Main St, City, State"
)

val result = supabaseApi.createCustomer(request, storeId, authToken)
result.fold(
    onSuccess = { customerWithPoints ->
        println("Customer created: ${customerWithPoints.customer.name}")
        println("Points: ${customerWithPoints.points}")
    },
    onFailure = { exception ->
        println("Error: ${exception.message}")
    }
)
```

### Searching Customers
```kotlin
val result = supabaseApi.searchCustomers("john", storeId, authToken)
result.fold(
    onSuccess = { customers ->
        customers.forEach { customerWithPoints ->
            println("${customerWithPoints.customer.name} - ${customerWithPoints.points} points")
        }
    },
    onFailure = { exception ->
        println("Search error: ${exception.message}")
    }
)
```

### Updating Customer
```kotlin
val request = UpdateCustomerRequest(
    name = "John Smith",
    phone = "+1987654321"
)

val result = supabaseApi.updateCustomer(customerId, request, storeId, authToken)
result.fold(
    onSuccess = { updatedCustomer ->
        if (updatedCustomer != null) {
            println("Customer updated: ${updatedCustomer.customer.name}")
        } else {
            println("Customer not found in this store")
        }
    },
    onFailure = { exception ->
        println("Update error: ${exception.message}")
    }
)
```

## Security Features

### Row Level Security (RLS)
- **Customer table**: Authenticated users can read all customers (for member_id lookup)
- **Customer_points table**: Store owners can only access their own store's customer points
- **Automatic scoping**: All operations automatically filter by store ownership

### Data Validation
- **Required fields**: Name, email, and phone are required
- **Email uniqueness**: Email addresses must be unique across all customers
- **Member_id generation**: Automatically generated from email hash
- **Store verification**: All operations verify customer exists in the specified store

## Error Handling

The API functions return `Result<T>` types that handle:
- **Network errors**: Connection issues, timeouts
- **Authentication errors**: Invalid tokens, expired sessions
- **Authorization errors**: Insufficient permissions
- **Validation errors**: Invalid data, missing required fields
- **Business logic errors**: Customer not found, already exists

## Performance Considerations

### Indexes
- Email index for fast customer lookup
- Member_id index for global customer identification
- Composite indexes for customer_points queries
- Store_id indexes for store-scoped operations

### Query Optimization
- Efficient joins between customers and customer_points
- Minimal data transfer with selective field queries
- Proper use of database constraints and relationships

## Future Enhancements

### Planned Features
1. **Bulk operations**: Import/export customer lists
2. **Advanced search**: Filter by points, creation date, etc.
3. **Customer analytics**: Purchase history, visit frequency
4. **Integration APIs**: Connect with external CRM systems
5. **Customer segmentation**: Group customers by behavior or demographics

### Google Maps Integration
- **Autocomplete API**: Real-time address suggestions
- **Geocoding**: Convert addresses to coordinates
- **Distance calculation**: Calculate distance between store and customer
- **Map visualization**: Show customer locations on map

## Troubleshooting

### Common Issues

1. **Customer not found in store**
   - Verify customer exists in the global customers table
   - Check if customer_points entry exists for the store
   - Ensure proper store_id is being used

2. **Permission denied**
   - Verify authentication token is valid
   - Check if user owns the specified store
   - Ensure RLS policies are properly configured

3. **Email already exists**
   - System will automatically link existing customer to store
   - Check if customer already has points in the store
   - Verify member_id generation is working correctly

### Debug Logging
All API functions include comprehensive logging:
- Request details (URLs, parameters, headers)
- Response status and content
- Error messages and stack traces
- Performance metrics

Use Android Logcat to monitor customer operations:
```bash
adb logcat | grep -E "(SupabaseApi|CustomerViewModel)"
``` 