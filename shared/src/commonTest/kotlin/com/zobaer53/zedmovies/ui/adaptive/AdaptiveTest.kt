
package com.zobaer53.zedmovies.ui.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveTest {

    @Test
    fun onlyPhoneUsesTheCompactLayout() {
        assertTrue(FormFactor.Phone.isWide().not())
        assertTrue(FormFactor.Tablet.isWide())
        assertTrue(FormFactor.Tv.isWide())
        assertTrue(FormFactor.Desktop.isWide())
        assertTrue(FormFactor.Web.isWide())
    }

    @Test
    fun inputModeDefaultsFollowFormFactor() {
        assertEquals(InputMode.Touch, inputModeFor(FormFactor.Phone))
        assertEquals(InputMode.Touch, inputModeFor(FormFactor.Tablet))
        assertEquals(InputMode.Directional, inputModeFor(FormFactor.Tv))
        assertEquals(InputMode.Pointer, inputModeFor(FormFactor.Desktop))
        assertEquals(InputMode.Pointer, inputModeFor(FormFactor.Web))
    }

    @Test
    fun wideFormFactorsAreNotPhones() {
        assertFalse(FormFactor.Desktop == FormFactor.Phone)
    }
}
