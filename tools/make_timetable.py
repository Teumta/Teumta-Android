"""
서울역~인천 일반열차 시간표를 앱 assets용 JSON으로 만든다.

사용법 (Teumta-Android 폴더에서):
    python3 tools/make_timetable.py

원본: tools/data/ (2026-11-06에 TAGO 지하철정보로 받아 열차 단위로 이어 붙인 결과)
결과: app/src/main/assets/timetable.json
"""
import json
from pathlib import Path

BASE_DATE = "2026-11-06"
TOOLS_DIR = Path(__file__).resolve().parent
TRIPS_FILE = TOOLS_DIR / "data/trips.json"
STATIONS_FILE = TOOLS_DIR / "data/stations.json"
OUTPUT = TOOLS_DIR.parent / "app/src/main/assets/timetable.json"

# 서울역 → 인천 구간 (29개 역)
SEGMENT = [
    "서울역", "남영", "용산", "노량진", "대방", "신길", "영등포", "신도림", "구로",
    "구일", "개봉", "오류동", "온수", "역곡", "소사", "부천", "중동", "송내", "부개",
    "부평", "백운", "동암", "간석", "주안", "도화", "제물포", "도원", "동인천", "인천",
]

ORDER = {name: i for i, name in enumerate(SEGMENT)}
GYEONGIN = set(SEGMENT[SEGMENT.index("구일"):])  # 경인선 구간 (구일~인천)

# TAGO 운행일 구분: 토요일(02) 데이터가 없어 토요일은 앱에서 HOLIDAY로 처리
DAY_TYPE = {"01": "WEEKDAY", "03": "HOLIDAY"}
DIRECTION = {"S": "DOWN", "N": "UP"}  # S: 인천 방면, N: 서울역 방면


def to_seconds(time):
    # '07:11', '07:11:30', '24:10' → 운행일 0시 기준 초 (자정 이후는 86400 이상)
    parts = list(map(int, time.split(":")))
    return parts[0] * 3600 + parts[1] * 60 + (parts[2] if len(parts) > 2 else 0)


def is_express(names):
    # 연속된 두 정차역이 구간에서 이웃하지 않으면(사이 역을 건너뛰면) 급행으로 판별
    return any(abs(ORDER[a] - ORDER[b]) != 1 for a, b in zip(names, names[1:]))


def check_train(train, station_order):
    # 만든 열차가 앱 계산에 쓰기에 이상하지 않은지 확인한다. 문제가 있으면 바로 멈춘다
    stops = train["stops"]
    times = [t for s in stops for t in (s["arr"], s["dep"])]
    if any(later < earlier for earlier, later in zip(times, times[1:])):
        raise ValueError(f"{train['id']}: 시각이 거꾸로 간다")

    orders = [station_order[s["stationId"]] for s in stops]
    going_down = orders[1] > orders[0]
    if going_down != (train["direction"] == "DOWN"):
        raise ValueError(f"{train['id']}: 방향({train['direction']})과 정차 순서가 다르다")


def main():
    groups = json.loads(TRIPS_FILE.read_text(encoding="utf-8"))
    station_ids = json.loads(STATIONS_FILE.read_text(encoding="utf-8"))

    missing = [name for name in SEGMENT if name not in station_ids]
    if missing:
        raise SystemExit(f"stations.json에 없는 역: {missing}")

    stations = [
        {"id": station_ids[name], "name": name, "order": i + 1}
        for i, name in enumerate(SEGMENT)
    ]
    station_order = {s["id"]: s["order"] for s in stations}  # 역 ID → 구간 순서

    trains_by_key = {}  # (dayType, direction) → 열차 목록

    for group in groups:
        day_type = DAY_TYPE.get(group["day"])
        if day_type is None:
            continue

        direction = DIRECTION[group["orient"]]

        for trip in group["trips"]:
            # stop = [역 이름, 도착, 출발, 통과(1이면 서지 않고 지나침)]
            stops = trip["stops"]

            # 구간 안에서 통과하는 역이 있으면 급행이라 제외
            if any(s[3] == 1 and s[0] in ORDER for s in stops):
                continue

            stops = [s for s in stops if s[3] == 0 and s[0] in ORDER]
            names = [s[0] for s in stops]

            # 정차역이 2개 미만, 경인선을 지나지 않음(천안·신창행 등), 급행이면 제외
            if len(stops) < 2 or not GYEONGIN.intersection(names) or is_express(names):
                continue

            trains_by_key.setdefault((day_type, direction), []).append({
                "dayType": day_type,
                "direction": direction,
                "headsign": trip["end"],
                "stops": [
                    {
                        "stationId": station_ids[s[0]],
                        "arr": to_seconds(s[1]),
                        "dep": to_seconds(s[2]),
                    }
                    for s in stops
                ],
            })

    # 첫 정차역 출발 시각 순으로 정렬하고 ID를 붙인다 (원본은 출발 순서가 아닌 곳이 있다)
    trains = []
    for (day_type, direction), group_trains in sorted(trains_by_key.items()):
        group_trains.sort(key=lambda t: t["stops"][0]["dep"])
        for n, train in enumerate(group_trains, start=1):
            train = {"id": f"{day_type}-{direction}-{n:03d}", **train}
            check_train(train, station_order)
            trains.append(train)

    # 앱 assets 디렉터리에 시간표 JSON 저장
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    result = {
        "baseDate": BASE_DATE,
        "stations": stations,
        "trains": trains,
    }

    OUTPUT.write_text(
        json.dumps(result, ensure_ascii=False, indent=1),
        encoding="utf-8",
    )

    print(f"역 {len(stations)}개, 열차 {len(trains)}편 → {OUTPUT}")

    for (day_type, direction), group_trains in sorted(trains_by_key.items()):
        first = min(t["stops"][0]["dep"] for t in group_trains)
        last = max(t["stops"][0]["dep"] for t in group_trains)
        print(f"  {day_type} {direction}: {len(group_trains)}편 "
              f"(첫 출발 {first // 3600:02d}:{first // 60 % 60:02d}, 마지막 출발 {last // 3600:02d}:{last // 60 % 60:02d})")


if __name__ == "__main__":
    main()
