# Receive Parts Form Consolidation - Complete

## Objective
Consolidate the QR Parts table and Receive Parts form into a single unified card with proper form wrapping to fix data collection and submission issues.

## Changes Implemented

### 1. **HTML Structure Consolidation** (`detail.html` lines 374-657)

#### Before
- QR Parts card with table (lines 374-xxx)
- Duplicate "Receive Parts Form Card" with separate form below (lines 640-670)
- Two separate form structures, causing confusion and data collection issues

#### After
- **Single consolidated QR Parts Card** containing:
  - Form wrapper around entire content (opened on CONFIRMED status)
  - Parts table with checkboxes and input fields inside the form
  - Receive parts controls (supplier field, button) inside the form
  - Single `</form>` closing tag after controls

### 2. **Form Structure**

```html
<!-- Line 384: Form opening (CONFIRMED status only) -->
<form id="receivePartsForm" 
      th:action="@{'/quotation-request/' + ${qr.id} + '/receive'}" 
      method="post">
  
  <!-- Lines 393-628: Parts table with -->
  <table>
    <!-- For each row:
         - Hidden fields: partIds[], originalModels[], originalSuppliers[], newSuppliers[]
         - Inputs: models[], receivedQuantities[], inspectorIds[] 
         - Checkboxes: part-checkbox for selection
    -->
  </table>
  
  <!-- Lines 631-650: Receive controls -->
  <div class="mt-4 pt-4 border-top">
    <label>New Supplier (Optional)</label>
    <input type="text" id="newSupplierInput" name="newSuppliers" />
    <button type="button" id="receiveButton" />
  </div>
  
</form> <!-- Line 651: Form closes here -->
```

### 3. **Data Collection Fields**

Each row in the table contains the following fields (using array indexing):

| Field Name | Type | Purpose |
|-----------|------|---------|
| `partIds[index]` | hidden | Part ID for tracking |
| `receivedQuantities[index]` | input-number | Quantity received |
| `models[index]` | input-text | New model if differs |
| `inspectorIds[index]` | select | Inspector who inspected |
| `newSuppliers[index]` | hidden | Original supplier |
| `newSupplierInput` | input-text | Global new supplier (for all parts) |

### 4. **JavaScript Data Collection** (lines 881-950)

The form submission handler:

```javascript
document.getElementById('receiveButton').addEventListener('click', function(e) {
  // 1. Get all checked parts
  const selectedCheckboxes = document.querySelectorAll('.part-checkbox:checked');
  
  // 2. For each selected part:
  selectedCheckboxes.forEach((checkbox) => {
    const row = checkbox.closest('tr');
    // Extract: partId, quantity, inspector, model
    // Add to FormData with sequential indexing
  });
  
  // 3. Add global new supplier field if provided
  
  // 4. POST to /quotation-request/{id}/receive
  fetch(receiveForm.action, {
    method: 'POST',
    body: formData,
    redirect: 'follow'
  })
  .then(response => {
    // Redirect back to QR detail page
    window.location.href = '/quotation-request/' + qrId;
  });
});
```

### 5. **Backend Endpoint** (QuotationRequestController.java lines 249-329)

POST `/quotation-request/{id}/receive` expects:
- `partIds[]` - Array of part IDs to receive
- `receivedQuantities[]` - Quantities for each part
- `inspectorIds[]` - Inspector IDs (optional)
- `models[]` - New models if changed (optional)
- `newSuppliers` - Supplier name if different from original

Service method (`QuotationRequestService.receivePart()`):
- Marks parts as received
- Updates QR status to COMPLETED when all parts received
- Handles model/supplier changes
- Calls `@Transactional receivePart()` for each part

## Benefits of Consolidation

✅ **Single Point of Entry**: One form, one submission point  
✅ **Cleaner HTML**: Removed duplicate form and card  
✅ **Better UX**: Users see related controls together  
✅ **Proper Data Collection**: Form fields all within single `<form>` tag  
✅ **Correct Form Submission**: FormData correctly associated with backend endpoint  
✅ **Semantic HTML**: Proper form nesting and structure  

## Testing Checklist

- [x] Project builds successfully
- [x] No duplicate form elements
- [x] Form structure is valid HTML
- [ ] Can select parts on CONFIRMED QR
- [ ] Can enter quantities
- [ ] Can select inspectors
- [ ] Can enter new supplier
- [ ] Receive button submits form
- [ ] Backend receives all data correctly
- [ ] QR status updates to COMPLETED
- [ ] Redirect works properly

## Files Modified

- `src/main/resources/templates/quotation-request/detail.html` (lines 374-657)
  - Consolidated QR Parts card
  - Single form wrapper
  - Removed duplicate receive form card
  - Updated receive controls placement

## Next Steps

1. Manual testing of receive workflow on CONFIRMED QR
2. Verify JavaScript console logs show correct data being sent
3. Check backend logs show all parameters received correctly
4. Verify database updates correctly with received quantities and inspector IDs
5. Test redirect back to QR detail page
