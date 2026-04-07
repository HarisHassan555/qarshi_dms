@echo off
REM Copy to setenv.bat and fill in values. Do not commit real secrets.
REM Example: call this before starting the Spring Boot app from the same shell.

set VELOCITY_DB_URL=jdbc:mysql://localhost:3308/your_database?useSSL=false^&allowPublicKeyRetrieval=true^&useUnicode=true^&characterEncoding=utf8
set VELOCITY_DB_USERNAME=root
set VELOCITY_DB_PASSWORD=changeme

set VELOCITY_SOAP_USERNAME=
set VELOCITY_SOAP_PASSWORD=

set VELOCITY_MAIL_SMTP_USERNAME=
set VELOCITY_MAIL_SMTP_PASSWORD=
