# Guide

이 문서는 README보다 한 단계 더 깊게 이 프로젝트를 이해하고 싶을 때 참고하는 안내서입니다.

## Sample Catalog

복잡한 샘플은 [`../src/test/java/io/github/heonny/clickhousedsl/samples`](../src/test/java/io/github/heonny/clickhousedsl/samples) 아래를 카탈로그처럼 관리합니다.

의도는 두 가지입니다.

1. README의 대표 예제는 짧고 선명하게 유지합니다.
2. 실제 복잡도에 가까운 조회와 집계 케이스는 샘플 테스트로 축적합니다.

샘플 작성 원칙은 아래와 같습니다.

- 회사 쿼리에서는 구조만 차용하고 이름은 모두 중립화합니다.
- 절대 경로, 사내 스키마, 내부 서비스명은 넣지 않습니다.
- 동일 SQL 렌더링 또는 동일한 논리 구조를 테스트로 고정합니다.

### basic

- `SampleAggregationQueriesTest`
  - 집계 + alias + 함수 중첩
- `DynamicQuerySamplesTest`
  - 조건 유무에 따라 달라지는 동적 조회

### advanced

- `WithUnionSamplesTest`
  - CTE + `UNION ALL`
- `WindowFunctionSamplesTest`
  - window function
- `AggregateStateSamplesTest`
  - state merge 계열
- `ExplainSamplesTest`
  - `EXPLAIN PLAN` 렌더링과 raw explain 분석

### realworld

- `RefinedCompanyStyleSamplesTest`
  - 실무 쿼리 구조를 정제한 공개용 샘플

## Usage Patterns

README에는 Quick Start만 남기고, 실제 사용 패턴 예시는 이 문서로 모읍니다.

### Execution Boundary

기본 원칙은 단순합니다.

1. DSL에서 `Query`를 만든다.
2. `validateOrThrow(...)` 또는 `renderValidated*` 경로로 검증하고 렌더링한다.
3. 실행은 기존 `PreparedStatement`, `JdbcTemplate`, MyBatis, 또는 사내 실행 계층에 맡긴다.

JDBC 예시는 아래와 같습니다.

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

`JdbcTemplate`은 결국 같은 `RenderedQuery`를 넘기면 됩니다.

```java
RenderedQuery rendered = renderValidatedQuery(query);

jdbcTemplate.query(
    rendered.sql(),
    rendered.parameters().toArray(),
    rowMapper
);
```

MyBatis도 같은 아이디어입니다. DSL은 `RenderedQuery`까지만 만들고, 기존 mapper/provider 계층이 `sql()`과 `parameters()`를 소비합니다.

### Dynamic Filters

null-safe 조건 조합이 필요하면 `allOf(...)`, `anyOf(...)`, `whereIfPresent(...)`를 함께 쓰는 편이 좋습니다.

```java
Query query = select(sessionId, country, duration)
    .from(sessions)
    .whereIfPresent(allOf(
        appId.eq(param(7L, Long.class)),
        duration.gt(param(1000L, Long.class)),
        countryCode != null ? country.eq(param(countryCode, String.class)) : null
    ))
    .orderBy(duration.desc(), sessionId.asc())
    .limit(20)
    .build();
```

`allOf(...)`와 `anyOf(...)`는 `null`을 건너뛰고, 최종적으로 남는 조건이 없으면 `null`을 반환합니다.

### Debug And Pretty Rendering

로그나 디버깅에서는 치환된 SQL 문자열을 볼 수 있습니다.

```java
String debugSql = rendered.debugSql();
```

이 문자열은 실행용이 아니라 디버그용입니다. 실제 실행은 계속 `rendered.sql()` + `rendered.parameters()` 경로를 사용해야 합니다.

가독성이 중요한 로그나 테스트 snapshot에서는 pretty render를 opt-in으로 사용할 수 있습니다.

```java
import io.github.heonny.clickhousedsl.render.RenderOptions;

String prettySql = render(query, RenderOptions.pretty());
```

이 옵션은 출력 포맷만 바꾸며, 실행 경로는 여전히 compact SQL + ordered parameters 입니다.

## Validation Philosophy

기본 원칙은 단순합니다. compile-time으로 최대한 잡되, 그 범위를 과장하지 않습니다.

현재 DSL과 analyzer가 집중하는 범위는 아래와 같습니다.

- DSL 단계 순서 제약
- join key 타입 mismatch의 많은 경우
- aggregate / group-by 일부 가드
- `ARRAY JOIN` 입력 타입 검증
- `UNION` 컬럼 수와 타입 위치 검증
- aggregate와 window misuse 검증

semantic analyzer가 주로 맡는 부분은 아래와 같습니다.

- 복잡한 aggregate legality
- alias scope
- ClickHouse 함수 타입 규칙의 나머지
- 더 깊은 semantic edge case

## Security

- identifier는 안전한 패턴만 허용합니다.
- 값은 placeholder `?`로 렌더링합니다.
- raw SQL fragment API는 아직 제공하지 않습니다.

이 프로젝트는 완전한 보안 프레임워크를 목표로 하기보다, 문자열 조립보다 훨씬 덜 위험한 기본값을 제공하는 데 가깝습니다.

## Development

```bash
./gradlew test
./gradlew check
```

## Real Project Check

이 라이브러리는 결국 실제 프로젝트에 붙여서 검증해야 합니다.

권장 순서는 아래와 같습니다.

1. 기존 실서비스나 배치 프로젝트에서 대표 ClickHouse 쿼리 3~5개를 고릅니다.
2. 같은 쿼리를 `clickhouse-dsl`로 다시 작성합니다.
3. 렌더링 SQL이 기존 SQL과 논리적으로 같은지 비교합니다.
4. 실제 ClickHouse에서 결과 row set과 성능 특성을 비교합니다.
5. 차이가 나면 DSL, renderer, analyzer 중 어느 쪽이 원인인지 찾아 수정합니다.
