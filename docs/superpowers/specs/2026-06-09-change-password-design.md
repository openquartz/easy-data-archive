# 修改密码功能设计

## 1. 背景与目标

当前系统支持用户登录、登出，但未提供修改密码入口。用户右上角仅有登出按钮。

目标：在用户头像/用户名区域新增下拉菜单，包含"修改密码"和"退出登录"两个选项，完整覆盖密码修改链路。

## 2. 需求确认

| 维度 | 结论 |
|------|------|
| 旧密码校验 | 不校验，直接设置新密码 |
| 新密码规则 | 最少 8 位，必须包含字母和数字 |
| 成功后行为 | 自动退出登录，用户需重新用新密码登录 |
| 前端组件形式 | 独立 Dialog 组件 (`ChangePasswordDialog.vue`) |
| 后端接口位置 | `AuthController` / `AuthService`，路径 `POST /api/v1/auth/change-password` |

## 3. 后端设计

### 3.1 新增 DTO

**文件：** `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ChangePasswordRequest.java`

```java
@Data
public class ChangePasswordRequest {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码长度不能少于8位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
             message = "密码必须包含字母和数字")
    private String newPassword;
}
```

### 3.2 AuthService 接口

```java
void changePassword(String newPassword);
```

### 3.3 AuthServiceImpl 实现

1. 从 `SecurityContextHolder` 获取当前登录用户名
2. 通过 `SysUserMapper.selectByUsername(username)` 查询用户
3. 用 `passwordEncoder.encode(newPassword)` 编码新密码
4. 调用 `SysUserMapper.update(user)` 持久化（只更新 password 字段）
5. 调用 `logout()` 强制清空 SecurityContext，触发后续重新登录

### 3.4 AuthController 接口

```
POST /api/v1/auth/change-password
Body: { "newPassword": "xxx" }
Response: ApiResponse<?>
```

需要登录认证（已有 JWT 过滤器保护）。

### 3.5 错误处理

| 场景 | 错误码 | 提示信息 |
|------|--------|---------|
| 未登录 | `AUTH_INVALID` | 认证失败，请重新登录 |
| 新密码格式不符 | `VALIDATION_ERROR` | 密码必须包含字母和数字 |
| 数据库更新失败 | `INTERNAL_ERROR` | 修改密码失败 |

## 4. 前端设计

### 4.1 API 层

**文件：** `easyarchive-ui/src/api/auth.ts`

新增函数：

```typescript
export function changePasswordApi(newPassword: string): Promise<void> {
  return http.post<void>("/auth/change-password", { newPassword });
}
```

### 4.2 独立 Dialog 组件

**文件：** `easyarchive-ui/src/components/ChangePasswordDialog.vue`

**Props：**
- `visible: boolean` — 控制显示/隐藏

**Emits：**
- `update:visible` — 同步关闭事件

**表单字段：**
- 新密码（`newPassword`）：输入框，type=password
- 确认密码（`confirmPassword`）：输入框，type=password，用于二次确认

**表单校验规则（与后端一致）：**
- 非空
- 最少 8 位
- 必须包含字母和数字
- 确认密码与新密码一致

**交互流程：**
1. 点击"确认修改"按钮
2. 显示 loading 状态，按钮置灰
3. 调用 `changePasswordApi`
4. 成功：`ElMessage.success("密码修改成功，请重新登录")` → emit `update:visible=false` → 跳转到登录页
5. 失败：`ElMessage.error(err.message)` → 保持弹窗

**UI 风格：** 使用项目已有 `ElDialog` + `ElForm` + `ElInput` + `ElButton`，与 `UserFormDialog` 风格保持一致。

### 4.3 AppLayout 修改

**文件：** `easyarchive-ui/src/layouts/AppLayout.vue`

改动点：

1. 将右上角 `account-pill` + 独立 `logout` 按钮改为**下拉菜单**，用户名作为下拉触发器。
2. 菜单内两项：
   - **修改密码** — 点击打开 `ChangePasswordDialog`
   - **退出登录** — 逻辑复用原 `handleLogout()`
3. 引入 `ChangePasswordDialog` 组件，挂载初始隐藏状态。

下拉菜单使用 `<el-dropdown>` 或原生 `<details>/<summary>` 实现（项目 Vue 3 + Element Plus，已引入 `ElMessage`，优先用 Element Plus 组件）。

**参考布局示意：**

```
┌─ topbar actions ──────────────────────────────────┐
│  [通知铃]  [admin ▼]  [语言]                        │
│              ├─ 修改密码                           │
│              └─ 退出登录                            │
└────────────────────────────────────────────────────┘
```

## 5. 改动文件清单

### 后端（Java）

| 文件 | 操作 |
|------|------|
| `model/dto/ChangePasswordRequest.java` | 新增 |
| `service/AuthService.java` | 新增方法 `changePassword` |
| `service/impl/AuthServiceImpl.java` | 实现 `changePassword` |
| `controller/AuthController.java` | 新增 `POST /change-password` 接口 |

### 前端（Vue/TS）

| 文件 | 操作 |
|------|------|
| `api/auth.ts` | 新增 `changePasswordApi` |
| `components/ChangePasswordDialog.vue` | 新增 |
| `layouts/AppLayout.vue` | 改造 topbar 为下拉菜单 |

## 6. 测试要点

- 前端：Dialog 表单校验（空密码 / 不足 8 位 / 无字母或数字 / 两次不一致）
- 前端：修改成功后是否正确跳转登录页
- 后端：新密码是否正确编码存储（不可逆，明文不落库）
- 后端：未登录请求是否返回 401
