# Parts Service Separation - Step-by-Step Implementation Guide

## Overview
Transform the monolithic Report Application into a multi-module Maven project by extracting the Parts service into its own module.

**Timeline:** ~2-3 hours
**Complexity:** Medium
**Risk:** Low (if done carefully)

---

## STEP 1: Backup Current Project
```bash
# Create a backup branch
git checkout -b backup-before-microservices
git push origin backup-before-microservices

# Return to main
git checkout main
```

---

## STEP 2: Create Root Parent pom.xml

**Location:** `d:\Code\New folder\MT-v.0.0.2\pom.xml`

This becomes the aggregator pom for all modules.

**Key points:**
- Move Spring Boot parent to child modules only (or keep here for shared version management)
- Define modules: report-service, parts-service
- Shared properties for all modules

---

## STEP 3: Reorganize Project Structure

**Current structure:**
```
MT-v.0.0.2/
├── pom.xml (current monolith)
├── src/
│   ├── main/
│   │   ├── java/ahqpck/maintenance/report/...
│   │   └── resources/
│   └── test/
└── target/
```

**New structure:**
```
MT-v.0.0.2/
├── pom.xml (NEW - parent/aggregator)
├── report-service/
│   ├── pom.xml (child)
│   └── src/ (move existing src here)
└── parts-service/
    ├── pom.xml (child)
    └── src/
        ├── main/java/ahqpck/parts/...
        └── resources/
```

**Actions needed:**
1. Create directory: `report-service/`
2. Move everything from current src/ into `report-service/src/`
3. Move current pom.xml to `report-service/pom.xml`
4. Update report-service/pom.xml (remove parent Spring Boot, make it child of new parent)
5. Create new parent pom.xml at root

---

## STEP 4: Update report-service/pom.xml

Remove the Spring Boot parent declaration and make it reference the root parent:

**Before:**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.8</version>
</parent>
```

**After:**
```xml
<parent>
    <groupId>ahqpck.maintenance</groupId>
    <artifactId>mt-platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>report-service</artifactId>
<name>Report Service</name>
```

Also add **OpenFeign dependency** for calling parts-service:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>4.1.3</version>
</dependency>
```

---

## STEP 5: Create Root Parent pom.xml

**Location:** `d:\Code\New folder\MT-v.0.0.2\pom.xml`

**Content:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>ahqpck.maintenance</groupId>
    <artifactId>mt-platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Maintenance Platform</name>
    <description>Multi-module microservices platform</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.8</version>
        <relativePath/>
    </parent>

    <modules>
        <module>report-service</module>
        <module>parts-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-cloud.version>2024.0.0</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## STEP 6: Create parts-service/pom.xml

**Location:** `d:\Code\New folder\MT-v.0.0.2\parts-service\pom.xml`

**Key points:**
- Much smaller and simpler than report-service
- Only needs Part-related dependencies
- Parent references root pom.xml

---

## STEP 7: Move Part-Related Code to parts-service

**Files to move from report-service to parts-service:**

```
ENTITIES:
- src/main/java/ahqpck/parts/entity/Part.java

DTOS:
- src/main/java/ahqpck/parts/dto/PartDTO.java

REPOSITORIES:
- src/main/java/ahqpck/parts/repository/PartRepository.java

MAPPERS:
- src/main/java/ahqpck/parts/mapper/PartMapper.java

SERVICES:
- src/main/java/ahqpck/parts/service/PartService.java

CONTROLLERS:
- src/main/java/ahqpck/parts/controller/PartRestController.java

SPECIFICATIONS:
- src/main/java/ahqpck/parts/specification/PartSpecification.java

UTILITIES:
- src/main/java/ahqpck/parts/util/FileUploadUtil.java
- src/main/java/ahqpck/parts/util/Base62.java

EXCEPTIONS:
- src/main/java/ahqpck/parts/exception/NotFoundException.java
```

**Package naming changes:**
- `ahqpck.maintenance.report.*` → `ahqpck.parts.*`

---

## STEP 8: Create PartsServiceApplication.java

**Location:** `parts-service/src/main/java/ahqpck/parts/PartsServiceApplication.java`

```java
package ahqpck.parts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PartsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PartsServiceApplication.class, args);
    }
}
```

---

## STEP 9: Create parts-service/application.properties

**Location:** `parts-service/src/main/resources/application.properties`

```properties
# Server
server.port=8002
server.servlet.context-path=/parts-service
spring.application.name=parts-service

# Database - SEPARATE from report-service
spring.datasource.url=jdbc:mysql://localhost:3306/parts_db?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# File Upload
app.upload-part-image.dir=src/main/resources/static/upload/part/image

# Logging
logging.level.root=INFO
logging.level.ahqpck.parts=DEBUG
```

---

## STEP 10: Create parts_db Database

```sql
CREATE DATABASE IF NOT EXISTS parts_db;
USE parts_db;

-- Parts table will be auto-created by JPA/Hibernate
-- Just need the database to exist
```

---

## STEP 11: Update report-service/application.properties

**Remove or comment out:**
```properties
# No longer needed - parts handled by parts-service
# app.upload-part-image.dir=...
```

**Keep:** Everything else for report database connection

---

## STEP 12: Clean Up report-service

**Remove from report-service:**

1. Delete Part-related folders:
   - `src/main/java/ahqpck/maintenance/report/entity/Part.java`
   - `src/main/java/ahqpck/maintenance/report/dto/PartDTO.java`
   - `src/main/java/ahqpck/maintenance/report/repository/PartRepository.java`
   - `src/main/java/ahqpck/maintenance/report/mapper/PartMapper.java`
   - `src/main/java/ahqpck/maintenance/report/service/PartService.java`
   - `src/main/java/ahqpck/maintenance/report/controller/PartController.java`
   - `src/main/java/ahqpck/maintenance/report/specification/PartSpecification.java`

2. Keep the utilities that other services also use (or duplicate them)

---

## STEP 13: Create PartServiceClient (Feign Interface)

**Location:** `report-service/src/main/java/ahqpck/maintenance/report/client/PartServiceClient.java`

```java
package ahqpck.maintenance.report.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "parts-service", url = "http://localhost:8002/parts-service")
public interface PartServiceClient {

    @GetMapping("/api/parts/{id}")
    ResponseEntity<?> getPartById(@PathVariable String id);

    @GetMapping("/api/parts")
    ResponseEntity<?> getAllParts(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    );

    @PostMapping(value = "/api/parts", consumes = "multipart/form-data")
    ResponseEntity<?> createPart(
        @RequestPart("partDTO") String partDTOJson,
        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    );

    @PutMapping(value = "/api/parts/{id}", consumes = "multipart/form-data")
    ResponseEntity<?> updatePart(
        @PathVariable String id,
        @RequestPart("partDTO") String partDTOJson,
        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    );

    @DeleteMapping("/api/parts/{id}")
    ResponseEntity<?> deletePart(@PathVariable String id);
}
```

---

## STEP 14: Enable Feign in report-service

**Update:** `report-service/src/main/java/ahqpck/maintenance/report/ReportApplication.java`

```java
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "ahqpck.maintenance.report.client")
public class ReportApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }
}
```

---

## STEP 15: Replace PartService Usage

**In all report-service files that use PartService:**

**Before:**
```java
@Service
public class PurchaseRequisitionService {
    private final PartService partService;
    
    public void addPartToPR(String partId) {
        PartDTO part = partService.getPartById(partId);
        // ...
    }
}
```

**After:**
```java
@Service
public class PurchaseRequisitionService {
    private final PartServiceClient partServiceClient;
    
    public void addPartToPR(String partId) {
        ResponseEntity<?> response = partServiceClient.getPartById(partId);
        // Extract PartDTO from response
        // ...
    }
}
```

**Files to update (check for PartService imports):**
- `PurchaseRequisitionService.java`
- `BOMService.java`
- `QuotationRequestService.java`
- Any controller using PartService

---

## STEP 16: Test the Separation

### **Test 1: Build modules independently**
```bash
cd report-service
mvn clean compile

cd ../parts-service
mvn clean compile
```

### **Test 2: Run parts-service first**
```bash
cd parts-service
mvn spring-boot:run
# Should start on port 8002
# Check: http://localhost:8002/parts-service/api/parts
```

### **Test 3: Run report-service**
```bash
cd report-service
mvn spring-boot:run
# Should start on port 8001
# Check: http://localhost:8001/dashboard
```

### **Test 4: Test inter-service communication**
- Create a part in parts-service
- Create a PR in report-service using that part
- Verify it calls parts-service successfully

---

## STEP 17: Verify Everything Works

**Checklist:**
- [ ] Parts service starts on port 8002
- [ ] Report service starts on port 8001
- [ ] Both services connect to their respective databases
- [ ] Can create, read, update, delete parts
- [ ] Can create PR/QR using parts from parts-service
- [ ] File uploads work in parts-service
- [ ] No error logs about missing classes

---

## STEP 18: Commit Changes

```bash
git add .
git commit -m "feat: Separate Parts service from monolith

- Create multi-module Maven structure
- Extract parts-service as independent module
- Configure separate parts_db database
- Add OpenFeign client for inter-service communication
- Parts service runs on port 8002
- Report service runs on port 8001"
git push origin main
```

---

## Rollback Plan (If Something Goes Wrong)

```bash
git checkout backup-before-microservices
# Back to original state
```

---

## Summary of Changes

| Component | Change |
|-----------|--------|
| Database | Add new `parts_db` database |
| Port | Parts service on 8002 (report still on 8001) |
| Modules | 2 Maven modules (report-service, parts-service) |
| Dependencies | Added OpenFeign to report-service |
| File upload | Separate upload dir for parts-service |
| Code | Parts code moved to parts-service |

---

## Expected Result

```
http://localhost:8001 → ReportApplication (reports, PR, QR, BOM, etc.)
http://localhost:8002 → PartsApplication (parts only)

Both services work together via REST API (Feign)
Completely independent codebases and databases
```

---

**Ready to start implementation? Let me know which step you want to begin with!**
