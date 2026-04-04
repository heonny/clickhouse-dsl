# Versioning

## 목표

이 프로젝트는 Maven Central release-only 배포를 기준으로 운영한다.  
즉, 저장소 안의 버전은 "다음에 실제 배포 가능한 release 버전"을 뜻한다.

## 규칙

- 버전 형식은 `MAJOR.MINOR.PATCH` 를 사용한다.
- `-SNAPSHOT` 버전은 사용하지 않는다.
- Git tag는 항상 `v` prefix를 사용한다.
  - 예: `v0.1.1`
- Git tag 버전과 [build.gradle](/Users/chang/Documents/workspace/backend/clickhouse-dsl/build.gradle)의 `version` 값은 반드시 같아야 한다.

## 증가 기준

- `PATCH`
  - 기존 API를 유지한 버그 수정
  - 문서 보완
  - 내부 validation 보강
  - 샘플/테스트 추가
- `MINOR`
  - 새로운 DSL 기능 추가
  - 새로운 ClickHouse 문법 지원 추가
  - 하위 호환 가능한 API 확장
- `MAJOR`
  - 공개 API 호환성 파괴
  - DSL 계약 또는 렌더링 규칙의 큰 변경

## 예시

- `0.1.0` 첫 공개 release
- `0.1.1` bug fix, sample 보강, validation 보강
- `0.2.0` executor, explain fetch, 더 넓은 함수 타입 시스템 추가
- `1.0.0` 공개 API 안정화 선언

## 릴리즈 흐름

1. 다음 release 버전으로 [build.gradle](/Users/chang/Documents/workspace/backend/clickhouse-dsl/build.gradle)을 올린다.
2. 테스트와 문서를 정리한다.
3. commit 한다.
4. `v<version>` tag를 만든다.
5. `main`과 tag를 push 한다.
6. GitHub Actions가 Central release 배포를 수행한다.

자세한 실행 순서는 [RELEASE.md](/Users/chang/Documents/workspace/backend/clickhouse-dsl/RELEASE.md)를 따른다.
