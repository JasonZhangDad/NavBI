# NavBI Pro

导航系统 + 访问统计 + BI 数据分析。Spring Boot 3 + Vue 3 + PostgreSQL + Redis。

详细设计见 [DESIGN.md](DESIGN.md)。

## 本地开发

```bash
# 后端（H2 内存库 + 本地 Redis，无需 Docker）
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local

# 前端
cd frontend && npm install && npm run dev   # http://localhost:5173

# 测试
cd backend && mvn test
```

本地默认管理员：`admin / admin123`（local profile）。

## 部署

```bash
cp .env.example .env   # 修改密码与 JWT 密钥
docker compose up -d --build
```

前端监听 `127.0.0.1:8098`，由宿主机 Caddy/Nginx 反代并终结 HTTPS（生产域名 nav.magies.top）。

## 功能

- 前台：分类导航、搜索、点击 PV/UV 统计、访问埋点
- 后台：导航管理（增删改查/启停）、BI 大屏（今日 PV/UV、趋势、设备/浏览器占比、地域分布、热门页面、访问日志）
- 埋点富化：ip2region 离线地域解析、User-Agent 设备识别、会话跟踪
- 实时计数：Redis INCR/HyperLogLog，故障时自动降级为数据库口径
