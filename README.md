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

이 예제는 [`SampleQuerySnapshotTest.java`](./src/test/java/io/github/chang/clickhousedsl/api/SampleQuerySnapshotTest.java) 와 같은 계열의 검증을 위해 쓰는 패턴이다. 즉, DSL이 예쁘게 보이는지만 확인하는 게 아니라, 기존 문자열 SQL과 논리적으로 같은 SQL을 꾸준히 내는지 테스트로 고정한다.

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
