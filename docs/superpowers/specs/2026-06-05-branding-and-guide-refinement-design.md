# Data Archive Platform Branding And Guide Refinement Design

## Goal

Refresh the EasyArchive frontend branding so the product presents itself as `数据归档平台`, add a simple logo that matches the existing blue-green UI language, move `操作指南` directly below `仪表盘` in the sidebar, and expand the guide page into a technical manual for archive rule configuration.

## Scope

- Frontend-only changes in `easyarchive-ui`
- No backend API, database schema, or rule engine changes
- Keep the existing color direction, card layout, and permission-based navigation behavior

## Success Criteria

- The visible product name changes from `EasyArchive` to `数据归档平台` in the main UI branding surfaces
- A new lightweight logo appears in the sidebar brand area and remains legible at small sizes
- The sidebar order shows `操作指南` immediately after `仪表盘`
- The guide page becomes a technical handbook with actionable rule configuration detail instead of only high-level usage notes
- Existing routing, permissions, and page behavior remain unchanged

## Visual Direction

### Brand Positioning

The interface should feel like an internal enterprise platform focused on data movement, control, and operational safety. The visual tone stays restrained and modern rather than decorative.

### Logo Concept

Adopt the approved `B` direction: abstract data streams entering a layered archive structure.

Design rules:

- Use a compact symbol that can sit to the left of the brand text in the sidebar
- Keep the shape geometric and rounded to match the current card and button language
- Reuse the current blue-green palette instead of introducing a new accent family
- Make the icon readable in monochrome-like contexts and on the dark teal sidebar background

Semantic mapping:

- Upper flow lines represent source data movement
- Lower stacked base represents archive layers or retained data strata
- The combined shape should imply controlled ingestion rather than a generic download arrow

## UX Changes

### Branding Surfaces

Update the main product name in these frontend-visible places where applicable:

- Sidebar brand area
- Login page hero title or subtitle
- Browser page title or any prominent top-level label that still shows `EasyArchive`

Do not rename API fields, package names, or backend class names as part of this task.

### Sidebar Order

Use the following navigation order for standard entries:

1. `仪表盘`
2. `操作指南`
3. `归档连接`
4. `归档分组`
5. `归档任务`

Admin-only entries remain after the standard entries and continue to depend on existing permission checks.

### Guide Page Positioning

The guide page should shift from onboarding copy to a technical reference that operators can use while configuring real rules.

The existing anchor-based layout is retained because it already fits long-form documentation well.

## Guide Content Design

### Content Structure

The Chinese guide should contain these sections:

1. 产品定位
2. 接入前准备
3. 归档连接配置
4. 归档分组配置
5. 规则表达配置总览
6. 按 ID 规则配置
7. 按时间规则配置
8. 查询与清理条件约束
9. 配置示例与验证流程
10. 常见错误与排查

The English guide can remain shorter, but it should still align with the new structure and not contradict the Chinese content.

### Rule Expression Guidance

This is the main expansion area. It should explain not only what each field is called, but how fields work together safely.

Required detail:

- Which scenarios fit ID-based rules versus time-based rules
- How batch size, step size, and execution interval affect source pressure
- Why fetch conditions, sort fields, and delete predicates must target the same data slice
- Why index coverage matters for archive safety and throughput
- Why cleanup should be disabled during first verification

### ID-Based Rule Section

The guide should explain:

- Suitable table characteristics such as monotonic primary keys or explicit historical ranges
- Meaning of start ID, end ID, current batch window, batch size, and execution order
- Recommended query pattern characteristics such as stable ordering and index-friendly ranges
- Cleanup condition expectations and why they must match fetched records exactly
- Common risks such as missing sort order, sparse IDs, and oversized range windows

### Time-Based Rule Section

The guide should explain:

- Suitable table characteristics such as logs,流水, and history records with a clear time field
- Meaning of archive cutoff time, retention days, step window, and time field choice
- Recommended query pattern characteristics such as bounded time windows and matching cleanup windows
- Special attention to timezone consistency, delayed writes, and hot partitions

### Query And Cleanup Constraints

The guide should explicitly state these operational rules:

- Query predicates must hit indexes whenever possible
- Query ordering should be deterministic
- Delete predicates must describe the same row set as the fetch query
- Write verification should happen before cleanup is enabled
- Rule changes should be validated with a small window before full rollout

### Example Section

Provide examples in operator-friendly prose, not raw backend configuration dumps only.

Include:

- A safe first-run pattern: write enabled, cleanup disabled
- A typical ID-based example
- A typical time-based example
- A validation checklist before enabling cleanup

## Component And File-Level Design

### App Layout

- Replace the plain text brand block with a small logo-plus-title composition
- Preserve current sidebar spacing and background
- Adjust navigation order without changing route names

### Guide View

- Keep the existing left anchor navigation and right content column structure
- Increase information density using subsection cards or richer grouped lists where needed
- Make important warnings visually distinct without introducing heavy alert styling

### I18n

- Update brand-related copy to `数据归档平台`
- Keep translation keys stable where practical; change only the displayed values unless structural copy additions need new keys

## Implementation Notes

- Prefer inline SVG or a small local component for the logo to avoid adding image asset management complexity
- Keep the logo self-contained in the frontend module
- Preserve responsive behavior for the guide page; if needed, stack the two-column guide layout on narrower screens
- Avoid broad theme changes; this task is a refinement, not a redesign

## Error Handling And Risk Control

- Do not break role-based visibility for admin-only navigation entries
- Do not introduce copy that implies unsupported rule-engine capabilities
- Keep guide examples generic enough to be safe, but concrete enough to execute

## Testing

Manual verification should cover:

- Sidebar renders logo and new product title correctly
- Sidebar ordering matches the approved sequence
- Login page and other key labels no longer show `EasyArchive` in user-facing branding
- Guide anchors navigate correctly
- Guide content remains readable on desktop and mobile widths
- Existing routes and protected pages continue to function

## Out Of Scope

- Backend terminology refactors
- Rule-engine feature additions
- New API endpoints
- Full visual redesign of dashboard cards or data tables
