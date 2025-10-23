# Quotation Receiving - New Part Feature

## Feature Overview
When receiving quotation request (QR) parts, if the **model** or **supplier changes** from the original, the system now automatically creates a **NEW part** instead of updating the existing one.

## Business Logic

### Scenario: Standard Receiving (No Changes)
- Model and supplier match the original
- **Action**: Update existing QR part quantity received
- **Inventory**: Original PR part quantity decremented from inventory normally

### Scenario: Model or Supplier Changed
- Either model differs OR supplier differs from original
- **Action**: Create new Part with unique code
- **Inventory**: 
  - Original PR part quantity is NOT decremented
  - New part is added to inventory with received quantity
  - New part gets its own part code

## Implementation Details

### Files Modified
1. **QuotationRequestService.java**
   - Enhanced `receivePart()` method with change detection logic
   - Added `updateExistingPartReceiving()` - handles standard receiving
   - Added `createNewPartForReceiving()` - handles new part creation
   - Added `generateNewPartCode()` - generates unique codes

2. **QuotationRequestDTO.java**
   - Added `getFormattedNotes()` method for timestamp formatting

3. **quotation-request/detail.html**
   - Notes display with formatted timestamps (inline layout, minimal spacing)

### Key Method: receivePart()

```java
public void receivePart(String qrId, String partId, Integer receivedQuantity, 
                       String inspectorId, String model, String newSupplier)
```

**Change Detection Logic:**
```
if (model != null && model differs from original) OR (newSupplier != null && supplier differs)
    → Create NEW part
else
    → Update existing part
```

### New Part Code Generation

**Pattern**: `{ORIGINAL_CODE}-ALT-{TIMESTAMP}`

Example:
- Original: `PART-001`
- Received with new model: `PART-001-ALT-4523891`

**Uniqueness**: Checked against database with counter if needed
- If `PART-001-ALT-4523891` exists, try `PART-001-ALT-4523891-1`

### New Part Properties

When a new part is created:
```java
Part newPart = Part.builder()
    .id(Base62.encode(UUID.randomUUID()))  // New unique ID
    .code(newPartCode)                      // Generated code
    .name(originalPart.getName())           // Keep original name
    .model(newModel)                        // Use new model
    .manufacturer(originalPart.getManufacturer())  // Keep original
    .supplierName(newSupplier)              // Use new supplier
    .stockQuantity(receivedQuantity)        // Set to received qty
    .build();
```

### Inventory Impact

**Original Part (unchanged):**
- PR part quantity NOT decremented
- Original part inventory unaffected

**New Part (created):**
- Added to inventory with received quantity
- Gets its own part code
- Can be tracked separately

### Inspector Assignment

Both standard and new part receiving support inspector assignment:
- Inspector ID is recorded
- Inspection timestamp recorded with `@InspectedAt`

## Database Changes

### New Records Created
1. **Parts table**: One new row for the alternate part
   - Unique code
   - Different model/supplier
   - Stock quantity = received quantity

2. **QuotationRequestParts table**: One new row linking QR to new part
   - Links to new part (not original)
   - Records model/supplier differences
   - Records inspector information

### Unchanged Records
- **PurchaseRequisitionParts**: Not updated
- **Original Part**: Not modified
- **QuotationRequest**: Status updated normally

## Usage Example

### Scenario
- Original QR part: `BEARING-A` from `Supplier X`
- Receiving: `50 units` with `BEARING-B` from `Supplier Y`

### Result
1. New part created: `BEARING-A-ALT-4523891`
   - Model: BEARING-B
   - Supplier: Supplier Y
   - Stock: +50

2. QR part linked to new part
   - Not linked to original part anymore
   - Quantity received: 50

3. Original part
   - Unchanged
   - PR quantity remains same

## API Parameters

**Controller Endpoint**: `POST /quotation-request/{id}/receive`

Parameters:
- `partIds[]` - Array of QR part IDs
- `receivedQuantities[]` - Array of received quantities
- `inspectorIds[]` - Array of inspector IDs (optional)
- `models[]` - Array of models (optional, new values)
- `newSuppliers[]` - Array of supplier names (optional, new values)
- `receivingNotes` - Receiving notes (optional)

## Validation Notes

- Part code uniqueness enforced by database unique constraint
- Model/Supplier comparison uses string equality
- Empty strings treated as "no change"
- Inspector ID validation ensures user exists

## Future Enhancements

1. **Audit Trail**: Track when parts are received with changes
2. **Approval Workflow**: Require approval for receiving with changes
3. **Reconciliation**: Report for parts received with changes vs. original
4. **Part Comparison UI**: Show diff between original and received part specs
