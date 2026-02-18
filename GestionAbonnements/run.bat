@echo off
echo ============================================================
echo  GestionAbonnements - Compilation et execution via Maven
echo ============================================================
echo.

REM Utilisation du JDK bundled avec IntelliJ IDEA (JBR 21)
set JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr
set MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd

echo JAVA_HOME = %JAVA_HOME%
echo.

REM Compilation
echo [1/2] Compilation du projet...
call "%MVN_CMD%" compile -q
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERREUR de compilation. Relancez avec :
    echo   call "%MVN_CMD%" compile
    pause
    exit /b 1
)
echo Compilation reussie!
echo.

REM Execution via le plugin JavaFX Maven
echo [2/2] Lancement de l'application JavaFX...
call "%MVN_CMD%" javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERREUR au lancement. Verifiez la configuration de la base de donnees.
    pause
    exit /b 1
)

pause
