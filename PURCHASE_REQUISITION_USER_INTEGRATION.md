# Purchase Requisition - User Integration Changes

## Overview
Modified the Purchase Requisition system to integrate with the User table instead of manually entering requestor information. The requestor name and email are now automatically retrieved from the User entity.

## Changes Made

### 1. Entity Changes

#### PurchaseRequisition.java
- **Removed fields:**
  - `String requestorName`
  - `String requestorEmail`

- **Added field:**
  - `User requestor` (ManyToOne relationship)

- **Added helper methods:**
  - `getRequestorName()` - returns requestor.getName()
  - `getRequestorEmail()` - returns requestor.getEmail()
  - `getRequestorEmployeeId()` - returns requestor.getEmployeeId()

### 2. DTO Changes

#### PurchaseRequisitionDTO.java
- **Modified validation:**
  - Changed `@NotBlank(message = "Requestor name is required")` to `@NotBlank(message = "Requestor is required")`
  - Removed `@Email` validation for requestorEmail

- **Modified fields:**
  - Changed `requestorName` from input field to derived field
  - Changed `requestorEmail` from input field to derived field
  - Added `requestorId` as required input field
  - Added `requestorEmployeeId` as derived field

### 3. Service Changes

#### PurchaseRequisitionService.java
- **Added dependency:**
  - `UserRepository userRepository`

- **Modified createPurchaseRequisition():**
  - Retrieves User entity using `requestorId` from DTO
  - Sets `requestor` field instead of `requestorName` and `requestorEmail`
  - Validates that the user exists before creating PR

- **Modified updatePurchaseRequisition():**
  - Allows changing requestor by providing new `requestorId`
  - Validates new requestor exists before updating

### 4. Controller Changes

#### PurchaseRequisitionController.java
- **Added dependency:**
  - `UserService userService`

- **Modified loadFormData():**
  - Added loading of users list for requestor dropdown
  - Changed attribute names from `equipments` to `equipmentList` and `parts` to `partsList` for consistency

### 5. Template Changes

#### create.html
- **Replaced requestor input fields:**
  - Removed manual `requestorName` and `requestorEmail` text inputs
  - Added `requestorId` dropdown to select from existing users
  - Display format: "User Name (Employee ID)"

- **Updated parts reference:**
  - Changed from `${parts}` to `${partsList}`

#### edit.html
- **Updated requestor section:**
  - Changed to `requestorId` dropdown for selection
  - Made `requestorEmail` field readonly (auto-populated from selected user)
  - Shows current requestor as selected option

#### detail.html
- **No changes needed** - Uses helper methods that now pull from User entity

### 6. Repository Query Updates

#### PurchaseRequisitionRepository.java
- **Updated search queries to use User JOIN:**
  - Modified `findByRequestorNameContainingIgnoreCase()` to join with User entity
  - Updated `searchPurchaseRequisitions()` to search in `u.name` and `u.employeeId` instead of `pr.requestorName`
  - Changed from direct field access to JOIN queries: `JOIN pr.requestor u WHERE LOWER(u.name) LIKE...`

- **Enhanced search functionality:**
  - Now searches by user name AND employee ID
  - Maintains same method signatures for backward compatibility
  - Improved search accuracy by using proper User entity relationships

### 7. Database Schema Impact

#### Required Database Migration
```sql
-- Add requestor_id column
ALTER TABLE purchase_requisitions 
ADD COLUMN requestor_id VARCHAR(22);

-- Update existing records (you'll need to map existing requestor names to user IDs)
-- UPDATE purchase_requisitions SET requestor_id = 'USER_ID' WHERE requestor_name = 'NAME';

-- Add foreign key constraint
ALTER TABLE purchase_requisitions 
ADD CONSTRAINT fk_pr_requestor 
FOREIGN KEY (requestor_id) REFERENCES users(id);

-- Make requestor_id NOT NULL after data migration
ALTER TABLE purchase_requisitions 
MODIFY COLUMN requestor_id VARCHAR(22) NOT NULL;

-- Remove old columns after verification
-- ALTER TABLE purchase_requisitions DROP COLUMN requestor_name;
-- ALTER TABLE purchase_requisitions DROP COLUMN requestor_email;
```

## Benefits

1. **Data Consistency:** Requestor information is always accurate and up-to-date
2. **Data Integrity:** No duplicate or inconsistent user information
3. **Better Relationship:** Proper foreign key relationship between PR and User
4. **Automated Information:** Email and employee ID are automatically populated
5. **User Management:** Changes to user information automatically reflect in all PRs

## Usage

1. **Creating PR:**
   - Select requestor from dropdown (shows Name and Employee ID)
   - Email is automatically filled from selected user

2. **Editing PR:**
   - Can change requestor by selecting different user from dropdown
   - Email field updates automatically

3. **Viewing PR:**
   - Shows requestor name, email, and employee ID from User table
   - All information remains consistent with User entity

## Migration Steps

1. **Deploy the code changes**
2. **Run database migration script**
3. **Map existing requestor names to User IDs**
4. **Test the functionality**
5. **Remove old columns once verified**

## Testing

- Compilation successful ✅
- All Purchase Requisition related files have no errors ✅
- Templates updated to use User dropdown ✅
- Service layer properly validates User existence ✅