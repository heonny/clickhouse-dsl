# Release Checklist

## 목적

이 문서는 `clickhouse-dsl` release 배포를 반복 가능하게 유지하기 위한 운영 체크리스트다.

## 사전 조건

- GitHub repository secrets 등록 완료
  - `CENTRAL_PORTAL_TOKEN_USERNAME`
  - `CENTRAL_PORTAL_TOKEN_PASSWORD`
  - `SIGNING_IN_MEMORY_KEY`
  - `SIGNING_IN_MEMORY_KEY_PASSWORD`
- [`../build.gradle`](../build.gradle)의 `version` 값이 release 버전으로 설정되어 있음
- 작업 트리가 정리되어 있음

## 릴리즈 전 점검

```bash
./gradlew clean check
./gradlew publishToMavenLocal
```

확인할 것:

- 테스트 통과
- Javadoc 생성 성공
- jacoco coverage 기준 통과
- README의 dependency 예시가 현재 release 버전과 일치

## 릴리즈 절차

예시 버전이 `0.1.2` 인 경우:

```bash
git checkout main
git pull --ff-only origin main
```

[`../build.gradle`](../build.gradle)의 버전을 `0.1.2`로 올린 뒤:

```bash
./gradlew clean check
git add build.gradle README.md README.en.md docs
git commit -m "release: prepare 0.1.2"
git tag v0.1.2
git push origin main
git push origin v0.1.2
```

## GitHub Actions 동작

- [`../.github/workflows/release.yml`](../.github/workflows/release.yml)은 `v*` tag push를 감지한다.
- workflow는 아래 순서로 동작한다.
  1. JDK 17 설정
  2. tag 버전과 project 버전 일치 여부 검증
  3. `./gradlew clean check`
  4. `./gradlew releaseToCentral`

## 배포 후 확인

- GitHub Actions workflow 성공 여부 확인
- [Central Portal](https://central.sonatype.com) 에서 deployment 상태 확인
- Maven Central 검색 반영 확인
- 필요 시 GitHub Release note 작성

## 주의사항

- 이미 배포된 버전 tag를 다시 push하지 않는다.
- 같은 버전을 두 번 release 하지 않는다.
- tag만 만들고 `main`을 push하지 않으면 소스 상태와 배포 상태가 어긋날 수 있다.
- release-only 운영 기준이므로 `-SNAPSHOT` 사용을 전제로 한 문서나 workflow를 추가하지 않는다.
