@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul
title Instalador - Cerrajeria Lujan
color 0A

echo.
echo ==========================================================
echo   INSTALADOR CERRAJERIA LUJAN
echo   MariaDB portable + Aplicacion
echo   Sin permisos de administrador
echo ==========================================================
echo.

:: ============================================================
:: 0) Detectar carpeta de destino
:: ============================================================
set "INSTALL_DIR=%LOCALAPPDATA%\CerrajeriaLujan"
echo Carpeta de instalacion: %INSTALL_DIR%
echo.

if exist "%INSTALL_DIR%" (
    set /p CONFIRM="Ya existe una instalacion. Sobreescribir? (s/n): "
    if /i "!CONFIRM!" neq "s" (
        echo Cancelado.
        pause
        exit /b 1
    )
)

:: ============================================================
:: 1) Verificar archivos del paquete
:: ============================================================
for %%F in (mariadb.zip app.zip sql\schema.sql sql\datos_iniciales.sql) do (
    if not exist "%%F" (
        echo [ERROR] Falta el archivo: %%F
        pause
        exit /b 1
    )
)
echo [OK] Paquete completo.

:: ============================================================
:: 2) Crear estructura y extraer
:: ============================================================
echo.
echo [1/5] Copiando archivos...
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
if not exist "%INSTALL_DIR%\sql" mkdir "%INSTALL_DIR%\sql"

echo Extrayendo MariaDB...
powershell -NoProfile -Command "Expand-Archive -Path 'mariadb.zip' -DestinationPath '%INSTALL_DIR%\mariadb' -Force"
if errorlevel 1 (
    echo [ERROR] No se pudo extraer MariaDB
    pause
    exit /b 1
)

:: Algunos zips extraen con subcarpeta mariadb-x.x.x-winx64\
:: Detectar y aplanar
for /d %%D in ("%INSTALL_DIR%\mariadb\*") do (
    if exist "%%D\bin\mariadbd.exe" (
        echo Aplanando %%D ...
        robocopy "%%D" "%INSTALL_DIR%\mariadb" /MOVE /E >nul
    )
)

if not exist "%INSTALL_DIR%\mariadb\bin\mariadbd.exe" (
    echo [ERROR] No se encontro mariadbd.exe despues de extraer
    pause
    exit /b 1
)
echo [OK] MariaDB extraido.

echo Extrayendo aplicacion...
powershell -NoProfile -Command "Expand-Archive -Path 'app.zip' -DestinationPath '%INSTALL_DIR%\app' -Force"
if errorlevel 1 (
    echo [ERROR] No se pudo extraer la app
    pause
    exit /b 1
)
echo [OK] Aplicacion extraida.

:: Copiar SQL
copy /Y "sql\schema.sql" "%INSTALL_DIR%\sql\" >nul
if exist "sql\datos_iniciales.sql" copy /Y "sql\datos_iniciales.sql" "%INSTALL_DIR%\sql\" >nul

:: ============================================================
:: 3) Inicializar MariaDB
:: ============================================================
echo.
echo [2/5] Inicializando MariaDB...
set /p DB_PASS="Contrasena para root de MariaDB (solo letras y numeros): "
if "!DB_PASS!"=="" set DB_PASS=cerrajeria2026

"%INSTALL_DIR%\mariadb\bin\mariadb-install-db.exe" ^
    --datadir="%INSTALL_DIR%\data" ^
    --password=!DB_PASS! ^
    --port=3307 >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Fallo la inicializacion de MariaDB
    pause
    exit /b 1
)
echo [OK] Data dir creado.

:: ============================================================
:: 4) Arrancar temporalmente y crear la base
:: ============================================================
echo.
echo [3/5] Creando base de datos...
start /B "" "%INSTALL_DIR%\mariadb\bin\mariadbd.exe" ^
    --datadir="%INSTALL_DIR%\data" ^
    --port=3307 ^
    --bind-address=127.0.0.1 ^
    --console >nul 2>&1

echo Esperando a MariaDB...
set /a INTENTOS=0
:waitdb
set /a INTENTOS+=1
if !INTENTOS! gtr 30 (
    echo [ERROR] MariaDB no arranco en 30 segundos
    taskkill /F /IM mariadbd.exe >nul 2>&1
    pause
    exit /b 1
)
timeout /t 1 >nul
"%INSTALL_DIR%\mariadb\bin\mariadb.exe" -u root -p!DB_PASS! -P 3307 -e "SELECT 1;" >nul 2>&1
if errorlevel 1 goto waitdb
echo [OK] MariaDB respondiendo.

"%INSTALL_DIR%\mariadb\bin\mariadb.exe" -u root -p!DB_PASS! -P 3307 -e "CREATE DATABASE IF NOT EXISTS cerrajeria CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if errorlevel 1 (
    echo [ERROR] No se pudo crear la base
    goto cleanup
)

echo Importando schema...
"%INSTALL_DIR%\mariadb\bin\mariadb.exe" -u root -p!DB_PASS! -P 3307 cerrajeria < "%INSTALL_DIR%\sql\schema.sql"
if errorlevel 1 (
    echo [ERROR] Fallo import del schema
    goto cleanup
)

if exist "%INSTALL_DIR%\sql\datos_iniciales.sql" (
    echo Importando datos iniciales...
    "%INSTALL_DIR%\mariadb\bin\mariadb.exe" -u root -p!DB_PASS! -P 3307 cerrajeria < "%INSTALL_DIR%\sql\datos_iniciales.sql"
)
echo [OK] Base lista.

:: ============================================================
:: 5) Detener MariaDB, crear .env, shortcuts
:: ============================================================
:cleanup
echo.
echo [4/5] Deteniendo MariaDB temporal...
"%INSTALL_DIR%\mariadb\bin\mariadb-admin.exe" -u root -p!DB_PASS! -P 3307 shutdown >nul 2>&1
timeout /t 2 >nul

:: .env
echo.
echo [5/5] Configurando .env y accesos directos...
(
    echo DB_URL=jdbc:mariadb://localhost:3307/cerrajeria
    echo DB_USER=root
    echo DB_PASSWORD=!DB_PASS!
    echo EMPRESA_NOMBRE=Lujan Cerrajeria Integral
    echo EMPRESA_DIRECCION=Av. Uruguay ^(Ex 160^) 254 - Villa Carlos Paz, Cordoba
    echo EMPRESA_TELEFONO=3541-614042
    echo EMPRESA_EMAIL=
    echo EMPRESA_CUIT=20140466450
    echo EMPRESA_IIBB=218057411
    echo EMPRESA_INICIO=01/03/2019
    echo EMPRESA_CONDICION_IVA=Responsable Monotributo
    echo EMPRESA_PUNTO_VENTA=00001
    echo PDF_COLORES=false
    echo PUERTO_MARIADB=3307
) > "%INSTALL_DIR%\.env"

:: Script para arrancar todo oculto
(
    echo Set WshShell = CreateObject^("WScript.Shell"^)
    echo WshShell.Run """%INSTALL_DIR%\app\CerrajeriaLujan.exe""", 0, False
) > "%INSTALL_DIR%\lanzar.vbs"

:: Acceso directo en el escritorio
powershell -NoProfile -Command ^
  "$WshShell = New-Object -ComObject WScript.Shell; $s = $WshShell.CreateShortcut([Environment]::GetFolderPath('Desktop') + '\Cerrajeria Lujan.lnk'); $s.TargetPath = '%INSTALL_DIR%\lanzar.vbs'; $s.WorkingDirectory = '%INSTALL_DIR%'; $s.IconLocation = '%INSTALL_DIR%\app\CerrajeriaLujan.exe,0'; $s.Save()"

echo.
echo ==========================================================
echo   INSTALACION COMPLETA
echo ==========================================================
echo.
echo Carpeta: %INSTALL_DIR%
echo Acceso directo: Escritorio -^> "Cerrajeria Lujan"
echo.
echo Al abrir la app, MariaDB arranca automaticamente en puerto 3307
echo y se apaga al cerrar la aplicacion.
echo.
pause
exit /b 0
