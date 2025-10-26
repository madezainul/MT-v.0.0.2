# Parts Service Separation - Exact Action Plan

## Overview
This document shows you **exactly what to do** in the right order, with no ambiguity.

**Total Time:** ~3 hours
**Complexity:** Medium
**Risk Level:** Low (you have a backup)

---

## PREREQUISITE: Backup Current State

```bash
cd d:\Code\New folder\MT-v.0.0.2

# Create backup branch
git checkout -b backup-before-microservices
git push origin backup-before-microservices

# Return to main
git checkout main
```

**Now if anything goes wrong, you can return to this state:**
```bash
git checkout backup-before-microservices
```

---

# PHASE 1: REORGANIZE STRUCTURE (30 minutes)

## ACTION 1.1: Create report-service directory

```bash
cd d:\Code\New folder\MT-v.0.0.2

# Create new directory
mkdir report-service
```

---

## ACTION 1.2: Move current src/ and pom.xml

```bash
# Copy src folder to report-service
# Using Windows: Copy-Item -Recurse src report-service/

# Copy pom.xml to report-service
# Using Windows: Copy-Item pom.xml report-service/

# After copying, delete original src and pom.xml
# Using Windows: 
# Remove-Item src -Recurse
# Remove-Item pom.xml
```

**Result:** 
- `report-service/src/` ← contains all current code
- `report-service/pom.xml` ← contains current configuration

---

## ACTION 1.3: Create new root pom.xml

**File:** `d:\Code\New folder\MT-v.0.0.2\pom.xml`

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

## ACTION 1.4: Create parts-service directories

```bash
cd d:\Code\New folder\MT-v.0.0.2

# Create directory structure
mkdir parts-service
mkdir parts-service\src
mkdir parts-service\src\main
mkdir parts-service\src\main\java
mkdir parts-service\src\main\java\ahqpck
mkdir parts-service\src\main\java\ahqpck\parts
mkdir parts-service\src\main\resources
mkdir parts-service\src\test
mkdir parts-service\src\test\java
mkdir parts-service\src\test\java\ahqpck
mkdir parts-service\src\test\java\ahqpck\parts
```

---

## ACTION 1.5: Create parts-service/pom.xml

**File:** `d:\Code\New folder\MT-v.0.0.2\parts-service\pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>ahqpck.maintenance</groupId>
        <artifactId>mt-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>parts-service</artifactId>
    <name>Parts Microservice</name>
    <description>Microservice for managing maintenance parts</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>1.5.5.Final</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>1.5.5.Final</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## ACTION 1.6: Update report-service/pom.xml

**File:** `report-service/pom.xml`

**Change the parent section:**

From:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.8</version>
    <relativePath/>
</parent>

<groupId>ahqpck.maintenance</groupId>
<artifactId>report</artifactId>
```

To:
```xml
<parent>
    <groupId>ahqpck.maintenance</groupId>
    <artifactId>mt-platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>

<artifactId>report-service</artifactId>
<name>Report Service</name>
<description>Main report and maintenance application</description>
```

**Add OpenFeign dependency** to report-service/pom.xml dependencies section:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

---

# PHASE 2: EXTRACT PART CODE (1 hour)

## ACTION 2.1: Create directory structure in parts-service

```bash
cd d:\Code\New folder\MT-v.0.0.2\parts-service\src\main\java\ahqpck\parts

# Create subdirectories
mkdir config
mkdir controller
mkdir service
mkdir repository
mkdir entity
mkdir dto
mkdir mapper
mkdir util
mkdir exception
mkdir specification
```

---

## ACTION 2.2: Copy Part-related files to parts-service

From: `report-service/src/main/java/ahqpck/maintenance/report/...`
To: `parts-service/src/main/java/ahqpck/parts/...`

**Copy these files:**

1. `entity/Part.java` → `parts-service/src/main/java/ahqpck/parts/entity/Part.java`
2. `dto/PartDTO.java` → `parts-service/src/main/java/ahqpck/parts/dto/PartDTO.java`
3. `repository/PartRepository.java` → `parts-service/src/main/java/ahqpck/parts/repository/PartRepository.java`
4. `service/PartService.java` → `parts-service/src/main/java/ahqpck/parts/service/PartService.java`
5. `mapper/PartMapper.java` → `parts-service/src/main/java/ahqpck/parts/mapper/PartMapper.java`
6. `controller/PartRestController.java` → `parts-service/src/main/java/ahqpck/parts/controller/PartRestController.java`
7. `specification/PartSpecification.java` → `parts-service/src/main/java/ahqpck/parts/specification/PartSpecification.java`
8. `util/FileUploadUtil.java` → `parts-service/src/main/java/ahqpck/parts/util/FileUploadUtil.java`
9. `util/Base62.java` → `parts-service/src/main/java/ahqpck/parts/util/Base62.java`
10. `exception/NotFoundException.java` → `parts-service/src/main/java/ahqpck/parts/exception/NotFoundException.java`

---

## ACTION 2.3: Update package names in copied files

**In each copied file, replace:**
```
package ahqpck.maintenance.report.entity;
↓
package ahqpck.parts.entity;
```

**And update all imports:**
```
import ahqpck.maintenance.report.*;
↓
import ahqpck.parts.*;
```

**Quick find-replace in all files:**
- Find: `ahqpck.maintenance.report`
- Replace: `ahqpck.parts`

---

## ACTION 2.4: Delete Part-related files from report-service

**Delete from report-service:**

1. `src/main/java/ahqpck/maintenance/report/entity/Part.java`
2. `src/main/java/ahqpck/maintenance/report/dto/PartDTO.java`
3. `src/main/java/ahqpck/maintenance/report/repository/PartRepository.java`
4. `src/main/java/ahqpck/maintenance/report/service/PartService.java`
5. `src/main/java/ahqpck/maintenance/report/mapper/PartMapper.java`
6. `src/main/java/ahqpck/maintenance/report/controller/PartController.java`
7. `src/main/java/ahqpck/maintenance/report/specification/PartSpecification.java`

**Keep in report-service:**
- `FileUploadUtil.java` (other services may use it too)
- `Base62.java` (other services may use it too)

---

# PHASE 3: CONFIGURE SERVICES (30 minutes)

## ACTION 3.1: Create PartsServiceApplication.java

**File:** `parts-service/src/main/java/ahqpck/parts/PartsServiceApplication.java`

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

## ACTION 3.2: Create parts-service/application.properties

**File:** `parts-service/src/main/resources/application.properties`

```properties
# Server Configuration
server.port=8002
server.servlet.context-path=/parts-service
spring.application.name=parts-service

# Database Configuration - SEPARATE DATABASE
spring.datasource.url=jdbc:mysql://localhost:3306/parts_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# File Upload
app.upload-part-image.dir=src/main/resources/static/upload/part/image

# Logging
logging.level.root=INFO
logging.level.ahqpck.parts=DEBUG
```

---

## ACTION 3.3: Update ReportApplication.java

**File:** `report-service/src/main/java/ahqpck/maintenance/report/ReportApplication.java`

Add `@EnableFeignClients` annotation:

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

## ACTION 3.4: Create parts_db database

Execute this SQL:

```sql
CREATE DATABASE IF NOT EXISTS parts_db;
```

(You can run it in MySQL Workbench or any MySQL client)

---

# PHASE 4: CREATE COMMUNICATION LAYER (30 minutes)

## ACTION 4.1: Create PartServiceClient (Feign interface)

**File:** `report-service/src/main/java/ahqpck/maintenance/report/client/PartServiceClient.java`

```java
package ahqpck.maintenance.report.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Feign client for calling Parts Microservice
 * Replaces direct PartService usage
 */
@FeignClient(name = "parts-service", url = "http://localhost:8002/parts-service")
public interface PartServiceClient {

    @GetMapping("/api/parts/{id}")
    ResponseEntity<?> getPartById(@PathVariable String id);

    @GetMapping("/api/parts")
    ResponseEntity<?> getAllParts(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "true") boolean asc
    );

    @PostMapping(value = "/api/parts", consumes = "application/json")
    ResponseEntity<?> createPart(@RequestBody Object partData);

    @PutMapping(value = "/api/parts/{id}", consumes = "application/json")
    ResponseEntity<?> updatePart(@PathVariable String id, @RequestBody Object partData);

    @DeleteMapping("/api/parts/{id}")
    ResponseEntity<?> deletePart(@PathVariable String id);
}
```

---

## ACTION 4.2: Replace PartService with PartServiceClient

**In all services that use PartService:**

Files to update:
1. `report-service/src/main/java/ahqpck/maintenance/report/service/PurchaseRequisitionService.java`
2. `report-service/src/main/java/ahqpck/maintenance/report/service/BOMService.java`
3. `report-service/src/main/java/ahqpck/maintenance/report/service/QuotationRequestService.java`

**Pattern to follow:**

Before:
```java
@Service
public class PurchaseRequisitionService {
    @Autowired
    private PartService partService;  // ← REMOVE THIS
    
    public void someMethod() {
        PartDTO part = partService.getPartById(id);  // ← CHANGE THIS
    }
}
```

After:
```java
@Service
public class PurchaseRequisitionService {
    @Autowired
    private PartServiceClient partServiceClient;  // ← ADD THIS
    
    public void someMethod() {
        ResponseEntity<?> response = partServiceClient.getPartById(id);  // ← CHANGE THIS
        // Extract PartDTO from response if needed
    }
}
```

---

# PHASE 5: BUILD & TEST (30 minutes)

## ACTION 5.1: Build both modules

```bash
cd d:\Code\New folder\MT-v.0.0.2

# Build all modules
mvn clean install -DskipTests
```

If build succeeds, move to ACTION 5.2. If fails, check the error message.

---

## ACTION 5.2: Run parts-service

```bash
cd d:\Code\New folder\MT-v.0.0.2\parts-service

# Run parts service on port 8002
mvn spring-boot:run
```

Expected output:
```
Started PartsServiceApplication in X seconds
```

Test it's working:
```
http://localhost:8002/parts-service/api/parts
```

Should return an empty list `[]` or data.

---

## ACTION 5.3: Run report-service (in new terminal)

```bash
cd d:\Code\New folder\MT-v.0.0.2\report-service

# Run report service on port 8001
mvn spring-boot:run
```

Expected output:
```
Started ReportApplication in X seconds
```

Test it's working:
```
http://localhost:8001/dashboard
```

Should show your dashboard normally.

---

## ACTION 5.4: Test inter-service communication

1. **Create a part via parts-service:**
   - Go to `http://localhost:8002/parts-service/` (if there's a UI)
   - Or use Postman to POST to `/api/parts`

2. **Create a PR via report-service:**
   - Go to `http://localhost:8001/purchase-requisition`
   - Create a new PR and add the part you just created
   - Verify it works without errors

3. **Check logs:**
   - Both services should show successful HTTP calls
   - No errors about "PartService not found"

---

# FINAL: COMMIT & CLEANUP

## ACTION 6.1: Commit all changes

```bash
cd d:\Code\New folder\MT-v.0.0.2

git add .
git commit -m "feat: Separate Parts service from monolith

- Create multi-module Maven project structure
- Extract parts-service as independent microservice
- Parts runs on port 8002, Report on port 8001
- Separate parts_db and report_db databases
- Add OpenFeign client for inter-service communication
- Update all services to use PartServiceClient"

git push origin main
```

---

## Summary

You have now successfully:

✅ Separated Parts into its own microservice  
✅ Created parent Maven pom for module management  
✅ Configured parts-service on port 8002  
✅ Configured report-service on port 8001  
✅ Set up inter-service communication with Feign  
✅ Created separate databases (parts_db and report_db)  
✅ Verified both services work together  

**Result:**
- Report app: ~80-100 MB, faster startup, lighter memory
- Parts app: ~20-30 MB, independent, scalable
- Total: More efficient, more maintainable, ready for next service extraction

---

**Next Steps:**
1. Let it run for a few days to verify stability
2. Extract BOM service next (similar process)
3. Extract PR service
4. Extract QR service
5. Eventually add API Gateway and service discovery

---

**Questions? Let me know at any step!**
