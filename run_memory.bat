@echo off
chcp 65001 >nul
echo ========================================
echo    RESTAURANT BOOKING MANAGEMENT
echo    (Memory-based Execution)
echo ========================================
echo.

REM Kiểm tra Java version
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    pause
    exit /b 1
)

echo Java version:
java -version
echo.

REM Tạo thư mục temp ẩn để compile
if not exist ".temp" mkdir .temp

REM Compile vào thư mục temp ẩn
echo Compiling to temporary directory...
javac -cp "lib/*" -d .temp src/restaurantbookingmanagement/*.java src/restaurantbookingmanagement/ai/*.java src/restaurantbookingmanagement/controller/*.java src/restaurantbookingmanagement/model/*.java src/restaurantbookingmanagement/service/*.java src/restaurantbookingmanagement/view/*.java src/restaurantbookingmanagement/utils/*.java
if errorlevel 1 (
    echo ERROR: Compilation failed
    rmdir /s /q .temp
    pause
    exit /b 1
)

REM Chạy từ thư mục temp ẩn
echo Running from temporary directory...
echo.

java -Dfile.encoding=UTF-8 -Duser.language=vi -Duser.country=VN -cp ".temp;lib/*" restaurantbookingmanagement.RestaurantBookingManagement

REM Xóa thư mục temp ẩn sau khi chạy
echo.
echo Cleaning up temporary files...
rmdir /s /q .temp

echo.
echo Program finished.
pause 