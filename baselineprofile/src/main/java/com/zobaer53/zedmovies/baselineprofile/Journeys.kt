package com.zobaer53.zedmovies.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val PACKAGE_NAME = "com.zobaer53.zedmovies"

private const val NETWORK_TIMEOUT_MS = 30_000L
private const val UI_TIMEOUT_MS = 10_000L

// Compose exposes both text and content descriptions; Maestro's text matcher checks both too.
private fun label(text: String): BySelector = By.text(text)

private fun description(text: String): BySelector = By.desc(text)

private fun MacrobenchmarkScope.waitFor(selector: BySelector, timeout: Long = UI_TIMEOUT_MS): UiObject2 =
    checkNotNull(device.wait(Until.findObject(selector), timeout)) { "Not found on screen: $selector" }

private fun MacrobenchmarkScope.anyOf(vararg selectors: BySelector, timeout: Long = UI_TIMEOUT_MS): UiObject2 {
    val deadline = System.currentTimeMillis() + timeout
    while (System.currentTimeMillis() < deadline) {
        selectors.firstNotNullOfOrNull { device.findObject(it) }?.let { return it }
        device.waitForIdle()
    }
    error("None of these found on screen: ${selectors.joinToString()}")
}

/** Home is ready once the TMDB lists have loaded and the first ratings show. */
internal fun MacrobenchmarkScope.waitForHome() {
    waitFor(label("Upcoming Movies"), NETWORK_TIMEOUT_MS)
    anyOf(label("Rating"), description("Rating"), timeout = NETWORK_TIMEOUT_MS)
}

/** Home: swipe the upcoming pager, scroll the feed down and back up. */
internal fun MacrobenchmarkScope.browseHome() {
    val width = device.displayWidth
    val pagerY = device.displayHeight * 22 / 100
    repeat(2) {
        device.swipe(width * 80 / 100, pagerY, width * 20 / 100, pagerY, 20)
        device.waitForIdle()
    }
    val feed = device.findObject(By.scrollable(true))
    feed?.fling(Direction.DOWN)
    device.waitForIdle()
    feed?.fling(Direction.UP)
    device.waitForIdle()
}

/** Details of the first movie: open it, scroll the cast and crew, go back. */
internal fun MacrobenchmarkScope.openDetails() {
    anyOf(label("Rating"), description("Rating"), timeout = NETWORK_TIMEOUT_MS).click()
    waitFor(label("Overview"), NETWORK_TIMEOUT_MS)
    device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
    device.waitForIdle()
    device.pressBack()
    waitFor(label("Upcoming Movies"))
}

/** Search tab: discover lists, the filter sheet, then the wish list tab. */
internal fun MacrobenchmarkScope.browseSearchAndWishlist() {
    // An unselected bottom-bar tab shows only its icon; its name is the content description.
    anyOf(label("Search"), description("Search")).click()
    waitFor(label("Discover"), NETWORK_TIMEOUT_MS)
    anyOf(description("Filters"), label("Filters")).click()
    waitFor(label("Show results"))
    device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
    device.waitForIdle()
    device.pressBack()
    waitFor(label("Discover"))
    anyOf(label("Wish list"), description("Wish list")).click()
    device.waitForIdle()
}
