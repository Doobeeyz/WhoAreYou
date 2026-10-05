package com.example.whoareyou.settings

import androidx.datastore.core.DataStore
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val key = booleanPreferencesKey("message_voiceover")

fun getAnnounceTextFlow(context: Context): Flow<Boolean>{
    return context.dataStore.data.map{preferences -> preferences[key] ?: false}
}

suspend fun setAnnounceText(context: Context, value: Boolean){
    context.dataStore.edit{preferences -> preferences[key] = value}
}