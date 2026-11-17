package com.teumta.app.domain

import com.teumta.app.domain.model.Station

// 1호선 역들의 나무 모양. 역마다 parentId로 연천 쪽 이웃 역을 가리킨다(연천은 null).
// 1호선은 구로·금천구청·병점에서 갈라져서, 한 줄 순서(order)로는 "두 역 사이"를 알 수 없다.
// 예: 광명은 order가 101이지만 금천구청(68) 옆이다
data class StationTree(val stations: List<Station>) {

    private val byId: Map<String, Station> = stations.associateBy { it.id }

    fun find(id: String): Station? = byId[id]

    // from에서 to까지 지나는 역들. 양 끝을 넣고 from부터 순서대로. 어느 한쪽이 없으면 빈 목록
    fun path(fromId: String, toId: String): List<Station> {
        val fromUp = upToRoot(fromId) // from, from의 parent, ..., 연천
        val toUp = upToRoot(toId)
        val fromIds = fromUp.mapTo(HashSet()) { it.id }
        // to에서 올라가다 from의 길과 처음 만나는 역. 두 역이 갈라지는 곳이다
        val meet = toUp.indexOfFirst { it.id in fromIds }
        if (meet == -1) return emptyList()
        val meetId = toUp[meet].id
        // from -> 만나는 역 바로 앞(올라가기) + 만나는 역 -> to(내려가기)
        return fromUp.takeWhile { it.id != meetId } + toUp.take(meet + 1).asReversed()
    }

    // 역에서 parent를 따라 연천까지 올라간다
    private fun upToRoot(id: String): List<Station> =
        generateSequence(byId[id]) { station -> station.parentId?.let { byId[it] } }.toList()
}
