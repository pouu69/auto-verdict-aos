package com.car.autoverdict.licenses

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Parses the real generated raw resources (spec 7.3, 7.5, 10). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class OssNoticesTest {

    private val catalog: OssCatalog by lazy {
        runBlocking {
            OssNoticeRepository(ApplicationProvider.getApplicationContext<Application>().resources).load()
        }
    }

    private val allArtifacts: Set<String>
        get() = catalog.notices.libraries.flatMap { it.artifacts }.toSet()

    @Test fun `both files parse and every reference resolves`() {
        val notices = catalog.notices
        val licenseIds = notices.licenses.map { it.id }
        assertEquals(licenseIds.size, licenseIds.toSet().size)
        notices.licenses.forEach { assertTrue("${it.id} has neither text nor url", it.text != null || it.url != null) }

        val libraryIds = notices.libraries.map { it.id }
        assertEquals(libraryIds.size, libraryIds.toSet().size)
        notices.libraries.forEach {
            assertTrue("${it.id} -> unknown ${it.licenseId}", it.licenseId in licenseIds)
            assertTrue("${it.id} has no artifacts", it.artifacts.isNotEmpty())
        }

        val google = catalog.google
        assertTrue(google.components.isNotEmpty())
        google.components.forEach { assertTrue("${it.name} -> missing text", it.textId in google.texts) }
        assertEquals(google.components.size, catalog.googleComponents.size)
    }

    @Test fun `detail ids never contain the route separator and all resolve`() {
        val ids = catalog.notices.libraries.map(OssCatalog::libraryDetailId) +
            catalog.google.components.map(OssCatalog::googleDetailId)
        ids.forEach {
            assertFalse("$it contains |", '|' in it)
            assertNotNull("$it does not resolve", catalog.detail(it))
        }
        assertNull(catalog.detail("lib:nope"))
        assertNull(catalog.detail("nope"))
    }

    @Test fun `representative entries are present`() {
        val artifacts = allArtifacts
        assertTrue(artifacts.any { it.startsWith("androidx.compose.ui:ui:") })
        assertTrue(artifacts.any { it.startsWith("com.google.android.gms:play-services-ads:") })

        val checker = catalog.notices.libraries.single { lib ->
            lib.artifacts.any { it.startsWith("org.checkerframework:checker-qual:") }
        }
        val mit = catalog.notices.licenses.single { it.id == checker.licenseId }
        assertTrue(mit.name.contains("MIT"))
        assertNotNull(mit.text)

        val pretendard = catalog.notices.libraries.single { it.id == "pretendard" }
        assertEquals("ofl-1.1", pretendard.licenseId)
        assertTrue(OssCatalog.isFont(pretendard))
        val detail = catalog.detail(OssCatalog.libraryDetailId(pretendard))!!
        assertTrue(detail.text!!.contains("SIL OPEN FONT LICENSE"))
    }

    @Test fun `every direct dependency in build gradle is listed`() {
        val gradle = listOf(File("build.gradle.kts"), File("app/build.gradle.kts")).first { it.isFile }
        // implementation("g:a:v") or BOM-managed implementation("g:a"); debug/test configurations are not shipped.
        val coordinates = Regex("""^\s*implementation\("([^"]+)"\)""", RegexOption.MULTILINE)
            .findAll(gradle.readText()).map { it.groupValues[1] }.toList()
        assertTrue(coordinates.size >= 10)
        val artifacts = allArtifacts
        coordinates.forEach { coordinate ->
            val listed = if (coordinate.count { it == ':' } >= 2) {
                coordinate in artifacts
            } else {
                artifacts.any { it.startsWith("$coordinate:") }
            }
            assertTrue("$coordinate missing from oss_notices.json; regenerate (scripts/oss-notices)", listed)
        }
    }

    @Test fun `sections put Google terms last`() {
        val sections = catalog.sections
        assertEquals(catalog.notices.libraries.size, sections.sumOf { it.libraries.size })
        assertEquals("apache-2.0", sections.first().license.id)
        assertTrue(sections.last().isGoogleTerms)
        assertEquals(1, sections.count { it.isGoogleTerms })
    }

    @Test fun `paragraphs split on blank lines and drop empty ones`() {
        assertEquals(listOf("a\nb", "  c"), OssCatalog.paragraphs("\na\nb\n\n \n  c\n\n"))
        val apache = catalog.notices.licenses.single { it.id == "apache-2.0" }.text!!
        assertTrue(OssCatalog.paragraphs(apache).size > 10)
    }
}
