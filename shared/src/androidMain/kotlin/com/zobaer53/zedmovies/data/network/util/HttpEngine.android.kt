package com.zobaer53.zedmovies.data.network.util

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun httpEngine(): HttpClientEngine = OkHttp.create()
