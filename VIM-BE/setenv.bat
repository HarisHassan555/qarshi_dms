@echo off
rem ---------------------------------------------------------------------------
rem Tomcat environment variables for Spring Boot (WAR deployment)
rem Place this file in:
rem   %CATALINA_BASE%\bin\setenv.bat
rem or
rem   %CATALINA_HOME%\bin\setenv.bat
rem ---------------------------------------------------------------------------

rem Mail (maps to mail.smtp.* and mail.debug in application.properties)
set "VELOCITY_MAIL_SMTP_HOST=smtp.gmail.com"
set "VELOCITY_MAIL_SMTP_PORT=465"
set "VELOCITY_MAIL_SMTP_AUTH=true"
set "VELOCITY_MAIL_SMTP_SSL_ENABLE=true"
set "VELOCITY_MAIL_SMTP_STARTTLS_ENABLE=false"
set "VELOCITY_MAIL_SMTP_USERNAME=talhahabib956@gmail.com"
set "VELOCITY_MAIL_SMTP_PASSWORD=wabxdvqupoueuwon"
set "VELOCITY_MAIL_DEBUG=false"

rem Optional: keep DB/SOAP secrets here too (already env-backed in properties)
set "VELOCITY_DB_USERNAME=root"
set "VELOCITY_DB_PASSWORD=strongpassword"
set "VELOCITY_DB_URL=jdbc:mysql://127.0.0.1:3306/velocity?useSSL=false&logAbandoned=true&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&sessionVariables=sort_buffer_size=2097152"
set "VELOCITY_SOAP_USERNAME=SAP-TMX"
set "VELOCITY_SOAP_PASSWORD=SaP@ExD123"

rem Optional: app-specific incident logging controls
set "VELOCITY_APP_LOG_HIKARI_LEVEL=INFO"
set "VELOCITY_APP_LOG_TX_LEVEL=INFO"
set "VELOCITY_APP_LOG_SQL_LEVEL=INFO"
set "VELOCITY_APP_LOG_BIND_LEVEL=INFO"
