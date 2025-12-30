@file:Suppress("ktlint:standard:filename")

package org.helllabs.android.xmp.di

import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.serializers.ImmutableListSerializer
import org.koin.dsl.module

val appModule = module {
    single { PrefManager(context = get(), json = get()) }
    single { StorageManager(context = get(), prefManager = get()) }
    single {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            serializersModule = SerializersModule {
                contextual(ImmutableList::class) { args ->
                    ImmutableListSerializer(args[0])
                }
            }
        }
    }
}
