package com.zobaer53.zedmovies.data.network.util

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun httpEngine(): HttpClientEngine = Darwin.create()
