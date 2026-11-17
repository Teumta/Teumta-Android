package com.teumta.app.data

import com.teumta.app.domain.model.Timetable
import java.io.File

// 앱 안 시간표(assets/line1, 서버 응답을 그대로 저장한 것)를 테스트에서 읽는다.
// 읽는 데 시간이 걸려서 처음 쓸 때 한 번만 읽고(lazy), 여러 테스트가 같이 쓴다
object Line1Assets {
    val timetable: Timetable by lazy {
        TimetableParser.parseServer(
            stationsText = read("stations.json"),
            trainsTexts = listOf(read("trains-weekday.json"), read("trains-holiday.json")),
        )
    }

    // 단위 테스트는 app 모듈 폴더에서 실행돼서 상대 경로로 읽는다
    private fun read(name: String): String = File("src/main/assets/line1/$name").readText()
}
