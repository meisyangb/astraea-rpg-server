@echo off
cd /d e:\RPG\plugins
set JAVA_HOME=D:\Deployment\jdk-21.0.10+7
D:\Deployment\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test