# Atomic Focus 后端

基于《原子习惯》的自控力与目标管理 APP 后端服务。完整实现了 PRD 与接口文档（[API.md](./API.md)）描述的全部 P0 闭环：目标创立 → 拆解 → 打卡 → 进度跟踪 → 奖励 → 复盘 → 个人中心 → 数据管理。

## 技术栈

- **框架**：Spring Boot 3.2.5
- **语言**：Java 17
- **数据库**：MySQL 8.0
- **持久层**：MyBatis-Plus 3.5.5（注解 + 逻辑删除 + 分页插件）
- **认证**：JJWT（HS256，无状态 access / refresh token）
- **构建**：Maven
- **风格**：RESTful + 统一 `R<T>` 信封 + 全局异常处理 + CORS 全开

## 目录结构

```
backend/
├── pom.xml
├── README.md
├── API.md                              # 接口契约（已提供）
├── 自控力与目标管理APP - 产品需求文档（PRD）.md
├── sql/
│   └── schema.sql                      # MySQL 业务库建表脚本
└── src/main/
    ├── java/com/atomic/focus/
    │   ├── AtomicFocusApplication.java # 启动类
    │   ├── common/                     # 公共组件
    │   │   ├── base/                   # BaseEntity / 自动填充
    │   │   ├── config/                 # CORS / WebMvc / MyBatis-Plus
    │   │   ├── context/                # UserContext (ThreadLocal)
    │   │   ├── exception/              # BusinessException + 全局异常处理
    │   │   ├── interceptor/            # AuthInterceptor (JWT 校验)
    │   │   ├── result/                 # R / PageResult / ResultCode
    │   │   └── util/                   # JwtUtil / IdGenerator / JsonUtil
    │   └── modules/                    # 业务模块
    │       ├── auth/                   # 游客 / 手机号登录、token 刷新
    │       ├── user/                   # 个人资料、身份标签、成就
    │       ├── settings/               # 应用设置、自定义鼓励语
    │       ├── goal/                   # 目标 + 阶段（CRUD、归档）
    │       ├── checkin/                # 打卡、漏打、计时器、今日清单
    │       ├── dashboard/              # 首页聚合、单目标进度、月历
    │       ├── reward/                 # 奖励 CRUD、解锁、领取、奖励中心
    │       ├── review/                 # 复盘 CRUD、自动生成、收藏、趋势
    │       ├── scene/                  # 特殊场景（缩短/延长/暂停）
    │       ├── data/                   # 历史、导出、增量同步、重置
    │       └── notification/           # 应用内通知 + 推送设备注册
    └── resources/
        └── application.yml             # 配置文件（数据库、JWT、Jackson）
```

## 数据库初始化

1. 已配置好 MySQL 连接：

   - host：`dbconn.sealoshzh.site`
   - port：`42698`
   - user / password：`root` / `ttp2gpqs`
   - database：`atomic_focus`（启动时若不存在会自动创建）

2. 执行建表脚本：

   ```bash
   mysql -h dbconn.sealoshzh.site -P 42698 -u root -p < sql/schema.sql
   ```

   或使用 Sealos 控制台 / DataGrip 等工具执行 `sql/schema.sql`。

## 启动方式

```bash
# 1. 安装依赖并打包
mvn clean package -DskipTests

# 2. 运行
java -jar target/atomic-focus.jar
# 或
mvn spring-boot:run
```

启动成功后访问 `http://localhost:8080/api/v1/health` 验证服务存活。

## 接口快速验证

```bash
# 1. 创建游客
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/guest \
  -H 'Content-Type: application/json' \
  -d '{"device_id":"local-dev"}' | jq -r .data.access_token)

# 2. 创建目标
curl -X POST http://localhost:8080/api/v1/goals \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"3 个月掌握 Python",
    "category":"ability",
    "deadline":"2026-07-25",
    "core_need":"完成基础语法、函数、数据结构",
    "daily_habit":{"description":"每天 7 点学 10 分钟","duration":10}
  }'
```

## 主要业务规则（与 PRD 对齐）

| 规则 | 实现位置 |
|------|---------|
| **R01** 每日时长 > 10 分钟仅提示不阻断 | `GoalServiceImpl.create` 返回 `code=2001` 警告 |
| **R02** 漏打卡可选扣进度，最低 0% | `CheckinServiceImpl.missed` + `manual_deduction` 字段累计 |
| **R03** 暂停场景 ≤ 3 天 | `SceneServiceImpl.validate` |
| **R04** 进度 100% 即固化、可继续打卡 | `CheckinServiceImpl.checkin` 中原子置 `fixed=true` 并解锁标签 |
| **R05** 数据默认留存 1 年，软删除 | 全部业务表 `deleted` 逻辑删除字段 + `data_retention` 设置 |

## 默认验证码

为方便联调，`/api/v1/auth/send-code` 接口仅打印日志；`/api/v1/auth/login` 验证码固定为 `123456`。生产环境请接入真实短信平台。

## 与 Vue 前端对接

- 所有接口均按照 [API.md](./API.md) 实现，字段名严格保持 `snake_case`。
- 已开启全局 CORS（`*`），开发期可直连。
- 统一返回信封：

  ```json
  { "code": 0, "message": "ok", "data": { ... }, "traceId": "..." }
  ```

- 业务错误请按 [API.md 第 14 章错误码表](./API.md) 处理。

## 后续可扩展点

- 推送：当前 `notifications` 接口仅落库，未对接 APNs / FCM；可在 `NotificationService` 中接入第三方推送
- 短信：`AuthService.sendCode` 已留好接入位置
- 头像存储：`UserService.uploadAvatar` 当前生成伪 URL，建议接入对象存储（OSS / COS）
- 自动进阶定时任务：可在 `Scheduled` 任务中扫描 `dh_auto_level_up=true` 的目标，按 7 天连续打卡判定后调整 `dh_duration`
