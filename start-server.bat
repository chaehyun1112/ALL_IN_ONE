@echo off
cd /d "C:\Users\SMHRD\Desktop\ALL_IN_ONE"

:loop
"C:\Program Files\Java\jdk-21.0.12\bin\java.exe" -jar "C:\Users\SMHRD\Desktop\ALL_IN_ONE\target\hospital-safety-0.0.1-SNAPSHOT.jar" 2>> "C:\Users\SMHRD\Desktop\ALL_IN_ONE\launch-error.log"
timeout /t 5 /nobreak >nul
goto loop
