# 작업 규칙 (Git · PR)

1인 개발이지만 이슈 → 브랜치 → PR 순서를 지켜서 무엇을 왜 바꿨는지 기록을 남긴다.

## 1. 흐름

- `작업 브랜치 → dev → main`. **main에 직접 push하지 않는다.** main 반영도 PR로 한다.
- 모든 작업 브랜치는 `dev`에서 딴다. 포크하지 않고 같은 저장소에 브랜치를 만든다.
- 이슈를 먼저 만들고, 그 번호로 브랜치·커밋·PR을 묶는다. 이슈 템플릿은 Feature, Bug, Question.

## 2. 브랜치 이름

- `feat/<kebab-case>`, `fix/<kebab-case>` (예: `feat/station-stay-time`, `fix/midnight-last-train`)

## 3. 커밋

형식: `[#이슈번호]- 유형: 설명` · 유형 `feat` `fix` `refactor` `docs` `chore`

예: `[#12]- feat: 역별 머물 수 있는 시간 계산`

- 한 커밋에 한 가지 일. **빌드가 통과하는 지점**에서 끊는다.
- 이름 바꾸기와 값 바꾸기를 섞지 않는다. 이름만 바꾼 커밋에서 동작 변화가 없음을 먼저 확인한다.
- 의존성을 바꾸면 `gradle/libs.versions.toml`과 모듈의 `build.gradle.kts`를 **같은 커밋에** 넣는다.
- 삭제는 "사용처 확인 → 삭제 → 빌드·테스트" 순서.

## 4. PR

- base는 `dev`. 본문에 `Closes #이슈번호`를 쓰고 PR 템플릿을 채운다.
- 기본 브랜치는 `main`이라 PR을 만들면 base가 `main`으로 잡힌다. **base를 `dev`로 바꿨는지 꼭 확인한다.**
- `Closes #`는 **기본 브랜치(`main`)에 머지될 때만** 이슈를 자동으로 닫는다. 작업 PR은 `dev`로 머지하므로 그때는 닫히지 않는다. `[Release]` PR 본문에 포함된 이슈를 `Closes #번호`로 모아 적어 `main` 승격 때 한꺼번에 닫는다.
- PR은 작게. 사용처가 많은 변경(예: 공통 토큰·이름 치환)은 따로 뺀다.
- 본문에 **알려진 한계**를 적고 후속 이슈로 닫는다.
- 쌓아 올린 PR은 아래부터 순서대로 머지한다. 롤백은 `git revert`.
- 1인 개발이라 승인 리뷰는 없다. 대신 머지 전에 Files changed를 처음부터 끝까지 한 번 읽고, 고민한 부분은 셀프 리뷰 코멘트로 남긴다.

## 5. main 승격

- **1주일 단위, 스프린트가 끝날 때.** `dev`에 모은 기능을 `[Release] …` PR로 `main`에 올린다. 발표 전에는 반드시 한 번 승격한다.
- 본문에 포함된 PR 번호를 나열하고, 그 PR들이 닫는 이슈를 `Closes #번호`로 모아 적는다.
- 머지 전에 배포 환경변수를 먼저 갱신한다(서버 주소, 키 등 GitHub Actions Secrets).
- `main`이 `dev`보다 크게 뒤처지면 시연 환경이 최신이 아니게 되므로 주기를 지킨다.

## 6. PR 검사 (CI)

- `dev`·`main` 대상 PR에서 `./gradlew lint testDebugUnitTest assembleDebug`를 자동 실행한다.
- 워크플로(`.github/workflows/`)는 Gradle 프로젝트를 만든 뒤 추가한다.

## 7. 저장소 설정 (첫날)

- 기본 브랜치: `main` (GitHub Pages와 시연 환경이 보는 브랜치)
- `dev`·`main` 보호 규칙: PR 필수, 검사 통과 필수(Require status checks to pass), 강제 push·삭제 금지. 1인 개발이라 필수 승인 수는 0.
- 비밀값은 저장소에 올리지 않는다. 키는 `local.properties`·`secrets.properties`에 두고, CI에서 필요한 값은 GitHub Actions Secrets에 둔다. 서명 키(`*.jks`)는 저장소 밖에 백업한다.
- 포맷터: 첫 주에 정한다(후보: ktlint 또는 Spotless + ktfmt).
