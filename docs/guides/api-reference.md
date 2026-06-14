# EasyArchive API 参考文档

## 1. 概述

EasyArchive 提供完整的 RESTful API，所有接口统一使用 `/api/v1` 作为前缀，采用 JSON 格式进行数据交换。

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| Base URL | `http://localhost:8789/api/v1` |
| 认证方式 | JWT Bearer Token |
| 请求格式 | application/json |
| 响应格式 | application/json |
| 字符编码 | UTF-8 |

### 1.2 统一响应格式

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "requestId": "req-xxxxx",
  "data": { ... }
}
```

**响应码说明：**

| 码值 | 说明 |
|------|------|
| `SUCCESS` | 操作成功 |
| `INVALID_PARAM` | 参数校验失败 |
| `RESOURCE_NOT_FOUND` | 资源不存在 |
| `PERMISSION_DENIED` | 权限不足 |
| `SYSTEM_ERROR` | 系统内部错误 |
| `BUSINESS_ERROR` | 业务逻辑错误 |

### 1.3 分页响应格式

```json
{
  "code": "SUCCESS",
  "data": {
    "total": 100,
    "page": 1,
    "size": 10,
    "items": [ ... ]
  }
}
```

### 1.4 认证方式

所有需要认证的接口，在请求头中携带 JWT Token：

```
Authorization: Bearer <token>
```

---

## 2. 认证接口

### 2.1 用户登录

```
POST /api/v1/auth/login
```

**请求体：**

```json
{
  "username": "admin",
  "password": "admin"
}
```

**响应：**

```json
{
  "code": "SUCCESS",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx.yyy",
    "expiresIn": 86400
  }
}
```

### 2.2 用户登出

```
POST /api/v1/auth/logout
```

**鉴权：** 需要

### 2.3 获取当前用户信息

```
POST /api/v1/auth/me
```

**鉴权：** 需要

**响应：**

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 1,
    "username": "admin",
    "role": "platform_admin",
    "datasourcePermissions": [
      {
        "datasourceId": 1,
        "permission": "MANAGE"
      }
    ]
  }
}
```

### 2.4 修改密码

```
POST /api/v1/auth/change-password
```

**鉴权：** 需要

**请求体：**

```json
{
  "oldPassword": "admin",
  "newPassword": "new_password_123"
}
```

---

## 3. 数据源接口

### 3.1 查询数据源列表

```
GET /api/v1/datasources
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| enabled | boolean | 否 | 按启用状态过滤 |

**鉴权：** 需要

### 3.2 创建数据源

```
POST /api/v1/datasources
```

**鉴权：** 需要

**请求体：**

```json
{
  "name": "生产数据库",
  "type": "MYSQL",
  "host": "192.168.1.100",
  "port": 3306,
  "database": "order_db",
  "username": "readonly",
  "password": "******",
  "enabled": true
}
```

### 3.3 更新数据源

```
PUT /api/v1/datasources/{id}
```

**鉴权：** 需要

### 3.4 删除数据源

```
DELETE /api/v1/datasources/{id}
```

**鉴权：** 需要

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 数据源 ID |

### 3.5 测试数据源连接

```
POST /api/v1/datasources/test
```

**鉴权：** 需要

**请求体：**

```json
{
  "host": "192.168.1.100",
  "port": 3306,
  "database": "order_db",
  "username": "readonly",
  "password": "******"
}
```

---

## 4. 归档分组接口

### 4.1 查询分组列表

```
GET /api/v1/archive/groups
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| enableStatus | Integer | 否 | 按启用状态过滤 |

**鉴权：** 需要

### 4.2 分页查询分组

```
GET /api/v1/archive/groups/page
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| enableStatus | Integer | 否 | 按启用状态过滤 |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页大小，默认 10 |

**鉴权：** 需要

### 4.3 树形结构查询

```
GET /api/v1/archive/groups/tree
```

**鉴权：** 需要

### 4.4 获取分组详情

```
GET /api/v1/archive/groups/{id}
```

**鉴权：** 需要

### 4.5 获取分组概览

```
GET /api/v1/archive/groups/{id}/overview
```

**鉴权：** 需要

### 4.6 创建分组

```
POST /api/v1/archive/groups
```

**鉴权：** 需要

**请求体：**

```json
{
  "name": "订单归档分组",
  "description": "订单表历史数据归档",
  "enabled": true,
  "ownerId": 1,
  "notifyEnabled": true,
  "notifyChannels": ["FEISHU", "IN_APP"],
  "notifyUserIds": [1, 2],
  "items": {
    "byIdRules": [],
    "byTimeRules": [
      {
        "sourceTable": "order",
        "targetTable": "order_history",
        "timeField": "created_at",
        "keepDays": 90
      }
    ]
  }
}
```

### 4.7 更新分组

```
PUT /api/v1/archive/groups/{id}
```

**鉴权：** 需要

### 4.8 修改分组状态

```
PATCH /api/v1/archive/groups/{id}/status
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| enableStatus | Integer | 是 | 0=禁用, 1=启用 |

**鉴权：** 需要

### 4.9 删除分组

```
DELETE /api/v1/archive/groups/{id}
```

**鉴权：** 需要

### 4.10 变更负责人

```
PUT /api/v1/archive/groups/{id}/owner
```

**鉴权：** 需要

**请求体：**

```json
{
  "newOwnerUserId": 3
}
```

### 4.11 触发归档

```
POST /api/v1/archive/groups/{id}/trigger
```

**鉴权：** 需要

### 4.12 取消运行任务

```
POST /api/v1/archive/groups/{id}/cancel-active-task
```

**鉴权：** 需要

**请求体（可选）：**

```json
{
  "cancelReason": "手动取消"
}
```

---

## 5. 归档规则接口

### 5.1 按 ID 规则

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/archive-group-items/by-id` | 查询规则列表 | ✅ |
| POST | `/api/v1/archive-group-items/by-id` | 新增规则 | ✅ |
| PUT | `/api/v1/archive-group-items/by-id/{id}` | 更新规则 | ✅ |
| DELETE | `/api/v1/archive-group-items/by-id/{id}` | 删除规则 | ✅ |

**按 ID 规则请求体：**

```json
{
  "groupId": 1,
  "sourceTable": "order",
  "targetTable": "order_history",
  "startId": 1,
  "endId": 1000000,
  "stepRds": 10000
}
```

### 5.2 按时间规则

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/archive-group-items/by-time` | 查询规则列表 | ✅ |
| POST | `/api/v1/archive-group-items/by-time` | 新增规则 | ✅ |
| PUT | `/api/v1/archive-group-items/by-time/{id}` | 更新规则 | ✅ |
| DELETE | `/api/v1/archive-group-items/by-time/{id}` | 删除规则 | ✅ |

**按时间规则请求体：**

```json
{
  "groupId": 1,
  "sourceTable": "order_log",
  "targetTable": "order_log_history",
  "timeField": "created_at",
  "startTime": "2023-01-01 00:00:00",
  "keepDays": 90,
  "stepMinutes": 60
}
```

---

## 6. 任务管理接口

### 6.1 查询任务列表

```
GET /api/v1/archive-tasks
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页大小，默认 10 |
| status | Integer | 否 | 按任务状态过滤 |
| groupId | Long | 否 | 按分组 ID 过滤 |

**鉴权：** 需要

### 6.2 触发归档任务

```
POST /api/v1/archive-tasks
```

**鉴权：** 需要

**请求体：**

```json
{
  "groupId": 1
}
```

### 6.3 获取任务详情

```
GET /api/v1/archive-tasks/{taskId}
```

**鉴权：** 需要

### 6.4 获取任务日志

```
GET /api/v1/archive-tasks/{taskId}/logs
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页大小，默认 20 |

**鉴权：** 需要

### 6.5 取消任务

```
POST /api/v1/archive-tasks/{taskId}/cancel
```

**鉴权：** 需要

---

## 7. 监控大盘接口

### 7.1 获取全局概览

```
GET /api/v1/dashboard/overview
```

**鉴权：** 需要

**响应：**

```json
{
  "code": "SUCCESS",
  "data": {
    "taskStatusCounts": [
      { "status": 1, "count": 2 },
      { "status": 2, "count": 15 },
      { "status": 3, "count": 1 }
    ],
    "datasourceStatusSummary": {
      "enabled": 3,
      "disabled": 1,
      "total": 4
    },
    "dailyTaskTrend": [
      { "day": "2024-01-13", "submittedCount": 3, "successCount": 2, "failedCount": 1 },
      { "day": "2024-01-14", "submittedCount": 5, "successCount": 5, "failedCount": 0 },
      { "day": "2024-01-15", "submittedCount": 2, "successCount": 2, "failedCount": 0 }
    ],
    "recentTasks": [ ... ],
    "failedTasks": [ ... ]
  }
}
```

---

## 8. 用户管理接口

### 8.1 用户管理

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/users` | 查询用户列表 | ✅ |
| POST | `/api/v1/users` | 创建用户 | ✅ |
| PUT | `/api/v1/users/{id}` | 更新用户 | ✅ |
| DELETE | `/api/v1/users/{id}` | 删除用户 | ✅ |

**创建用户请求体：**

```json
{
  "username": "john",
  "password": "password123",
  "role": "archive_admin",
  "enabled": true
}
```

### 8.2 数据源权限管理

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/datasource-permissions` | 查询权限列表 | ✅ |
| POST | `/api/v1/datasource-permissions` | 设置权限 | ✅ |

**设置权限请求体：**

```json
{
  "userId": 2,
  "datasourceId": 1,
  "permission": "MANAGE"
}
```

---

## 9. 通知中心接口

### 9.1 站内消息

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/v1/notifications` | 获取消息列表 | ✅ |
| GET | `/api/v1/notifications/unread-count` | 获取未读数 | ✅ |

---

## 10. 操作日志接口

### 10.1 查询操作日志

```
GET /api/v1/operation-logs
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页大小，默认 10 |
| module | String | 否 | 按模块过滤 |
| action | String | 否 | 按操作类型过滤 |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |

**鉴权：** 需要

---

## 11. 错误码参考

| 错误码 | HTTP 状态码 | 说明 |
|--------|------------|------|
| INVALID_PARAM | 400 | 请求参数无效 |
| RESOURCE_NOT_FOUND | 404 | 资源不存在 |
| PERMISSION_DENIED | 403 | 权限不足 |
| AUTH_EXPIRED | 401 | Token 已过期 |
| UNAUTHORIZED | 401 | 未认证 |
| SYSTEM_ERROR | 500 | 系统内部错误 |
| BUSY | 503 | 系统忙，请稍后重试 |

---

## 12. 客户端 SDK 使用示例

### 12.1 JavaScript/TypeScript

```typescript
const API_BASE = 'http://localhost:8789/api/v1';

// 登录
async function login(username: string, password: string) {
  const resp = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const { data } = await resp.json();
  return data.token;
}

// 触发归档任务
async function triggerArchive(groupId: number, token: string) {
  const resp = await fetch(`${API_BASE}/archive/groups/${groupId}/trigger`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  return await resp.json();
}
```

### 12.2 Python

```python
import requests

API_BASE = 'http://localhost:8789/api/v1'

# 登录
resp = requests.post(f'{API_BASE}/auth/login', json={
    'username': 'admin',
    'password': 'admin'
})
token = resp.json()['data']['token']

headers = {'Authorization': f'Bearer {token}'}

# 查询任务列表
resp = requests.get(f'{API_BASE}/archive-tasks', headers=headers)
tasks = resp.json()['data']['items']
```

### 12.3 cURL

```bash
# 登录
curl -X POST http://localhost:8789/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# 获取任务列表
curl http://localhost:8789/api/v1/archive-tasks \
  -H "Authorization: Bearer YOUR_TOKEN"
```
