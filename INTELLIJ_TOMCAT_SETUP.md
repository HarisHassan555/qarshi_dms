# IntelliJ IDEA Tomcat Configuration Guide for VIM Project

This guide explains how to configure IntelliJ IDEA to run the VIM (Vendor Invoice Management) backend project on Apache Tomcat.

## Project Overview

- **Project Type**: Spring Boot WAR application
- **Main Class**: `com.bezkoder.spring.login.SpringBootSecurityJwtApplication`
- **Packaging**: WAR
- **Final WAR Name**: `VIM.war`
- **Context Path**: `/VIM`
- **Java Version**: 17
- **Build Tool**: Maven

## Prerequisites

1. **IntelliJ IDEA** (Ultimate Edition - required for Tomcat support)
2. **Apache Tomcat** (version 9.x or 10.x recommended)
3. **Java 17 JDK** installed and configured
4. **Maven** (or use Maven wrapper included in project)

## Step 1: Install and Configure Tomcat

1. Download Apache Tomcat from https://tomcat.apache.org/
2. Extract it to a location like `C:\apache-tomcat-9.0.xx` or `C:\Program Files\Apache Software Foundation\Tomcat 9.0`
3. Note the Tomcat installation path - this will be your **CATALINA_HOME**

## Step 2: Configure Tomcat in IntelliJ IDEA

### 2.1 Add Tomcat Server

1. Open IntelliJ IDEA
2. Go to **File** → **Settings** (or **Ctrl+Alt+S**)
3. Navigate to **Build, Execution, Deployment** → **Application Servers**
4. Click the **+** button and select **Tomcat Server**
5. In the dialog:
   - **Name**: `Tomcat 9.0` (or your version)
   - **Tomcat Home**: Browse to your Tomcat installation directory (e.g., `C:\apache-tomcat-9.0.xx`)
   - IntelliJ will automatically detect **CATALINA_BASE** and **CATALINA_HOME**
6. Click **OK**

### 2.2 CATALINA_BASE Configuration

**CATALINA_BASE** is automatically set by IntelliJ IDEA when you configure the Tomcat server. It typically points to:
- **Windows**: `C:\Users\<YourUsername>\.IntelliJIdea<version>\system\tomcat\<ServerName>`
- Or a custom location if you specify it

**For this project, you can use the default IntelliJ-managed CATALINA_BASE**, which is recommended because:
- IntelliJ manages it automatically
- It's isolated per run configuration
- No manual cleanup needed

**If you need a custom CATALINA_BASE:**
- In the Tomcat server configuration, you can specify a custom base directory
- Recommended location: `C:\Users\<YourUsername>\tomcat-base-vim` or similar

## Step 3: Create Tomcat Run Configuration

### 3.1 Create Run Configuration

1. Click on **Run** → **Edit Configurations...** (or click the dropdown next to the run button)
2. Click the **+** button and select **Tomcat Server** → **Local**
3. Configure the following:

#### **General Tab:**
- **Name**: `VIM Tomcat`
- **Application server**: Select your configured Tomcat server
- **Open browser**: Check this if you want to open browser automatically
- **URL**: `http://localhost:8080/VIM` (matches your context path)

#### **Deployment Tab:**
1. Click the **+** button under "Deploy at the server startup"
2. In the popup menu, select **Artifact...**
3. In the "Select Artifact to Deploy" dialog, you should see:
   - **VIM:war** (this is the WAR artifact built from your Maven project)
   - OR **vendor-invoice-management:war** (if the artifact name is different)
4. Select the WAR artifact and click **OK**
5. After adding the artifact, you'll see it in the list with a column for **Application context**
6. In the **Application context** column, enter: `/VIM`
   - This must match your `server.servlet.context-path=/VIM` from `application.properties`
7. The **Type** should automatically be set to **war exploded** or **war**

**What gets deployed:**
- The WAR file will be automatically deployed to: `%CATALINA_BASE%\webapps\VIM`
- IntelliJ manages this automatically - you don't need to manually specify a deployment directory
- The physical deployment happens in: `C:\Users\<YourUsername>\.IntelliJIdea<version>\system\tomcat\<ServerName>\webapps\VIM`

**Note**: If you don't see the WAR artifact in the list:
- First build the project: Right-click `VIM-BE/pom.xml` → **Maven** → **Reload Project**
- Then build: **Maven** tool window → **Lifecycle** → **package**
- The artifact should appear after building

#### **Server Tab:**
- **HTTP port**: `8080` (default, change if needed)
- **JMX port**: `1099` (default)
- **VM options**: Add if needed:
  ```
  -Dfile.encoding=UTF-8
  -Duser.timezone=UTC
  --add-opens java.base/java.security=ALL-UNNAMED
  ```
- **On 'Update' action**: Select **Update classes and resources** (for hot reload)
- **On frame deactivation**: Select **Update classes and resources**

#### **Startup/Connection Tab:**
- **Port**: `8080`
- **JMX port**: `1099`

## Step 4: Build the WAR File

Before running, you need to build the WAR file:

### Option 1: Using IntelliJ Maven Tool Window
1. Open **View** → **Tool Windows** → **Maven**
2. Expand **VIM-BE** → **Lifecycle**
3. Double-click **package** (or right-click → **Run Maven Goal**)
4. The WAR file will be created in `VIM-BE\target\VIM.war`

### Option 2: Using Terminal
```bash
cd VIM-BE
mvn clean package
```

## Step 5: Database Configuration

Before running, ensure your MySQL database is configured:

1. **Database**: `vim_3`
2. **Port**: `3308` (as per `application.properties`)
3. **Username**: `root`
4. **Password**: `root`

Update `VIM-BE\src\main\resources\application.properties` if your database settings differ.

## Step 6: Run the Application

1. Select the **VIM Tomcat** configuration from the run configuration dropdown
2. Click the **Run** button (green play icon) or press **Shift+F10**
3. IntelliJ will:
   - Build the project (if needed)
   - Deploy the WAR to Tomcat
   - Start Tomcat server
   - Open the browser (if configured)

4. Access your application at: `http://localhost:8080/VIM`

## Step 7: Verify Deployment

### Check Deployment Directory

The deployment happens in one of these locations:
- **IntelliJ-managed**: `C:\Users\<YourUsername>\.IntelliJIdea<version>\system\tomcat\<ServerName>\webapps\VIM`
- **Custom CATALINA_BASE**: `<YourCustomBase>\webapps\VIM`

### Check Logs

- **IntelliJ Console**: Shows Tomcat startup logs and application logs
- **Tomcat Logs**: Located in `%CATALINA_BASE%\logs\`
  - `catalina.out` - Main log file
  - `localhost.log` - Application-specific logs

## Troubleshooting

### Issue: WAR file not found
**Solution**: Build the project first using Maven (`mvn clean package`)

### Issue: Port 8080 already in use
**Solution**: 
- Change the port in the Server tab of run configuration
- Or stop the service using port 8080

### Issue: Context path mismatch
**Solution**: Ensure the Application context in Deployment tab matches `server.servlet.context-path` in `application.properties` (should be `/VIM`)

### Issue: Database connection errors
**Solution**: 
- Verify MySQL is running on port 3308
- Check database credentials in `application.properties`
- Ensure database `vim_3` exists

### Issue: ClassNotFoundException or NoClassDefFoundError
**Solution**: 
- Clean and rebuild: `mvn clean package`
- Invalidate IntelliJ caches: **File** → **Invalidate Caches / Restart**

## Summary of Key Settings

| Setting | Value |
|---------|-------|
| **Server Type** | Tomcat Server (Local) |
| **HTTP Port** | 8080 |
| **Application Context** | `/VIM` |
| **WAR Artifact** | `VIM:war` |
| **CATALINA_BASE** | Auto-managed by IntelliJ (recommended) |
| **Deployment Directory** | `%CATALINA_BASE%\webapps\VIM` |
| **Main Class** | `com.bezkoder.spring.login.SpringBootSecurityJwtApplication` |
| **Java Version** | 17 |

## Alternative: Run as Spring Boot Application

If you prefer to run as a standalone Spring Boot application (without external Tomcat):

1. Create a **Spring Boot** run configuration instead
2. Main class: `com.bezkoder.spring.login.SpringBootSecurityJwtApplication`
3. This will use the embedded Undertow server (as configured in `pom.xml`)

However, since the project is packaged as WAR and extends `SpringBootServletInitializer`, deploying to Tomcat is the recommended approach for production-like testing.


