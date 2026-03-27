@ECHO OFF
SETLOCAL
CD /D "%~dp0spider"
CALL mvnw.cmd %*
