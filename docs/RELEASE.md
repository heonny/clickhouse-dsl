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
- README와 외부에 노출되는 버전 정보가 release 버전 기준으로 정리되어 있음
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
- [`../build.gradle`](../build.gradle)의 버전과 README의 dependency 예시가 현재 release 버전과 일치
- README에서 노출되는 버전 정보가 현재 release 버전과 일치
  - 동적 배지를 쓴다면 Maven Central 반영 상태까지 확인
- 검증 후 작업 트리에 의도하지 않은 변경이 남지 않음

## 릴리즈 절차

예시 버전이 `0.1.5` 인 경우:

```bash
git checkout main
git pull --ff-only origin main
```

[`../build.gradle`](../build.gradle)의 버전을 `0.1.5`로 올린 뒤:

```bash
rg -n "0\\.1\\.4|0\\.1\\.5" README.md README.en.md docs build.gradle .github
```

위 검색 결과를 보고 아래를 모두 확인한다.

- 이전 버전 문자열이 남아 있지 않음
- 새 버전 문자열이 의도한 파일에만 존재함
- README, 문서, build metadata가 같은 버전을 가리킴

```bash
./gradlew clean check
./gradlew publishToMavenLocal
git add build.gradle README.md README.en.md docs
git commit -m "chore: 0.1.5 릴리즈 준비"
git tag v0.1.5
git push origin main
git push origin v0.1.5
```

주의:

- tag는 version 정합성 확인과 검증이 끝난 뒤 마지막에 만든다.
- 이미 push된 release tag는 수정하거나 재사용하지 않는다.
- release 버전을 잘못 올렸다면 기존 tag를 움직이지 말고 다음 patch 버전으로 다시 준비한다.
- 동적 버전 배지를 쓰는 경우 release 직후 Maven Central 반영 전까지 표시가 늦을 수 있다.

## GitHub Actions 동작

- [`../.github/workflows/release.yml`](../.github/workflows/release.yml)은 `v*` tag push만 감지한다.
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
- tag를 먼저 만들고 이후에 version 파일이나 README를 수정하지 않는다.
- release 직전에는 반드시 `rg`로 이전/새 버전 문자열을 전역 검색해 누락 여부를 확인한다.
- release-only 운영 기준이므로 `-SNAPSHOT` 사용을 전제로 한 문서나 workflow를 추가하지 않는다.
