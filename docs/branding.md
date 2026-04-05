# Branding

## Overview

`clickhouse-dsl`의 브랜드는 "ClickHouse 로고를 닮은 라이브러리"를 만드는 데 목적이 있지 않습니다.

핵심은 더 명확합니다.

- ClickHouse 쿼리를 문자열이 되기 전에 구조로 다룹니다.
- 가능한 범위의 실수를 compile-time guardrail과 validation으로 앞당깁니다.
- 최종적으로는 읽기 쉽고 안전한 SQL 문자열을 만듭니다.

즉, 이 프로젝트의 정체성은 데이터베이스 그 자체보다 `typed query structure`에 더 가깝습니다.

## Logo Philosophy

메인 로고는 세 요소로 읽히도록 설계했습니다.

### 1. Bracket frame

바깥 bracket은 쿼리를 문자열이 아니라 구조화된 프레임 안에서 다룬다는 뜻입니다.

이 프로젝트가 단순 문자열 템플릿이 아니라, 조립 가능한 DSL이라는 점을 가장 먼저 보여주는 요소입니다.

### 2. 3x3 dot grid

가운데 3x3 점 패턴은 ClickHouse를 직접 복사하지 않고 연상시키는 셀 그리드입니다.

이 패턴은 아래 의미를 같이 담습니다.

- columnar / analytical database의 셀 감각
- 쿼리 노드가 구조화된 블록으로 쌓인다는 느낌
- 작게 줄였을 때도 식별 가능한 단순성

### 3. Three accent points

포인트 색은 의도적으로 세 단계로 나뉩니다.

- 좌상단 레몬 옐로: 시작점, 입력, 첫 진입
- 중앙 브라이트 오렌지: 가장 강한 활성 포인트, 현재 중심
- 우하단 자몽 계열: 결과 쪽으로 흐르는 마무리 포인트

이 셋은 단순 장식이 아니라, 쿼리가 구조 안에서 만들어지고 정리되어 결과로 향하는 흐름을 암시합니다.

## Color Direction

기본 팔레트는 ClickHouse를 연상시키는 밝은 노랑과 오렌지 계열을 사용하되, 전체는 너무 시끄럽지 않게 중립 회색과 차콜 선으로 잡습니다.

- bracket / wordmark: 차콜
- neutral dots: 절제된 쿨 그레이
- accent dots: lemon, bright orange, grapefruit

이 방향은 "데이터베이스 툴"처럼 차갑거나, 반대로 "마케팅 장식"처럼 과한 느낌을 피하기 위한 선택입니다.

## Assets

- 메인 로고 락업: [`assets/logo.svg`](./assets/logo.svg)
- 아이콘 전용 자산: [`assets/icon.svg`](./assets/icon.svg)

## Usage Notes

- `logo.svg`는 README, 문서, 소개 페이지처럼 워드마크가 필요한 곳에 사용합니다.
- `icon.svg`는 favicon 변환, social preview 일부 요소, 앱 아이콘, 작은 배지처럼 아이콘만 필요한 곳에 사용합니다.
- 로고 간격과 포인트 색은 이미 작은 크기에서 렌더링 확인까지 마친 상태라, 이후 수정은 특별한 이유가 없으면 피하는 편이 좋습니다.
