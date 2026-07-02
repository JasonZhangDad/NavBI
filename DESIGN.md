# NavBI Pro 详细设计文档（MVP 版）

> 版本：Enterprise v1.0-MVP　|　日期：2026-07-02
> 定位：导航系统 + 访问统计 + BI 数据分析。本文档在原始交付大纲基础上做**可实施细化**，并明确 MVP 边界。

## 1. MVP 范围

| 模块 | MVP 包含 | 留到后续 |
|---|---|---|
| 导航 | 分类/链接 CRUD、图标、排序、搜索、启用状态、点击 PV/UV | 拖拽排序、批量导入 |
| 埋点 | 前端 beacon 上报 + 后端富化（IP/UA/地域/会话） | 爬虫识别、风险识别 |
| 地域解析 | ip2region 离线 xdb（缺文件时降级为"未知"） | GeoLite2 双引擎 |
| 设备识别 | UserAgentUtils 解析 设备/OS/浏览器 | 精细机型识别 |
| BI | summary/trend/top-pages/device/browser/geo/logs 七个接口 + 后台大屏 | WebSocket 实时大屏、ClickHouse |
| 安全 | Spring Security + JWT，单管理员（环境变量配置） | 多管理员/RBAC |
| 部署 | docker-compose（backend/frontend/postgres/redis） | K8s、多站点 SaaS |

## 2. 架构

```
Browser ──> Nginx(frontend 静态 + /api 反代) ──> Spring Boot 3
                                                  ├─ TrackingService(@Async) ──> PostgreSQL(visit_log/user_session)
                                                  ├─ CounterService ──> Redis(今日 PV/UV/IP HyperLogLog)
                                                  └─ BiService ──> PostgreSQL 聚合查询
```

**与原大纲的一个偏差（重要）**：原文档采集方式为"Spring Interceptor 在请求进 Controller 前记录"。该方式适合服务端渲染页面；本系统前端是 Vue3 SPA，静态资源由 Nginx 提供，后端拦截器只能看到 API 调用，无法代表页面浏览。因此 MVP 采用 **前端路由级 beacon 上报**（`POST /api/track`），后端在该端点完成 IP、UA、地域、会话的富化与异步落库。语义等价、数据更准。

## 3. 技术栈与版本

- 后端：Java 17（编译目标）、Spring Boot 3.4、MyBatis Plus 3.5.x、Spring Security + jjwt、ip2region、UserAgentUtils
- 前端：Vue 3 + Vite、Element Plus、ECharts 5、Pinia、Axios、vue-router
- 存储：PostgreSQL 16（生产）/ H2 PostgreSQL 兼容模式（本地 dev/test）、Redis 7
- 部署：Docker Compose + Nginx

## 4. 数据库设计（DDL 见 `backend/src/main/resources/schema.sql`）

### 4.1 nav_item
| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| title | VARCHAR(128) | 标题 |
| url | VARCHAR(512) | 地址 |
| category | VARCHAR(64) | 分类名（MVP 用字符串，不建分类表） |
| icon | VARCHAR(512) | 图标 URL 或 emoji |
| sort | INT | 越小越靠前 |
| click_count | BIGINT | 点击 PV |
| uv_count | BIGINT | 点击 UV（Redis SET 去重后累加） |
| enabled | BOOLEAN | |
| created_at / updated_at | TIMESTAMP | |

### 4.2 visit_log（索引：created_at、url、ip、session_id）
ip / country / province / city / device / os / browser / url / referer / user_agent / session_id / created_at

### 4.3 user_session
session_id UNIQUE / ip / start_time / end_time / page_count。每次上报 upsert：end_time=now、page_count+1。

## 5. 接口契约（统一响应 `{code, message, data}`，code=0 成功）

### 公开接口
| 接口 | 说明 |
|---|---|
| `GET /api/nav/list?keyword=` | 启用的导航，按 category 分组返回 |
| `POST /api/nav/click/{id}` | 点击计数（带 X-Session-Id 时做 UV 去重） |
| `POST /api/track` | 埋点上报 `{url, referer, sessionId?}`；无 sessionId 时服务端生成并返回 |
| `POST /api/auth/login` | `{username, password}` → `{token}` |

### 管理接口（Bearer JWT）
| 接口 | 说明 |
|---|---|
| `GET /api/nav/all` | 全部导航（含禁用，供管理表格） |
| `POST /api/nav/add`・`PUT /api/nav/update`・`DELETE /api/nav/delete/{id}` | 导航 CRUD |
| `GET /api/bi/summary` | `{todayPv, todayUv, totalPv, totalUv, ipCount, yesterdayPv}` |
| `GET /api/bi/trend?dimension=hour\|day` | hour=近24小时，day=近30天 `[{bucket, pv, uv}]` |
| `GET /api/bi/top-pages` | Top10 `[{url, pv}]` |
| `GET /api/bi/device`・`/browser`・`/geo?level=country\|province` | `[{name, value}]` |
| `GET /api/bi/logs?page=&size=` | 访问日志分页，created_at 倒序 |

## 6. 统计口径

- **PV**：visit_log 行数；今日 PV 走 Redis `INCR navbi:pv:{yyyyMMdd}`（48h 过期），Redis 不可用时降级为 DB count。
- **UV**：以 sessionId 去重。今日 UV 走 Redis HyperLogLog `PFADD navbi:uv:{yyyyMMdd}`；历史 UV `COUNT(DISTINCT session_id)`。
- **IP 数**：`COUNT(DISTINCT ip)`。
- **会话**：前端 localStorage 持久化 sessionId（服务端首次生成 UUID）。
- **导航 UV**：`SADD navbi:navclick:{id} sessionId` 返回 1 时 uv_count+1；Redis 不可用时只计 PV。

## 7. 安全设计

- 单管理员：`NAVBI_ADMIN_USERNAME` / `NAVBI_ADMIN_PASSWORD`（启动时 BCrypt 加密驻留内存）。
- JWT：HS256，密钥 `NAVBI_JWT_SECRET`（≥32 字节），有效期 24h。
- 放行：`/api/auth/login`、`/api/nav/list`、`/api/nav/click/**`、`/api/track`；其余 `/api/**` 需认证。
- 登录失败限制、Nginx 限流、HTTPS：由部署层（Caddy/Nginx + acme）承担，MVP 不在应用内实现。

## 8. 性能设计

- 落库走 `@Async`（独立线程池），埋点接口 P99 目标 <10ms（不含网络）。
- Redis 承担全部"今日"实时计数，BI 历史查询命中 created_at 索引。
- 日志表按月分区、ClickHouse：数据量 >千万级 时再引入（YAGNI）。

## 9. 部署

`docker-compose.yml`：postgres:16-alpine、redis:7-alpine、navbi-backend（多阶段 Maven 构建）、navbi-frontend（Nginx 托管静态 + `/api` 反代 backend）。环境变量见 `.env.example`。适配 Oracle A1 / 常规 VPS。

## 10. 本地开发

- 后端：`cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local`（H2 内存库 + 本地 Redis，无 Redis 也可运行——自动降级）
- 前端：`cd frontend && npm run dev`（Vite 代理 `/api` → `localhost:8080`）
- 测试：`cd backend && mvn test`（H2 + MockMvc 集成测试）
