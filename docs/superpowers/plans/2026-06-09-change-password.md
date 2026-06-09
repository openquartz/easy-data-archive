# 修改密码功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在用户登录后右上角区域新增下拉菜单（修改密码 + 退出登录），完整实现密码修改前后端链路

**Architecture:** 后端在 `AuthController` 新增 `POST /api/v1/auth/change-password` 接口，从 SecurityContext 获取当前用户，校验新密码格式后编码存储并强制登出；前端新建 `ChangePasswordDialog.vue` 组件，AppLayout 右上角改为下拉菜单触发

**Tech Stack:** Java 11 + Spring Boot + Vue 3 + TypeScript + Pinia + Element Plus（项目未引入，使用自定义 modal 模式）

---

## 文件结构总览

### 后端（Java）

| 文件 | 操作 |
|------|------|
| `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ChangePasswordRequest.java` | **新建** |
| `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/AuthService.java` | 修改：新增 `changePassword` 方法 |
| `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/AuthServiceImpl.java` | 修改：实现 `changePassword` |
| `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/AuthController.java` | 修改：新增 `POST /change-password` 端点 |

### 前端（Vue/TS）

| 文件 | 操作 |
|------|------|
| `easyarchive-ui/src/api/auth.ts` | 修改：新增 `changePasswordApi` |
| `easyarchive-ui/src/components/ChangePasswordDialog.vue` | **新建** |
| `easyarchive-ui/src/layouts/AppLayout.vue` | 修改：右上角改为下拉菜单 |
| `easyarchive-ui/src/i18n/messages.ts` | 修改：新增 i18n key |

---

### Task 1: 后端 DTO — ChangePasswordRequest

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ChangePasswordRequest.java`

- [ ] **Step 1: 创建 ChangePasswordRequest DTO**

```java
package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改密码请求DTO
 */
@Data
public class ChangePasswordRequest implements Serializable {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码长度不能少于8位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
             message = "密码必须包含字母和数字")
    private String newPassword;
}
```

- [ ] **Step 2: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ChangePasswordRequest.java
git commit -m "feat: add ChangePasswordRequest DTO"
```

---

### Task 2: 后端 AuthService 接口扩展

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/AuthService.java`

- [ ] **Step 1: 在 AuthService 接口中新增 changePassword 方法**

在接口中追加：

```java
void changePassword(String newPassword);
```

- [ ] **Step 2: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/AuthService.java
git commit -m "feat: add changePassword to AuthService interface"
```

---

### Task 3: 后端 AuthServiceImpl 实现

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/AuthServiceImpl.java`

- [ ] **Step 1: 实现 changePassword 方法**

在 `AuthServiceImpl` 类体末尾（`}` 之前）追加：

```java
    @Override
    public void changePassword(String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BadCredentialsException("未登录");
        }

        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            username = ((User) principal).getUsername();
        } else {
            username = String.valueOf(principal);
        }

        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null || user.getDeleted() != null && user.getDeleted() != 0) {
            throw new BadCredentialsException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.update(user);

        // 修改成功后强制登出
        logout();
    }
```

同时需要在 import 区域确认已有以下 import（如果没有则补充）：
- `org.springframework.security.authentication.BadCredentialsException`（已有）
- `org.springframework.security.core.Authentication`（已有）
- `org.springframework.security.core.context.SecurityContextHolder`（已有）
- `org.springframework.security.core.userdetails.User`（已有）

- [ ] **Step 2: 编译验证**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master && mvn compile -pl easyarchive-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/AuthServiceImpl.java
git commit -m "feat: implement changePassword in AuthServiceImpl"
```

---

### Task 4: 后端 AuthController 新增端点

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/AuthController.java`

- [ ] **Step 1: 新增 changePassword 接口**

在 `AuthController` 类中（`logout` 方法之后、类体 `}` 之前）追加：

```java
    @PostMapping("/change-password")
    public ApiResponse<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getNewPassword());
        return ApiResponse.success();
    }
```

同时在 import 区域追加：

```java
import com.openquartz.easyarchive.starter.model.dto.ChangePasswordRequest;
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl easyarchive-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/AuthController.java
git commit -m "feat: add POST /api/v1/auth/change-password endpoint"
```

---

### Task 5: 前端 API 层扩展

**Files:**
- Modify: `easyarchive-ui/src/api/auth.ts`

- [ ] **Step 1: 新增 changePasswordApi 函数**

在文件末尾追加：

```typescript
export function changePasswordApi(newPassword: string): Promise<void> {
  return http.post<void>("auth/change-password", { newPassword });
}
```

- [ ] **Step 2: 提交**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master && git add easyarchive-ui/src/api/auth.ts && git commit -m "feat: add changePasswordApi to auth API"
```

---

### Task 6: 前端 i18n 新增翻译 key

**Files:**
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: 在 `layout.actions` 中（`en-US` 和 `zh-CN` 两处）新增 key**

在 `layout.actions` 对象中，`logout` / `loggingOut` 之后追加：

```typescript
        changePassword: "Change Password",   // en-US
        changePassword: "修改密码",           // zh-CN
```

在 `en-US` 区块的 `layout.actions` 中：
```typescript
      actions: {
        logout: "Sign out",
        loggingOut: "Signing out...",
        changePassword: "Change Password"
      }
```

在 `zh-CN` 区块的 `layout.actions` 中：
```typescript
      actions: {
        logout: "退出登录",
        loggingOut: "退出中...",
        changePassword: "修改密码"
      }
```

同时在 `common` 区块（两处）末尾追加：

```typescript
      passwordChanged: "Password changed, please sign in again.",  // en-US
      passwordChanged: "密码修改成功，请重新登录。",                   // zh-CN
```

- [ ] **Step 2: 提交**

```bash
git add easyarchive-ui/src/i18n/messages.ts && git commit -m "feat: add changePassword i18n keys"
```

---

### Task 7: 前端 ChangePasswordDialog 组件

**Files:**
- Create: `easyarchive-ui/src/components/ChangePasswordDialog.vue`

- [ ] **Step 1: 创建 ChangePasswordDialog.vue（script 部分）**

```typescript
// easyarchive-ui/src/components/ChangePasswordDialog.vue

<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { useI18n } from "../i18n";
import { changePasswordApi } from "../api/auth";

const props = defineProps<{
  visible: boolean;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "password-changed"): void;
}>();

const { t } = useI18n();

const form = reactive({
  newPassword: "",
  confirmPassword: ""
});

const errorMessage = ref("");

const passwordPattern = /^(?=.*[a-zA-Z])(?=.*\d).{8,}$/;

watch(
  () => props.visible,
  (next) => {
    if (next) {
      form.newPassword = "";
      form.confirmPassword = "";
      errorMessage.value = "";
    }
  }
);

function validate(): boolean {
  if (!form.newPassword) {
    errorMessage.value = t("changePassword.validation.required");
    return false;
  }
  if (form.newPassword.length < 8) {
    errorMessage.value = t("changePassword.validation.minLength");
    return false;
  }
  if (!passwordPattern.test(form.newPassword)) {
    errorMessage.value = t("changePassword.validation.pattern");
    return false;
  }
  if (form.newPassword !== form.confirmPassword) {
    errorMessage.value = t("changePassword.validation.mismatch");
    return false;
  }
  return true;
}

async function handleSubmit(): Promise<void> {
  if (props.submitting) {
    return;
  }
  errorMessage.value = "";
  if (!validate()) {
    return;
  }
  try {
    await changePasswordApi(form.newPassword);
    emit("password-changed");
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : t("changePassword.failed");
    errorMessage.value = message;
  }
}
</script>
```

- [ ] **Step 2: 追加 template 部分**

```vue
<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <section class="modal-card">
      <header class="modal-card__header">
        <h3>{{ t("changePassword.title") }}</h3>
      </header>
      <form class="form-grid" @submit.prevent="handleSubmit">
        <label>
          {{ t("changePassword.newPassword") }}
          <input
            v-model="form.newPassword"
            type="password"
            :disabled="submitting"
            autocomplete="new-password"
          />
        </label>
        <label>
          {{ t("changePassword.confirmPassword") }}
          <input
            v-model="form.confirmPassword"
            type="password"
            :disabled="submitting"
            autocomplete="new-password"
          />
        </label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <footer class="modal-card__footer">
          <button type="button" class="btn btn--subtle" :disabled="submitting" @click="emit('close')">
            {{ t("common.cancel") }}
          </button>
          <button type="submit" class="btn btn--primary" :disabled="submitting">
            {{ submitting ? t("common.saving") : t("common.save") }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
```

- [ ] **Step 3: 追加 style 部分**

```vue
<style scoped>
.modal-card {
  width: min(480px, calc(100vw - 3rem));
}

.modal-card .form-grid {
  align-content: start;
}
</style>
```

**完整文件写入时注意：** `<script setup>` + `<template>` + `<style scoped>` 三段合一，写入同一文件。

- [ ] **Step 4: 提交**

```bash
git add easyarchive-ui/src/components/ChangePasswordDialog.vue && git commit -m "feat: add ChangePasswordDialog component"
```

---

### Task 8: 前端 i18n 补充 changePassword 专属 key

**Files:**
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: 在 `en-US` 和 `zh-CN` 顶层区域各新增 `changePassword` 命名空间**

在 `en-US` 区块（与 `layout`、`login` 等同级）追加：

```typescript
    changePassword: {
      title: "Change Password",
      newPassword: "New Password",
      confirmPassword: "Confirm Password",
      validation: {
        required: "Password is required",
        minLength: "Password must be at least 8 characters",
        pattern: "Password must contain both letters and numbers",
        mismatch: "Passwords do not match"
      },
      failed: "Failed to change password"
    },
```

在 `zh-CN` 区块追加：

```typescript
    changePassword: {
      title: "修改密码",
      newPassword: "新密码",
      confirmPassword: "确认密码",
      validation: {
        required: "请输入新密码",
        minLength: "密码长度不能少于8位",
        pattern: "密码必须包含字母和数字",
        mismatch: "两次输入的密码不一致"
      },
      failed: "修改密码失败"
    },
```

- [ ] **Step 2: 提交**

```bash
git add easyarchive-ui/src/i18n/messages.ts && git commit -m "feat: add changePassword i18n messages"
```

---

### Task 9: 前端 AppLayout 改造 — 右上角下拉菜单

**Files:**
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`

- [ ] **Step 1: 更新 script 部分**

在现有 `<script setup>` 中：

1. 新增 ref：
```typescript
const userMenuOpen = ref(false);
```

2. 新增关闭下拉方法：
```typescript
function closeUserMenu(): void {
  userMenuOpen.value = false;
}
```

3. 新增打开修改密码 dialog 的 ref 和方法：
```typescript
import ChangePasswordDialog from "../components/ChangePasswordDialog.vue";
import { showSuccessToast } from "../stores/toast";

const changePasswordVisible = ref(false);
const changePasswordSubmitting = ref(false);

function openChangePassword(): void {
  userMenuOpen.value = false;
  changePasswordVisible.value = true;
}

async function handlePasswordChanged(): Promise<void> {
  changePasswordVisible.value = false;
  showSuccessToast(t("common.passwordChanged"));
  // 清除 auth 并跳转登录
  await authStore.logout();
  await router.push({ name: "login" });
}
```

4. 在 `handleLogout` 开头关闭下拉：
```typescript
async function handleLogout(): Promise<void> {
  if (loggingOut.value) {
    return;
  }
  userMenuOpen.value = false;
  loggingOut.value = true;
  // ... 原有逻辑不变
}
```

5. 更新 import 区域，追加：
```typescript
import ChangePasswordDialog from "../components/ChangePasswordDialog.vue";
import { showSuccessToast } from "../stores/toast";
```

6. 在 return 对象中导出新变量：
```typescript
return {
  // ... 已有导出
  userMenuOpen,
  changePasswordVisible,
  changePasswordSubmitting,
  openChangePassword,
  handlePasswordChanged,
  // ... 已有导出
};
```

- [ ] **Step 2: 更新 template 部分**

将 topbar actions 区域（原 `<!-- 第116-123行附近 -->`）替换为：

```vue
        <div class="app-shell__topbar-actions">
          <div class="user-menu" :class="{ 'user-menu--open': userMenuOpen }">
            <button
              type="button"
              class="account-pill user-menu__trigger"
              @click="userMenuOpen = !userMenuOpen"
            >
              {{ accountLabel }} ▾
            </button>
            <div v-if="userMenuOpen" class="user-menu__dropdown" @click.stop>
              <button type="button" class="user-menu__item" @click="openChangePassword">
                {{ t("layout.actions.changePassword") }}
              </button>
              <button
                type="button"
                class="user-menu__item"
                :disabled="loggingOut"
                @click="handleLogout"
              >
                {{ loggingOut ? t("layout.actions.loggingOut") : t("layout.actions.logout") }}
              </button>
            </div>
          </div>
          <InAppNotificationBell />
          <LanguageSwitcher />
        </div>
```

在 `</template>` 结束标签之前（`</script>` 之前）追加 dialog：

```vue
  <ChangePasswordDialog
    :visible="changePasswordVisible"
    :submitting="changePasswordSubmitting"
    @close="changePasswordVisible = false"
    @password-changed="handlePasswordChanged"
  />
```

**注意：** 上面的 dialog 应放在 `</template>` 之后、`</script>` 之前，即 template 区域末尾。

- [ ] **Step 3: 追加 style 部分**

在 `</style>` 之前追加：

```css
.user-menu {
  position: relative;
  display: inline-block;
}

.user-menu__trigger {
  cursor: pointer;
  font-weight: 600;
}

.user-menu--open .user-menu__trigger {
  color: #164b5a;
}

.user-menu__dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 160px;
  background: #fff;
  border: 1px solid rgba(16, 57, 71, 0.12);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  padding: 6px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-menu__item {
  width: 100%;
  text-align: left;
  padding: 8px 12px;
  border: 0;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #1e293b;
  transition: background 120ms ease;
}

.user-menu__item:hover {
  background: rgba(16, 57, 71, 0.06);
}

.user-menu__item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

- [ ] **Step 4: 提交**

```bash
git add easyarchive-ui/src/layouts/AppLayout.vue && git commit -m "feat: add user dropdown menu with change password option"
```

---

### Task 10: 验证

- [ ] **Step 1: 后端编译验证**

```bash
mvn clean compile -pl easyarchive-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 启动后端，测试 change-password 接口**

```bash
mvn spring-boot:run -pl easyarchive-starter
```

使用 curl 或 Postman 测试：

```bash
# 1. 登录获取 token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. 使用 token 修改密码
curl -X POST http://localhost:8080/api/v1/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"newPassword":"newPass123"}'

# 期望：{"code":0,"data":null,...} 表示成功
# 再次用旧密码登录应失败，用新密码登录应成功
```

- [ ] **Step 3: 前端构建验证**

```bash
cd easyarchive-ui && npx vue-tsc --noEmit 2>&1 | head -20
```

Expected: 无 TS 编译错误（或仅有已知的无关错误）

- [ ] **Step 4: 最终提交确认**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master && git status
```

---

## 关键注意事项

1. **密码不落库明文**：`passwordEncoder.encode()` 使用 BCrypt，不可逆
2. **SecurityContext 获取当前用户**：无需前端传 userId，后端从 JWT 解析，防止越权
3. **修改后强制登出**：调用 `logout()` 清除 SecurityContext，前端接收到成功响应后清除 localStorage 并跳转登录页
4. **前端校验规则与后端一致**：8 位最小 + 必须含字母和数字，正则 `/^(?=.*[a-zA-Z])(?=.*\d).{8,}$/`
5. **项目未使用 Element Plus**：`ChangePasswordDialog` 使用项目统一的 `modal-backdrop` + `modal-card` CSS 模式（与 `UserFormDialog` 一致）
6. **下拉菜单点击外部区域需关闭**：通过 `@click.stop` 阻止冒泡，在 `handleLogout` 和 `openChangePassword` 中手动 `userMenuOpen.value = false`
