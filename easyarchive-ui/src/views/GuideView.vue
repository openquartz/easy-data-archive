<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "../i18n";

type GuideSection = {
  id: string;
  title: string;
  intro?: string;
  items?: string[];
  notes?: string[];
};

const { isZhCN } = useI18n();

const sections = computed<GuideSection[]>(() =>
  isZhCN.value
    ? [
        {
          id: "overview",
          title: "产品简介",
          intro:
            "EasyArchive 用于把业务历史数据从源库分批迁移到目标库，并支持在确认写入成功后按规则清理源表数据，适合日志、流水、订单历史等场景。"
        },
        {
          id: "quick-start",
          title: "快速上手",
          items: [
            "登录系统后先进入归档连接管理，创建源归档连接和目标归档连接。",
            "进入归档分组管理，创建一个归档任务分组，指定源归档连接、目标归档连接和启用状态。",
            "在归档分组下新增归档明细，按表的特点选择按 ID 或按时间的规则。",
            "首次联调建议先开启写入、关闭清理，确认目标库入库正确后再打开清理。",
            "在归档分组列表触发执行后，可进入归档任务页面查看进度、速度、错误信息和日志。"
          ]
        },
        {
          id: "datasource",
          title: "归档连接管理说明",
          items: [
            "编码：归档连接唯一标识，建议按环境和用途命名，例如 `prod_order_src`。",
            "名称：用于界面展示的人类可读名称。",
            "类型：当前主要使用 MySQL。",
            "连接地址：数据库 JDBC 连接串，例如 `jdbc:mysql://127.0.0.1:3306/order_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai`。",
            "用户名 / 密码：归档执行时连接数据库使用的账号。",
            "状态：停用后不会再被新的归档配置选中。",
            "备注：记录用途、环境、风险提示等补充信息。"
          ],
          notes: [
            "建议为归档账号授予最小必要权限，并提前验证源库和目标库网络连通性。",
            "修改密码后可重新编辑归档连接并保存，避免执行时因凭据过期失败。"
          ]
        },
        {
          id: "archive-task",
          title: "归档任务配置说明",
          items: [
            "归档分组用于组织一批需要一起执行的归档明细，通常按业务域或库表集合划分。",
            "源归档连接表示读取和清理发生在哪个库，目标归档连接表示写入发生在哪个库。",
            "归档明细对应具体表规则，可按优先级控制执行顺序。",
            "批大小、步进轮次、暂停毫秒等参数决定了单次执行压力，建议先小后大逐步放量。"
          ],
          notes: [
            "如果多个表存在主从关系，建议先归档子表，再归档主表。",
            "生产环境建议错峰执行，并持续观察归档任务日志。"
          ]
        },
        {
          id: "rule-scenarios",
          title: "规则表达式与生成场景",
          intro: "规则配置要和表结构、索引条件、数据清理策略一起设计，避免大范围扫描或误删。",
          items: [
            "按 ID 归档：适合主键递增、可以按区间搬迁的业务表。关键字段包括 ID 字段、起始 ID、结束 ID、批大小、读取 SQL、删除条件。",
            "按时间归档：适合日志、流水、订单历史等按时间沉淀的数据。关键字段包括开始时间、保留天数、步进分钟、读取 SQL、删除条件。",
            "读取 SQL 应尽量使用可命中索引的条件，并确保结果集和删除条件针对的是同一批数据。",
            "删除条件建议先在数据库单独验证命中范围，再用于自动清理。"
          ],
          notes: [
            "推荐场景：订单日志、访问日志、操作流水优先使用按时间归档。",
            "推荐场景：主键连续、历史区间明确的业务表可优先使用按 ID 归档。",
            "如果不确定是否安全，先只写入不清理，待比对结果确认后再启用清理。"
          ]
        },
        {
          id: "recommendations",
          title: "使用建议",
          items: [
            "执行前先确认源表筛选字段、排序字段和删除条件均有合适索引。",
            "首次上线从小批量开始，逐步扩大批大小和执行频率。",
            "高峰期避免执行大批量归档，减少对在线业务的影响。",
            "目标库表结构、字段类型、字符集需要与源库兼容。",
            "每次调整规则后都建议先做一次小范围验证。"
          ]
        },
        {
          id: "faq",
          title: "常见问题",
          items: [
            "连接失败：优先检查连接地址、账号密码、数据库白名单和网络策略。",
            "任务没有数据：检查读取 SQL 条件、时间范围、ID 范围和启用状态是否正确。",
            "写入成功但未清理：确认是否开启清理开关，并检查删除条件是否命中。",
            "执行速度慢：优先检查索引、批大小、目标库写入性能和任务并发影响。"
          ]
        }
      ]
    : [
        {
          id: "overview",
          title: "Overview",
          intro:
            "EasyArchive moves historical data from a source database to a target database in controlled batches and can optionally clean source records after successful writes."
        },
        {
          id: "quick-start",
          title: "Quick Start",
          items: [
            "Create both source and target connections in Archive Connection Management.",
            "Create an archive task group and bind the source and target connections.",
            "Add archive items under the group and choose either ID-based or time-based rules.",
            "For the first rollout, enable write first and keep clean disabled until target data is verified.",
            "Trigger the group and monitor progress, speed, errors, and logs from Archive Tasks."
          ]
        },
        {
          id: "datasource",
          title: "Archive Connection Guidance",
          items: [
            "Code: unique identifier for the archive connection.",
            "Name: readable display name in the UI.",
            "Type: currently mainly MySQL.",
            "Connection Address: JDBC connection string.",
            "Username / Password: database credentials used during execution.",
            "Status: disabled archive connections should not be used by new archive configs.",
            "Remark: optional operational notes."
          ]
        },
        {
          id: "archive-task",
          title: "Archive Task Guidance",
          items: [
            "An archive group organizes multiple archive items that run together.",
            "Source archive connection is used for reading and cleaning; target archive connection is used for writes.",
            "Priority controls execution order across archive items.",
            "Batch size, rounds, and pause settings determine runtime pressure."
          ]
        },
        {
          id: "rule-scenarios",
          title: "Rule Expressions And Scenarios",
          items: [
            "ID-based rules fit tables with increasing primary keys and clear ID ranges.",
            "Time-based rules fit logs, event streams, and historical business records.",
            "Fetch SQL should use index-friendly predicates and match the intended delete scope.",
            "Delete conditions should be verified independently before enabling clean mode."
          ]
        },
        {
          id: "recommendations",
          title: "Operational Recommendations",
          items: [
            "Start with small batches and grow carefully.",
            "Prefer off-peak execution windows.",
            "Verify indexes, schema compatibility, and cleanup conditions before production runs.",
            "Re-test after every rule change."
          ]
        },
        {
          id: "faq",
          title: "Common Issues",
          items: [
            "Connection failures usually come from invalid JDBC strings, credentials, or network policy.",
            "Empty task results usually indicate rule scope or status configuration issues.",
            "Slow execution often points to indexing, write throughput, or oversized batches."
          ]
        }
      ]
);
</script>

<template>
  <section class="guide-page">
    <aside class="guide-page__nav page-card">
      <h1 class="guide-page__title">{{ isZhCN ? "操作指南" : "Operation Guide" }}</h1>
      <p class="guide-page__intro">
        {{
          isZhCN
            ? "按章节了解系统定位、快速上手步骤、规则配置方式和常见风险点。"
            : "Use the sections below to understand setup flow, rule configuration, and common operational risks."
        }}
      </p>
      <nav class="guide-page__anchors">
        <a v-for="section in sections" :key="section.id" class="guide-page__anchor" :href="`#${section.id}`">
          {{ section.title }}
        </a>
      </nav>
    </aside>

    <div class="guide-page__content">
      <article v-for="section in sections" :id="section.id" :key="section.id" class="page-card guide-section">
        <h2 class="guide-section__title">{{ section.title }}</h2>
        <p v-if="section.intro" class="guide-section__intro">{{ section.intro }}</p>
        <ul v-if="section.items?.length" class="guide-section__list">
          <li v-for="item in section.items" :key="item">{{ item }}</li>
        </ul>
        <div v-if="section.notes?.length" class="guide-section__notes">
          <strong>{{ isZhCN ? "补充建议" : "Notes" }}</strong>
          <ul class="guide-section__list">
            <li v-for="note in section.notes" :key="note">{{ note }}</li>
          </ul>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.guide-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
}

.guide-page__nav {
  position: sticky;
  top: 24px;
  align-self: start;
}

.guide-page__title {
  margin: 0 0 10px;
  font-size: 1.25rem;
}

.guide-page__intro {
  margin: 0 0 16px;
  color: var(--ea-text-muted);
  line-height: 1.6;
}

.guide-page__anchors {
  display: grid;
  gap: 8px;
}

.guide-page__anchor {
  color: var(--ea-primary);
  text-decoration: none;
  padding: 8px 10px;
  border-radius: 10px;
  background: #f5fafb;
}

.guide-page__content {
  display: grid;
  gap: 16px;
}

.guide-section__title {
  margin: 0 0 10px;
  font-size: 1.1rem;
}

.guide-section__intro {
  margin: 0 0 12px;
  color: var(--ea-text-muted);
  line-height: 1.7;
}

.guide-section__list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}

.guide-section__notes {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f7fafc;
}

@media (max-width: 960px) {
  .guide-page {
    grid-template-columns: 1fr;
  }

  .guide-page__nav {
    position: static;
  }
}
</style>
