<p align="center">
  <img src="./docs/assets/logo.svg" alt="clickhouse-dsl logo" width="320" />
</p>

<p align="center">
  <a href="./README.md">한국어 README</a>
</p>

<p align="center">
  <img src="./docs/badges/coverage.svg" alt="coverage" />
  <img src="./docs/badges/tests.svg" alt="tests" />
  <img src="./docs/badges/version.svg" alt="version" />
  <img src="./docs/badges/license.svg" alt="license" />
</p>

<h1 align="center">Fluent Queries for ClickHouse</h1>

<p align="center">
  Java 17+ typed Query DSL for building safer, more readable ClickHouse queries.
  <br />
  Build structured queries in Java, validate them early, and render clean SQL strings without falling back to fragile string concatenation.
</p>

<p align="center">
  <a href="./docs/guide.md"><strong>Documentation</strong></a>
  ·
  <a href="./src/test/java/io/github/heonny/clickhousedsl/samples"><strong>Examples</strong></a>
  ·
  <a href="https://mvnrepository.com/artifact/io.github.heonny/clickhouse-dsl"><strong>Maven Repository</strong></a>
  ·
  <a href="https://github.com/heonny/clickhouse-dsl"><strong>GitHub</strong></a>
</p>

```xml
<dependency>
    <groupId>io.github.heonny</groupId>
    <artifactId>clickhouse-dsl</artifactId>
    <version>0.1.2</version>
</dependency>
```

`clickhouse-dsl` is a typed Query DSL for building ClickHouse queries more safely and readably from Java code.

The released artifact targets `Java 17+` as its minimum runtime baseline.

This project is intentionally focused on assembling, validating, and rendering SQL strings from Java. It is not trying to become a full ORM or a transport-heavy execution framework.

- Express ClickHouse-oriented syntax naturally in Java code.
- Push as many mistakes as possible into compile-time guardrails.
- Use semantic validation for rules that Java's type system cannot enforce cleanly.
- Keep the model POJO-friendly, immutable, and lightweight.
- Render with placeholders to reduce SQL injection risk compared with raw string assembly.

## AI Guides

If you want an AI agent to use this repository predictably, start here.

- [`skills/clickhouse-dsl/SKILL.md`](./skills/clickhouse-dsl/SKILL.md)
- [`docs/ai/CODEX.md`](./docs/ai/CODEX.md)
- [`docs/ai/CLAUDE.md`](./docs/ai/CLAUDE.md)

## Getting Started

You can use the released artifact directly from Maven Central.

Gradle:

```gradle
dependencies {
    implementation("io.github.heonny:clickhouse-dsl:0.1.2")
}
```

Maven:

```xml
<dependency>
    <groupId>io.github.heonny</groupId>
    <artifactId>clickhouse-dsl</artifactId>
    <version>0.1.2</version>
</dependency>
```

To verify the project locally:

```bash
./gradlew test
./gradlew check
```

The README keeps the fast-start path short. Deeper documents live under `docs/`.

- [`docs/guide.md`](./docs/guide.md)
- [`docs/branding.md`](./docs/branding.md)
- [`docs/VERSIONING.md`](./docs/VERSIONING.md)
- [`docs/RELEASE.md`](./docs/RELEASE.md)

Recommended reading order:

1. `Quick Example` below
2. [`ReadmeExampleTest.java`](./src/test/java/io/github/heonny/clickhousedsl/api/ReadmeExampleTest.java)
3. [`docs/guide.md`](./docs/guide.md)
4. `samples/basic`
5. `samples/advanced`
6. `samples/realworld`

## Current Scope

Supported today:

- `SELECT`
- `FROM`
- `WHERE`
- `PREWHERE`
- `JOIN`
- `GROUP BY`
- `HAVING`
- `ORDER BY`
- `LIMIT`
- `SETTINGS`
- `ARRAY JOIN`
- `SAMPLE`
- `WITH`
- `UNION` / `UNION ALL`
- window functions such as `rowNumber` and `sum(...).over(...)`
- aggregate state helpers such as `sumState` and `sumMerge`
- `EXPLAIN` query model plus raw explain text analysis
- execution metrics POJOs

Still intentionally incomplete:

- deeper ClickHouse transport integration
- server-side explain fetching
- benchmark runner
- broader function type coverage
- detailed window frame syntax

## Safe Usage

For production code, prefer this flow:

1. Build a `Query` with the DSL.
2. Call `validateOrThrow(query)` or use a `renderValidated*` path.
3. Pass the rendered SQL and ordered parameters to your existing execution layer.

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

Avoid:

- calling `render(query)` on user-influenced queries and skipping validation
- ignoring `ValidationResult`
- adding the same setting twice

The center of this library is `typed DSL + validation + SQL rendering`. In practice, execution is still best handled by tools you already use, such as `JdbcTemplate`, MyBatis, or your own internal execution boundary.

## Why Not JPA / JdbcTemplate / MyBatis

| Approach | Strength | Limitation |
|--------|------|------|
| JPA / Criteria | Familiar for entity-centric CRUD | Weak fit for ClickHouse-heavy analytics queries and specialized functions |
| JdbcTemplate + string SQL | Direct and fast | No compile-time guardrails, dynamic branching degrades quickly |
| MyBatis XML / `@NativeQuery` | Explicit SQL feels familiar | Complex dynamic queries become noisy and structurally hard to validate |
| `clickhouse-dsl` | Keeps ClickHouse syntax while improving guardrails, snapshots, and composability | Execution is still best delegated to existing tools such as `JdbcTemplate` |

What matters most here:

- `Compile-time safety`: move as many query mistakes as possible earlier.
- `Testability`: lock both the DSL object and the rendered SQL with snapshot-style checks.
- `Dynamic query flexibility`: compose conditions in POJOs instead of fragile string branches.

## Quick Example

```java
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.*;

Table users = Table.of("analytics.users").as("u").finalTable();
Table events = Table.of("analytics.events").as("e");

var userId = users.column("id", Long.class);
var userName = users.column("name", String.class);
var age = users.column("age", Integer.class);
var score = users.column("score", Integer.class);
var tags = users.arrayColumn("tags", String.class);
var eventUserId = events.column("user_id", Long.class);

Query query = select(
        userName,
        count(),
        rowNumber(window().partitionBy(userName).orderBy(age.desc())),
        io.github.heonny.clickhousedsl.model.Expressions.sum(score)
            .over(window().partitionBy(userName).orderBy(age.asc()))
    )
    .from(users)
    .innerJoin(events).on(userId, eventUserId)
    .arrayJoin(tags)
    .prewhere(age.gt(18))
    .where(userName.eq("alice"))
    .groupBy(userName)
    .having(count().gt(param(1L, Long.class)))
    .limit(100)
    .settings(maxThreads(4), maxMemoryUsage(268_435_456L), useUncompressedCache(true))
    .build();

RenderedQuery rendered = renderValidatedQuery(query);
```
