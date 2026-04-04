# Claude adapter

Use [`../../skills/clickhouse-dsl/SKILL.md`](../../skills/clickhouse-dsl/SKILL.md) as the source of truth.

Claude-specific reminders:

- Prefer DSL composition over direct SQL writing.
- Do not ignore `ValidationResult`.
- Show execution examples through existing host tools such as `JdbcTemplate`.
- Keep public wording natural, concise, and free of internal company naming.
- Protect the current project scope instead of expanding transport features by default.
