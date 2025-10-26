# PR Approval Workflow Changes - Implementation Summary

## Overview
Successfully implemented a new approval workflow for Purchase Requisitions (PR) that mirrors the QR (Quotation Request) workflow. Instead of approving the entire PR at once, reviewers can now approve individual parts with checkboxes and action buttons, similar to the QR receive-multi pattern.

## Architecture Changes

### 1. Database Entity Changes

#### PurchaseRequisitionPart.java
Added three new fields to track per-part approval status:
- `isPartApproved` (Boolean): null = pending, true = approved, false = rejected
- `partApprovalNotes` (String): Stores reviewer notes for the part approval decision
- `partApprovalNotes` is stored in `part_approval_notes` column

**SQL Migration (if needed):**
```sql
ALTER TABLE purchase_requisition_parts ADD COLUMN is_part_approved BOOLEAN NULL;
ALTER TABLE purchase_requisition_parts ADD COLUMN part_approval_notes TEXT NULL;
```

### 2. DTO Changes

#### PurchaseRequisitionPartDTO.java
Added mapping for new approval fields:
- `Boolean isPartApproved`
- `String partApprovalNotes`

These fields are now available in the template layer for display and form handling.

### 3. Service Layer Changes

#### PurchaseRequisitionService.java
**New Method:** `approvePartInRequisition()`
```java
@Transactional
public void approvePartInRequisition(String prId, String partId, Boolean isApproved, 
                                      String partApprovalNotes, String reviewerId, String reviewNotes)
```

**Functionality:**
- Updates individual part approval status
- Automatically updates overall PR status when ALL parts are reviewed
- If all parts approved → PR status = APPROVED, isApproved = true
- If any part rejected → PR isApproved = false
- Tracks reviewer information at PR level

### 4. Controller Layer Changes

#### PurchaseRequisitionController.java
**New Endpoint:** `POST /purchase-requisition/{id}/approve-parts`
```java
@PostMapping("/{id}/approve-parts")
public ResponseEntity<Map<String, Object>> approveSelectedParts(
    @PathVariable String id,
    @RequestBody Map<String, Object> payload)
```

**Request Payload Structure:**
```json
{
  "parts": [
    {
      "partId": "part-id-1",
      "isApproved": true,
      "approvalNotes": "Looks good"
    },
    {
      "partId": "part-id-2",
      "isApproved": false,
      "approvalNotes": "Need alternate supplier"
    }
  ],
  "reviewerId": "reviewer-id",
  "reviewNotes": "Overall review notes"
}
```

**Response:**
```json
{
  "success": true,
  "message": "2 part(s) approval status updated successfully!",
  "redirectUrl": "/purchase-requisition/{id}"
}
```

**Error Handling:**
- Returns `400 Bad Request` if no parts selected
- Returns `500 Internal Server Error` if processing fails
- Includes descriptive error messages

### 5. Template Changes

#### purchase-requisition/detail.html

**Old Approval Section:**
- Simple form with two buttons: "Approve All" or "Reject All"
- Applied PR-level approval to entire requisition

**New Approval Section:**
1. **Reviewer Selection** - Required dropdown to select reviewer
2. **Review Notes** - Optional text area for overall review notes
3. **Part Selection Table** - Scrollable table with:
   - Checkbox column for part selection
   - Part details (code, name, quantity)
   - Decision dropdown (Approve/Reject) for each part
   - Notes field for individual part feedback
   - "Select All" checkbox in header

4. **Part Organization:**
   - **Unapproved Parts (Top)** - Editable rows with checkboxes
   - **Approved Parts (Middle)** - Read-only green background
   - **Rejected Parts (Bottom)** - Read-only red background

5. **Action Button:**
   - "Submit Approvals" button sends JSON payload via fetch

### 6. JavaScript Changes

#### purchase-requisition/detail.html Script Block
New JavaScript handler for approval form:

**Features:**
- Select/Deselect all parts checkbox with row highlighting
- Validates reviewer selection before submission
- Collects selected parts with their approval decisions
- Builds JSON payload
- Submits via fetch POST with JSON Content-Type
- Handles success/error responses
- Redirects on success

**Validation:**
- At least one reviewer must be selected
- At least one part must be selected
- Each selected part must have an approval decision (Approve or Reject)

## Implementation Details

### Per-Part Approval Decision Making

When a reviewer uses the new approval form:

1. **Submits Form with Selected Parts:**
   - Checks which parts are selected
   - Gets approval decision (Approve/Reject) for each selected part
   - Gets individual notes for each part

2. **Backend Processing:**
   - For each part: sets `isPartApproved` and `partApprovalNotes`
   - Checks if ALL parts in the PR have been reviewed
   - If all reviewed:
     - If ALL approved → PR status = APPROVED, isApproved = true
     - If ANY rejected → PR isApproved = false
   - Updates PR-level reviewer info: `reviewerName`, `reviewNotes`, `reviewedAt`

3. **Database Consistency:**
   - Part-level approval stored in `purchase_requisition_parts`
   - PR-level approval status stored in `purchase_requisition`
   - Both synchronized to maintain consistency

## UI/UX Improvements

### Visual Organization
- **Color-coded rows:**
  - White background: Unapproved parts (editable)
  - Green background (#f0f8f5): Approved parts (read-only)
  - Red background (#fef5f5): Rejected parts (read-only)
  
- **Status badges:**
  - "Approved" badge in green
  - "Rejected" badge in red
  
- **Scrollable table:**
  - Fixed header with select-all checkbox
  - Max height 400px for better visibility
  - Z-index management for sticky header

### User Flow
1. Reviewer navigates to PR detail page
2. If PR is "SUBMITTED" and not yet approved:
   - Approval form section appears
   - Reviewer selects reviewer name
   - Optionally adds overall review notes
   - Reviews each part:
     - Checks parts to review
     - Selects Approve/Reject decision
     - Adds part-specific notes
   - Clicks "Submit Approvals"
   - Form sends JSON to backend
   - Success: Redirects back to PR detail page
   - Error: Shows error message

## Sorting Strategy

Parts are displayed in this order (ascending by approval status):
1. **Unapproved parts** (isPartApproved = null) - Displayed with editable controls
2. **Approved parts** (isPartApproved = true) - Displayed as read-only with green background
3. **Rejected parts** (isPartApproved = false) - Displayed as read-only with red background

This order helps reviewers focus on pending items first while showing historical decisions.

## Backward Compatibility

### Legacy Approval Endpoint
The old `/purchase-requisition/{id}/approve` endpoint remains for backward compatibility:
- Still accepts form data
- Still applies PR-level approval
- Can be used for "Approve All" workflows if needed

### Migration Path
1. Keep existing endpoint for gradual migration
2. New UI uses new part-based approval
3. Both workflows can coexist

## Testing Recommendations

1. **Unit Tests:**
   - Test `approvePartInRequisition()` with various part approval combinations
   - Test overall PR status calculation when all parts are reviewed
   - Test validation of selected parts

2. **Integration Tests:**
   - Test JSON request parsing in controller
   - Test database updates for part and PR status
   - Test response serialization

3. **UI Tests:**
   - Test checkbox select-all functionality
   - Test part selection and decision making
   - Test form submission and error handling
   - Test redirect on success

4. **Manual Testing:**
   - Create PR with multiple parts
   - Approve/reject individual parts
   - Verify PR status updates correctly
   - Test with partial part selection
   - Test with multiple reviewers over time

## Files Modified

1. **Entity Layer:**
   - `src/main/java/ahqpck/maintenance/report/entity/PurchaseRequisitionPart.java`

2. **DTO Layer:**
   - `src/main/java/ahqpck/maintenance/report/dto/PurchaseRequisitionPartDTO.java`

3. **Service Layer:**
   - `src/main/java/ahqpck/maintenance/report/service/PurchaseRequisitionService.java`

4. **Controller Layer:**
   - `src/main/java/ahqpck/maintenance/report/controller/PurchaseRequisitionController.java`

5. **Template Layer:**
   - `src/main/resources/templates/purchase-requisition/detail.html`

## Build Status

✅ **Compilation:** Successful (0 errors)
✅ **JAR Package:** Created (106.1 MB)
✅ **All imports:** Resolved correctly

## Next Steps

1. Start application with new approval workflow
2. Test PR approval functionality with checkboxes
3. Verify database updates for part-level approval
4. Monitor for any runtime issues
5. Perform user acceptance testing

## Comparison: Old vs New Workflow

### Old Approval Workflow
```
Reviewer → Submit Form → Approve/Reject Entire PR → PR Status Updated
```

### New Approval Workflow
```
Reviewer → Select Parts → Choose Decision per Part → Add Notes → 
Submit Form → Each Part Updated → PR Status Auto-calculated → 
Parts Grouped (Unapproved/Approved/Rejected)
```

The new workflow provides:
- ✅ Granular control over individual parts
- ✅ More detailed feedback per part
- ✅ Flexible approval logic (approve some, reject others)
- ✅ Visual organization of decisions
- ✅ Automatic PR status calculation
- ✅ Audit trail for each part

---

**Implementation Date:** October 26, 2025
**Version:** 0.0.2
**Status:** Ready for Testing
