# QUICK START: Parts Service Separation

## What You Need to Do (Executive Summary)

### Phase 1: Setup Structure (30 minutes)
1. **Create backup branch:** `git checkout -b backup-before-microservices`
2. **Create new directory:** `report-service/` in MT-v.0.0.2
3. **Move current code:** Move existing `src/` to `report-service/src/`
4. **Move current pom.xml:** Move to `report-service/pom.xml`
5. **Create new root pom.xml** in MT-v.0.0.2 (as aggregator)
6. **Create parts-service directory** with its own pom.xml

### Phase 2: Extract Parts Code (1 hour)
1. **Copy Part-related files** from report-service to parts-service:
   - Entity: `Part.java`
   - DTO: `PartDTO.java`
   - Repository: `PartRepository.java`, `PartSpecification.java`
   - Service: `PartService.java`
   - Mapper: `PartMapper.java`
   - Controller: `PartRestController.java`
   - Utils: `FileUploadUtil.java`, `Base62.java`
   - Exception: `NotFoundException.java`

2. **Update package names:** Change `ahqpck.maintenance.report` → `ahqpck.parts`

3. **Delete from report-service** all Part-related files

### Phase 3: Configure Services (30 minutes)
1. **parts-service:**
   - Create `PartsServiceApplication.java`
   - Create `application.properties` with port 8002, parts_db database
   - Create `pom.xml` with Spring Boot and database dependencies

2. **report-service:**
   - Update `pom.xml` to reference parent and add OpenFeign dependency
   - Update `ReportApplication.java` to enable Feign clients
   - Create separate `application.properties` for report_db (keep current)

3. **Database:**
   - Create new MySQL database: `parts_db`

### Phase 4: Create Communication Layer (30 minutes)
1. **Create Feign client** in report-service:
   - New file: `report-service/src/main/java/ahqpck/maintenance/report/client/PartServiceClient.java`
   - Defines REST interface to call parts-service

2. **Update all services** that use PartService:
   - Replace `@Autowired private PartService partService;`
   - With: `@Autowired private PartServiceClient partServiceClient;`
   - Update method calls accordingly

### Phase 5: Test & Verify (30 minutes)
1. **Build both modules:**
   ```bash
   mvn clean install
   ```

2. **Run parts-service:**
   ```bash
   cd parts-service
   mvn spring-boot:run  # Runs on port 8002
   ```

3. **Run report-service** (in new terminal):
   ```bash
   cd report-service
   mvn spring-boot:run  # Runs on port 8001
   ```

4. **Test functionality:**
   - Create a part in parts-service
   - Create PR/QR using that part
   - Verify it calls parts-service successfully

---

## File Structure After Separation

```
MT-v.0.0.2/
├── pom.xml                          ← NEW (parent/aggregator)
├── PARTS_SEPARATION_GUIDE.md        ← This file
├── MICROSERVICES_ARCHITECTURE.md
├── report-service/                  ← MOVED (was root src)
│   ├── pom.xml
│   ├── src/main/java/ahqpck/maintenance/report/
│   │   ├── ReportApplication.java (updated with @EnableFeignClients)
│   │   ├── client/
│   │   │   └── PartServiceClient.java  ← NEW
│   │   ├── service/
│   │   │   ├── PurchaseRequisitionService.java (updated - uses Feign)
│   │   │   ├── BOMService.java (updated - uses Feign)
│   │   │   ├── QuotationRequestService.java (updated - uses Feign)
│   │   │   └── (other services - Parts code removed)
│   │   ├── controller/ (PartController removed)
│   │   ├── entity/ (Part.java removed)
│   │   ├── dto/ (PartDTO.java removed)
│   │   ├── repository/ (PartRepository.java removed)
│   │   └── specification/ (PartSpecification.java removed)
│   └── src/main/resources/application.properties
│
├── parts-service/                   ← NEW microservice
│   ├── pom.xml                      ← NEW
│   ├── src/main/java/ahqpck/parts/
│   │   ├── PartsServiceApplication.java    ← NEW
│   │   ├── entity/
│   │   │   └── Part.java            ← MOVED
│   │   ├── dto/
│   │   │   └── PartDTO.java         ← MOVED
│   │   ├── service/
│   │   │   └── PartService.java     ← MOVED
│   │   ├── controller/
│   │   │   └── PartRestController.java ← MOVED
│   │   ├── repository/
│   │   │   ├── PartRepository.java  ← MOVED
│   │   │   └── PartSpecification.java ← MOVED
│   │   ├── mapper/
│   │   │   └── PartMapper.java      ← MOVED
│   │   ├── util/
│   │   │   ├── FileUploadUtil.java  ← MOVED
│   │   │   └── Base62.java          ← MOVED
│   │   └── exception/
│   │       └── NotFoundException.java ← MOVED
│   └── src/main/resources/
│       ├── application.properties    ← NEW (port 8002, parts_db)
│       └── static/upload/part/image/ ← File upload directory
```

---

## Database Changes

### Current (Monolith)
```
report_db (single database)
├── users
├── parts ← ← to be moved
├── purchase_requisitions
├── quotation_requests
└── ...
```

### After Separation
```
report_db (report-service database)     parts_db (parts-service database)
├── users                               ├── parts ← ← moved
├── purchase_requisitions               ├── part_categories (if needed)
├── quotation_requests                  ├── part_suppliers (if needed)
├── equipment                           └── part_sections (if needed)
└── ...
```

---

## Port Configuration

| Service | Port | Context Path | Database |
|---------|------|--------------|----------|
| Report Service | 8001 | `/` | report_db |
| Parts Service | 8002 | `/parts-service` | parts_db |

---

## What Each Step Accomplishes

| Step | What Happens | File Size Reduction |
|------|--------------|------------------|
| **Before** | Monolith 106 MB | - |
| **After Step 3** | 2 modules, separate code | Report: ~80 MB, Parts: ~20 MB |
| **After Step 4** | Feign communication | Same size, but services independent |
| **After Step 5** | Full separation | Each service runs independently |

---

## Key Differences You'll Notice

### Now (Monolith)
```
Start: java -jar report-0.0.1-SNAPSHOT.jar
Result: Single JVM (8001) with everything inside
```

### After Separation
```
Start Part 1: java -jar parts-service-0.0.1-SNAPSHOT.jar
Start Part 2: java -jar report-service-0.0.1-SNAPSHOT.jar
Result: 2 separate JVMs communicating via REST
```

---

## Commands You'll Use

```bash
# At root MT-v.0.0.2 directory
mvn clean install                    # Build all modules

# Run parts-service
cd parts-service
mvn spring-boot:run

# In another terminal, run report-service
cd report-service
mvn spring-boot:run

# Test parts API
curl http://localhost:8002/parts-service/api/parts

# Test report app
http://localhost:8001/dashboard
```

---

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| "Part entity not found" | Code not moved | Check parts-service has Part.java |
| Port 8001/8002 conflict | Service already running | Kill existing Java process |
| "parts_db doesn't exist" | Database not created | Run: `CREATE DATABASE parts_db;` |
| Feign client error | URL wrong | Check application.properties has correct service URL |
| "PartServiceClient not found" | Feign not enabled | Add @EnableFeignClients to ReportApplication.java |

---

## Success Criteria

✅ Parts service starts on port 8002  
✅ Report service starts on port 8001  
✅ Can create/read/update/delete parts via parts-service API  
✅ Can create PR using parts from parts-service  
✅ No errors in logs about missing classes  
✅ File uploads still work  
✅ Database has separate parts_db and report_db  

---

## Next Steps After This Works

1. **Separation confirmed?** → Move to BOM service extraction
2. **Need to scale?** → Run multiple instances of parts-service
3. **Want load balancing?** → Add Eureka service discovery (optional)
4. **Want API gateway?** → Add Spring Cloud Gateway (optional)

---

## Timeline Estimate

| Phase | Time | Status |
|-------|------|--------|
| Phase 1: Setup Structure | 30 min | Ready to start |
| Phase 2: Extract Code | 1 hour | Ready to start |
| Phase 3: Configure | 30 min | Ready to start |
| Phase 4: Communication | 30 min | Ready to start |
| Phase 5: Test | 30 min | Ready to start |
| **Total** | **~3 hours** | |

---

**Ready to start? Let me know and I'll guide you through each step!**
