@echo off
cd /d e:\原创RPG服务端
set JAVA_HOME=e:\原创RPG服务端\tools\jdk-21.0.10+7
D:\gradle\gradle-9.4.0\bin\gradle.bat :plugins:GuangDianCaveFu:build :plugins:GuangDianMobHealth:build :plugins:GuangDianName:build --no-configuration-cache -x test
