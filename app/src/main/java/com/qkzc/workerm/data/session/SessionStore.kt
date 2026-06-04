package com.qkzc.workerm.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val SESSION_DATASTORE_NAME = "supervision_session"

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SESSION_DATASTORE_NAME,
)

interface SessionDataSource {
    val sessionFlow: Flow<LoginSession>

    suspend fun saveSession(session: LoginSession)

    suspend fun clearSession()
}

class SessionStore(context: Context) : SessionDataSource {

    private val appContext = context.applicationContext

    override val sessionFlow: Flow<LoginSession> = appContext.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            LoginSession(
                accessToken = preferences[Keys.accessToken].orEmpty(),
                refreshToken = preferences[Keys.refreshToken].orEmpty(),
                userId = preferences[Keys.userId].orEmpty(),
                userName = preferences[Keys.userName].orEmpty(),
                mobile = preferences[Keys.mobile].orEmpty(),
                userType = preferences[Keys.userType].orEmpty(),
                clientType = preferences[Keys.clientType].orEmpty(),
                roleName = preferences[Keys.roleName].orEmpty(),
                organizationName = preferences[Keys.organizationName].orEmpty(),
                projectId = preferences[Keys.projectId].orEmpty(),
                projectName = preferences[Keys.projectName].orEmpty(),
            )
        }

    override suspend fun saveSession(session: LoginSession) {
        appContext.sessionDataStore.edit { preferences ->
            preferences[Keys.accessToken] = session.accessToken
            preferences[Keys.refreshToken] = session.refreshToken
            preferences[Keys.userId] = session.userId
            preferences[Keys.userName] = session.userName
            preferences[Keys.mobile] = session.mobile
            preferences[Keys.userType] = session.userType
            preferences[Keys.clientType] = session.clientType
            preferences[Keys.roleName] = session.roleName
            preferences[Keys.organizationName] = session.organizationName
            preferences[Keys.projectId] = session.projectId
            preferences[Keys.projectName] = session.projectName
        }
    }

    override suspend fun clearSession() {
        appContext.sessionDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val userId = stringPreferencesKey("user_id")
        val userName = stringPreferencesKey("user_name")
        val mobile = stringPreferencesKey("mobile")
        val userType = stringPreferencesKey("user_type")
        val clientType = stringPreferencesKey("client_type")
        val roleName = stringPreferencesKey("role_name")
        val organizationName = stringPreferencesKey("organization_name")
        val projectId = stringPreferencesKey("project_id")
        val projectName = stringPreferencesKey("project_name")
    }
}
