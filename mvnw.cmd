@ECHO OFF
SETLOCAL
SET "ROOT_DIR=%~dp0"
CALL "%ROOT_DIR%spider\mvnw.cmd" %*
