/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.plaidapp.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.capture
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.plaidapp.core.R
import io.plaidapp.core.data.DataManager
import io.plaidapp.core.data.Source
import io.plaidapp.core.data.prefs.SourcesRepository
import io.plaidapp.core.designernews.data.login.LoginRepository
import io.plaidapp.core.ui.filter.FiltersChangedCallback
import io.plaidapp.core.ui.filter.SourceUiModel
import io.plaidapp.core.ui.filter.SourcesHighlightUiModel
import io.plaidapp.core.util.event.Event
import io.plaidapp.test.shared.LiveDataTestUtil
import io.plaidapp.test.shared.provideFakeCoroutinesDispatcherProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

/**
 * Tests for [HomeViewModel], with dependencies mocked.
 */
class HomeViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val designerNewsSource = Source.DesignerNewsSearchSource(
            "query",
            true
    )
    private val designerNewsSourceUiModel = SourceUiModel(
            designerNewsSource.key,
            designerNewsSource.name,
            designerNewsSource.active,
            designerNewsSource.iconRes,
            designerNewsSource.isSwipeDismissable,
            {},
            {}
    )
    private val dribbbleSource = Source.DribbbleSearchSource("dribbble", true)
    private val dribbbleSourceUiModel = SourceUiModel(
            dribbbleSource.key,
            dribbbleSource.name,
            dribbbleSource.active,
            dribbbleSource.iconRes,
            dribbbleSource.isSwipeDismissable,
            {},
            {}
    )
    private val phSourceKey = "PH"
    private val productHuntSource = Source(
            phSourceKey,
            500,
            "product hung",
            R.drawable.ic_product_hunt,
            false
    )
    private val productHuntSourceUiModel = SourceUiModel(
            productHuntSource.key,
            productHuntSource.name,
            productHuntSource.active,
            productHuntSource.iconRes,
            productHuntSource.isSwipeDismissable,
            {},
            {}
    )
    private val defaultSources = listOf(designerNewsSource, dribbbleSource, productHuntSource)

    private val dataModel: DataManager = mock()
    private val loginRepository: LoginRepository = mock()
    private val sourcesRepository: SourcesRepository = mock()

    @Captor
    private lateinit var filtersChangedCallback: ArgumentCaptor<FiltersChangedCallback>

    @Before
    fun setup() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun logoutFromDesignerNews() {
        // Given a viewmodel with empty sources
        val homeViewModel = createViewModelWithDefaultSources(emptyList())
        // When logging out from designer news
        homeViewModel.logoutFromDesignerNews()

        // Then logout is called
        verify(loginRepository).logout()
    }

    @Test
    fun isDesignerNewsLoggedIn() {
        // Given a view model
        val homeViewModel = createViewModelWithDefaultSources(emptyList())
        // Given a login status
        whenever(loginRepository.isLoggedIn).thenReturn(false)

        // When getting the login status
        val isLoggedIn = homeViewModel.isDesignerNewsUserLoggedIn()

        // The login status is the expected one
        assertFalse(isLoggedIn)
    }

    @Test
    fun addSources_Dribbble() = runBlocking {
        // Given a view model
        val homeViewModel = createViewModelWithDefaultSources(emptyList())

        // When adding a dribbble source
        homeViewModel.addSources("query", isDribbble = true, isDesignerNews = false)

        // Then a Dribbble source is added to the repository
        val expected = listOf(Source.DribbbleSearchSource("query", true))
        verify(sourcesRepository).addOrMarkActiveSources(expected)
    }

    @Test
    fun addSources_DesignerNews() = runBlocking {
        // Given a view model
        val homeViewModel = createViewModelWithDefaultSources(emptyList())

        // When adding a Designer News source
        homeViewModel.addSources("query", isDribbble = false, isDesignerNews = true)

        // Then a Designer News source is added to the repository
        val expected = listOf(Source.DesignerNewsSearchSource("query", true))
        verify(sourcesRepository).addOrMarkActiveSources(expected)
    }

    @Test
    fun addSources_DribbbleDesignerNews() = runBlocking {
        // Given a view model
        val homeViewModel = createViewModelWithDefaultSources(emptyList())

        // When adding a dribbble and a designer news source
        homeViewModel.addSources("query", isDribbble = true, isDesignerNews = true)

        // Then two sources are added to the repository
        val expected = listOf(
                Source.DribbbleSearchSource("query", true),
                Source.DesignerNewsSearchSource("query", true)
        )
        verify(sourcesRepository).addOrMarkActiveSources(expected)
    }

    @Test
    fun filtersUpdated_newSources() {
        // Given a view model
        val homeViewModel = createViewModelWithDefaultSources(emptyList())
        Mockito.verify(sourcesRepository).registerFilterChangedCallback(
                capture(filtersChangedCallback))

        // When updating the filters to a new list of sources
        filtersChangedCallback.value.onFiltersUpdated(listOf(designerNewsSource, dribbbleSource))

        // Thenwhen getting the
        val sources = LiveDataTestUtil.getValue(homeViewModel.sources)
        // Then all sources are highlighted
        val sourcesHighlightUiModel = SourcesHighlightUiModel(listOf(0, 1), 1)
        assertEquals(Event(sourcesHighlightUiModel), sources?.highlightSources)
        //
        assertEquals(2, sources?.sourceUiModels?.size)
    }

    @Test
    fun filtersRemoved() {
        // Given a view model
        val homeViewModel = createViewModelWithDefaultSources(emptyList())
        Mockito.verify(sourcesRepository).registerFilterChangedCallback(
                capture(filtersChangedCallback))

        // When a source was removed
        filtersChangedCallback.value.onFilterRemoved(designerNewsSource)

        // Then source removed value is the expected one
        val source = LiveDataTestUtil.getValue(homeViewModel.sourceRemoved)
        assertEquals(designerNewsSource, source)
    }

    @Test
    fun filtersChanged_activeSource() {
        // Given a view model
        val homeViewModel = createViewModelWithDefaultSources(emptyList())
        Mockito.verify(sourcesRepository).registerFilterChangedCallback(
                capture(filtersChangedCallback))

        // When an inactive source was changed
        val activeSource = Source.DribbbleSearchSource("dribbble", true)
        filtersChangedCallback.value.onFiltersChanged(activeSource)

        // Then source removed value is null
        val source = LiveDataTestUtil.getValue(homeViewModel.sourceRemoved)
        assertNull(source)
    }

    @Test
    fun filtersChanged_inactiveSource() {
        // Given a view model
        val homeViewModel = createViewModelWithDefaultSources(emptyList())
        Mockito.verify(sourcesRepository).registerFilterChangedCallback(
                capture(filtersChangedCallback))

        // When an inactive source was changed
        val inactiveSource = Source.DribbbleSearchSource("dribbble", false)
        filtersChangedCallback.value.onFiltersChanged(inactiveSource)

        // Then the source removed contains the inactive source
        val source = LiveDataTestUtil.getValue(homeViewModel.sourceRemoved)
        assertEquals(inactiveSource, source)
    }

    private fun createViewModelWithDefaultSources(list: List<Source>): HomeViewModel = runBlocking {
        whenever(sourcesRepository.getSources()).thenReturn(list)
        return@runBlocking HomeViewModel(dataModel, loginRepository, sourcesRepository,
                provideFakeCoroutinesDispatcherProvider())
    }
}
