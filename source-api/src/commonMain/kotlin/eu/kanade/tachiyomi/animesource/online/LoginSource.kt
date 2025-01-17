package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.AnimeSource

interface LoginSource : AnimeSource {
    val requiresLogin: Boolean

    val twoFactorAuth: AuthSupport

    fun isLogged(): Boolean

    fun getUsername(): String

    fun getPassword(): String

    suspend fun login(username: String, password: String, twoFactorCode: String?): Boolean = false

    suspend fun login(authCode: String): Boolean = false

    suspend fun logout(): Boolean

    enum class AuthSupport {
        NOT_SUPPORTED,
        SUPPORTED,
        REQUIRED,
    }
}
