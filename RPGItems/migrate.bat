@echo off
chcp 65001
cd /d e:\RPG\原创RPG服务端-插件最多提交\plugins\RPGItems
javac -encoding UTF-8 MigrateItems.java
java MigrateItems
pause
