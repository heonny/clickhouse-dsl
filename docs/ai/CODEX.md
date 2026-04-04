# Codex adapter

Use [`../../skills/clickhouse-dsl/SKILL.md`](../../skills/clickhouse-dsl/SKILL.md) as the source of truth.

Codex-specific reminders:

- Generate DSL code before considering raw SQL.
- Prefer `renderValidatedQuery(query)` for integration examples.
- Reuse sample test patterns instead of inventing a new style.
- Keep transport code secondary to validator and renderer quality.
- Reject company-looking names in public examples and docs.
