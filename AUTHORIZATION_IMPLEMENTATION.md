# Authorization Implementation in QuotationRequestController

## Summary
Added Spring Security `@PreAuthorize` annotations to all endpoints in `QuotationRequestController` following the authorization pattern from `PartController`.

## Authorization Pattern Reference (from PartController)
- **View Operations (GET)**: `@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER', 'VIEWER')")`
- **Create/Update Operations (POST)**: `@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENGINEER')")`
- **Delete Operations**: `@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")`

---

## Implemented Authorization Rules

### 1. **Dashboard & List Operations** (View All)
```java
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER', 'INSPECTOR', 'VIEWER')")
```
**Endpoints:**
- `GET /quotation-request` - Dashboard
- `GET /quotation-request/list` - List QRs
- `GET /quotation-request/{id}` - View QR details

**Users:** Everyone (readonly access)

### 2. **Create Operations** (Create, Edit, Status Update, Complete, Cancel)
```java
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'REVIEWER')")
```
**Endpoints:**
- `GET /quotation-request/create-from-pr` - Create form (single supplier)
- `GET /quotation-request/create-multi` - Create form (multi-supplier)
- `POST /quotation-request/create` - Create QR
- `POST /quotation-request/{id}/status` - Update status (Send/Confirm)
- `POST /quotation-request/{id}/edit` - Update QR
- `GET /quotation-request/{id}/edit` - Edit form
- `POST /quotation-request/{id}/complete` - Complete QR
- `POST /quotation-request/{id}/cancel` - Cancel QR

**Users:** SUPERADMIN, ADMIN, REVIEWER (create/modify operations)

### 3. **Receiving Operations** (Warehouse/Inspection)
```java
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'INSPECTOR')")
```
**Endpoint:**
- `POST /quotation-request/{id}/receive` - Receive parts

**Users:** SUPERADMIN, ADMIN, INSPECTOR (warehouse operations)

### 4. **Delete Operations** (Admin Only)
```java
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
```
**Endpoint:**
- `POST /quotation-request/{id}/delete` - Delete QR

**Users:** SUPERADMIN, ADMIN (admin operations)

---

## Role Definitions

| Role | Dashboard | List | View | Create | Edit | Send | Confirm | Receive | Delete |
|------|-----------|------|------|--------|------|------|---------|---------|--------|
| SUPERADMIN | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| REVIEWER | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| INSPECTOR | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| VIEWER | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## Import Added
```java
import org.springframework.security.access.prepost.PreAuthorize;
```

---

## File Modified
- `src/main/java/ahqpck/maintenance/report/controller/QuotationRequestController.java`

---

## Compilation Status
✅ **Successfully compiled** - All annotations properly integrated with Spring Security

---

## Security Behavior

1. **Unauthorized Access**: Users without required roles will receive a `403 Forbidden` response
2. **Access Control**: Spring Security intercepts requests before they reach the controller method
3. **Consistency**: Authorization is enforced at controller level, preventing unauthorized access even if security gaps exist in the service layer

---

## Workflow Authorization

### Quotation Request Workflow
1. **CREATED Status** → REVIEWER can edit, send
2. **SENT Status** → REVIEWER can confirm
3. **CONFIRMED Status** → INSPECTOR can receive parts
4. **DELIVERED Status** → REVIEWER can complete
5. **COMPLETED Status** → Only ADMIN can delete

All operations respect the authorization rules defined above.
