package com.defusername.standby.data.local

import androidx.datastore.core.Serializer
import com.defusername.standby.domain.model.AppSettings
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer : Serializer<AppSettings> {

    override val defaultValue: AppSettings = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            Json.decodeFromString(AppSettings.serializer(), input.readBytes().decodeToString())
        } catch (_: SerializationException) {
            defaultValue
        } catch (_: IllegalArgumentException) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        output.write(Json.encodeToString(AppSettings.serializer(), t).toByteArray())
    }
}
