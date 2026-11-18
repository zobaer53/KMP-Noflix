package com.zobaer53.zedmovies.data.database.converter

import com.zobaer53.zedmovies.data.database.model.common.Genre
import kotlin.test.Test
import kotlin.test.assertEquals

// Pins the stored format of genre lists. Genre is @Serializable (an enum without @SerialName encodes by
// name): Kotlin/Native has no reflective enum serializer, and rows written by the JVM must stay readable.
class ListConverterTest {
    private val converter = ListConverter()

    @Test
    fun genresAreStoredByEnumName() {
        assertEquals("[\"ACTION\"]", converter.genreListToString(listOf(Genre.ACTION)))
        assertEquals("[\"ACTION\",\"ACTION_ADVENTURE\"]", converter.genreListToString(listOf(Genre.ACTION, Genre.ACTION_ADVENTURE)))
    }

    @Test
    fun genresRoundTrip() {
        val genres = Genre.entries.toList()
        assertEquals(genres, converter.stringToGenreList(converter.genreListToString(genres)))
    }

    @Test
    fun rowsWrittenByTheJvmFallbackAreReadable() {
        assertEquals(listOf(Genre.ACTION, Genre.ACTION_ADVENTURE), converter.stringToGenreList("[\"ACTION\",\"ACTION_ADVENTURE\"]"))
    }

    @Test
    fun intAndStringListsRoundTrip() {
        assertEquals("[1,2,3]", converter.intListToString(listOf(1, 2, 3)))
        assertEquals(listOf(1, 2, 3), converter.stringToIntList("[1,2,3]"))
        assertEquals("[\"a\",\"b\"]", converter.stringListToString(listOf("a", "b")))
        assertEquals(listOf("a", "b"), converter.stringToStringList("[\"a\",\"b\"]"))
    }
}
