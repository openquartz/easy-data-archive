import type { Locale } from "../i18n/messages";

export type GuideSection = {
  id: string;
  title: string;
  intro?: string;
  items?: string[];
  notes?: string[];
};

const zhCNSections: GuideSection[] = [
  {
    id: "positioning",
    title: "产品定位",
    intro:
      "数据归档平台用于把历史数据从源库分批迁移到目标库，并在确认写入正确后按规则清理源表数据，适用于日志、流水、订单历史和归档分层场景。"
  },
  {
    id: "preparation",
    title: "接入前准备",
    items: [
      "确认源库、目标库与配置库网络连通，归档账号具备最小必要读写权限。",
      "提前核对源表与目标表的字段类型、字符集、索引和主键策略，避免写入阶段出现结构不兼容。",
      "梳理业务低峰窗口、可接受的单批耗时和回滚方案，再确定批大小、步进窗口和执行频率。",
      "首次上线建议只选择一张样本表做小范围演练，先开启写入，关闭清理。"
    ],
    notes: [
      "接入准备阶段最容易遗漏索引与数据量评估，建议先用数据库执行计划确认查询会命中预期索引。"
    ]
  },
  {
    id: "datasource-config",
    title: "归档连接配置",
    items: [
      "编码：归档连接唯一标识，建议按环境和用途命名，例如 `prod_order_src`、`archive_history_tgt`。",
      "名称：界面展示名，便于运维人员识别业务域和环境。",
      "连接地址：JDBC 地址，需明确库名、字符集和时区参数，避免时间类规则出现边界偏差。",
      "用户名 / 密码：执行归档时使用的数据库凭据，密码变更后需要重新编辑保存。",
      "状态：停用后不会被新的归档配置选中，也不应作为正式任务的执行连接。",
      "备注：记录用途、风险提示、负责人或变更说明。"
    ],
    notes: [
      "建议源库和目标库分别使用独立账号，并在数据库侧限制权限范围，避免误操作扩大影响面。"
    ]
  },
  {
    id: "group-config",
    title: "归档分组配置",
    items: [
      "归档分组用于组织需要一起执行的一批规则，通常按业务域、库表集合或同一执行窗口进行划分。",
      "源归档连接用于读取和清理，目标归档连接用于写入，二者必须与实际数据流向一致。",
      "启用状态决定分组是否允许参与执行；生产切换前建议先在停用状态下完成规则录入与复核。",
      "通知开关、通知渠道和 Webhook 用于在任务完成后同步执行结果，适合接入飞书或企业微信告警。"
    ],
    notes: [
      "如果多个表存在主从关系或外键依赖，建议把子表规则优先级设得更高，先归档子表后归档主表。"
    ]
  },
  {
    id: "rule-overview",
    title: "规则表达配置总览",
    intro:
      "规则配置不仅是字段填写问题，更关键的是让读取范围、排序方式、删除条件和数据库索引保持一致，保证每一批数据都可验证、可回放、可停止。",
    items: [
      "按 ID 规则适合主键递增、历史区间明确且能按 ID 窗口读取的数据表。",
      "按时间规则适合日志、流水、历史订单等按时间沉淀的数据表。",
      "批大小决定单批取数和写入规模；步进轮次、步进分钟和暂停毫秒共同决定执行节奏与源库压力。",
      "取数 SQL 应优先使用范围条件与稳定排序，避免全表扫描和同一批次重复读取。",
      "删除条件必须与取数范围描述同一批数据，不能比查询更宽，也不能漏掉已成功写入的记录。"
    ],
    notes: [
      "规则上线前先开启写入，关闭清理，待目标库对账完成后再启用清理，是最稳妥的首发方式。"
    ]
  },
  {
    id: "id-rule",
    title: "按 ID 规则配置",
    items: [
      "适用场景：主键或业务 ID 单调递增，且可以按历史区间逐段搬迁的业务表。",
      "ID 字段：应选择有索引、能稳定排序且与取数范围直接关联的列，通常为主键或唯一业务流水号。",
      "开始 ID / 结束 ID：定义归档边界，建议先选一段可人工复核的小范围，确认逻辑后再放大窗口。",
      "批大小：控制每批取数条数，过大可能拉高锁等待和写入耗时，过小则会增加调度轮次。",
      "步进轮次：一次任务内部连续推进的批次数，适合在固定窗口内分段搬迁历史区间。",
      "取数 SQL：建议包含 `ID > lowerBound`、`ID <= upperBound` 这一类可命中索引的条件，并配合稳定排序。",
      "删除条件：必须与成功写入的 ID 范围一一对应，避免出现取数按范围、删除按宽条件导致误删。"
    ],
    notes: [
      "常见风险包括：ID 不连续导致窗口过大、未显式排序导致批次重叠、结束 ID 估算过宽导致任务执行时间不可控。"
    ]
  },
  {
    id: "time-rule",
    title: "按时间规则配置",
    items: [
      "适用场景：日志、访问流水、通知记录、历史订单等天然按时间沉淀的数据表。",
      "开始时间：定义本次归档从哪个时间点开始扫描，建议与业务保留策略和库中实际历史范围一致。",
      "保留天数：用于确定归档截止线，例如保留近 30 天数据，则只处理 30 天之前的记录。",
      "步进分钟：定义单批时间窗口长度，窗口越大，单批 SQL 触达的数据越多，对热点分区压力越高。",
      "时间字段：优先选择有索引、值稳定且真正反映沉淀时间的列，例如创建时间或归档候选时间。",
      "取数 SQL：建议使用明确的起止时间边界，例如 `create_time >= windowStart and create_time < windowEnd`。",
      "删除条件：必须复用同一时间窗口表达，不要混入更宽的状态条件或模糊时间比较。"
    ],
    notes: [
      "需要特别关注时区一致性、延迟写入和跨天边界。若业务存在晚到数据，建议预留缓冲时间，不要处理过近的数据。"
    ]
  },
  {
    id: "query-cleanup",
    title: "查询与清理条件约束",
    items: [
      "查询条件优先走索引，至少保证范围字段和排序字段具备可用索引，避免长时间扫描在线热表。",
      "查询排序必须确定且稳定，否则同一批窗口在多轮执行中可能出现重复或漏读。",
      "删除条件必须描述和取数 SQL 相同的数据切片，建议先把删除条件单独改写成查询语句核对命中范围。",
      "如果启用了清理，必须以“写入成功且可核对”为前提；只要目标库校验没有通过，就不要开启清理。",
      "每次调整规则后都先用小窗口回归验证，再扩大批大小或时间跨度。"
    ],
    notes: [
      "高风险信号包括：删除条件缺少边界字段、查询条件包含函数导致索引失效、时间窗口使用闭区间造成交叉覆盖。"
    ]
  },
  {
    id: "examples-validation",
    title: "配置示例与验证流程",
    items: [
      "安全首发模式：开启写入，关闭清理，选择一段可人工核对的 ID 或时间窗口，执行后比对源库与目标库记录数和关键字段。",
      "按 ID 示例：订单历史表以 `order_id` 为边界，每批 2000 条，先归档 `1000001` 到 `1020000` 范围，确认目标表写入完整后再扩大范围。",
      "按时间示例：访问日志表以 `create_time` 为边界，保留近 30 天数据，步进 60 分钟，先验证某一天凌晨 2 小时窗口的归档结果。",
      "启用清理前检查项：目标表记录数一致、关键字段抽样一致、删除条件单独查询命中范围正确、回滚方案已准备、执行窗口已确认。"
    ],
    notes: [
      "首次运行和规则变更后都建议采用“先开启写入，关闭清理”的方式完成验收，再切换到正式清理模式。"
    ]
  },
  {
    id: "troubleshooting",
    title: "常见错误与排查",
    items: [
      "任务无数据：检查启用状态、起止范围、保留天数、时间字段以及取数 SQL 是否真的覆盖历史数据。",
      "写入成功但未清理：检查是否启用了清理开关，并确认删除条件与取数范围完全一致。",
      "执行缓慢：优先查看执行计划、批大小、步进窗口和目标库写入吞吐，确认是否出现全表扫描或热点索引争用。",
      "结果不一致：重点核对排序字段、删除条件边界、目标表唯一键冲突以及是否存在晚到数据。",
      "连接失败：检查 JDBC 地址、白名单、用户名密码、SSL 或时区参数是否与数据库环境一致。"
    ]
  }
];

const enUSSections: GuideSection[] = [
  {
    id: "positioning",
    title: "Platform Positioning",
    intro:
      "Data Archive Platform moves historical records from source databases into target storage in controlled batches, then optionally cleans source data after verification."
  },
  {
    id: "preparation",
    title: "Before You Start",
    items: [
      "Validate network access, credentials, schema compatibility, and index coverage before onboarding a table.",
      "Define an off-peak execution window, an acceptable batch size, and a rollback plan.",
      "Use a small write-only rehearsal before enabling cleanup."
    ]
  },
  {
    id: "datasource-config",
    title: "Archive Connection Configuration",
    items: [
      "Use clear connection codes, stable JDBC settings, least-privilege accounts, and operational remarks.",
      "Treat disabled connections as non-production options until credentials and connectivity are verified."
    ]
  },
  {
    id: "group-config",
    title: "Archive Group Configuration",
    items: [
      "Group related rules by domain or execution window and keep source and target connections aligned with real data flow.",
      "Use notification settings for completion alerts after rollout."
    ]
  },
  {
    id: "rule-overview",
    title: "Rule Configuration Overview",
    items: [
      "Choose ID-based rules for monotonic key ranges and time-based rules for retention-driven history tables.",
      "Keep fetch predicates, ordering, and cleanup predicates aligned to the same slice of data.",
      "Batch size and pacing settings control source pressure and runtime."
    ]
  },
  {
    id: "id-rule",
    title: "ID-Based Rules",
    items: [
      "Prefer indexed key columns, explicit range boundaries, stable ordering, and cleanup conditions that exactly match fetched rows."
    ]
  },
  {
    id: "time-rule",
    title: "Time-Based Rules",
    items: [
      "Use indexed time fields, bounded windows, retention cutoffs, and timezone-safe comparisons.",
      "Leave buffer time for delayed writes before archiving recent data."
    ]
  },
  {
    id: "query-cleanup",
    title: "Query And Cleanup Constraints",
    items: [
      "Queries should stay index-friendly and deterministic.",
      "Cleanup must stay disabled until writes are verified against the target."
    ]
  },
  {
    id: "examples-validation",
    title: "Examples And Validation",
    items: [
      "Start with write enabled and cleanup disabled.",
      "Validate counts, sample rows, and delete scopes before production cleanup."
    ]
  },
  {
    id: "troubleshooting",
    title: "Troubleshooting",
    items: [
      "Investigate empty results, slow execution, and mismatched counts by reviewing scope, indexes, and cleanup boundaries."
    ]
  }
];

export function buildGuideSections(locale: Locale): GuideSection[] {
  return locale === "zh-CN" ? zhCNSections : enUSSections;
}
