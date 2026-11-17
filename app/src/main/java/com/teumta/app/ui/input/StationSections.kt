package com.teumta.app.ui.input

import com.teumta.app.domain.StationTree
import com.teumta.app.domain.model.Station

// 노선도를 갈림길에서 끊은 한 토막. branchFrom은 이 토막이 갈라져 나온 역이다(맨 처음 토막은 null)
data class StationSection(val branchFrom: Station?, val stations: List<Station>) {
    // 토막 위에 보여 줄 제목. 예: "구로에서 갈라짐 · 구일 ~ 인천", "금천구청에서 갈라짐 · 광명"
    val title: String
        get() {
            val range = if (stations.size == 1) stations.first().name else "${stations.first().name} ~ ${stations.last().name}"
            return if (branchFrom == null) range else "${branchFrom.name}에서 갈라짐 · $range"
        }
}

// 연천부터 나무를 따라 내려가며 토막으로 나눈다. 이웃 역이 하나면 같은 토막에 잇고, 갈라지는 역에서 끊는다.
// 갈래가 여럿이면 더 갈라지지 않는 짧은 갈래(광명, 서동탄)를 먼저 둔다. 그래야 갈라지는 역 바로 아래에 보인다
fun stationSections(tree: StationTree): List<StationSection> {
    val sections = mutableListOf<StationSection>()

    fun walk(start: Station, branchFrom: Station?) {
        val line = mutableListOf(start)
        var next = tree.childrenOf(start.id)
        while (next.size == 1) {
            line += next.single()
            next = tree.childrenOf(line.last().id)
        }
        sections += StationSection(branchFrom, line)
        // 갈라지는 역: 끝나는 갈래(이웃 역이 없는 역) 먼저, 그다음 순서(order)대로
        next.sortedWith(compareBy({ tree.childrenOf(it.id).isNotEmpty() }, { it.order }))
            .forEach { walk(it, branchFrom = line.last()) }
    }

    tree.roots.forEach { walk(it, branchFrom = null) }
    return sections
}
