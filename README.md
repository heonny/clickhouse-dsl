# clickhouse-dsl

Java 17 기반의 ClickHouse typed Query DSL.

목표는 단순한 string builder가 아니다.

- ClickHouse 특화 문법을 Java 코드에서 자연스럽게 표현
- 가능한 범위의 compile-time guardrail 제공
- 나머지는 semantic analyzer로 명시적으로 검증
- POJO 중심, immutable 지향, 경량 구조
- parameter placeholder 기반 렌더링으로 SQL injection 경로 축소

## Current Scope

현재 구현된 범위:

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
- aggregate state helpers (`sumState`, `sumMerge`)
- `EXPLAIN` query model + raw explain text analyzer

아직 구현하지 않은 것:

- 실제 JDBC / HTTP executor
- ClickHouse 서버 연동 explain fetch
- benchmark runner
- 함수 타입 시스템의 전체 커버리지
- window frame 세부 문법

## Design Position

이 라이브러리는 범용 SQL DSL이 아니다.

포지션은 이렇다.

`범용 DSL보다 ClickHouse에 더 충실하고, 문자열 SQL보다 더 안전한 typed middle layer`

## Why Not JPA / JdbcTemplate / MyBatis

대표 비교는 이 관점으로 보는 게 맞다.

| Approach | 장점 | 한계 |
|--------|------|------|
| JPA / Criteria | 엔티티 중심 CRUD에는 익숙하다 | ClickHouse 문법 충실도가 낮고, 분석 쿼리나 특화 함수 표현이 어색하다 |
| JdbcTemplate + string SQL | 가장 직접적이고 빠르다 | 컴파일 안정성이 없고, 동적 조건이 늘수록 문자열 조립이 급격히 나빠진다 |
| MyBatis XML / `@NativeQuery` | SQL 자체는 명시적이라 익숙하다 | 복잡한 동적 쿼리는 XML/문자열 분기 관리가 어렵고, IDE 차원의 구조 검증이 약하다 |
| `clickhouse-dsl` | ClickHouse 문법을 유지하면서 compile guardrail, 테스트 스냅샷, 동적쿼리 조립성을 같이 챙긴다 | 아직 executor/benchmark/함수 타입 시스템 전체는 진행 중이다 |

이 프로젝트가 특히 강하게 가져가려는 포인트는 세 가지다.

- `컴파일 안정성`: 가능한 범위의 쿼리 실수를 컴파일 단계로 끌어올린다
- `테스트 용이성`: DSL 객체와 렌더링 SQL을 둘 다 snapshot처럼 고정할 수 있다
- `동적쿼리 유연성`: `JdbcTemplate`나 annotation 기반 native query에서 지저분해지는 조건 조합을 POJO 조립으로 가져온다

## Quick Example

```java
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.*;

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
        io.github.chang.clickhousedsl.model.Expressions.sum(score)
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
    .settings(maxThreads(4), useUncompressedCache(true))
    .build();

String sql = render(query);
```

렌더링 결과:

```sql
SELECT `u`.`name`, count(), rowNumber() OVER (PARTITION BY `u`.`name` ORDER BY `u`.`age` DESC), sum(`u`.`score`) OVER (PARTITION BY `u`.`name` ORDER BY `u`.`age` ASC)
FROM `analytics`.`users` FINAL AS `u`
INNER JOIN `analytics`.`events` AS `e` ON `u`.`id` = `e`.`user_id`
ARRAY JOIN `u`.`tags`
PREWHERE `u`.`age` > ?
WHERE `u`.`name` = ?
GROUP BY `u`.`name`
HAVING count() > ?
LIMIT ?
SETTINGS `max_threads` = ?, `use_uncompressed_cache` = ?
```

## WITH / UNION Example

```java
Query activeUsers = select(Table.of("raw_users").column("name", String.class))
    .from(Table.of("raw_users"))
    .where(Table.of("raw_users").column("name", String.class).eq("alice"))
    .build();

Query archivedUsers = select(Table.of("archived_users").column("name", String.class))
    .from(Table.of("archived_users"))
    .build();

Query merged = select(Table.of("active_users").column("name", String.class))
    .with(with("active_users", activeUsers))
    .from(Table.of("active_users"))
    .unionAll(archivedUsers)
    .build();
```

## Explain Example

```java
ExplainQuery explainQuery = explain(ExplainType.PLAN, query);
String explainSql = render(explainQuery);

ExplainResult result = analyze(
    ExplainType.PLAN,
    """
    ReadFromStorage
    Prewhere
    Filter
    Join
    Aggregating
    Sorting
    """
);
```

분석 결과에서는 이런 힌트를 얻을 수 있다.

- storage read 여부
- filter / prewhere 여부
- join stage 여부
- aggregation 여부
- sorting 여부

## Before / After

실제 현업 쿼리와 비슷한 복잡도의 예제를, 공개용 샘플 형태로 치장하면 이런 그림이 된다.

### Before: string SQL

```sql
SELECT count() AS count,
       toUInt8(0) AS isOther,
       event_message AS message
FROM app_errors
WHERE app_id = ?
  AND event_time >= toDateTime64(?, 3, 'UTC')
  AND event_time < toDateTime64(addDays(toDate(?), 1), 3, 'UTC')
GROUP BY message
ORDER BY count DESC, message ASC
LIMIT ?
```

문제는 익숙하다.

- alias, function nesting, date wrapper가 길어질수록 오타가 늘어난다
- refactor해도 IDE가 쿼리 구조를 잘 이해하지 못한다
- 재사용하려고 문자열 조각을 나누면 더 읽기 어려워진다

### After: clickhouse-dsl

```java
import static io.github.chang.clickhousedsl.api.ClickHouseDsl.*;

Table errors = Table.of("app_errors");

var appId = errors.column("app_id", Long.class);
var eventTime = errors.column("event_time", Long.class);
var eventMessage = errors.column("event_message", String.class);

Query query = select(
        count().as("count"),
        function("toUInt8", Integer.class, literal(0, Integer.class)).as("isOther"),
        eventMessage.as("message")
    )
    .from(errors)
    .where(
        appId.eq(param(100L, Long.class))
            .and(eventTime.gte(
                function(
                    "toDateTime64",
                    Long.class,
                    param("2026-04-01T00:00:00", String.class),
                    literal(3, Integer.class),
                    literal("UTC", String.class)
                )
            ))
            .and(eventTime.lt(
                function(
                    "toDateTime64",
                    Long.class,
                    function(
                        "addDays",
                        String.class,
                        function("toDate", String.class, param("2026-04-02", String.class)),
                        literal(1, Integer.class)
                    ),
                    literal(3, Integer.class),
                    literal("UTC", String.class)
                )
            ))
    )
    .groupBy(ref("message", String.class))
    .orderBy(ref("count", Long.class).desc(), ref("message", String.class).asc())
    .limit(50)
    .build();
```

### Compare

같은 구조를 더 명시적으로 보여준다.

| Concern | String SQL | clickhouse-dsl |
|--------|------------|----------------|
| Alias consistency | 사람이 맞춘다 | expression 단위로 alias 고정 |
| Nested function readability | 괄호를 눈으로 센다 | expression 트리로 조립 |
| Parameter placement | 순서를 사람이 기억 | renderer가 placeholder 순서 보존 |
| Sort / group references | 문자열 참조 | typed reference expression |
| Regression testing | raw SQL snapshot만 가능 | DSL + SQL snapshot 둘 다 가능 |

이 예제는 [`SampleAggregationQueriesTest.java`](./src/test/java/io/github/chang/clickhousedsl/samples/SampleAggregationQueriesTest.java) 와 같은 계열의 검증을 위해 쓰는 패턴이다. 즉, DSL이 예쁘게 보이는지만 확인하는 게 아니라, 기존 문자열 SQL과 논리적으로 같은 SQL을 꾸준히 내는지 테스트로 고정한다.

## Sample Cases

복잡한 샘플은 [`src/test/java/io/github/chang/clickhousedsl/samples`](./src/test/java/io/github/chang/clickhousedsl/samples) 아래에 모은다.

의도는 두 가지다.

1. README 대표 예제는 대비형으로 짧고 선명하게 유지
2. 실제 복잡도에 가까운 조회/집계 케이스는 샘플 테스트로 축적

샘플 작성 원칙:

- 회사 쿼리에서 구조만 차용하고 이름은 전부 중립화
- 절대 경로, 사내 스키마, 내부 서비스명 금지
- “동일 SQL 렌더링” 또는 “동일한 논리 구조”를 테스트로 고정

현재 샘플:

- `SampleAggregationQueriesTest`
  - 집계 + alias + 함수 중첩
  - README before/after와 연결되는 공개용 샘플
- `DynamicQuerySamplesTest`
  - 조건 유무에 따라 where/order/limit이 달라지는 동적 조회 샘플
  - annotation 기반 native query에서 특히 지저분해지는 케이스를 의도

## Validation Philosophy

compile-time으로 최대한 잡되, 과장하지 않는다.

현재 방향:

- DSL 단계 순서 제약
- join key 타입 mismatch의 많은 경우
- aggregate / group-by 일부 가드
- `ARRAY JOIN` 입력 타입 검증
- `UNION` 컬럼 수 / 타입 위치 검증

semantic analyzer가 맡는 것:

- 복잡한 aggregate legality
- alias scope
- ClickHouse 함수 타입 규칙의 나머지
- 더 깊은 semantic edge case

## Security

- identifier는 안전한 패턴만 허용
- 값은 placeholder `?` 로 렌더링
- raw SQL fragment API는 아직 제공하지 않음

이건 완전한 보안 보장이 아니라, 문자열 조립보다 훨씬 덜 위험한 기본값을 주는 방향이다.

## Development

```bash
./gradlew test
./gradlew check
```

## Next Real Check

이 라이브러리는 결국 실제 프로젝트에 붙여봐야 한다.

권장 순서:

1. 기존 실서비스/배치 프로젝트에서 대표 ClickHouse 쿼리 3~5개를 고른다
2. 같은 쿼리를 `clickhouse-dsl`로 재작성한다
3. 렌더링 SQL이 기존 SQL과 논리적으로 동일한지 비교한다
4. 실제 ClickHouse에서 결과 row set과 성능 특성을 비교한다
5. 차이가 나면 DSL, renderer, analyzer 중 어디가 틀렸는지 수정한다

이 단계가 지나야 이 프로젝트가 장난감이 아니라 실제 라이브러리가 된다.
