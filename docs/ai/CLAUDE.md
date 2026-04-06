# Claude adapter

Use [`../../skills/clickhouse-dsl/SKILL.md`](../../skills/clickhouse-dsl/SKILL.md) as the source of truth.

Claude-specific reminders:

- Read [`../guide.md`](../guide.md) before writing or revising public-facing examples.
- Treat [`../../src/test/java/io/github/heonny/clickhousedsl/api/ReadmeExampleTest.java`](../../src/test/java/io/github/heonny/clickhousedsl/api/ReadmeExampleTest.java) and the sample tests under `src/test/java/io/github/heonny/clickhousedsl/samples` as the canonical example set.
- Prefer DSL composition over direct SQL writing.
- Do not ignore `ValidationResult`.
- Show execution examples through existing host tools such as `JdbcTemplate`.
- Keep execution on `RenderedQuery.sql()` plus `parameters()`. Use `debugSql()` only for logs and debugging.
- Prefer dynamic-condition helpers when documenting optional filters.
- Keep public wording natural, concise, and free of internal company naming.
- Protect the current project scope instead of expanding transport features by default.
