# GuangDianAuth

> 光点登录系统 — 密码登录/自动登录/IP绑定/会话管理

---

## 一、简介

GuangDianAuth 提供玩家登录验证系统，支持密码注册/登录、自动登录（IP绑定）、会话保持。

### 功能特性

- **密码注册** — 首次加入自动引导注册
- **密码登录** — 密码验证后解锁操作
- **自动登录** — 同IP自动登录
- **会话管理** — 退出游戏后会话保持

---

## 二、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/login <密码>` | 无 | 登录 |
| `/register <密码> <确认>` | 无 | 注册 |
| `/changepw <旧密码> <新密码>` | 无 | 修改密码 |
| `/authadmin reset <玩家>` | `guangdian.auth.admin` | 重置玩家密码 |
| `/authadmin reload` | `guangdian.auth.admin` | 重载配置 |

---

## 三、配置

```yaml
# 会话超时(分钟)
session-timeout: 30
# 登录超时(秒)
login-timeout: 60
# 自动登录
auto-login:
  enabled: true
  by-ip: true
```

---

*最后更新: 2026-06-13*
