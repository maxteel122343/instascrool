package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `test search engine urls with safe search disabled`() {
    val duckUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.DUCKDUCKGO,
        query = "zendaya",
        isSafeSearchDisabled = true
    )
    assertTrue(duckUrl.contains("duckduckgo.com"))
    assertTrue(duckUrl.contains("kp=-2"))

    val bingUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.BING,
        query = "zendaya",
        isSafeSearchDisabled = true
    )
    assertTrue(bingUrl.contains("bing.com"))
    assertTrue(bingUrl.contains("adlt=off"))

    val googleUrl = com.example.util.GoogleSearchHelper.buildSearchUrl(
        engine = com.example.model.SearchEngine.GOOGLE,
        query = "zendaya",
        isSafeSearchDisabled = true
    )
    assertTrue(googleUrl.contains("google.com"))
    assertTrue(googleUrl.contains("safe=off"))
  }

  @Test
  fun `test safe search toggle in state bus`() {
    com.example.data.ScrollSearchStateBus.setSafeSearchDisabled(true)
    assertTrue(com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value)

    com.example.data.ScrollSearchStateBus.toggleSafeSearch()
    assertFalse(com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value)

    com.example.data.ScrollSearchStateBus.setSafeSearchDisabled(true)
    assertTrue(com.example.data.ScrollSearchStateBus.isSafeSearchDisabled.value)
  }

  @Test
  fun `test search engine selection in state bus`() {
    com.example.data.ScrollSearchStateBus.setSearchEngine(com.example.model.SearchEngine.BING)
    assertEquals(com.example.model.SearchEngine.BING, com.example.data.ScrollSearchStateBus.selectedSearchEngine.value)

    com.example.data.ScrollSearchStateBus.setSearchEngine(com.example.model.SearchEngine.DUCKDUCKGO)
    assertEquals(com.example.model.SearchEngine.DUCKDUCKGO, com.example.data.ScrollSearchStateBus.selectedSearchEngine.value)

    com.example.data.ScrollSearchStateBus.setSearchEngine(com.example.model.SearchEngine.GOOGLE)
    assertEquals(com.example.model.SearchEngine.GOOGLE, com.example.data.ScrollSearchStateBus.selectedSearchEngine.value)
  }
}
