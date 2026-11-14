package com.zobaer53.zedmovies.ios

/** Implemented in Swift by iosApp/iosApp/Playback/PlayerPresenter.swift. Called on the main thread. */
interface IosPlaybackHost {
    /** Presents the full-screen web player for [request] over the current screen. */
    fun presentPlayer(request: IosPlayerRequest)
}

/** Everything the Swift player needs. IosMediaLauncher builds it from a PlayRequest. */
class IosPlayerRequest(
    /** buildStreamUrl(tmdbId, type). */
    val url: String,
    /** "<name> <year>" for the player's accessibility label. */
    val title: String,
    /** STREAM_DOCUMENT_START_SCRIPT: WKUserScript at .atDocumentStart, main frame only. */
    val documentStartScript: String,
    /** STREAM_PAGE_SCRIPT: WKUserScript at .atDocumentEnd, main frame only, and re-run on didFinish (idempotent). */
    val pageScript: String,
)
