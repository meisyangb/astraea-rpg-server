#!/usr/bin/env pwsh
# Astraea RPG 版本发布脚本
# 用法: .\release.ps1 -Version "1.0.0" -Type "release"

param(
    [Parameter(Mandatory=$true)]
    [string]$Version,
    
    [Parameter(Mandatory=$false)]
    [ValidateSet("release", "hotfix")]
    [string]$Type = "release"
)

$ErrorActionPreference = "Stop"

# 颜色定义
$Colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
}

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Colors[$Color]
}

function Test-GitStatus {
    Write-ColorOutput "检查 Git 状态..." "Info"
    
    # 检查是否有未提交的更改
    $status = git status --porcelain
    if ($status) {
        Write-ColorOutput "错误: 有未提交的更改，请先提交或暂存" "Error"
        git status
        exit 1
    }
    
    Write-ColorOutput "Git 状态正常" "Success"
}

function Test-Branch {
    param([string]$ExpectedBranch)
    
    $currentBranch = git branch --show-current
    if ($currentBranch -ne $ExpectedBranch) {
        Write-ColorOutput "错误: 当前分支是 $currentBranch，期望是 $ExpectedBranch" "Error"
        exit 1
    }
    
    Write-ColorOutput "分支检查通过: $currentBranch" "Success"
}

function Update-Version {
    param([string]$NewVersion)
    
    Write-ColorOutput "更新版本号到 $NewVersion..." "Info"
    
    # 更新根 build.gradle
    $buildGradle = Get-Content "build.gradle" -Raw
    $buildGradle = $buildGradle -replace 'version = "[\d.]+"', "version = \"$NewVersion\""
    Set-Content "build.gradle" $buildGradle
    
    Write-ColorOutput "版本号已更新" "Success"
}

function Update-Changelog {
    param([string]$NewVersion)
    
    Write-ColorOutput "更新 CHANGELOG.md..." "Info"
    
    $today = Get-Date -Format "yyyy-MM-dd"
    $changelogEntry = @"

## [$NewVersion] - $today

### 🆕 Added
- 

### 🔄 Changed
- 

### 🐛 Fixed
- 

"@
    
    $changelog = Get-Content "CHANGELOG.md" -Raw
    # 在 Unreleased 后插入新版本
    $changelog = $changelog -replace '(## \[Unreleased\].*?\n)(?=## \[|$)', "`$1$changelogEntry`n"
    Set-Content "CHANGELOG.md" $changelog
    
    Write-ColorOutput "CHANGELOG 已更新" "Success"
}

function Invoke-Tests {
    Write-ColorOutput "运行测试..." "Info"
    
    & gradle clean test
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "错误: 测试失败" "Error"
        exit 1
    }
    
    Write-ColorOutput "测试通过" "Success"
}

function Invoke-Build {
    Write-ColorOutput "构建项目..." "Info"
    
    & gradle clean build -x test
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "错误: 构建失败" "Error"
        exit 1
    }
    
    Write-ColorOutput "构建成功" "Success"
}

function New-ReleaseBranch {
    param([string]$NewVersion, [string]$BaseBranch)
    
    $branchName = "release/v$NewVersion"
    Write-ColorOutput "创建发布分支: $branchName..." "Info"
    
    git checkout -b $branchName $BaseBranch
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "错误: 创建分支失败" "Error"
        exit 1
    }
    
    Write-ColorOutput "分支创建成功" "Success"
    return $branchName
}

function Submit-Changes {
    param([string]$Message)
    
    Write-ColorOutput "提交更改..." "Info"
    
    git add .
    git commit -m $Message
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "错误: 提交失败" "Error"
        exit 1
    }
    
    Write-ColorOutput "提交成功" "Success"
}

function Merge-Branch {
    param(
        [string]$SourceBranch,
        [string]$TargetBranch,
        [string]$Message
    )
    
    Write-ColorOutput "合并 $SourceBranch 到 $TargetBranch..." "Info"
    
    git checkout $TargetBranch
    git pull origin $TargetBranch
    git merge --no-ff $SourceBranch -m $Message
    
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "错误: 合并失败" "Error"
        exit 1
    }
    
    Write-ColorOutput "合并成功" "Success"
}

function New-GitTag {
    param([string]$NewVersion)
    
    $tagName = "v$NewVersion"
    Write-ColorOutput "创建标签: $tagName..." "Info"
    
    git tag -a $tagName -m "Release version $NewVersion"
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "错误: 创建标签失败" "Error"
        exit 1
    }
    
    Write-ColorOutput "标签创建成功" "Success"
}

function Push-Changes {
    param([string]$Branch, [string]$Tag)
    
    Write-ColorOutput "推送更改到远程..." "Info"
    
    git push origin $Branch
    git push origin $Tag
    
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "错误: 推送失败" "Error"
        exit 1
    }
    
    Write-ColorOutput "推送成功" "Success"
}

function Remove-Branch {
    param([string]$BranchName)
    
    Write-ColorOutput "删除分支: $BranchName..." "Info"
    
    git branch -d $BranchName
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "警告: 删除分支失败，请手动删除" "Warning"
    } else {
        Write-ColorOutput "分支删除成功" "Success"
    }
}

# ==================== 主流程 ====================

Write-ColorOutput "========================================" "Info"
Write-ColorOutput "Astraea RPG 版本发布脚本" "Info"
Write-ColorOutput "版本: $Version, 类型: $Type" "Info"
Write-ColorOutput "========================================" "Info"

# 确认
$confirm = Read-Host "确认发布 v$Version? (yes/no)"
if ($confirm -ne "yes") {
    Write-ColorOutput "已取消" "Warning"
    exit 0
}

# 根据类型选择流程
if ($Type -eq "release") {
    # 标准发布流程
    Test-GitStatus
    Test-Branch -ExpectedBranch "develop"
    
    # 创建 release 分支
    $releaseBranch = New-ReleaseBranch -NewVersion $Version -BaseBranch "develop"
    
    # 更新版本号
    Update-Version -NewVersion $Version
    Update-Changelog -NewVersion $Version
    
    # 运行测试和构建
    Invoke-Tests
    Invoke-Build
    
    # 提交更改
    Submit-Changes -Message "chore: 准备 v$Version 发布"
    
    # 合并到 main
    Merge-Branch -SourceBranch $releaseBranch -TargetBranch "main" -Message "release: v$Version"
    
    # 创建标签
    New-GitTag -NewVersion $Version
    
    # 合并回 develop
    Merge-Branch -SourceBranch $releaseBranch -TargetBranch "develop" -Message "merge release v$Version back to develop"
    
    # 推送
    Push-Changes -Branch "main" -Tag "v$Version"
    git push origin develop
    
    # 清理
    Remove-Branch -BranchName $releaseBranch
    
} elseif ($Type -eq "hotfix") {
    # 热修复流程
    Test-GitStatus
    Test-Branch -ExpectedBranch "main"
    
    # 创建 hotfix 分支
    $hotfixBranch = "hotfix/v$Version"
    git checkout -b $hotfixBranch main
    
    Write-ColorOutput "请在 $hotfixBranch 分支上进行修复..." "Warning"
    Write-ColorOutput "修复完成后，运行: git add . && git commit -m 'fix: ...'" "Info"
    Write-ColorOutput "然后重新运行此脚本完成发布" "Info"
    exit 0
}

Write-ColorOutput "========================================" "Success"
Write-ColorOutput "版本 v$Version 发布成功!" "Success"
Write-ColorOutput "========================================" "Success"
