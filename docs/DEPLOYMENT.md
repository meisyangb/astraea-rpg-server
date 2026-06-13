# 文档部署指南

> 本文档介绍如何将 Astraea RPG 插件文档部署到各种云服务器

---

## 📋 目录

- [一、部署方式概览](#一部署方式概览)
- [二、部署到 Vercel](#二部署到-vercel)
- [三、部署到 Netlify](#三部署到-netlify)
- [四、部署到 GitHub Pages](#四部署到-github-pages)
- [五、部署到云服务器 (VPS)](#五部署到云服务器-vps)
- [六、使用 Docker 部署](#六使用-docker-部署)
- [七、自动化部署 (CI/CD)](#七自动化部署-cicd)

---

## 一、部署方式概览

### 1.1 部署方式对比

| 部署方式 | 难度 | 费用 | 推荐场景 |
|----------|------|------|----------|
| [Vercel](#二部署到-vercel) | ⭐ | 免费 | 快速部署、个人项目 |
| [Netlify](#三部署到-netlify) | ⭐ | 免费 | 快速部署、个人项目 |
| [GitHub Pages](#四部署到-github-pages) | ⭐⭐ | 免费 | 开源项目、文档站点 |
| [云服务器 VPS](#五部署到云服务器-vps) | ⭐⭐⭐ | 付费 | 企业生产、自定义域名 |
| [Docker](#六使用-docker-部署) | ⭐⭐ | 取决于服务器 | 容器化部署、微服务 |

### 1.2 准备工作

无论选择哪种部署方式，都需要：

1. **文档代码** - 确保本地文档可以正常运行
2. **域名** (可选) - 如果需要自定义域名
3. **Git 仓库** - 如果使用自动化部署

---

## 二、部署到 Vercel

[Vercel](https://vercel.com) 是最简单的部署方式，免费且功能强大。

### 2.1 部署步骤

#### 步骤 1: 注册 Vercel

1. 访问 [https://vercel.com](https://vercel.com)
2. 使用 GitHub/GitLab/Bitbucket 账号登录

#### 步骤 2: 导入项目

1. 点击 "New Project"
2. 导入你的 Git 仓库（包含文档代码的仓库）
3. 选择文档所在目录

#### 步骤 3: 配置项目

```yaml
# Vercel 配置文件 (vercel.json)
{
  "name": "astraea-rpg-docs",
  "version": 2,
  "builds": [
    {
      "src": "**/*",
      "use": "@vercel/static"
    }
  ],
  "routes": [
    {
      "src": "/(.*)",
      "dest": "/$1"
    },
    {
      "src": "/",
      "dest": "/index.html"
    }
  ]
}
```

#### 步骤 4: 部署

点击 "Deploy" 按钮，等待部署完成。

### 2.2 自定义域名

1. 在 Vercel 项目设置中添加域名
2. 在域名服务商处添加 Vercel 提供的 DNS 记录
3. 等待 DNS 生效（通常几分钟到几小时）

### 2.3 自动化部署

Vercel 会自动监听 Git 仓库的 push 事件，自动部署。

---

## 三、部署到 Netlify

[Netlify](https://www.netlify.com) 是另一个优秀的静态网站托管平台。

### 3.1 部署步骤

#### 步骤 1: 注册 Netlify

1. 访问 [https://www.netlify.com](https://www.netlify.com)
2. 使用 GitHub/GitLab/Bitbucket 账号登录

#### 步骤 2: 导入项目

1. 点击 "New site from Git"
2. 选择 Git 提供商
3. 选择包含文档的仓库

#### 步骤 3: 配置构建

```bash
# 构建命令 (因为是纯静态网站，不需要构建)
# 留空

# 发布目录
.
```

#### 步骤 4: 部署

点击 "Deploy site" 按钮。

### 3.2 Netlify 配置文件

创建 `netlify.toml` 文件：

```toml
[build]
  publish = "."

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

### 3.3 自定义域名

1. 在 Netlify 项目设置中添加域名
2. 配置 DNS 记录
3. 启用 HTTPS (Netlify 自动提供 SSL 证书)

---

## 四、部署到 GitHub Pages

如果你使用 GitHub 托管代码，可以使用 GitHub Pages 免费托管文档。

### 4.1 部署步骤

#### 步骤 1: 创建 GitHub 仓库

```bash
# 初始化 Git 仓库
git init

# 添加文件
git add .

# 提交
git commit -m "Initial commit"

# 添加远程仓库
git remote add origin https://github.com/你的用户名/astraea-rpg-docs.git

# 推送
git push -u origin main
```

#### 步骤 2: 启用 GitHub Pages

1. 进入 GitHub 仓库设置
2. 找到 "Pages" 选项
3. 选择分支（通常是 `main` 或 `gh-pages`）
4. 选择目录（通常是 `/ (root)`）
5. 点击 Save

#### 步骤 3: 访问文档

等待几分钟，访问 `https://你的用户名.github.io/astraea-rpg-docs/`

### 4.2 使用 GitHub Actions 自动化部署

创建 `.github/workflows/deploy.yml`：

```yaml
name: Deploy Docs

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: .
```

---

## 五、部署到云服务器 (VPS)

如果你有云服务器（阿里云、腾讯云、AWS、DigitalOcean 等），可以手动部署。

### 5.1 使用 Nginx 部署

#### 步骤 1: 安装 Nginx

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx -y

# CentOS/RHEL
sudo yum install nginx -y
```

#### 步骤 2: 上传文档文件

```bash
# 创建目录
sudo mkdir -p /var/www/docs

# 上传文件（使用 scp 或 rsync）
scp -r ./* user@your-server:/var/www/docs/

# 或使用 rsync
rsync -avz --delete ./ user@your-server:/var/www/docs/
```

#### 步骤 3: 配置 Nginx

创建 Nginx 配置文件 `/etc/nginx/sites-available/docs`：

```nginx
server {
    listen 80;
    server_name docs.your-domain.com;
    
    root /var/www/docs;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 启用 gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    
    # 缓存静态资源
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

#### 步骤 4: 启用配置

```bash
# 创建符号链接
sudo ln -s /etc/nginx/sites-available/docs /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

#### 步骤 5: 配置防火墙

```bash
# Ubuntu
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw reload

# CentOS
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 5.2 使用 Apache 部署

#### 步骤 1: 安装 Apache

```bash
# Ubuntu/Debian
sudo apt install apache2 -y

# CentOS/RHEL
sudo yum install httpd -y
```

#### 步骤 2: 配置 Apache

创建 Apache 配置文件 `/etc/apache2/sites-available/docs.conf`（Ubuntu）或 `/etc/httpd/conf.d/docs.conf`（CentOS）：

```apache
<VirtualHost *:80>
    ServerName docs.your-domain.com
    DocumentRoot /var/www/docs
    
    <Directory /var/www/docs>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>
    
    # 启用重写
    <IfModule mod_rewrite.c>
        RewriteEngine On
        RewriteBase /
        RewriteRule ^index\.html$ - [L]
        RewriteCond %{REQUEST_FILENAME} !-f
        RewriteCond %{REQUEST_FILENAME} !-d
        RewriteRule . /index.html [L]
    </IfModule>
</VirtualHost>
```

#### 步骤 3: 启用配置

```bash
# Ubuntu
sudo a2ensite docs.conf
sudo a2enmod rewrite
sudo systemctl restart apache2

# CentOS
sudo systemctl restart httpd
```

### 5.3 配置 HTTPS (SSL)

使用 Let's Encrypt 免费 SSL 证书：

```bash
# 安装 Certbot
# Ubuntu
sudo apt install certbot python3-certbot-nginx -y

# CentOS
sudo yum install certbot python3-certbot-nginx -y

# 获取证书
sudo certbot --nginx -d docs.your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

---

## 六、使用 Docker 部署

Docker 可以快速部署到任何支持 Docker 的服务器。

### 6.1 创建 Dockerfile

```dockerfile
# 使用 Nginx 镜像
FROM nginx:alpine

# 维护者信息
LABEL maintainer="GuangDian <email@example.com>"

# 删除 Nginx 默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/

# 复制文档文件
COPY . /usr/share/nginx/html/

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
```

### 6.2 创建 Nginx 配置

创建 `nginx.conf`：

```nginx
server {
    listen 80;
    server_name localhost;
    
    root /usr/share/nginx/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript;
}
```

### 6.3 构建和运行

```bash
# 构建镜像
docker build -t astraea-rpg-docs .

# 运行容器
docker run -d -p 80:80 --name docs astraea-rpg-docs

# 查看日志
docker logs docs

# 停止容器
docker stop docs

# 删除容器
docker rm docs
```

### 6.4 使用 Docker Compose

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  docs:
    build: .
    image: astraea-rpg-docs:latest
    container_name: astraea-docs
    ports:
      - "80:80"
    restart: unless-stopped
```

运行：

```bash
# 启动
docker-compose up -d

# 停止
docker-compose down

# 重新构建
docker-compose up -d --build
```

---

## 七、自动化部署 (CI/CD)

### 7.1 使用 GitHub Actions

创建 `.github/workflows/deploy.yml`：

```yaml
name: Deploy to Server

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Deploy to server
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.HOST }}
        username: ${{ secrets.USERNAME }}
        key: ${{ secrets.SSH_KEY }}
        script: |
          cd /var/www/docs
          git pull origin main
          echo "Deployment completed!"
```

### 7.2 配置 GitHub Secrets

在 GitHub 仓库设置中添加以下 Secrets：

- `HOST` - 服务器 IP 地址
- `USERNAME` - SSH 用户名
- `SSH_KEY` - SSH 私钥

### 7.3 使用 GitLab CI/CD

创建 `.gitlab-ci.yml`：

```yaml
stages:
  - deploy

deploy:
  stage: deploy
  only:
    - main
  script:
    - ssh user@your-server "cd /var/www/docs && git pull origin main"
```

---

## 八、常见问题

### Q1: 部署后页面显示 404？

**A:** 确保服务器配置支持 SPA（单页应用）模式，添加 fallback 到 `index.html`。

### Q2: 如何更新文档？

**A:** 
- **Vercel/Netlify**: 推送代码到 Git 仓库，自动部署
- **VPS**: 拉取最新代码，重启 Nginx/Apache
- **Docker**: 重新构建镜像并运行

### Q3: 如何配置自定义域名？

**A:** 
1. 在域名服务商处添加 A 记录或 CNAME 记录
2. 在托管平台配置域名
3. 等待 DNS 生效

### Q4: 如何启用 HTTPS？

**A:** 
- **Vercel/Netlify**: 自动提供 SSL 证书
- **VPS**: 使用 Certbot 获取 Let's Encrypt 证书

---

## 九、推荐方案

### 个人项目 / 快速部署

推荐使用 **Vercel** 或 **Netlify**：
- ✅ 免费
- ✅ 自动部署
- ✅ 自动 HTTPS
- ✅ CDN 加速

### 企业项目 / 自定义需求

推荐使用 **云服务器 + Nginx**：
- ✅ 完全控制
- ✅ 自定义配置
- ✅ 高性能
- ✅ 可扩展

---

*最后更新: 2026-06-11*
