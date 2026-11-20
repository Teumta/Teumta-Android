package com.teumta.app.ui.input

import com.teumta.app.domain.model.Station

// 검색어와 맞는 역. 이름의 start부터 length 글자가 맞았다(화면에서 그 부분만 색을 바꾼다)
data class StationMatch(val station: Station, val start: Int, val length: Int)

// 역 이름으로 찾는다. 글자를 그대로 쳐도 되고 초성만 쳐도 된다. 섞어도 된다.
// 예: "구로" → 구로, "ㄱㄹ" → 구로, "가ㅅ" → 가산디지털단지
// 이름 앞부분부터 맞는 역을 먼저, 그다음은 노선 순서(order)대로 보여 준다
fun searchStations(stations: List<Station>, query: String): List<StationMatch> {
    val q = query.filterNot { it.isWhitespace() }
    if (q.isEmpty()) return emptyList()
    return stations
        .mapNotNull { station -> matchStart(station.name, q)?.let { StationMatch(station, it, q.length) } }
        .sortedWith(compareBy({ it.start != 0 }, { it.station.order }))
}

// 이름에서 검색어가 처음 맞는 자리. 없으면 null
private fun matchStart(name: String, q: String): Int? =
    (0..name.length - q.length).firstOrNull { start ->
        q.indices.all { i -> charMatches(name[start + i], q[i]) }
    }

// 검색어 글자가 초성(ㄱ~ㅎ)이면 이름 글자의 초성과 비교하고, 아니면 글자끼리 비교한다
private fun charMatches(nameChar: Char, queryChar: Char): Boolean =
    if (queryChar in 'ㄱ'..'ㅎ') choseongOf(nameChar) == queryChar else nameChar == queryChar

// 한글 음절의 초성. 음절은 (초성 × 588) + (중성 × 28) + 종성 순서로 '가'부터 놓여 있다
internal fun choseongOf(c: Char): Char? =
    if (c in '가'..'힣') CHOSEONG[(c - '가') / 588] else null

private const val CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
