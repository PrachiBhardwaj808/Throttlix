# Deployment Guide — Throttlix

## 1. Overview
This document guides administrators and developers through deploying the **Throttlix** backend web archive (WAR) to **Apache Tomcat 10.1+** and hosting the React frontend dashboard.

---

## 2. Deployment Topology (Standalone Server)

```text
┌────────────────────────────────────────────────────────┐
│                   Host Machine / Server                │
│                                                        │
│  ┌─────────────────────────┐  ┌─────────────────────┐  │
│  │   Apache Tomcat 10.1+   │  │   PostgreSQL 15+    │  │
│  │  ┌───────────────────┐  │  │  ┌───────────────┐  │  │
│  │  │ throttlix-backend │  │  │  │ Schema & Tables│ │  │
│  │  │ .war archive      │  │  │  │ (Port 5432)    │  │
│  │  │ (Port 8080)       │  │  │  └───────────────┘  │  │
│  │  └───────────────────┘  │  └─────────────────────┘  │
│  └─────────────────────────┘                           │
│               ▲                           ▲            │
│               │ JDBC                      │            │
│  ┌─────────────────────────┐              │            │
│  │  React Dashboard (Vite) │──────────────┘            │
│  │  (Port 5173 / Static)   │                           │
│  └─────────────────────────┘                           │
└────────────────────────────────────────────────────────┘
```

---

## 3. Backend Deployment Steps

### Step 1: Prepare Database
1. Connect to PostgreSQL and create the application database and user:
   ```sql
   CREATE DATABASE throttlix_db;
   CREATE USER throttlix_user WITH ENCRYPTED PASSWORD 'your_secure_password';
   GRANT ALL PRIVILEGES ON DATABASE throttlix_db TO throttlix_user;
   ```
2. Execute the initialization DDL script:
   ```bash
   psql -U throttlix_user -d throttlix_db -f database/schema.sql
   ```

### Step 2: Configure Environment Properties
Update `backend/src/main/resources/application.properties` (or supply environment variables):
```properties
db.url=jdbc:postgresql://localhost:5432/throttlix_db
db.username=throttlix_user
db.password=your_secure_password
db.pool.maxSize=10
```

### Step 3: Package the Web Application Archive (WAR)
```bash
cd backend
mvn clean package
```
This generates `target/throttlix-backend.war`.

### Step 4: Deploy to Apache Tomcat
1. Copy the generated WAR file into the Tomcat webapps directory:
   ```bash
   cp target/throttlix-backend.war $CATALINA_HOME/webapps/
   ```
2. Start Apache Tomcat:
   - **Linux / macOS:** `$CATALINA_HOME/bin/startup.sh`
   - **Windows:** `%CATALINA_HOME%\bin\startup.bat`
3. Verify deployment health:
   ```bash
   curl http://localhost:8080/throttlix-backend/api/health
   ```
   **Expected Response:**
   ```json
   {
       "status": "UP",
       "application": "Throttlix",
       "message": "Backend is running successfully"
   }
   ```

---

## 4. Frontend Deployment Steps

### Option A: Local Development Server
```bash
cd frontend
npm install
npm run dev
```
The dashboard runs at `http://localhost:5173`.

### Option B: Production Static Build
```bash
cd frontend
npm run build
```
Deploy the generated `frontend/dist/` directory to an Nginx, Apache HTTP Server, or serve as static resources.
