package no.jobvacancyanalysis.ingestion.adapter.nav

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tools.jackson.core.exc.StreamReadException
import tools.jackson.databind.json.JsonMapper

class NavFeedPageMapperTests {
	private val mapper = NavFeedPageMapper(JsonMapper.builder().build())

	@Test
	fun `maps a NAV feed page and ignores unknown fields`() {
		val page = mapper.mapFeedPage(
			"""
			{
			  "items": [
			    {
			      "id": "synthetic-id",
			      "url": "https://example.test/vacancy",
			      "title": "Synthetic role",
			      "content_text": "Synthetic description",
			      "date_modified": "2026-01-02T03:04:05Z",
			      "unknown_field": "ignored"
			    }
			  ],
			  "next_url": "/api/v1/feed?next=synthetic",
			  "unknown_envelope_field": true
			}
			""".trimIndent(),
		)

		assertThat(page.nextUrl).isEqualTo("/api/v1/feed?next=synthetic")
		assertThat(page.items).hasSize(1)
		val item = page.items.single()
		assertThat(item.id).isEqualTo("synthetic-id")
		assertThat(item.url).isEqualTo("https://example.test/vacancy")
		assertThat(item.title).isEqualTo("Synthetic role")
		assertThat(item.contentText).isEqualTo("Synthetic description")
		assertThat(item.dateModified).isEqualTo("2026-01-02T03:04:05Z")
	}

	@Test
	fun `allows an empty tail page and missing optional item fields`() {
		val emptyPage = mapper.mapFeedPage("""{"items":[],"next_url":null}""")
		assertThat(emptyPage.nextUrl).isNull()
		assertThat(emptyPage.items).isEmpty()

		val page = mapper.mapFeedPage("""{"items":[{}],"next_url":null}""")

		assertThat(page.nextUrl).isNull()
		assertThat(page.items.single().id).isNull()
	}

	@Test
	fun `rejects non-object pages and empty input`() {
		assertThrows(IllegalArgumentException::class.java) {
			mapper.mapFeedPage("[]")
		}
		assertThrows(IllegalArgumentException::class.java) {
			mapper.mapFeedPage("")
		}
	}

	@Test
	fun `rejects missing or invalid items`() {
		assertThrows(IllegalArgumentException::class.java) {
			mapper.mapFeedPage("""{"next_url":null}""")
		}
		assertThrows(IllegalArgumentException::class.java) {
			mapper.mapFeedPage("""{"items":{},"next_url":null}""")
		}
		assertThrows(IllegalArgumentException::class.java) {
			mapper.mapFeedPage("""{"items":[null],"next_url":null}""")
		}
		assertThrows(IllegalArgumentException::class.java) {
			mapper.mapFeedPage("""{"items":[{"title":42}],"next_url":null}""")
		}
	}

	@Test
	fun `rejects invalid next url types and malformed json`() {
		assertThrows(IllegalArgumentException::class.java) {
			mapper.mapFeedPage("""{"items":[],"next_url":42}""")
		}
		assertThrows(StreamReadException::class.java) {
			mapper.mapFeedPage("""{"items":""")
		}
	}
}
