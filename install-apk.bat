@echo off
chcp 65001 >nul
title 安装 APK 到指定设备
cd /d D:\Code\MYComfyUI\app\release

echo ========================================
echo  当前目录：%cd%
echo  正在检查连接的设备...
echo ========================================
adb devices

echo.
echo ========================================
echo  开始安装 app-release.apk 到设备 10AF9J1UCS00532 ...
echo ========================================
adb -s 10AF9J1UCS00532 install -r app-release.apk

echo.
echo ========================================
echo  安装完成！
echo ========================================

REM 自动退出，不用 pause
pause
