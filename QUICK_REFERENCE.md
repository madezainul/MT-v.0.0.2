# Quick Reference: JSON String Approach

## What Changed

**FormData array parameters** → **JSON string payload**

---

## Frontend Change (JavaScript)

### Before
```javascript
const formData = new FormData();
formData.append(`partIds[0]`, partId1);
formData.append(`receivedQuantities[0]`, qty1);
// ... complex indexing

fetch(url, { method: 'POST', body: formData });
```

### After
```javascript
const payload = {
    parts: [
        { partId: partId1, receivedQuantity: qty1, model: null, inspectorId: null },
        { partId: partId2, receivedQuantity: qty2, model: "NEW", inspectorId: "insp1" }
    ],
    newSupplier: null
};

fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
});
```

---

## Backend Change (Controller)

### Before
```java
public String receiveParts(
    @RequestParam String[] partIds,
    @RequestParam Integer[] receivedQuantities,
    @RequestParam String[] inspectorIds,
    @RequestParam String[] models,
    @RequestParam String newSuppliers,
    RedirectAttributes ra) {
    
    for (int i = 0; i < partIds.length; i++) {
        // Index matching logic
    }
}
```

### After
```java
public String receiveParts(
    @PathVariable String id,
    @RequestBody Map<String, Object> payload,
    RedirectAttributes ra) {
    
    List<Map<String, Object>> partsData = (List<Map<String, Object>>) payload.get("parts");
    String newSupplier = (String) payload.get("newSupplier");
    
    for (Map<String, Object> partData : partsData) {
        String partId = (String) partData.get("partId");
        Integer quantity = (Integer) partData.get("receivedQuantity");
    }
}
```

---

## JSON Payload Structure

```json
{
  "parts": [
    {
      "partId": "uuid-string",
      "receivedQuantity": 10,
      "model": "MODEL-NAME or null",
      "inspectorId": "uuid-string or null"
    }
  ],
  "newSupplier": "supplier-name or null"
}
```

---

## Controller Import Additions

```java
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestBody;
```

---

## Key Differences

| Point | FormData | JSON |
|-------|----------|------|
| Content-Type | multipart/form-data | application/json |
| Spring Binding | @RequestParam | @RequestBody |
| Data Format | Array parameters | Object list |
| Debugging | Complex | Simple (stringify) |
| Null Handling | Extra logic | Native |
| Add Fields | New parameter | Add property |

---

## Testing Command

```powershell
# From project root
.\mvnw clean compile -q
# Should complete without errors
```

---

## Files Modified

1. `src/main/resources/templates/quotation-request/detail.html` - Lines 455-535
2. `src/main/java/ahqpck/maintenance/report/controller/QuotationRequestController.java` - Lines 1-23, 250-302

---

## Service Layer

✅ No changes needed - already accepts individual parameters

---

## Browser Console Example

```
Receive button clicked
Selected parts count: 2
[0] Part: xxx, Qty: 10, Model: "NEW", Inspector: "yyy"
✓ Added part: xxx
[1] Part: aaa, Qty: 5, Model: "", Inspector: ""
✓ Added part: aaa
Total parts to submit: 2
Payload to send:
{
  "parts": [...],
  "newSupplier": null
}
Submitting to: /quotation-request/qr-id/receive
Response status: 200
```

---

## Deployment Steps

1. Build: `mvnw clean package -DskipTests`
2. Start: `java -jar target/report-0.0.1-SNAPSHOT.jar`
3. Test: Navigate to QR detail page
4. Select parts → Enter quantities → Click Receive
5. Verify success message and database updates

---

## Status

✅ All changes implemented
✅ Code compiles cleanly
✅ JAR created successfully
✅ Ready for testing

---

**Date**: October 25, 2025
