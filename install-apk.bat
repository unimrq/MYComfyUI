@echo off
chcp 65001 >nul
title 安装 APK 到设备
cd /d D:\Code\MYComfyUI\app\release

echo ========================================
echo  当前目录：%cd%
echo  正在检查连接的设备...
echo ========================================
adb devices

echo.
echo ========================================
echo  开始安装 app-release.apk ...
echo ========================================
adb install -r app-release.apk

echo.
echo ========================================
echo  安装完成！
echo  按任意键退出...
echo ========================================
pause >nul
