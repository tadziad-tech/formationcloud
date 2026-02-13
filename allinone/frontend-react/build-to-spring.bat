@echo off
setlocal
cd /d %~dp0
if not exist node_modules (
  echo Installing dependencies...
  npm install
)

echo Building React for production...
npm run build

set TARGET=..\src\main\resources\static\app

if exist "%TARGET%" (
  rmdir /s /q "%TARGET%"
)
mkdir "%TARGET%"

xcopy /E /I /Y dist\* "%TARGET%\" >nul

echo.
echo OK. React copied to Spring Boot static folder.
echo Open: http://localhost:8080/app/#/login
pause
