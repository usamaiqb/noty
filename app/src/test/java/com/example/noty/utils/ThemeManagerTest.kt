package com.example.noty.utils

import org.junit.Test
import org.junit.Assert.*

class ThemeManagerTest {

    @Test
    fun testThemeModeEnum() {
        assertEquals("SYSTEM", ThemeManager.ThemeMode.SYSTEM.name)
        assertEquals("LIGHT", ThemeManager.ThemeMode.LIGHT.name)
        assertEquals("DARK", ThemeManager.ThemeMode.DARK.name)
    }
}
