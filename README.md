<p align="center">
  <img src="./docs/assets/logo.svg" alt="clickhouse-dsl logo" width="320" />
</p>

<p align="center">
  <a href="./README.en.md">English README</a>
</p>

<p align="center">
  <img src="https://codecov.io/gh/heonny/clickhouse-dsl/graph/badge.svg" alt="coverage" />
  <img src="https://github.com/heonny/clickhouse-dsl/actions/workflows/ci.yml/badge.svg?branch=main" alt="tests" />
  <img src="https://img.shields.io/maven-central/v/io.github.heonny/clickhouse-dsl?label=maven" alt="Maven Central version" />
  <img src="https://img.shields.io/badge/java-17%2B-007396" alt="Java 17+" />
  <img src="https://img.shields.io/badge/license-Apache--2.0-informational" alt="license" />
</p>

<h1 align="center">Fluent Queries for ClickHouse</h1>

<p align="center">
  Java 17+ typed Query DSL for building safer, more readable ClickHouse queries.
  <br />
  문자열 조립 대신 구조화된 DSL과 validation으로 쿼리를 만들고, 최종적으로 안전한 SQL 문자열로 렌더링합니다.
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
    <version>0.1.5</version>
</dependency>
```

`clickhouse-dsl`은 ClickHouse 쿼리를 Java 코드에서 더 안전하고 읽기 좋게 만들기 위한 typed Query DSL입니다.

현재 산출물은 `Java 17+`를 최소 실행 기준으로 합니다.

이 프로젝트는 직접 DB 실행까지 책임지는 프레임워크보다는, Java 코드에서 쿼리를 조립하고 검증한 뒤 안전한 SQL 문자열로 렌더링하는 데 집중합니다.

- ClickHouse 특화 문법을 Java 코드에서 자연스럽게 표현합니다.
- 가능한 범위의 실수를 compile-time guardrail로 끌어올립니다.
- 타입 시스템만으로 막기 어려운 규칙은 semantic analyzer로 검증합니다.
- POJO 중심, immutable 지향, 경량 구조를 유지합니다.
- placeholder 기반 렌더링으로 문자열 조립보다 덜 위험한 기본값을 제공합니다.

## Getting Started

현재는 Maven Central release 배포 기준으로 바로 사용할 수 있습니다.

Gradle:

```gradle
dependencies {
    implementation("io.github.heonny:clickhouse-dsl:0.1.5")
}
```

Maven:

```xml
<dependency>
    <groupId>io.github.heonny</groupId>
    <artifactId>clickhouse-dsl</artifactId>
    <version>0.1.5</version>
</dependency>
```

로컬에서 먼저 상태를 확인하려면 아래 명령으로 충분합니다.

```bash
./gradlew test
./gradlew check
```

가장 먼저 기억할 사용 패턴은 아래 한 줄입니다.

- DSL은 `Query`를 만들고 검증/렌더링까지 담당합니다.
- 실제 실행은 `PreparedStatement`, `JdbcTemplate`, MyBatis 같은 기존 계층이 담당합니다.

가장 단순한 JDBC 실행 예제는 아래와 같습니다.

```java
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.*;

import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;
import java.sql.Connection;
import java.sql.PreparedStatement;

Table users = Table.of("analytics.users").as("u");
var userName = users.column("name", String.class);
var appId = users.column("app_id", Long.class);

Query query = select(userName, count())
    .from(users)
    .where(appId.eq(param(7L, Long.class)))
    .groupBy(userName)
    .build();

RenderedQuery rendered = renderValidatedQuery(query);

try (Connection connection = dataSource.getConnection();
     PreparedStatement statement = connection.prepareStatement(rendered.sql())) {
    for (int index = 0; index < rendered.parameters().size(); index++) {
        statement.setObject(index + 1, rendered.parameters().get(index));
    }
    statement.executeQuery();
}
```

`JdbcTemplate`을 쓴다면 결국 같은 `RenderedQuery`를 넘기면 됩니다.

```java
RenderedQuery rendered = renderValidatedQuery(query);

jdbcTemplate.query(
    rendered.sql(),
    rendered.parameters().toArray(),
    rowMapper
);
```

MyBatis를 쓰는 경우에도 아이디어는 같습니다. DSL에서 `RenderedQuery`를 만들고, 기존 mapper/provider 계층에서 `sql()`과 `parameters()`를 소비하면 됩니다.

Quick Start 외의 실행 패턴, dynamic filter helper, debug/pretty render 예시는 [`docs/guide.md`](./docs/guide.md)로 분리했습니다.

- [`docs/guide.md`](./docs/guide.md)
- [`docs/branding.md`](./docs/branding.md)
- [`docs/VERSIONING.md`](./docs/VERSIONING.md)
- [`docs/RELEASE.md`](./docs/RELEASE.md)

처음 보신다면 아래 순서로 읽는 것을 권장합니다.

1. 이 README의 `Quick Example`
2. [`ReadmeExampleTest.java`](./src/test/java/io/github/heonny/clickhousedsl/api/ReadmeExampleTest.java)
3. [`docs/guide.md`](./docs/guide.md)
4. `samples/basic`
5. `samples/advanced`
6. `samples/realworld`

## Current Scope

현재 지원하는 범위는 아래와 같습니다.

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
- window function (`rowNumber`, `sum(...).over(...)`)
- aggregate state helper (`sumState`, `sumMerge`)
- `EXPLAIN` query model과 raw explain text analyzer
- execution metrics POJO (`maxMemoryUsageBytes`, `usedThreads`)

아직 의도적으로 남겨둔 범위도 있습니다.

- 더 넓은 공식 execution integration (`RenderedQuery` 기반 executor 확장, `JdbcTemplate` 래퍼 등)
- 서버 연동 explain fetch
- benchmark runner
- 함수 타입 시스템의 전체 커버리지
- window frame 세부 문법

## Safe Usage

운영 코드에서는 아래 흐름을 권장합니다.

1. DSL로 `Query`를 만듭니다.
2. `validateOrThrow(query)` 또는 `renderValidated*` 경로를 사용합니다.
3. 렌더링된 SQL과 파라미터를 기존 실행 계층으로 넘깁니다.

실행 예제, dynamic filter helper, `debugSql()`, `RenderOptions.pretty()` 예시는 [`docs/guide.md`](./docs/guide.md)에서 모아 설명합니다.

반대로 아래 패턴은 피하는 편이 좋습니다.

- 외부 입력이 섞인 query를 `render(query)`만 호출하고 바로 실행하는 패턴
- `ValidationResult`를 무시하는 패턴
- 같은 setting 이름을 여러 번 넣는 패턴

추가로, DSL 객체의 내부 상태는 안정적으로 다루되 `param(...)`이나 `literal(...)`에 넘기는 값 자체까지 깊은 복사를 하지는 않습니다. 동시 사용이 필요한 값은 immutable 객체를 넘기거나 호출자 쪽에서 변경되지 않도록 다루는 편을 권장합니다.

현재 이 라이브러리의 중심 기능은 실행기보다 `typed DSL + validation + SQL rendering`입니다. 실제 실행은 `JdbcTemplate`, MyBatis, 또는 기존 사내 실행 계층에 맡기는 편을 권장합니다.

## Why Not JPA / JdbcTemplate / MyBatis

비슷한 선택지와 비교하면 이런 차이가 있습니다.

| Approach | 장점 | 한계 |
|--------|------|------|
| JPA / Criteria | 엔티티 중심 CRUD에는 익숙합니다 | ClickHouse 문법 충실도가 낮고 분석 쿼리 표현이 어색합니다 |
| JdbcTemplate + string SQL | 가장 직접적이고 빠릅니다 | 컴파일 안정성이 없고 동적 조건이 늘수록 문자열 조립이 빠르게 무너집니다 |
| MyBatis XML / `@NativeQuery` | SQL 자체는 명시적이라 익숙합니다 | 복잡한 동적 쿼리는 XML과 문자열 분기가 빠르게 지저분해집니다 |
| `clickhouse-dsl` | ClickHouse 문법을 유지하면서 guardrail, 테스트, 동적 조합성을 함께 챙길 수 있습니다 | 실행은 기존 도구와 조합해서 쓰는 편이 더 자연스럽습니다 |

특히 아래 세 가지를 중요하게 봅니다.

- `컴파일 안정성`: 가능한 범위의 실수를 컴파일 단계로 끌어올립니다.
- `테스트 용이성`: DSL 객체와 렌더링 SQL을 함께 고정할 수 있습니다.
- `동적쿼리 유연성`: 조건 조합이 복잡해질수록 문자열보다 POJO 조립이 훨씬 안정적입니다.

## Quick Example

```java
import static io.github.heonny.clickhousedsl.api.ClickHouseDsl.*;

import io.github.heonny.clickhousedsl.model.Query;
import io.github.heonny.clickhousedsl.model.RenderedQuery;
import io.github.heonny.clickhousedsl.model.Table;

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
        sum(score).over(window().partitionBy(userName).orderBy(age.asc()))
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

이 예제의 렌더링 결과와 dynamic filter helper, debug/pretty render, 추가 실행 예시는 [`docs/guide.md`](./docs/guide.md)에서 이어서 볼 수 있습니다.

## More Examples

`WITH`, `UNION`, window function, aggregate state, `EXPLAIN`, 실전형 샘플은 README보다 [`docs/guide.md`](./docs/guide.md)와 `samples/*` 테스트에서 보는 편이 더 좋습니다.

대표적으로 아래를 참고하시면 됩니다.

- [`src/test/java/io/github/heonny/clickhousedsl/samples/basic`](./src/test/java/io/github/heonny/clickhousedsl/samples/basic)
- [`src/test/java/io/github/heonny/clickhousedsl/samples/advanced`](./src/test/java/io/github/heonny/clickhousedsl/samples/advanced)
- [`src/test/java/io/github/heonny/clickhousedsl/samples/realworld`](./src/test/java/io/github/heonny/clickhousedsl/samples/realworld)

## Security

기본 원칙은 단순합니다.

- identifier는 안전한 패턴만 허용합니다.
- 값은 placeholder `?`로 렌더링합니다.
- raw SQL fragment API는 아직 제공하지 않습니다.

완전한 보안 프레임워크를 목표로 하지는 않지만, 문자열 조립보다 훨씬 덜 위험한 기본값을 제공하는 방향을 유지합니다.

## Development

```bash
./gradlew test
./gradlew check
```

실전 적용 전 검증 순서와 릴리즈 절차는 아래 문서를 참고해 주세요.

- [`docs/guide.md`](./docs/guide.md)
- [`docs/VERSIONING.md`](./docs/VERSIONING.md)
- [`docs/RELEASE.md`](./docs/RELEASE.md)

## AI Guides

AI가 이 저장소를 일관된 방식으로 사용하도록 안내하는 문서입니다.

- [`skills/clickhouse-dsl/SKILL.md`](./skills/clickhouse-dsl/SKILL.md)
- [`docs/ai/CODEX.md`](./docs/ai/CODEX.md)
- [`docs/ai/CLAUDE.md`](./docs/ai/CLAUDE.md)
