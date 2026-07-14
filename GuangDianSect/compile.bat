@echo off
cd /d "e:\RPG\plugins\GuangDianSect"
call gradle build
if exist "build\libs\GuangDianSect.jar" (
    copy /Y "build\libs\GuangDianSect.jar" "e:\RPG\译梦传说2\plugins\GuangDianSect-1.0.0.jar"
    echo 部署成功!
) else (
    echo 编译失败!
)
pause