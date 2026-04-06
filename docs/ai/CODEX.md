# Codex adapter

Use [`../../skills/clickhouse-dsl/SKILL.md`](../../skills/clickhouse-dsl/SKILL.md) as the source of truth.

Codex-specific reminders:

- Read [`../guide.md`](../guide.md) before proposing new public examples or usage guidance.
- Reuse [`../../src/test/java/io/github/heonny/clickhousedsl/api/ReadmeExampleTest.java`](../../src/test/java/io/github/heonny/clickhousedsl/api/ReadmeExampleTest.java) and the `samples/*` tests as canonical example shapes.
- Generate DSL code before considering raw SQL.
- Prefer `renderValidatedQuery(query)` for integration examples.
- Keep `RenderedQuery.sql()` + `parameters()` as the execution path. Treat `debugSql()` as debug-only.
- Use dynamic-condition helpers (`allOf(...)`, `anyOf(...)`, `whereIfPresent(...)`) when examples would otherwise branch on nulls.
- Reuse sample test patterns instead of inventing a new style.
- Keep transport code secondary to validator and renderer quality.
- Reject company-looking names in public examples and docs.
