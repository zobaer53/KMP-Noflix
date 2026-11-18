package com.zobaer53.zedmovies.data.network.util

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

internal actual fun httpEngine(): HttpClientEngine = Js.create()
