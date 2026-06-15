# Atomic 后端接口文档

> 基于前端代码（`src/types`、`src/stores/app.ts`、`src/data/seed.ts` 与各 View）反推得到的 RESTful 接口契约。
>
> - **版本**：v1（接口契约）
> - **文档修订**：§15（Web 认证 · 2026-05-15）、§16（打卡页达成池）、**§17（综合迭代汇总 · 2026-05-16 14:12）**
> - **基础路径**：`/api/v1`
> - **协议**：HTTPS
> - **数据格式**：JSON（`Content-Type: application/json; charset=utf-8`）
> - **字符集**：UTF-8
> - **时间格式**：日期统一使用 `YYYY-MM-DD`（本地时区，由前端按用户所在时区转换），时间戳使用 ISO 8601（UTC 带 `Z`，如 `2026-04-25T12:00:00Z`）

---

## 目录

1. [通用约定](#1-通用约定)
2. [认证模块](#2-认证模块)
3. [用户与个人中心](#3-用户与个人中心)
4. [设置模块](#4-设置模块)
5. [目标模块](#5-目标模块)
6. [打卡模块](#6-打卡模块)
7. [进度与统计](#7-进度与统计)
8. [奖励模块](#8-奖励模块)
9. [复盘模块](#9-复盘模块)
10. [特殊场景模块](#10-特殊场景模块)
11. [数据管理](#11-数据管理)
12. [推送通知](#12-推送通知)
13. [数据模型 Schema 汇总](#13-数据模型-schema-汇总)
14. [错误码](#14-错误码)

版本迭代：**§15**（Web 认证 · 2026-05-15）、**§16**（打卡页达成池）、**§17**（综合交付 · **2026-05-16 14:12**）

---

## 1. 通用约定

### 1.1 请求头

| Header | 必填 | 示例 | 说明 |
|--------|------|------|------|
| `Authorization` | 登录后必填 | `Bearer eyJ...` | JWT 访问令牌 |
| `Content-Type` | POST/PUT/PATCH 必填 | `application/json; charset=utf-8` | |
| `X-Client-Version` | 可选 | `1.0.0` | 客户端版本，用于灰度 |
| `X-Device-Id` | 可选 | UUID | 设备唯一标识，多端同步用 |
| `X-Timezone` | 可选 | `Asia/Shanghai` | 用户所在时区，影响"今日"判定 |

### 1.2 响应统一信封

```json
{
  "code": 0,
  "message": "ok",
  "data": { },
  "trace_id": "f8c1c7e0-1c0a-4b9c-9c8e-71e0e9a7d111"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 0 表示成功，非 0 见 [错误码](#14-错误码) |
| `message` | string | 人可读消息 |
| `data` | object \| array \| null | 业务数据 |
| `trace_id` | string | 链路追踪 ID |

### 1.3 分页约定

列表接口统一支持：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | int | 1 | 页码（从 1 起） |
| `page_size` | int | 20 | 每页大小（≤100） |

分页响应：

```json
{
  "code": 0,
  "data": {
    "items": [],
    "page": 1,
    "page_size": 20,
    "total": 35,
    "has_more": true
  }
}
```

### 1.4 离线同步

PRD 要求离线可打卡，联网后自动同步。批量同步走 `POST /api/v1/sync/push` 接口（见 [11.3](#113-离线增量同步)）。客户端为每条本地操作生成 `client_op_id`（UUID v4），服务端基于 `client_op_id` 实现幂等。

---

## 2. 认证模块

支持游客模式（无需登录即可使用本地功能），登录后数据同步到云端。

### 2.1 创建游客账户（首次启动）

`POST /api/v1/auth/guest`

**请求**：
```json
{
  "device_id": "9D8E-..."
}
```

**响应**：
```json
{
  "code": 0,
  "data": {
    "user_id": "u_a1b2c3",
    "is_guest": true,
    "access_token": "eyJ...",
    "refresh_token": "rk_...",
    "expires_in": 7200
  }
}
```

### 2.2 手机号 + 密码登录

`POST /api/v1/auth/login`

使用 **HTTPS** 传输；服务端仅校验密码并对其做安全存储（如 bcrypt / Argon2 哈希），**禁止明文落库**。新用户请先调用 [2.3 注册](#23-手机号注册)。

**请求**：
```json
{
  "phone": "+8613800001111",
  "password": "PlaintextPassword",
  "merge_guest_user_id": "u_a1b2c3"
}
```

- `merge_guest_user_id`：可选。若当前会话为游客且需保留其数据，传入游客 `user_id`，服务端将数据合并至该手机号账号后再签发令牌。

**响应** `data` 同 [2.1](#21-创建游客账户首次启动)（含 `access_token`、`refresh_token`、`expires_in`、`is_guest`、`user_id`）。

### 2.3 手机号注册

`POST /api/v1/auth/register`

创建正式账号（`is_guest: false`），密码哈希规则同登录。**手机号已存在时应返回明确业务错误**（建议 HTTP 409 或约定业务码），前端可提示用户改用登录。

**请求**（与登录字段一致；`merge_guest_user_id` 可选，用于游客升级并入数据）：

```json
{
  "phone": "+8613800001111",
  "password": "PlaintextPassword",
  "merge_guest_user_id": "u_a1b2c3"
}
```

**响应**：推荐与 [2.2](#22-手机号--密码登录) 相同，在 `data` 中直接返回 `access_token`、`refresh_token`、`expires_in`、`user_id`、`is_guest`，Web 客户端注册成功后即可写入令牌并拉取远程数据。若仅返回 `user_id` 而无 token，客户端需再调用一次登录（不推荐）。

### 2.4 发送验证码（可选）

后端若另行提供短信验证码能力时使用；当前 Web 客户端采用 [2.2](#22-手机号--密码登录) / [2.3](#23-手机号注册)，不调用本接口。

`POST /api/v1/auth/send-code`

```json
{ "phone": "+8613800001111", "scene": "login" }
```

### 2.5 刷新 Token

`POST /api/v1/auth/refresh`

```json
{ "refresh_token": "rk_..." }
```

### 2.6 登出

`POST /api/v1/auth/logout`

---

## 3. 用户与个人中心

### 3.1 获取当前用户信息

`GET /api/v1/me`

**响应** `data`：

```json
{
  "user_id": "u_a1b2c3",
  "nickname": "清晨的微光",
  "avatar": "https://cdn.atomic.app/u/u_a1b2c3.jpg",
  "is_guest": false,
  "joined_at": "2026-03-12",
  "badges": ["原子坚持者", "早起初心人"],
  "stats": {
    "total_checkin_days": 42,
    "fixed_habits_count": 0,
    "active_goals_count": 3,
    "continuous_days": 7
  }
}
```

### 3.2 更新个人资料

`PATCH /api/v1/me`

```json
{
  "nickname": "新昵称",
  "avatar": "https://..."
}
```

### 3.3 上传头像

`POST /api/v1/me/avatar`

`multipart/form-data`，字段 `file`，返回 `{ "url": "https://..." }`

### 3.4 获取身份标签 / 成就

`GET /api/v1/me/achievements`

**响应** `data.items`：

```json
[
  { "key": "atom_keeper", "name": "原子坚持者", "desc": "完成 7 天连续打卡", "earned": true,  "earned_at": "2026-04-19" },
  { "key": "habit_master","name": "习惯掌控者", "desc": "至少固化一个习惯",   "earned": false, "earned_at": null }
]
```

---

## 4. 设置模块

### 4.1 获取设置

`GET /api/v1/settings`

```json
{
  "reminder_time": "19:00",
  "reminder_repeat": 3,
  "review_reminder_enabled": true,
  "review_reminder_time": "19:00",
  "push_enabled": true,
  "theme": "light",
  "data_retention": "1y",
  "default_progress_deduction": 1,
  "custom_encouragements": ["今天，做一个让明天感谢的人"]
}
```

### 4.2 更新设置（部分更新）

`PATCH /api/v1/settings`

```json
{ "theme": "dark", "reminder_time": "21:00" }
```

| 字段 | 类型 | 取值范围 |
|------|------|---------|
| `reminder_time` | string | `HH:mm` |
| `reminder_repeat` | int | 1–3 |
| `review_reminder_enabled` | bool | |
| `review_reminder_time` | string | `HH:mm` |
| `push_enabled` | bool | |
| `theme` | enum | `light` \| `dark` |
| `data_retention` | enum | `1y` \| `3y` \| `5y` \| `forever` |
| `default_progress_deduction` | int | 1–5 |

### 4.3 添加自定义鼓励语

`POST /api/v1/settings/encouragements`

```json
{ "content": "你已经走到了昨天羡慕的位置" }
```

### 4.4 删除自定义鼓励语

`DELETE /api/v1/settings/encouragements/{index}`

---

## 5. 目标模块

### 5.1 获取目标列表

`GET /api/v1/goals`

**Query**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | enum | `active`(默认) \| `fixed` \| `archived` \| `all` |
| `category` | enum | `habit` \| `ability` \| `state` \| `custom` |
| `keyword` | string | 模糊匹配 `name` / `final_goal` |
| `page` / `page_size` | int | 同分页约定 |

**响应** `data.items[i]`：见 [Goal Schema](#131-goal)

### 5.2 获取目标详情

`GET /api/v1/goals/{goal_id}`

返回完整 Goal（包含 `phases`、`rewards`、`daily_habit`、最近 35 天 `checkins`）。
`checkins` 完整列表请使用 [6.5](#65-获取打卡记录)。

### 5.3 创建目标

`POST /api/v1/goals`

```json
{
  "name": "3 个月掌握 Python",
  "category": "ability",
  "custom_category_name": null,
  "final_goal": "能独立编写简单小程序",
  "core_need": "完成基础语法、函数、数据结构的学习",
  "deadline": "2026-07-25",
  "total_description": "系统性学习 Python",
  "color": "brand",
  "icon": "📚",
  "phases": [
    {
      "name": "第 1 月",
      "description": "掌握 Python 基础语法",
      "total_minutes": 300,
      "start_date": "2026-04-25",
      "end_date": "2026-05-25"
    }
  ],
  "daily_habit": {
    "description": "每天晚上 7 点学 Python 10 分钟",
    "duration": 10,
    "auto_level_up": true,
    "level_up_step": 2
  },
  "client_op_id": "uuid-v4"
}
```

**响应**：返回创建后的完整 Goal（含服务端生成的 `id`、`created_at`）

**校验规则**：

- `daily_habit.duration` 为正整数分钟即可，**不再**因超过 10 分钟返回提示类响应（旧版 `code=2001` 已弃用）。
- `phases[i].end_date >= phases[i].start_date`
- `deadline >= today`

### 5.4 更新目标

`PATCH /api/v1/goals/{goal_id}`

支持以下字段（任选）：

```json
{
  "name": "...",
  "final_goal": "...",
  "core_need": "...",
  "deadline": "2026-08-01",
  "color": "mint",
  "icon": "🌅",
  "daily_habit": {
    "description": "...",
    "duration": 12,
    "auto_level_up": true,
    "level_up_step": 2
  }
}
```

### 5.5 阶段管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/goals/{goal_id}/phases` | 列出阶段 |
| `POST` | `/api/v1/goals/{goal_id}/phases` | 新增阶段 |
| `PATCH` | `/api/v1/goals/{goal_id}/phases/{phase_id}` | 更新阶段 |
| `DELETE` | `/api/v1/goals/{goal_id}/phases/{phase_id}` | 删除阶段 |
| `POST` | `/api/v1/goals/{goal_id}/phases/{phase_id}/complete` | 手动标记阶段完成 |

阶段完成会触发推送 + 鼓励语，详见 [12.2](#122-推送事件类型)。

### 5.6 归档 / 结束目标

`POST /api/v1/goals/{goal_id}/archive`

PRD R04：习惯固化后用户可选「结束目标」或「继续打卡」。归档后 `archived = true`，进度不再变化。

### 5.7 删除目标

`DELETE /api/v1/goals/{goal_id}`

R05：默认软删除，目标进入「历史记录」并保留 1 年（受 `data_retention` 设置控制）。

`?hard=true` 立即硬删除（用户主动操作 + 二次确认）。

---

## 6. 打卡模块

### 6.1 完成打卡

`POST /api/v1/goals/{goal_id}/checkins`

```json
{
  "date": "2026-04-25",
  "duration": 11,
  "status": "done",
  "note": "晚上完成，状态不错",
  "client_op_id": "uuid-v4"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `date` | string | YYYY-MM-DD，默认今天 |
| `duration` | int | 实际打卡分钟数（≥1），且须 **≥ ⌈当日有效目标时长（分钟）÷ 2⌉**（见下） |
| `status` | enum | `done` \| `late`，参见 [打卡状态](#状态机) |
| `note` | string | 可选备注 |

当日有效目标时长：`daily_habit.duration`；若存在生效中的「缩短时长」特殊场景，则为 `min(daily_habit.duration, shorten_to)`。完成 / 延迟打卡时，上报的 `duration` 不得低于该有效时长的一半（按分钟取 `⌈有效分钟数 / 2⌉` 的整数下限，与客户端计时「累计满一半」一致）。

**响应**：

```json
{
  "code": 0,
  "data": {
    "checkin": { "date": "2026-04-25", "status": "done", "duration": 11 },
    "goal_progress": 38,
    "phase_completed": null,           // 若有阶段完成则返回 phase 对象
    "habit_fixed": false,              // 是否首次达 100%
    "rewards_unlocked": [],            // 本次打卡触发解锁的奖励
    "encouragement": "完成胜过完美，继续保持这份节奏"
  }
}
```

**幂等**：同一日同 `goal_id` 重复调用，状态保持最新一次的 `duration` 与 `status`，不重复累加 `total_checkin_days`。

### 6.2 延迟打卡

业务上等同于 `status: "late"`，由前端在「打卡截止时间 + 1 小时」内调用。服务端校验：仅允许 `last_reminder_time + 1h` 之内提交 `late`，否则返回 `code=2010`。

### 6.3 标记今日未打卡

`POST /api/v1/goals/{goal_id}/checkins/missed`

```json
{
  "date": "2026-04-25",
  "deduct_progress": true,
  "client_op_id": "uuid-v4"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `deduct_progress` | bool | 是否扣除进度，true 时按 `default_progress_deduction` |

**响应**：

```json
{
  "code": 0,
  "data": {
    "goal_progress": 35,
    "deducted": 1,
    "manual_deduction_total": 3
  }
}
```

> 服务端将扣除累计在 `goal.manual_deduction` 字段，避免被后续重新计算覆盖（与前端 `recomputeProgress` 实现一致）。

### 6.4 删除某日打卡（撤销误操作）

`DELETE /api/v1/goals/{goal_id}/checkins/{date}`

### 6.5 获取打卡记录

`GET /api/v1/goals/{goal_id}/checkins`

**Query**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `start_date` | string | 起始日期 |
| `end_date` | string | 截止日期，默认今天 |
| `status` | enum | 可选过滤 |

**响应** `data.items[i]`：见 [CheckinRecord Schema](#132-checkinrecord)

### 6.6 跨目标的"今日打卡"

`GET /api/v1/checkins/today`

```json
{
  "code": 0,
  "data": {
    "date": "2026-04-25",
    "items": [
      {
        "goal_id": "g_xxx",
        "goal_name": "3 个月掌握 Python",
        "goal_icon": "📚",
        "daily_habit": "每天晚上 7 点学 Python 10 分钟",
        "duration_target": 10,
        "checked": false
      }
    ],
    "checked_count": 1,
    "total_count": 3,
    "progress_percent": 33
  }
}
```

### 6.7 计时器会话（可选，多端同步用）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/timer/start` | 启动计时（返回 `session_id`） |
| `POST` | `/api/v1/timer/{session_id}/pause` | 暂停 |
| `POST` | `/api/v1/timer/{session_id}/resume` | 继续 |
| `POST` | `/api/v1/timer/{session_id}/finish` | 结束并自动调用 [6.1](#61-完成打卡) |
| `GET`  | `/api/v1/timer/active` | 获取当前活跃 session |

---

## 7. 进度与统计

### 7.1 Dashboard 首页聚合

`GET /api/v1/dashboard`

```json
{
  "code": 0,
  "data": {
    "today": {
      "date": "2026-04-25",
      "weekday": "周六",
      "checked_count": 1,
      "total_count": 3,
      "progress_percent": 33
    },
    "stats": {
      "active_goals": 3,
      "continuous_days": 7,
      "fixed_habits": 0
    },
    "weekly_rates": [
      { "date": "2026-04-19", "rate": 100 },
      { "date": "2026-04-20", "rate": 67  },
      { "date": "2026-04-21", "rate": 67  },
      { "date": "2026-04-22", "rate": 100 },
      { "date": "2026-04-23", "rate": 33  },
      { "date": "2026-04-24", "rate": 100 },
      { "date": "2026-04-25", "rate": 33  }
    ],
    "available_rewards": [],
    "motto": "完成胜过完美，继续保持这份节奏",
    "active_scene": null
  }
}
```

### 7.2 单目标进度详情

`GET /api/v1/goals/{goal_id}/progress`

```json
{
  "data": {
    "progress": 36,
    "manual_deduction": 0,
    "completed_minutes": 280,
    "total_minutes": 1350,
    "remaining_minutes": 1070,
    "checkin_stats": {
      "total": 28,
      "done": 24,
      "late": 2,
      "missed": 2
    },
    "continuous_days": 5,
    "phases": [
      { "id": "p_xxx", "name": "第 1 月", "completed": true, "completed_at": "2026-04-25" }
    ]
  }
}
```

### 7.3 月历轨迹

`GET /api/v1/goals/{goal_id}/calendar?year=2026&month=4`

```json
{
  "data": {
    "year": 2026,
    "month": 4,
    "cells": [
      { "date": "2026-04-01", "status": "done",   "duration": 12 },
      { "date": "2026-04-02", "status": "missed", "duration": 0  },
      { "date": "2026-04-03", "status": "late",   "duration": 8  },
      { "date": "2026-04-04", "status": "paused", "duration": 0  }
    ]
  }
}
```

---

## 8. 奖励模块

### 8.1 获取目标的奖励列表

`GET /api/v1/goals/{goal_id}/rewards`

### 8.2 跨目标的奖励中心

`GET /api/v1/rewards`

**Query**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | enum | `available` \| `locked` \| `claimed` \| `all` |

**响应** `data.items[i]`：

```json
{
  "id": "r_xxx",
  "goal_id": "g_xxx",
  "goal_name": "3 个月掌握 Python",
  "goal_icon": "📚",
  "goal_color": "brand",
  "name": "半程奖励",
  "content": "一本想读已久的书",
  "trigger_type": "progress",
  "trigger_value": 50,
  "trigger_label": "进度达 50%",
  "status": "available",
  "claimed_at": null
}
```

### 8.3 添加奖励阶段

`POST /api/v1/goals/{goal_id}/rewards`

```json
{
  "name": "半程奖励",
  "content": "一本想读已久的书",
  "trigger_type": "progress",
  "trigger_value": 50,
  "client_op_id": "uuid-v4"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `trigger_type` | enum | `progress`(0–100) \| `phase`(阶段序号 ≥1) \| `days`(连续天数 ≥1) |
| `trigger_value` | int | 与 `trigger_type` 对应的阈值 |

### 8.4 更新奖励

`PATCH /api/v1/goals/{goal_id}/rewards/{reward_id}`

### 8.5 删除奖励

`DELETE /api/v1/goals/{goal_id}/rewards/{reward_id}`

### 8.6 领取奖励

`POST /api/v1/goals/{goal_id}/rewards/{reward_id}/claim`

仅 `status = available` 可领取，领取后置为 `claimed`，记录 `claimed_at`。

---

## 9. 复盘模块

### 9.1 获取复盘列表

`GET /api/v1/reviews`

**Query**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | enum | `weekly` \| `monthly` \| `manual` \| `all` |
| `goal_id` | string | 按目标过滤 |
| `start_date` / `end_date` | string | 日期范围 |
| `keyword` | string | 内容模糊搜索 |
| `is_favorite` | bool | 仅看收藏 |

### 9.2 获取复盘详情

`GET /api/v1/reviews/{review_id}`

### 9.3 添加手动复盘

`POST /api/v1/reviews`

```json
{
  "type": "manual",
  "title": "阶段小结：Python 基础语法",
  "date": "2026-04-23",
  "goal_id": "g_xxx",
  "content": "基础语法部分已经过完一遍...",
  "client_op_id": "uuid-v4"
}
```

### 9.4 更新复盘

`PATCH /api/v1/reviews/{review_id}`

### 9.5 删除复盘

`DELETE /api/v1/reviews/{review_id}`

### 9.6 收藏 / 取消收藏

`POST /api/v1/reviews/{review_id}/favorite`

```json
{ "is_favorite": true }
```

### 9.7 触发自动生成本周 / 本月报告

`POST /api/v1/reviews/generate`

```json
{ "scope": "weekly", "force": false }
```

| 字段 | 取值 | 说明 |
|------|-----|------|
| `scope` | `weekly` \| `monthly` | |
| `force` | bool | 已存在时是否覆盖 |

> 后端定时任务说明：
> - 周报：每周日 22:00 自动生成（PRD 要求）
> - 月报：每月最后一天 22:00 自动生成
>
> 此接口主要用于"立即生成"或客户端补偿调用。

报告内容必须包含 `metrics` 与 `suggestions`：

```json
{
  "id": "rev_xxx",
  "type": "weekly",
  "title": "本周复盘报告",
  "date": "2026-04-25",
  "goal_id": null,
  "metrics": {
    "checkin_rate": 86,
    "avg_duration": 11,
    "missed_days": 1,
    "progress_delta": 6,
    "total_minutes": 77
  },
  "suggestions": [
    "未完成原因多为「忘记提醒」，建议将打卡提醒与「晚饭后」绑定形成习惯堆叠",
    "本周平均时长 11 分钟，已超目标 10%，可以考虑下周自动进阶到 12 分钟"
  ]
}
```

### 9.8 复盘趋势数据（图表用）

`GET /api/v1/reviews/trend?limit=8`

返回最近 N 份带 `metrics` 的报告，按日期升序，用于首页/复盘中心的折线图。

### 9.9 引导问题

`GET /api/v1/reviews/guides`

```json
{
  "data": {
    "items": [
      "本周/本月哪些时段最容易完成打卡？背后的原因是什么？",
      "中断或低谷出现在什么场景？下次如何用「习惯堆叠」化解？",
      "哪些细节让你坚持下来？哪些奖励/反馈最有效？"
    ]
  }
}
```

---

## 10. 特殊场景模块

### 10.1 获取场景列表

`GET /api/v1/scenes`

```json
{
  "data": {
    "items": [
      {
        "id": "s_xxx",
        "type": "holiday",
        "label": "清明假期",
        "start_date": "2026-04-04",
        "end_date": "2026-04-06",
        "mode": "shorten",
        "shorten_to": 3,
        "extend_hours": null,
        "active": true
      }
    ],
    "active_scene": {
      "id": "s_xxx",
      "mode": "shorten",
      "shorten_to": 3
    }
  }
}
```

> `active` 由服务端基于当前日期实时判断，不是持久化字段。

### 10.2 添加场景

`POST /api/v1/scenes`

```json
{
  "type": "travel",
  "label": "出差北京",
  "start_date": "2026-05-01",
  "end_date": "2026-05-03",
  "mode": "pause",
  "client_op_id": "uuid-v4"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | enum | `holiday` \| `travel` \| `sick` \| `other` |
| `mode` | enum | `shorten` \| `extend` \| `pause` |
| `shorten_to` | int | mode = shorten 时必填，最低 1 |
| `extend_hours` | int | mode = extend 时必填，1–3 |

**校验规则**（PRD R03）：

- `end_date >= start_date`
- `mode = pause` 时 `(end_date - start_date + 1) ≤ 3`
- 同一时间段最多一个生效场景，重叠返回 `code=2030`

### 10.3 更新场景

`PATCH /api/v1/scenes/{scene_id}`

### 10.4 删除场景

`DELETE /api/v1/scenes/{scene_id}`

---

## 11. 数据管理

### 11.1 历史记录检索

`GET /api/v1/history`

**Query**：

| 参数 | 说明 |
|------|------|
| `kind` | `goal` \| `checkin` \| `review` \| `reward` \| `all` |
| `goal_id` | 按目标过滤 |
| `start_date` / `end_date` | 时间范围 |

### 11.2 数据导出

`POST /api/v1/data/export`

```json
{ "format": "json" }
```

| `format` | 说明 |
|----------|------|
| `json` | 全量导出，返回下载链接 |
| `csv` | 按目标拆成多个 CSV，打包 zip |

**响应**：

```json
{
  "data": {
    "task_id": "task_xxx",
    "status": "pending",
    "download_url": null
  }
}
```

通过 `GET /api/v1/data/export/{task_id}` 轮询任务状态。

### 11.3 离线增量同步

客户端在重新联网后批量上传操作日志，服务端按 `client_op_id` 幂等执行：

`POST /api/v1/sync/push`

```json
{
  "operations": [
    {
      "client_op_id": "uuid-1",
      "client_ts": "2026-04-25T03:00:00Z",
      "type": "checkin.create",
      "payload": {
        "goal_id": "g_xxx",
        "date": "2026-04-25",
        "duration": 11,
        "status": "done"
      }
    },
    {
      "client_op_id": "uuid-2",
      "client_ts": "2026-04-25T03:01:00Z",
      "type": "review.create",
      "payload": { "type": "manual", "content": "..." }
    }
  ]
}
```

**响应**：

```json
{
  "data": {
    "results": [
      { "client_op_id": "uuid-1", "ok": true,  "data": { /* 同 6.1 响应 */ } },
      { "client_op_id": "uuid-2", "ok": false, "error": { "code": 4002, "message": "校验失败" } }
    ],
    "server_ts": "2026-04-25T03:05:01Z"
  }
}
```

支持的 `type`：

```
goal.create | goal.update | goal.archive | goal.delete
phase.create | phase.update | phase.delete | phase.complete
checkin.create | checkin.missed | checkin.delete
reward.create | reward.update | reward.delete | reward.claim
review.create | review.update | review.delete | review.favorite
scene.create | scene.update | scene.delete
profile.update | settings.update
```

### 11.4 全量拉取（首次登录或换设备）

`GET /api/v1/sync/pull?since=2026-04-25T00:00:00Z`

返回 `since` 之后变更的所有实体，用于差量同步：

```json
{
  "data": {
    "server_ts": "2026-04-25T05:00:00Z",
    "goals":    { "upserts": [], "deletes": [] },
    "checkins": { "upserts": [], "deletes": [] },
    "rewards":  { "upserts": [], "deletes": [] },
    "reviews":  { "upserts": [], "deletes": [] },
    "scenes":   { "upserts": [], "deletes": [] },
    "profile":  { /* 完整对象 */ },
    "settings": { /* 完整对象 */ }
  }
}
```

### 11.5 清除全部数据（重置）

`POST /api/v1/data/reset`

```json
{ "confirm": "RESET" }
```

---

## 12. 推送通知

### 12.1 注册推送 Token

`POST /api/v1/notifications/devices`

```json
{
  "platform": "ios",
  "push_token": "fcm/apns token",
  "device_id": "..."
}
```

### 12.2 推送事件类型

后端根据用户设置定时调度推送，事件类型：

| Event | 触发时机 | 默认时间 |
|-------|---------|---------|
| `checkin_reminder` | 每日打卡提醒 | `settings.reminder_time`，未打卡每 2 小时重复（最多 `reminder_repeat`） |
| `weekly_review_reminder` | 周复盘提醒 | 每周日 19:00 |
| `monthly_review_reminder` | 月复盘提醒 | 每月最后一天 19:00 |
| `weekly_review_ready` | 周报已生成 | 每周日 22:00 |
| `monthly_review_ready` | 月报已生成 | 每月最后一天 22:00 |
| `phase_completed` | 阶段达成 | 触发即推 |
| `habit_fixed` | 习惯固化 | 触发即推 |
| `reward_unlocked` | 奖励解锁 | 触发即推 |
| `level_up` | 自动进阶时长调整 | 触发即推 |
| `scene_ended` | 特殊场景结束当日 | 当日触发 |

### 12.3 应用内通知列表

`GET /api/v1/notifications`

**Query**：`unread_only=true`

### 12.4 标记已读

`POST /api/v1/notifications/read`

```json
{ "notification_ids": ["n_xxx", "n_yyy"], "all": false }
```

---

## 13. 数据模型 Schema 汇总

> 字段命名约定：API 使用 `snake_case`，前端 TypeScript 使用 `camelCase`，两端通过 mapper 转换。下面以 API 命名为准。

### 13.1 Goal

```ts
{
  id: string;                    // 主键
  user_id: string;
  name: string;
  category: 'habit' | 'ability' | 'state' | 'custom';
  custom_category_name?: string;
  final_goal: string;
  core_need: string;             // 习惯固化判定标准
  total_description: string;
  deadline: string;              // YYYY-MM-DD
  created_at: string;            // YYYY-MM-DD
  updated_at: string;            // ISO 8601
  color: 'brand' | 'mint' | 'lavender' | 'peach';
  icon: string;
  daily_habit: DailyHabit;
  phases: Phase[];
  rewards: Reward[];
  progress: number;              // 0-100
  manual_deduction: number;      // 累计手动扣除百分比
  archived: boolean;
  fixed: boolean;                // 是否已固化（progress >= 100）
}
```

### 13.2 CheckinRecord

```ts
{
  date: string;                  // YYYY-MM-DD
  status: 'done' | 'late' | 'missed' | 'paused';
  duration: number;              // 实际完成的分钟数
  note?: string;
  created_at: string;            // ISO 8601
}
```

#### 状态机

```
pending --[完成打卡]--> done
pending --[超时1h内]--> late
pending --[宣告未打卡]--> missed
pending --[特殊场景pause]--> paused

done    --[再次打卡]--> done   (覆盖时长，不累加 total_checkin_days)
missed  --[补打卡]--> done / late
```

### 13.3 DailyHabit

```ts
{
  description: string;
  duration: number;              // 单位：分钟，常规默认建议 30
  days_per_week: number;         // 每周计划打卡天数，1–7，默认 7
  auto_level_up: boolean;        // 兼容旧数据；新创建目标建议固定 false
  level_up_step: number;         // 兼容旧数据；与 auto_level_up 配套
}
```

### 13.4 Phase

```ts
{
  id: string;
  name: string;
  description: string;
  total_minutes: number;         // 阶段总时长目标（可为 0，表示纯习惯类）
  start_date: string;            // YYYY-MM-DD
  end_date: string;              // YYYY-MM-DD
  completed: boolean;
  completed_at?: string;
}
```

### 13.5 Reward

```ts
{
  id: string;
  name: string;
  content: string;
  trigger_type: 'progress' | 'phase' | 'days';
  trigger_value: number;
  status: 'locked' | 'available' | 'claimed';
  claimed_at?: string;
}
```

### 13.6 Review

```ts
{
  id: string;
  type: 'weekly' | 'monthly' | 'manual';
  title: string;
  date: string;                  // YYYY-MM-DD
  goal_id?: string;
  goal_name?: string;
  content?: string;              // 手动复盘文本
  metrics?: {
    checkin_rate: number;        // 0-100
    avg_duration: number;        // 分钟
    missed_days: number;
    progress_delta: number;
    total_minutes: number;
  };
  suggestions?: string[];
  is_favorite: boolean;
  created_at: string;
}
```

### 13.7 SpecialScene

```ts
{
  id: string;
  type: 'holiday' | 'travel' | 'sick' | 'other';
  label: string;
  start_date: string;
  end_date: string;
  mode: 'shorten' | 'extend' | 'pause';
  shorten_to?: number;           // mode=shorten 时必填
  extend_hours?: number;         // mode=extend 时必填，1-3
  active: boolean;               // 服务端实时计算
}
```

### 13.8 UserProfile

```ts
{
  user_id: string;
  nickname: string;
  avatar?: string;
  is_guest: boolean;
  joined_at: string;             // YYYY-MM-DD
  badges: string[];
  total_checkin_days: number;
  fixed_habits_count: number;
}
```

### 13.9 AppSettings

```ts
{
  reminder_time: string;         // HH:mm
  reminder_repeat: number;       // 1-3
  review_reminder_enabled: boolean;
  review_reminder_time: string;
  push_enabled: boolean;
  theme: 'light' | 'dark';
  data_retention: '1y' | '3y' | '5y' | 'forever';
  default_progress_deduction: number;  // 1-5
  custom_encouragements: string[];
}
```

---

## 14. 错误码

| code | HTTP | 含义 |
|------|------|------|
| 0    | 200  | 成功 |
| 1001 | 401  | 未登录 / Token 无效 |
| 1002 | 401  | Token 过期，需 refresh |
| 1003 | 403  | 无权限 |
| 1004 | 429  | 频率限制 |
| 2001 | 200  | （已弃用）曾：每日时长超过 10 分钟的非阻断警告；现行产品不要求该校验 |
| 2002 | 400  | 参数校验失败 |
| 2003 | 404  | 资源不存在 |
| 2004 | 409  | 资源冲突（如 client_op_id 重复且参数不一致） |
| 2010 | 400  | 延迟打卡超出允许时间窗（>1h） |
| 2011 | 400  | 重复操作（同日已打卡且参数完全一致，幂等忽略） |
| 2020 | 400  | 阶段日期范围非法 |
| 2030 | 409  | 特殊场景重叠 |
| 2031 | 400  | 暂停场景超过 3 天上限 |
| 2040 | 400  | 奖励触发条件未达成（领取时） |
| 5001 | 500  | 服务内部错误 |
| 5002 | 503  | 服务暂不可用（维护中） |

---

## 附录 A：业务规则与接口的对应关系（PRD R01–R05）

| 规则 | 涉及接口 | 实现位置 |
|------|---------|---------|
| **R01** ~~每日初始时长 ≤ 10 分钟，超过仅提示~~ **已取消** | `POST /goals`、`PATCH /goals/{id}` | 每日时长由用户自定，服务端无需因 `duration > 10` 另行警告 |
| **R02** 未打卡可选扣进度（默认 1–2%），最低 0% | `POST /goals/{id}/checkins/missed` | `deduct_progress=true` 时按 `default_progress_deduction` 扣 `manual_deduction` |
| **R03** 暂停最多 3 天 | `POST /scenes` | 校验 `mode=pause` && 区间 ≤ 3 天 |
| **R04** 进度达 100% 即固化，可继续打卡 | `POST /goals/{id}/checkins`（响应含 `habit_fixed`） | 服务端原子事务：`progress >= 100 && !fixed` 时置 `fixed=true` 并下发推送 |
| **R05** 数据默认留存 1 年，支持自定义最长永久 | `PATCH /settings`、`POST /data/reset` | 后台定时任务按 `data_retention` 清理过期数据；删除目标默认软删除 |

## 附录 B：自动进阶定时任务

后端每日凌晨 03:00 扫描所有 `daily_habit.auto_level_up = true` 的目标：

- 若该目标"最近连续打卡天数"是 7 的整数倍，则将 `daily_habit.duration` 增加 `level_up_step` 分钟
- 推送 `level_up` 事件，附带前后时长对比
- 写入审计日志 `audit_logs`，便于复盘报告引用

---

## 15. 版本迭代说明（Web 认证 · 2026-05-15）

> **综合迭代清单（2026-05-16 14:12）**另见 **§17**。
>
> 以下变更与当前 `atomic` Web 前端实现一致，供后端对照实现与联调。**若历史方案曾使用「手机号 + 短信验证码」登录，本文以「手机号 + 密码」为准；短信能力保留为可选扩展。**

### 15.1 接口清单变更摘要

| 方法 | 路径 | 变更类型 | 说明 |
|------|------|----------|------|
| `POST` | `/api/v1/auth/guest` | 无变更 | 仍为游客签发 token；Web 在 remote 且无本地 token 时会优先走此接口（可通过前端环境关闭，见 15.5）。 |
| `POST` | `/api/v1/auth/login` | **请求体变更** | 使用 **`password` 字段**，**不再使用短信 `code`**。请求：`phone`（E.164）、`password`、可选 `merge_guest_user_id`。响应 `data` 仍同 §2.1（含 `access_token`、`refresh_token`、`expires_in`、`user_id`、`is_guest` 等）。 |
| `POST` | `/api/v1/auth/register` | **新增** | 请求体与 `login` **相同**（`phone`、`password`、可选 `merge_guest_user_id`）。**推荐**响应与登录一致并**直接返回 token**，便于注册后免二次请求；手机号已存在时返回明确错误（建议 HTTP **409** 或约定业务码）。 |
| `POST` | `/api/v1/auth/send-code` | **可选；Web 未调用** | 后端若提供短信验证码可保留接口；当前 Web 客户端默认不依赖。 |
| `POST` | `/api/v1/auth/refresh` | 无变更 | 见 §2.5。 |
| `POST` | `/api/v1/auth/logout` | 无变更 | 见 §2.6。 |
| `GET` | `/api/v1/me` | **字段约定强化** | `data` 需含 **`user_id`**、**`is_guest`**（及 `joined_at`、`stats`、昵称等），供前端区分游客/正式用户及合并游客逻辑；键名 `snake_case`。 |

### 15.2 `merge_guest_user_id`（登录与注册共用）

- **可选**。客户端当前为**游客**且需保留其数据时传入**游客** `user_id`。  
- **登录**：并入该手机号账号后签发令牌。  
- **注册**：创建该手机号账号的同时并入游客数据（游客升级）。

### 15.3 与附录类型（§13.8 UserProfile）对齐

- `GET /me` 返回的用户对象应包含文档中的 `user_id`、`is_guest` 等字段，与 §13.8 一致。

### 15.4 建议错误语义（可选同步至 §14）

| 场景 | 建议 |
|------|------|
| 注册手机号已占用 | HTTP 409 或业务码 + `message`（如提示改用登录） |
| 登录手机号或密码错误 | 401 或 400 + 防枚举的统一文案 |
| 暴力尝试 | §14 `1004` 频率限制 |

### 15.5 Web 前端环境与路由（后端排障参考）

| 项 | 说明 |
|----|------|
| `VITE_DATA_SOURCE` | `remote` 调用本文档 API；`mock` 为纯本地演示，不发认证请求。 |
| `VITE_ENABLE_GUEST` | `0` 时无 token 仅允许 `#/login`、`#/register`，其它路由跳转登录。 |
| 页面路由 | `#/login` 登录；`#/register` 注册（确认密码）；成功后若响应含 token，与登录相同写入并拉取全量数据。 |

---

## 16. 版本迭代说明（打卡页「目标达成池」· Web 前端）

> **汇总与时间线**：整包迭代说明见 **§17（2026-05-16 14:12）**。
>
> **本节不改变任何 REST 路径、请求体或响应 JSON 的约定**；仅记录 `atomic` 打卡页（`CheckinView`）已落地的**用户可见行为与动效**。进度与数据来源仍以 **§6 打卡模块**及目标资源中的 `progress` 等字段为准。

### 16.1 行为摘要

| 项 | 说明 |
|----|------|
| 入口与数据 | 达成池展示**当前路由/选中目标**的 `progress`（0–100），由既有目标查询/本地 Store 提供，**不新增**独立「水池」接口。 |
| 视觉 | 液面、徽章、器皿与涟漪为**统一活力绿色系**；达成池卡片外层为中性浅底 + 柔阴影（无额外硬描边双层框）。水池器皿在卡片内**横向通栏**对齐（抵消大卡片水平内边距）。 |
| 打卡成功反馈 | 触发水滴下落（总时长约 **2.5s**）、入水时刻的**涟漪**与**液面短时 pulse**；动效为**竖直下落**，分段 `animation-timing-function` 以减轻顿挫感。 |
| 与接口的衔接 | 打卡仍走 `POST /api/v1/goals/{goal_id}/checkins`（或客户端等价封装）；成功后再播放上述动效；失败不播放。 |

### 16.2 实现索引（排障）

| 文件 | 说明 |
|------|------|
| `atomic/src/views/CheckinView.vue` | 达成池 DOM/CSS、水滴 `energy-drop` 关键帧、`triggerPoolDrop` 与入水时刻定时器（与 CSS 进度对齐）。 |

### 16.3 关联文档

- 产品侧：**PRD** `自控力与目标管理APP - 产品需求文档（PRD） .md` **§7.4**、**§7.5（2026-05-16 14:12）**。

---

## 17. 版本迭代说明（综合交付 · atomic Web · 2026-05-16 14:12）

> 本条汇总 **`atomic` Web 本会话相关交付**，标记 **2026-05-16 14:12**。**§15**（认证）、**§16**（达成池）为分主题；本节供联调整体清单。若在 §1–§14 中已有定义且无冲突，则契约不变。

### 17.1 交付范围一览

| 域 | 说明 |
|------|------|
| **认证与账号** | **`POST .../auth/login`** 使用 **`password`**；**`POST .../auth/register`**；**`GET .../me`** 含 **`user_id`、`is_guest`**；可选 **`merge_guest_user_id`。细则：§15、§2。** |
| **打卡：最低录入时长** | **`status`** 为 **`done` / `late`** 时，服务端 **不再** 强制 `duration` 达到半数有效目标；**`duration` 可为 0**（一键完成）。`GET .../checkins/today` 中 **`minimum_completed_minutes` 固定为 0**；`effective_daily_target_minutes` 仍可用于展示。** |
| **打卡：计时与前端** | `CheckinView` 在完成前校验累计秒数；**`await store.checkin` 成功后**清空计时再 **`triggerPoolDrop`**。达成池专项见 **§16**。 |
| **目标创建向导** | `GoalCreateView`：**完成天数 + 每日时分**、折合总小时、第三步时长与第二步默认同源可编辑、每周打卡天数。**不改变 §5 既有 Goal JSON 契约。** |
| **公式同源** | `effectiveDailyTargetMinutes`、`minCheckinRecordedMinutes`、`minCheckinElapsedSeconds`（`atomic/src/composables/utils.ts`），与 **`stores/app.ts` → `checkin`** 同源。 |

### 17.2 实现索引（排障）

| 路径 | 说明 |
|------|------|
| `atomic/src/views/LoginView.vue`、`RegisterView.vue` | 登录、注册 |
| `atomic/src/views/CheckinView.vue` | 计时、半数阈值、`complete`、水滴与达成池 |
| `atomic/src/views/GoalCreateView.vue` | 三步向导与时长摘要 |
| `atomic/src/stores/app.ts`、`atomic/src/composables/utils.ts` | 打卡校验与有效每日时长 |

### 17.3 关联文档（PRD）

- `自控力与目标管理APP - 产品需求文档（PRD） .md`：**§7.3**、**§7.4**、**§7.5（与本节同批次时间戳）**
