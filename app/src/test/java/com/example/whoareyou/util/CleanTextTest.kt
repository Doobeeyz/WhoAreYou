package com.example.whoareyou.util

import org.junit.Test
import org.junit.Assert.assertEquals

class CleanTest {

    @Test
    fun `removes links`(){
        val result = TextCleaner.cleanText("Смотри тут https://example.com/abc крутая штука")
        assertEquals("Смотри тут ссылка крутая штука", result)
    }

    @Test
    fun `collapses extra whitespace`() {
        val result = TextCleaner.cleanText("Пришли   www.test.ru   плиз")
        assertEquals("Пришли ссылка плиз", result)
    }

    @Test
    fun `removes emoji`() {
        val result = TextCleaner.cleanText("Привет 😀 как дела 🎉🚀")
        assertEquals("Привет как дела", result)
    }
}