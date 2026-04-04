---
name: clickhouse-dsl
description: Use this skill when writing or reviewing Java code that should build ClickHouse SQL with this repository's typed DSL instead of raw string SQL. Prefer validated rendering, reuse the sample catalog, keep the library focused on DSL plus validation plus SQL rendering, and avoid turning it into a transport-heavy execution framework.
---

# clickhouse-dsl

Use this skill as the onboarding guide for this repository's DSL.

The library's center of gravity is:

1. typed query construction
2. semantic validation
3. SQL string rendering

It is not meant to be a full execution framework or ORM.

## Default path

Prefer this flow:

1. Build a `Query` with the DSL.
2. Call `renderValidatedQuery(query)` or `validateOrThrow(query)`.
3. Pass the resulting SQL and ordered parameters to an existing execution tool such as `JdbcTemplate`.

Example:

```java
Query query = select(userName, count())
    .from(users)
    .groupBy(userName)
    .build();

RenderedQuery rendered = renderValidatedQuery(query);

jdbcTemplate.query(
    rendered.sql(),
    rendered.parameters().toArray(),
    rowMapper
);
```

## Prefer

- Prefer DSL nodes over raw SQL strings.
- Prefer `renderValidatedQuery(query)` over `render(query)` for production paths.
- Prefer existing execution infrastructure such as `JdbcTemplate`.
- Prefer expanding validation, samples, and docs before expanding transport features.

## Avoid

- Avoid assembling raw SQL strings by hand when the DSL can express the query.
- Avoid calling `render(query)` and skipping validation on user-influenced queries.
- Avoid duplicate setting names.
- Avoid aggregate expressions in `WHERE` or `PREWHERE`.
- Avoid window functions in `WHERE`, `PREWHERE`, `GROUP BY`, or `HAVING`.
- Avoid examples that look like internal company schema or business terms.

## Aggregate state usage

For aggregate-state tables:

1. Declare state fields with `stateColumn(...)`.
2. Merge them with `countMerge`, `countIfMerge`, `uniqMerge`, or `sumMerge`.
3. Finish with `renderValidatedQuery(query)` and hand the result to the host application's execution layer.

Do not model raw aggregate state values as if they were normal public-facing values.

## Read in this order

Start here when adding or reviewing DSL code:

1. `README.md`
2. `src/test/java/io/github/heonny/clickhousedsl/api/ReadmeExampleTest.java`
3. `src/test/java/io/github/heonny/clickhousedsl/samples/basic`
4. `src/test/java/io/github/heonny/clickhousedsl/samples/advanced`
5. `src/test/java/io/github/heonny/clickhousedsl/samples/realworld`

Reuse sample shapes before inventing a new pattern.

## Output rules for AI agents

- Show DSL code first, then rendered SQL only when it helps.
- If validation fails, surface the relevant `ValidationCode` clearly instead of forcing a workaround.
- Keep public examples sanitized and neutral.
- Preserve the repository position: DSL plus validation plus rendering first, transport second.
