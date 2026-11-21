package com.teumta.app.ui.account

import androidx.compose.ui.text.intl.LocaleList

// 가입·로그인 입력 규칙. 서버(SignupRequest)와 같은 규칙을 앱에서 먼저 검사해서,
// 서버까지 가지 않고 바로 어느 칸이 틀렸는지 알려 준다. 서버도 다시 검사하므로 앱 검사는 편의일 뿐이다
object AccountRules {

    fun emailError(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "이메일을 입력해 주세요."
            trimmed.length > 254 -> "이메일이 너무 길어요."
            !EMAIL.matches(trimmed) -> "이메일 형식이 아니에요."
            else -> null
        }
    }

    // 영문·숫자·기호 8~64자. 서버의 BCrypt가 72바이트까지만 써서 한글(글자당 3바이트)과 빈칸은 받지 않는다
    fun passwordError(password: String): String? = when {
        password.isEmpty() -> "비밀번호를 입력해 주세요."
        !PASSWORD.matches(password) -> "비밀번호는 영문·숫자·기호 8~64자예요."
        else -> null
    }

    // 가입할 때 비밀번호를 한 번 더 쳐서 오타를 막는다. 서버에는 보내지 않는 앱만의 검사다
    fun passwordConfirmError(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "비밀번호를 한 번 더 입력해 주세요."
        confirm != password -> "비밀번호가 서로 달라요."
        else -> null
    }

    // 로그인은 빈칸만 막는다(서버 LoginRequest와 같다). 형식은 가입 때 이미 검사했다
    fun loginEmailError(email: String): String? = if (email.isBlank()) "이메일을 입력해 주세요." else null

    fun loginPasswordError(password: String): String? = if (password.isEmpty()) "비밀번호를 입력해 주세요." else null

    // 골뱅이 앞뒤에 글자가 있고, 뒤쪽에 점이 하나 이상 있으면 이메일로 본다(진짜 주소인지는 알 수 없다)
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val PASSWORD = Regex("^[\\x21-\\x7E]{8,64}$")
}

// 이메일·비밀번호 칸은 영어 키보드를 먼저 띄워 달라고 키보드 앱에 알린다.
// 한국어 키보드로 치면 "ㅡㄷ@..."처럼 자모가 들어간다(키보드 앱이 영어를 지원할 때만 바뀐다)
val EnglishKeyboard = LocaleList("en")
