package com.teumta.app.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 앱의 입력 검사가 서버(SignupRequest)와 같은 값을 받고 같은 값을 거절하는지
class AccountRulesTest {

    @Test
    fun 이메일은_앞뒤_빈칸을_빼고_검사한다() {
        assertNull(AccountRules.emailError(" me@example.com "))
        assertEquals("이메일을 입력해 주세요.", AccountRules.emailError("   "))
        assertEquals("이메일 형식이 아니에요.", AccountRules.emailError("me@example"))
        assertEquals("이메일 형식이 아니에요.", AccountRules.emailError("me example@a.com"))
        assertEquals("이메일이 너무 길어요.", AccountRules.emailError("a".repeat(250) + "@a.com"))
    }

    @Test
    fun 비밀번호는_영문_숫자_기호_8자에서_64자() {
        assertNull(AccountRules.passwordError("abcd1234"))
        assertNull(AccountRules.passwordError("P@ss!" + "x".repeat(59))) // 64자
        assertEquals("비밀번호를 입력해 주세요.", AccountRules.passwordError(""))
        assertEquals("비밀번호는 영문·숫자·기호 8~64자예요.", AccountRules.passwordError("abc1234")) // 7자
        assertEquals("비밀번호는 영문·숫자·기호 8~64자예요.", AccountRules.passwordError("x".repeat(65)))
        assertEquals("비밀번호는 영문·숫자·기호 8~64자예요.", AccountRules.passwordError("비밀번호12345678")) // 한글
        assertEquals("비밀번호는 영문·숫자·기호 8~64자예요.", AccountRules.passwordError("pass word1")) // 빈칸
    }

    @Test
    fun 비밀번호_확인은_두_칸이_똑같아야_한다() {
        assertNull(AccountRules.passwordConfirmError("abcd1234", "abcd1234"))
        assertEquals("비밀번호를 한 번 더 입력해 주세요.", AccountRules.passwordConfirmError("abcd1234", ""))
        assertEquals("비밀번호가 서로 달라요.", AccountRules.passwordConfirmError("abcd1234", "abcd1235"))
        // 대소문자도 다른 글자로 본다(서버도 그대로 비교한다)
        assertEquals("비밀번호가 서로 달라요.", AccountRules.passwordConfirmError("abcd1234", "ABCD1234"))
    }

    @Test
    fun 로그인은_빈칸만_막는다() {
        assertNull(AccountRules.loginEmailError("me"))
        assertNull(AccountRules.loginPasswordError("short"))
        assertEquals("이메일을 입력해 주세요.", AccountRules.loginEmailError(" "))
        assertEquals("비밀번호를 입력해 주세요.", AccountRules.loginPasswordError(""))
    }
}
