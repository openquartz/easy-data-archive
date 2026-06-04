export const messages = {
  "en-US": {
    language: {
      zhCN: "中文",
      enUS: "English",
      switch: "Language"
    },
    layout: {
      brand: "EasyArchive",
      nav: {
        dashboard: "Dashboard",
        datasources: "Archive Connections",
        archiveGroups: "Archive Groups",
        tasks: "Archive Tasks",
        guide: "Guide",
        operationLogs: "Operation Logs",
        users: "Users"
      },
      topbar: "Operations Console",
      actions: {
        logout: "Sign out",
        loggingOut: "Signing out..."
      }
    },
    common: {
      refresh: "Refresh",
      create: "Create",
      edit: "Edit",
      save: "Save",
      saving: "Saving...",
      cancel: "Cancel",
      back: "Back",
      detail: "Detail",
      enable: "Enable",
      disable: "Disable",
      test: "Test",
      testing: "Testing...",
      delete: "Delete",
      actions: "Actions",
      status: "Status",
      unknown: "Unknown",
      yes: "Yes",
      no: "No",
      noData: "No data.",
      total: "Total",
      page: "Page",
      prev: "Prev",
      next: "Next",
      loading: "Loading..."
    },
    login: {
      title: "EasyArchive Login",
      username: "Username",
      password: "Password",
      submit: "Sign in",
      submitting: "Signing in...",
      validation: {
        usernameRequired: "Username is required",
        passwordRequired: "Password is required"
      },
      failed: "Login failed"
    },
    dashboard: {
      title: "Dashboard",
      loading: "Loading dashboard...",
      empty: "No dashboard data.",
      cards: {
        running: "Tasks Running",
        succeeded: "Tasks Succeeded",
        failed: "Tasks Failed",
        datasourceEnabled: "Active Archive Connections",
        datasourceDisabled: "Disabled Archive Connections",
        datasourceTotal: "Archive Connections Total"
      },
      recentTasks: "Recent Tasks",
      noRecentTasks: "No recent tasks.",
      failedTasks: "Failed Tasks",
      noFailedTasks: "No failed tasks.",
      loadFailed: "Failed to load dashboard",
      columns: {
        id: "ID",
        groupId: "Group ID",
        status: "Status",
        processed: "Processed",
        speed: "Speed",
        startTime: "Start Time",
        endTime: "End Time",
        error: "Error"
      }
    },
    datasource: {
      title: "Archive Connection Management",
      new: "New Archive Connection",
      emptyLoading: "Loading archive connections...",
      empty: "No archive connection records.",
      created: "Archive connection created.",
      updated: "Archive connection updated.",
      statusUpdated: "Archive connection status updated.",
      saveFailed: "Save failed",
      loadFailed: "Failed to load archive connections",
      statusUpdateFailed: "Status update failed",
      connectionTestFailed: "Connection test failed",
      connectionTip: "For security, edit the archive connection and provide password before testing connection.",
      form: {
        createTitle: "Create Archive Connection",
        editTitle: "Edit Archive Connection",
        code: "Code",
        name: "Name",
        type: "Type",
        jdbcUrl: "Connection Address",
        username: "Username",
        password: "Password",
        status: "Status",
        remark: "Remark",
        keepPassword: "Leave blank to keep unchanged",
        validation: {
          codeRequired: "Archive connection code is required",
          codeInvalid: "Archive connection code must be 2-64 chars, start with a letter, and use letters, numbers, _ or -",
          nameRequired: "Archive connection name is required",
          typeRequired: "Archive connection type is required",
          jdbcRequired: "Connection address is required",
          jdbcInvalid: "Connection address must start with jdbc:",
          usernameRequired: "Username is required"
        }
      },
      columns: {
        code: "Code",
        name: "Name",
        type: "Type",
        jdbcUrl: "Connection Address",
        username: "Username",
        status: "Status",
        actions: "Actions"
      }
    },
    archiveGroup: {
      title: "Archive Group Management",
      new: "New Group",
      emptyLoading: "Loading archive groups...",
      empty: "No archive group records.",
      created: "Archive group created.",
      updated: "Archive group updated.",
      deleted: "Archive group deleted.",
      statusUpdated: "Archive group status updated.",
      triggered: "Archive task #{id} submitted.",
      cancelSubmitted: "Cancel request submitted for task #{id}.",
      saveFailed: "Save failed",
      loadFailed: "Failed to load archive groups",
      actionFailed: "Action failed",
      deleteConfirm: "Delete this archive group?",
      items: "Items",
      trigger: "Trigger",
      viewTask: "View Archive Task",
      columns: {
        code: "Code",
        name: "Name",
        source: "Source",
        target: "Target",
        status: "Status",
        activeTask: "Active Task",
        activeTaskStartTime: "Task Start Time",
        actions: "Actions"
      },
      form: {
        createTitle: "Create Archive Group",
        editTitle: "Edit Archive Group",
        code: "Code",
        name: "Name",
        sourceDatasource: "Source Archive Connection",
        targetDatasource: "Target Archive Connection",
        selectDatasource: "Select archive connection",
        status: "Status",
        notifyEnabled: "Notify On Completion",
        notifyChannel: "Notification Channel",
        selectNotifyChannel: "Select notification channel",
        notifyWebhookUrl: "Notification Webhook URL",
        notifyChannels: {
          feishu: "Feishu",
          wecom: "WeCom"
        },
        triggerMode: "Trigger Mode",
        remark: "Remark",
        validation: {
          codeRequired: "Group code is required",
          codeInvalid: "Group code must be 2-64 chars, start with a letter, and use letters, numbers, _ or -",
          nameRequired: "Group name is required",
          sourceRequired: "Source archive connection is required",
          targetRequired: "Target archive connection is required",
          notifyChannelRequired: "Notification channel is required when completion notification is enabled",
          notifyWebhookRequired: "Notification webhook URL is required when completion notification is enabled"
        }
      },
      item: {
        title: "Archive Group Items",
        emptyLoading: "Loading archive items...",
        empty: "No archive item records.",
        loadFailed: "Failed to load archive items",
        saveFailed: "Save failed",
        saved: "Archive item saved.",
        deleted: "Archive item deleted.",
        statusUpdated: "Archive item status updated.",
        deleteConfirm: "Delete this archive item?",
        newId: "New ID Item",
        newTime: "New Time Item",
        idCreateTitle: "Create ID Archive Item",
        idEditTitle: "Edit ID Archive Item",
        timeCreateTitle: "Create Time Archive Item",
        timeEditTitle: "Edit Time Archive Item",
        type: "Type",
        sourceTable: "Source Table",
        targetTable: "Target Table",
        priority: "Priority",
        stepCount: "Step Count",
        status: "Status",
        idColumn: "ID Column",
        startId: "Start ID",
        endId: "End ID",
        stepRounds: "Step Rounds",
        pauseMs: "Pause Ms",
        enableWrite: "Write",
        enableClean: "Clean",
        fetchSql: "Fetch SQL",
        deleteWhere: "Delete Where",
        startTime: "Start Time",
        keepDay: "Keep Days",
        stepMinutes: "Step Minutes",
        validation: {
          tableRequired: "Source table, target table, and key fields are required",
          sqlRequired: "Fetch SQL and required range fields are required",
          positiveRequired: "Priority and step fields must be positive"
        }
      }
    },
    archiveGroupDetail: {
      title: "Archive Group Detail",
      summary: "Overview",
      recentTasks: "Recent Tasks",
      openDetail: "Open Detail",
      viewTask: "View Task",
      notFound: "Archive group not found.",
      emptyTasks: "No recent tasks."
    },
    user: {
      title: "User Management",
      new: "New User",
      emptyLoading: "Loading users...",
      empty: "No user records.",
      created: "User created.",
      updated: "User updated.",
      statusUpdated: "User status updated.",
      saveFailed: "Save failed",
      loadFailed: "Failed to load users",
      statusUpdateFailed: "Status update failed",
      noAccess: "Only administrators can manage users and archive connection permissions.",
      roles: {
        ADMIN: "Administrator",
        USER: "User"
      },
      permissions: {
        action: "Archive Connections",
        title: "Archive Connection Permissions · {username}",
        search: "Search archive connections",
        empty: "No archive connection records.",
        loadFailed: "Failed to load archive connection permissions",
        updated: "Archive connection permissions updated.",
        updateFailed: "Failed to update archive connection permissions",
        adminReadonly: "Administrators can view all data and do not require additional archive connection permissions."
      },
      form: {
        createTitle: "Create User",
        editTitle: "Edit User",
        username: "Username",
        password: "Password",
        realName: "Real Name",
        mobile: "Mobile",
        email: "Email",
        roleCode: "Role",
        status: "Status",
        remark: "Remark",
        keepPassword: "Leave blank to keep unchanged",
        validation: {
          usernameRequired: "Username is required",
          usernameInvalid: "Username must be 3-32 chars, start with a letter, and use letters, numbers, _, ., or -",
          passwordRequired: "Password is required",
          emailInvalid: "Email format is invalid",
          mobileInvalid: "Mobile format is invalid"
        }
      },
      columns: {
        username: "Username",
        realName: "Real Name",
        email: "Email",
        mobile: "Mobile",
        roleCode: "Role",
        status: "Status",
        lastLogin: "Last Login",
        actions: "Actions"
      }
    },
    operationLog: {
      title: "Operation Logs",
      emptyLoading: "Loading operation logs...",
      empty: "No operation logs.",
      loadFailed: "Failed to load operation logs",
      noAccess: "Only administrators can access operation logs.",
      actions: {
        search: "Search",
        reset: "Reset"
      },
      pager: "Page {page} / {totalPages} · Total {total}",
      result: {
        all: "All",
        success: "Success",
        failed: "Failed"
      },
      filters: {
        startTime: "Start Time",
        endTime: "End Time",
        operator: "Operator",
        module: "Module",
        result: "Result",
        operatorPlaceholder: "Enter operator",
        modulePlaceholder: "Enter module"
      },
      columns: {
        time: "Time",
        operator: "Operator",
        module: "Module",
        button: "Button",
        result: "Result",
        content: "Operation Content",
        failureReason: "Failure Reason"
      }
    },
    task: {
      title: "Archive Tasks",
      emptyLoading: "Loading archive tasks...",
      empty: "No archive task records.",
      loadFailed: "Failed to load archive tasks",
      detailTitle: "Archive Task Detail",
      detailLoading: "Loading archive task detail...",
      detailEmpty: "Archive task not found.",
      detailLoadFailed: "Failed to load archive task detail",
      logsLoadFailed: "Failed to load archive task logs",
      invalidId: "Invalid archive task ID.",
      cancelAction: "Cancel Archive Task",
      cancelling: "Cancelling...",
      cancelConfirm: "Cancel archive task #{id}?",
      cancelSubmitted: "Cancel request submitted.",
      cancelFailed: "Archive task cancel failed",
      logs: "Archive Task Logs",
      logEmptyLoading: "Loading archive task logs...",
      logEmpty: "No archive task logs.",
      pager: "Page {page} / {totalPages} · Total {total}",
      filters: {
        all: "All"
      },
      columns: {
        id: "ID",
        groupId: "Group ID",
        status: "Status",
        processed: "Processed",
        speed: "Speed",
        startTime: "Start Time",
        endTime: "End Time",
        actions: "Actions",
        heartbeat: "Heartbeat",
        start: "Start",
        end: "End",
        error: "Error",
        time: "Time",
        level: "Level",
        type: "Type",
        phase: "Phase",
        content: "Content"
      },
      status: {
        waiting: "Waiting",
        running: "Running",
        success: "Success",
        failed: "Failed",
        cancelling: "Cancelling",
        cancelled: "Cancelled"
      }
    },
    status: {
      enabled: "Enabled",
      disabled: "Disabled"
    },
    workTab: {
      groupDetail: "Group Detail",
      task: "Task"
    }
  },
  "zh-CN": {
    language: {
      zhCN: "中文",
      enUS: "English",
      switch: "语言"
    },
    layout: {
      brand: "EasyArchive",
      nav: {
        dashboard: "仪表盘",
        datasources: "归档连接",
        archiveGroups: "归档分组",
        tasks: "归档任务",
        guide: "操作指南",
        operationLogs: "操作日志",
        users: "用户"
      },
      topbar: "运维控制台",
      actions: {
        logout: "退出登录",
        loggingOut: "退出中..."
      }
    },
    common: {
      refresh: "刷新",
      create: "新建",
      edit: "编辑",
      save: "保存",
      saving: "保存中...",
      cancel: "取消",
      back: "返回",
      detail: "详情",
      enable: "启用",
      disable: "停用",
      test: "测试",
      testing: "测试中...",
      delete: "删除",
      actions: "操作",
      status: "状态",
      unknown: "未知",
      yes: "是",
      no: "否",
      noData: "暂无数据",
      total: "总计",
      page: "第",
      prev: "上一页",
      next: "下一页",
      loading: "加载中..."
    },
    login: {
      title: "EasyArchive 登录",
      username: "用户名",
      password: "密码",
      submit: "登录",
      submitting: "登录中...",
      validation: {
        usernameRequired: "请输入用户名",
        passwordRequired: "请输入密码"
      },
      failed: "登录失败"
    },
    dashboard: {
      title: "仪表盘",
      loading: "正在加载仪表盘...",
      empty: "暂无仪表盘数据。",
      cards: {
        running: "运行中任务",
        succeeded: "成功任务",
        failed: "失败任务",
        datasourceEnabled: "已启用归档连接",
        datasourceDisabled: "已停用归档连接",
        datasourceTotal: "归档连接总数"
      },
      recentTasks: "最近任务",
      noRecentTasks: "暂无最近任务。",
      failedTasks: "失败任务",
      noFailedTasks: "暂无失败任务。",
      loadFailed: "加载仪表盘失败",
      columns: {
        id: "ID",
        groupId: "分组 ID",
        status: "状态",
        processed: "已处理",
        speed: "速度",
        startTime: "开始时间",
        endTime: "结束时间",
        error: "错误"
      }
    },
    datasource: {
      title: "归档连接管理",
      new: "新建归档连接",
      emptyLoading: "正在加载归档连接...",
      empty: "暂无归档连接记录。",
      created: "归档连接已创建。",
      updated: "归档连接已更新。",
      statusUpdated: "归档连接状态已更新。",
      saveFailed: "保存失败",
      loadFailed: "加载归档连接失败",
      statusUpdateFailed: "更新状态失败",
      connectionTestFailed: "连接测试失败",
      connectionTip: "出于安全考虑，请先编辑归档连接并填写密码后再测试连接。",
      form: {
        createTitle: "新建归档连接",
        editTitle: "编辑归档连接",
        code: "编码",
        name: "名称",
        type: "类型",
        jdbcUrl: "连接地址",
        username: "用户名",
        password: "密码",
        status: "状态",
        remark: "备注",
        keepPassword: "留空则保持不变",
        validation: {
          codeRequired: "请输入归档连接编码",
          codeInvalid: "归档连接编码长度需为 2-64 位，必须以字母开头，并且只能包含字母、数字、_ 或 -",
          nameRequired: "请输入归档连接名称",
          typeRequired: "请输入归档连接类型",
          jdbcRequired: "请输入连接地址",
          jdbcInvalid: "连接地址必须以 jdbc: 开头",
          usernameRequired: "请输入用户名"
        }
      },
      columns: {
        code: "编码",
        name: "名称",
        type: "类型",
        jdbcUrl: "连接地址",
        username: "用户名",
        status: "状态",
        actions: "操作"
      }
    },
    archiveGroup: {
      title: "归档分组管理",
      new: "新建分组",
      emptyLoading: "正在加载归档分组...",
      empty: "暂无归档分组记录。",
      created: "归档分组已创建。",
      updated: "归档分组已更新。",
      deleted: "归档分组已删除。",
      statusUpdated: "归档分组状态已更新。",
      triggered: "归档任务 #{id} 已提交。",
      cancelSubmitted: "已提交任务 #{id} 的取消请求。",
      saveFailed: "保存失败",
      loadFailed: "加载归档分组失败",
      actionFailed: "操作失败",
      deleteConfirm: "确认删除该归档分组吗？",
      items: "明细",
      trigger: "触发",
      viewTask: "查看归档任务",
      columns: {
        code: "编码",
        name: "名称",
        source: "源归档连接",
        target: "目标归档连接",
        status: "状态",
        activeTask: "当前任务",
        activeTaskStartTime: "任务开始时间",
        actions: "操作"
      },
      form: {
        createTitle: "新建归档分组",
        editTitle: "编辑归档分组",
        code: "编码",
        name: "名称",
        sourceDatasource: "源归档连接",
        targetDatasource: "目标归档连接",
        selectDatasource: "选择归档连接",
        status: "状态",
        notifyEnabled: "执行完成通知",
        notifyChannel: "通知方式",
        selectNotifyChannel: "请选择通知方式",
        notifyWebhookUrl: "通知地址",
        notifyChannels: {
          feishu: "飞书",
          wecom: "企业微信"
        },
        triggerMode: "触发模式",
        remark: "备注",
        validation: {
          codeRequired: "请输入分组编码",
          codeInvalid: "分组编码长度需为 2-64 位，必须以字母开头，并且只能包含字母、数字、_ 或 -",
          nameRequired: "请输入分组名称",
          sourceRequired: "请选择源归档连接",
          targetRequired: "请选择目标归档连接",
          notifyChannelRequired: "启用执行完成通知时必须选择通知方式",
          notifyWebhookRequired: "启用执行完成通知时必须填写通知地址"
        }
      },
      item: {
        title: "归档分组明细",
        emptyLoading: "正在加载归档明细...",
        empty: "暂无归档明细记录。",
        loadFailed: "加载归档明细失败",
        saveFailed: "保存失败",
        saved: "归档明细已保存。",
        deleted: "归档明细已删除。",
        statusUpdated: "归档明细状态已更新。",
        deleteConfirm: "确认删除该归档明细吗？",
        newId: "新建 ID 明细",
        newTime: "新建时间明细",
        idCreateTitle: "新建 ID 归档明细",
        idEditTitle: "编辑 ID 归档明细",
        timeCreateTitle: "新建时间归档明细",
        timeEditTitle: "编辑时间归档明细",
        type: "类型",
        sourceTable: "源表",
        targetTable: "目标表",
        priority: "优先级",
        stepCount: "批大小",
        status: "状态",
        idColumn: "ID 字段",
        startId: "起始 ID",
        endId: "结束 ID",
        stepRounds: "滚动轮数",
        pauseMs: "停顿毫秒",
        enableWrite: "写入",
        enableClean: "清理",
        fetchSql: "抓取 SQL",
        deleteWhere: "删除条件",
        startTime: "开始时间",
        keepDay: "保留天数",
        stepMinutes: "滚动分钟",
        validation: {
          tableRequired: "源表、目标表和关键字段必填",
          sqlRequired: "抓取 SQL 和范围字段必填",
          positiveRequired: "优先级和步进字段必须为正数"
        }
      }
    },
    archiveGroupDetail: {
      title: "归档分组详情",
      summary: "概览",
      recentTasks: "最近任务",
      openDetail: "打开详情",
      viewTask: "查看任务",
      notFound: "未找到归档分组。",
      emptyTasks: "暂无最近任务。"
    },
    user: {
      title: "用户管理",
      new: "新建用户",
      emptyLoading: "正在加载用户...",
      empty: "暂无用户记录。",
      created: "用户已创建。",
      updated: "用户已更新。",
      statusUpdated: "用户状态已更新。",
      saveFailed: "保存失败",
      loadFailed: "加载用户失败",
      statusUpdateFailed: "更新状态失败",
      noAccess: "仅系统管理员支持管理用户和归档连接权限。",
      roles: {
        ADMIN: "系统管理员",
        USER: "普通用户"
      },
      permissions: {
        action: "归档连接权限",
        title: "归档连接权限 · {username}",
        search: "搜索归档连接",
        empty: "暂无归档连接记录。",
        loadFailed: "加载归档连接权限失败",
        updated: "归档连接权限已更新。",
        updateFailed: "更新归档连接权限失败",
        adminReadonly: "系统管理员默认可查看全部数据，无需额外分配归档连接权限。"
      },
      form: {
        createTitle: "新建用户",
        editTitle: "编辑用户",
        username: "用户名",
        password: "密码",
        realName: "姓名",
        mobile: "手机号",
        email: "邮箱",
        roleCode: "角色",
        status: "状态",
        remark: "备注",
        keepPassword: "留空则保持不变",
        validation: {
          usernameRequired: "请输入用户名",
          usernameInvalid: "用户名长度需为 3-32 位，必须以字母开头，并且只能包含字母、数字、_、. 或 -",
          passwordRequired: "请输入密码",
          emailInvalid: "邮箱格式不正确",
          mobileInvalid: "手机号格式不正确"
        }
      },
      columns: {
        username: "用户名",
        realName: "姓名",
        email: "邮箱",
        mobile: "手机号",
        roleCode: "角色",
        status: "状态",
        lastLogin: "最近登录",
        actions: "操作"
      }
    },
    operationLog: {
      title: "操作日志",
      emptyLoading: "正在加载操作日志...",
      empty: "暂无操作日志。",
      loadFailed: "加载操作日志失败",
      noAccess: "仅系统管理员可访问操作日志。",
      actions: {
        search: "查询",
        reset: "重置"
      },
      pager: "第 {page} / {totalPages} 页 · 共 {total} 条",
      result: {
        all: "全部",
        success: "成功",
        failed: "失败"
      },
      filters: {
        startTime: "开始时间",
        endTime: "结束时间",
        operator: "操作人",
        module: "模块",
        result: "结果",
        operatorPlaceholder: "请输入操作人",
        modulePlaceholder: "请输入模块"
      },
      columns: {
        time: "时间",
        operator: "操作人",
        module: "模块",
        button: "按钮",
        result: "结果",
        content: "操作内容",
        failureReason: "失败原因"
      }
    },
    task: {
      title: "归档任务",
      emptyLoading: "正在加载归档任务...",
      empty: "暂无归档任务记录。",
      loadFailed: "加载归档任务失败",
      detailTitle: "归档任务详情",
      detailLoading: "正在加载归档任务详情...",
      detailEmpty: "未找到归档任务。",
      detailLoadFailed: "加载归档任务详情失败",
      logsLoadFailed: "加载归档任务日志失败",
      invalidId: "归档任务 ID 无效。",
      cancelAction: "取消归档任务",
      cancelling: "取消中...",
      cancelConfirm: "确认取消归档任务 #{id} 吗？",
      cancelSubmitted: "已提交取消请求。",
      cancelFailed: "取消归档任务失败",
      logs: "归档任务日志",
      logEmptyLoading: "正在加载归档任务日志...",
      logEmpty: "暂无归档任务日志。",
      pager: "第 {page} / {totalPages} 页 · 共 {total} 条",
      filters: {
        all: "全部"
      },
      columns: {
        id: "ID",
        groupId: "分组 ID",
        status: "状态",
        processed: "已处理",
        speed: "速度",
        startTime: "开始时间",
        endTime: "结束时间",
        actions: "操作",
        heartbeat: "心跳时间",
        start: "开始",
        end: "结束",
        error: "错误",
        time: "时间",
        level: "级别",
        type: "类型",
        phase: "阶段",
        content: "内容"
      },
      status: {
        waiting: "等待中",
        running: "运行中",
        success: "成功",
        failed: "失败",
        cancelling: "取消中",
        cancelled: "已取消"
      }
    },
    status: {
      enabled: "已启用",
      disabled: "已停用"
    },
    workTab: {
      groupDetail: "分组详情",
      task: "任务"
    }
  }
} as const;

export type Locale = keyof typeof messages;
