package com.zobaer53.zedmovies.data.network.serializer

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LocalDateSerializerTest {
    @Serializable
    private data class Holder(@Serializable(LocalDateSerializer::class) val date: LocalDate?)

    private val json = Json

    @Test
    fun `valid ISO date is parsed`() {
        assertEquals(Holder(LocalDate(2024, 2, 29)), json.decodeFromString<Holder>("""{"date":"2024-02-29"}"""))
    }

    @Test
    fun `empty string is null`() {
        // TMDB sends "" for unknown release dates.
        assertNull(json.decodeFromString<Holder>("""{"date":""}""").date)
    }

    @Test
    fun `invalid date fails with a SerializationException`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<Holder>("""{"date":"2024-13-40"}""")
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<Holder>("""{"date":"not a date"}""")
        }
    }

    @Test
    fun `date round-trips through its ISO string`() {
        val holder = Holder(LocalDate(1999, 12, 31))
        assertEquals("""{"date":"1999-12-31"}""", json.encodeToString(Holder.serializer(), holder))
        assertEquals(holder, json.decodeFromString<Holder>(json.encodeToString(Holder.serializer(), holder)))
    }
}
