# RPGCore 监控配置指南

**版本**: 2.0.0  
**更新日期**: 2026-04-26

---

## 📋 目录

1. [监控架构](#监控架构)
2. [Prometheus 配置](#prometheus-配置)
3. [Grafana 配置](#grafana-配置)
4. [告警配置](#告警配置)
5. [指标说明](#指标说明)
6. [故障排查](#故障排查)

---

## 监控架构

```
┌─────────────────┐
│   RPGCore       │
│  (指标暴露端点)  │
└────────┬────────┘
         │ HTTP /metrics
         ▼
┌─────────────────┐
│   Prometheus    │
│  (指标收集存储)  │
└────────┬────────┘
         │ PromQL
         ▼
┌─────────────────┐
│    Grafana      │
│  (可视化监控)    │
└─────────────────┘
```

---

## Prometheus 配置

### 1. 安装 Prometheus

**Windows**:
```powershell
# 下载 Prometheus
Invoke-WebRequest -Uri "https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.windows-amd64.zip" -OutFile "prometheus.zip"

# 解压
Expand-Archive -Path "prometheus.zip" -DestinationPath "C:\prometheus"
```

**Linux**:
```bash
# 下载 Prometheus
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz

# 解压
tar xvfz prometheus-2.45.0.linux-amd64.tar.gz
```

### 2. 配置 Prometheus

复制配置文件:
```bash
cp monitoring/prometheus.yml /etc/prometheus/
cp monitoring/rpgcore_alerts.yml /etc/prometheus/
```

编辑 `prometheus.yml`:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'rpgcore'
    scrape_interval: 10s
    metrics_path: '/metrics'
    static_configs:
      - targets: ['localhost:8080']  # RPGCore 指标端点
        labels:
          server: 'astraea-rpg'
          environment: 'production'

rule_files:
  - 'rpgcore_alerts.yml'

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']
```

### 3. 启动 Prometheus

**Windows**:
```powershell
cd C:\prometheus
.\prometheus.exe --config.file=prometheus.yml
```

**Linux**:
```bash
cd prometheus-2.45.0.linux-amd64
./prometheus --config.file=prometheus.yml
```

访问 `http://localhost:9090` 查看 Prometheus 控制台。

---

## Grafana 配置

### 1. 安装 Grafana

**Windows**:
```powershell
# 下载 Grafana
Invoke-WebRequest -Uri "https://dl.grafana.com/oss/release/grafana-10.0.0.windows-amd64.zip" -OutFile "grafana.zip"

# 解压
Expand-Archive -Path "grafana.zip" -DestinationPath "C:\grafana"
```

**Linux**:
```bash
# Ubuntu/Debian
sudo apt-get install -y software-properties-common
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
sudo apt-get update
sudo apt-get install grafana

# CentOS/RHEL
sudo yum install grafana
```

### 2. 启动 Grafana

**Windows**:
```powershell
cd C:\grafana\bin
.\grafana-server.exe
```

**Linux**:
```bash
sudo systemctl start grafana-server
sudo systemctl enable grafana-server
```

访问 `http://localhost:3000` (默认账号: admin/admin)

### 3. 配置数据源

1. 登录 Grafana
2. 导航到 **Configuration** -> **Data Sources**
3. 点击 **Add data source**
4. 选择 **Prometheus**
5. 配置:
   - **Name**: RPGCore Prometheus
   - **URL**: `http://localhost:9090`
   - **Access**: Server (default)
6. 点击 **Save & Test**

### 4. 导入监控面板

1. 导航到 **Dashboards** -> **Import**
2. 上传 `monitoring/grafana_dashboard.json`
3. 选择 Prometheus 数据源
4. 点击 **Import**

### 5. 面板说明

导入的面板包含以下监控项:

| 面板名称 | 说明 | 刷新间隔 |
|---------|------|----------|
| **JVM 内存使用** | 堆内存使用量和最大值 | 10s |
| **事件处理性能** | 事件发布速率和处理时间 | 10s |
| **数据库连接池** | 活跃连接和泄漏连接 | 10s |
| **玩家锁统计** | 锁获取和超时速率 | 10s |
| **缓存命中率** | 缓存命中率百分比 | 10s |
| **异步任务队列** | 待处理任务和活跃线程 | 10s |
| **服务器 TPS** | 服务器 TPS 值 | 10s |
| **GC 统计** | GC 时间统计 | 10s |

---

## 告警配置

### 1. 安装 Alertmanager (可选)

**下载 Alertmanager**:
```bash
wget https://github.com/prometheus/alertmanager/releases/download/v0.25.0/alertmanager-0.25.0.linux-amd64.tar.gz
tar xvfz alertmanager-0.25.0.linux-amd64.tar.gz
```

### 2. 配置 Alertmanager

创建 `alertmanager.yml`:
```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://localhost:5001/alert'
```

### 3. 告警规则说明

| 告警名称 | 触发条件 | 持续时间 | 严重级别 |
|---------|---------|----------|----------|
| **RPGCoreHighMemoryUsage** | 堆内存使用 > 80% | 5 分钟 | Warning |
| **RPGCoreSlowEventProcessing** | 事件处理时间 > 10ms | 2 分钟 | Warning |
| **RPGCoreDatabaseConnectionLeak** | 检测到连接泄漏 | 1 分钟 | Critical |
| **RPGCoreLockTimeouts** | 锁超时频率 > 1/秒 | 2 分钟 | Warning |
| **RPGCoreLowCacheHitRate** | 缓存命中率 < 70% | 5 分钟 | Warning |
| **RPGCoreAsyncQueueFull** | 异步队列 > 900 | 1 分钟 | Critical |
| **RPGCoreLowTPS** | TPS < 18 | 3 分钟 | Warning |
| **RPGCoreHighGCTime** | GC时间占比 > 10% | 5 分钟 | Warning |

---

## 指标说明

### JVM 指标

```promql
# 堆内存使用量
rpgcore_jvm_memory_used_bytes{area="heap"}

# 堆内存最大值
rpgcore_jvm_memory_max_bytes{area="heap"}

# GC 时间
rpgcore_jvm_gc_collection_seconds_sum{gc="G1 Young Generation"}
```

### 事件指标

```promql
# 事件发布总数
rpgcore_events_total

# 事件处理时间 (平均)
rate(rpgcore_events_duration_seconds_sum[1m]) / rate(rpgcore_events_duration_seconds_count[1m])
```

### 数据库指标

```promql
# 活跃连接数
rpgcore_database_connections_active

# 泄漏连接数
rpgcore_database_connections_leaked
```

### 锁指标

```promql
# 锁获取速率
rate(rpgcore_locks_total[1m])

# 锁超时速率
rate(rpgcore_locks_timeouts[1m])
```

### 缓存指标

```promql
# 缓存命中率
rpgcore_cache_hit_rate

# 缓存大小
rpgcore_cache_size
```

---

## 故障排查

### 问题 1: Prometheus 无法抓取指标

**症状**: Prometheus 控制台显示 Target 状态为 Down

**解决方案**:
1. 检查 RPGCore 是否正常运行
2. 检查指标端点是否可访问: `http://localhost:8080/metrics`
3. 检查防火墙配置
4. 检查 Prometheus 配置文件中的 targets 地址

### 问题 2: Grafana 无法连接 Prometheus

**症状**: Grafana 数据源测试失败

**解决方案**:
1. 检查 Prometheus 是否正常运行: `http://localhost:9090`
2. 检查 Grafana 数据源配置中的 URL
3. 检查网络连接
4. 查看 Grafana 日志: `/var/log/grafana/grafana.log`

### 问题 3: 告警未触发

**症状**: 满足告警条件但未收到告警

**解决方案**:
1. 检查告警规则文件是否正确加载
2. 检查 Alertmanager 是否正常运行
3. 查看 Prometheus 告警页面: `http://localhost:9090/alerts`
4. 检查告警规则的 `for` 持续时间

### 问题 4: 指标数据缺失

**症状**: Grafana 面板显示 No Data

**解决方案**:
1. 检查 RPGCore MetricsExporter 是否正常工作
2. 检查 Prometheus 是否正常抓取数据
3. 使用 Prometheus 控制台查询指标是否存在
4. 检查时间范围选择是否正确

---

## 📚 参考资源

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [Alertmanager 官方文档](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [PromQL 查询语言](https://prometheus.io/docs/prometheus/latest/querying/basics/)

---

**文档版本**: 1.0.0  
**最后更新**: 2026-04-26
