# EasyArchive UI Guide And Labels Design

## Goal

Adjust the EasyArchive frontend so the default language is Chinese, the language control uses a dropdown, task-related navigation is labeled as archive tasks, datasource JDBC labels are shown as connection address, and a dedicated operation guide page helps users understand setup and rule configuration.

## Scope

- Frontend-only changes in `easyarchive-ui`
- No backend API or database schema changes
- Preserve existing `jdbcUrl` payload fields and task APIs

## UX Changes

### Language

- Replace the two-button language switcher with a dropdown selector
- Default first-time visits to `zh-CN`
- Preserve the user's stored locale choice after selection

### Navigation And Labels

- Rename `任务` to `归档任务`
- Add a new sidebar entry for `操作指南`
- Keep the existing app layout and navigation structure

### Datasource Copy

- Change visible `JDBC 地址` / `JDBC URL` labels to `连接地址` / `Connection Address`
- Keep validation logic based on the `jdbc:` prefix unchanged

### Operation Guide Page

Add a dedicated page with section navigation and concise, practical content:

- Product overview
- Quick start flow
- Datasource configuration field guidance
- Archive task configuration guidance
- Rule expression and scenario guidance for ID-based and time-based rules
- Operational recommendations
- Common issues

## Implementation Notes

- Add a new route and view component for the guide page
- Update i18n text for navigation and datasource wording
- Keep guide content local to the view component so this change stays isolated
- Use scoped styles for guide-page-specific layout and section cards
