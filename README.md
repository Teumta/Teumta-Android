# 틈타 (Teumta-Android)

1호선에서 **도착해야 하는 시각**을 정하면, 늦어도 몇 시 열차를 타야 하는지와 가는 길에 들를 역에서 얼마나 머물 수 있는지를 거꾸로 계산해 주는 안드로이드 앱이다.

- 버전 0.3.1 (versionCode 4), Google Play 비공개 테스트 중
- 서버: [Teumta-Server](https://github.com/Teumta/Teumta-Server) · [개인정보처리방침](https://teumta.github.io/Teumta-Server/privacy.html) · [계정 삭제 안내](https://teumta.github.io/Teumta-Server/delete-account.html)

## 무엇을 하나

| 화면 | 하는 일 |
| --- | --- |
| 입력 | 출발역·들를 역(비워 둬도 된다)·도착역, 도착 시각, 들를 역에서 머무를 시간(0분~5시간 55분, 5분 단위)을 고른다. 마지막 입력을 기억한다 |
| 역 고르기 | 1호선 102개 역. 초성 검색(예: `ㄱㄹ` → 구로), 갈림길별 묶음 |
| 결과 | 늦어도 탈 열차와 출발 시각, 갈아타는 곳까지 그린 동선, 머무를 수 있는 최대 시간. 못 가면 대신 해 볼 일을 알려 준다 |
| 실시간 | 결과 동선의 타는 역마다 지금 오는 열차(당역·전역 등)를 서버에서 받아 보여 준다. 새로고침 버튼 |
| 로그인·설정 | 로그인은 선택이다. 로그인하면 자주 쓰는 두 역을 서버에 저장해 버튼 하나로 불러온다. 탈퇴, 방침 안내 |
| 업데이트 | Play에 새 버전이 있으면 앱 안에서 업데이트를 안내한다 |

계산은 휴대폰 안에서 한다. 서버는 역·시간표 데이터와 실시간 위치, 회원 기능만 맡는다.

## 기술 스택

| 분류 | 쓴 것 |
| --- | --- |
| 언어·빌드 | Kotlin 2.2.10, AGP 9.5.1, Gradle 버전 카탈로그(`gradle/libs.versions.toml`) |
| 화면 | Jetpack Compose(BOM 2026.02.01), Material 3, Navigation Compose(타입 안전 경로), core-splashscreen |
| 의존성 주입 | Hilt 2.60.1 + KSP |
| 저장 | DataStore Preferences (마지막 입력, 토큰, 방침 안내 본 버전) |
| 네트워크 | Retrofit 3, OkHttp 5, kotlinx.serialization |
| 기타 | Play In-App Update, R8(release) |
| SDK | minSdk 26, targetSdk·compileSdk 37 |

## 구조

```
app/                      앱 모듈
  data/                   시간표 받기(서버 → 실패하면 앱 안 시간표), 마지막 입력 저장
    account/              로그인·회원가입 API, 토큰 저장과 자동 갱신
    realtime/             실시간 도착 API
  di/                     Hilt 모듈 (네트워크, DataStore, 저장소)
  domain/                 계산: 도착 시각 기준 역산, 갈아타기 경로 찾기, 운행일
    model/                역·열차·시간표 모델
  ui/
    input/                입력 화면, 역 고르기, 시각·머무를 시간 시트(바퀴 직접 구현), 동선 레일 그리기
    result/               결과 화면, 실시간 상자
    account/              로그인·회원가입·설정·자주 쓰는 두 역
    info/  update/  navigation/
  src/main/assets/line1/  앱 안 시간표(역, 평일, 휴일). 서버 응답과 같은 모양
core/designsystem/        색·글꼴(Pretendard)·모양·간격 토큰과 공통 컴포넌트
tools/                    v0.1.0 때 시간표 원본(TAGO)을 앱 JSON으로 바꾸던 스크립트
```

### 계산 방법

- **도착 시각 기준 역산**: "몇 시까지 도착"에서 거꾸로 거슬러 올라가, 늦어도 탈 수 있는 열차를 찾는다.
- **갈아타기**: Connection Scan Algorithm(`domain/JourneySearch.kt`). 모든 열차를 이웃한 두 역 사이 구간으로 잘라 시각 순으로 한 번 훑는다. 노선 모양을 몰라도 돼서 1호선처럼 갈라지는 노선도 그대로 계산된다.
- **운행일**: 시각을 "운행일 0시부터 몇 초"로 다룬다. 자정을 넘는 막차도 같은 날 운행으로 계산한다. 공휴일은 2027년까지 넣어 두었다.
- **시간표가 없을 때**: 서버에서 못 받으면 앱 안 시간표(기준일 2026-11-06)로 계산한다. 인터넷이 없어도 앱은 동작한다.

## 내 컴퓨터에서 실행

Android Studio로 폴더를 열고 `app`을 실행한다. 기본으로 배포한 서버(`https://teumta.duckdns.org/`)를 본다.

### 내 컴퓨터의 서버로 개발하려면

저장소 밖 `~/.gradle/gradle.properties`에 아래를 넣고,

```properties
teumta.apiBaseUrl=http://localhost:8080/
```

에뮬레이터·기기를 내 컴퓨터 포트와 잇는다(한 번만).

```bash
adb reverse tcp:8080 tcp:8080
```

- http localhost는 debug 빌드에서만 허락된다.
- Android 17부터 `10.0.2.2` 같은 로컬 네트워크 주소는 권한이 필요해서 `localhost`로 잇는다.

## 테스트

```bash
./gradlew testDebugUnitTest
```

단위 테스트 129개(v0.3.1). 역산·갈아타기 계산, 시간표 파싱, 서버 실패 시 앱 안 시간표로 바꾸기, 토큰 자동 갱신(MockWebServer), 실시간 응답 처리, 입력 규칙을 확인한다.

## release 빌드

서명 정보는 저장소에 올리지 않는 `keystore.properties`(루트, `.gitignore`)에서 읽는다. 파일이 없으면 서명하지 않은 release가 만들어진다.

```properties
storeFile=<키스토어 경로>
storePassword=<비밀번호>
keyAlias=<별칭>
keyPassword=<비밀번호>
```

```bash
./gradlew bundleRelease
```

- R8 줄이기 범위는 전체(`**`)로 둔다. 일부 패키지만 줄였더니 v0.2.0 준비 중 켜자마자 꺼졌다(`IllegalAccessError`).
- 키스토어(`*.jks`)는 저장소 밖에 백업한다. 잃어버리면 같은 앱으로 업데이트를 올릴 수 없다.

## 버전

| 태그 | 내용 |
| --- | --- |
| v0.1.0 | 서울역~인천 구간, 앱 안 시간표만으로 역산 (3일 출시안) |
| v0.2.0 | Hilt, 마지막 입력 기억, 큰 글꼴 대응, R8 크래시 수정 |
| v0.3.0 | 1호선 102개 역, 갈아타기, 서버 시간표, 로그인·자주 쓰는 두 역, 원래 기획 디자인 |
| v0.3.1 | 동선 속 실시간 열차, 회원가입 비밀번호 확인, 앱 안 업데이트 안내 |

## 작업 규칙

이슈 → 브랜치 → `dev`로 PR → 스프린트 끝에 `[Release]` PR로 `main`. 커밋은 `[#이슈]- 유형: 내용`. 자세한 건 [CONTRIBUTING.md](CONTRIBUTING.md).
