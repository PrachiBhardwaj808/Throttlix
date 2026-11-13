# Environment Setup & Local Developer Prerequisites — Throttlix

## 1. Overview
This guide provides step-by-step instructions for configuring a local developer machine or CI workstation for building, running, and testing the **Throttlix** platform.

---

## 2. Hardware & Software Prerequisites

| Component | Minimum Specification | Recommended Specification |
| :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS 12+, Ubuntu 22.04+ | 64-bit OS |
| **CPU** | 2 Cores | 4+ Cores |
| **RAM** | 8 GB | 16 GB |
| **Disk Space** | 5 GB free disk space | 10 GB SSD |

---

## 3. Required Development Tools

### 3.1 Java Development Kit (JDK 21 LTS)
1. Install JDK 21 (Eclipse Temurin, Oracle JDK, or Amazon Corretto).
2. Configure environment variable:
   - `JAVA_HOME`: Path to JDK 21 directory (e.g., `C:\Program Files\Java\jdk-21` or `/usr/lib/jvm/java-21-openjdk`).
   - Add `%JAVA_HOME%\bin` (Windows) or `$JAVA_HOME/bin` (Unix) to your system `PATH`.
3. Verify installation:
   ```bash
   java -version
   # Expected: java version "21.0.x"
   ```

### 3.2 Apache Maven (3.9+)
1. Download Apache Maven 3.9+ from [maven.apache.org](https://maven.apache.org/).
2. Add the Maven `bin` directory to your system `PATH`.
3. Verify installation:
   ```bash
   mvn -version
   # Expected: Apache Maven 3.9+ or 3.10+
   ```

### 3.3 Apache Tomcat 10.1+
1. Download Apache Tomcat 10.1 (Core .zip or tar.gz) from [tomcat.apache.org](https://tomcat.apache.org/).
   - *Note:* Tomcat 10.1+ is mandatory because it implements the **Jakarta EE 10 / Servlet 6.0** specification. Tomcat 9 (javax.servlet) is incompatible.
2. Set `CATALINA_HOME` environment variable pointing to the extracted Tomcat directory.

### 3.4 PostgreSQL (15+)
1. Install PostgreSQL 15 or 16 from [postgresql.org](https://www.postgresql.org/).
2. Ensure the PostgreSQL service is active on standard port `5432`.
3. Verify connection:
   ```bash
   psql -U postgres -c "SELECT version();"
   ```

### 3.5 Node.js & npm (Frontend Tooling)
1. Install Node.js 20+ LTS or 22+ from [nodejs.org](https://nodejs.org/).
2. Verify installation:
   ```bash
   node -v   # Expected: v20.x or higher
   npm -v    # Expected: 10.x or higher
   ```

---

## 4. Repository Initialization & Verification Checklist

1. **Clone the repository:**
   ```bash
   git clone https://github.com/PrachiBhardwaj808/Throttlix.git
   cd Throttlix
   ```
2. **Compile Backend:**
   ```bash
   cd backend
   mvn clean test
   ```
3. **Verify Frontend Build:**
   ```bash
   cd ../frontend
   npm install
   npm run build
   ```
4. **Deploy Schema to Local Database:**
   ```bash
   cd ..
   psql -U postgres -d postgres -f database/schema.sql
   ```
