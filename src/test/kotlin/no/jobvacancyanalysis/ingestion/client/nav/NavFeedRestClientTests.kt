package no.jobvacancyanalysis.ingestion.client.nav

import no.jobvacancyanalysis.ingestion.application.FeedFetchResult
import no.jobvacancyanalysis.ingestion.application.FeedPageCursor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

class NavFeedRestClientTests {
	private val jsonMapper = JsonMapper.builder().build()
	private val pageMapper = NavFeedPageMapper(jsonMapper)

	@Test
	fun `fetches first page and returns next cursor`() {
		val builder = RestClient.builder()
		val server = MockRestServiceServer.bindTo(builder).build()
		val client = NavFeedRestClient(builder.build(), pageMapper, TEST_BASE_URL)

		server.expect(requestTo(FEED_URL))
			.andRespond(withSuccess(feedPageJson(nextUrl = NEXT_CURSOR_URL), MediaType.APPLICATION_JSON))

		val result = client.fetchPage(null) as FeedFetchResult.Page

		assertThat(result.page.items).hasSize(1)
		assertThat(result.cursor.url).isEqualTo(FEED_URL)
		assertThat(result.nextCursor?.url).isEqualTo("$FEED_URL?next=synthetic-next")
		server.verify()
	}

	@Test
	fun `follows continuation cursor and detects tail`() {
		val builder = RestClient.builder()
		val server = MockRestServiceServer.bindTo(builder).build()
		val client = NavFeedRestClient(builder.build(), pageMapper, TEST_BASE_URL)

		val cursor = FeedPageCursor("$FEED_URL?next=synthetic-next")
		server.expect(requestTo(cursor.url))
			.andRespond(withSuccess(feedPageJson(nextUrl = null), MediaType.APPLICATION_JSON))

		val result = client.fetchPage(cursor) as FeedFetchResult.Page

		assertThat(result.page.items).hasSize(1)
		assertThat(result.nextCursor).isNull()
		server.verify()
	}

	@Test
	fun `returns unchanged when server responds 304`() {
		val builder = RestClient.builder()
		val server = MockRestServiceServer.bindTo(builder).build()
		val client = NavFeedRestClient(builder.build(), pageMapper, TEST_BASE_URL)

		val cursor = FeedPageCursor(
			url = "$FEED_URL?next=synthetic-next",
			etag = "\"abc123\"",
			lastModified = "Wed, 01 Oct 2026 12:00:00 GMT",
		)
		server.expect(requestTo(cursor.url))
			.andExpect(header("If-None-Match", cursor.etag!!))
			.andExpect(header("If-Modified-Since", cursor.lastModified!!))
			.andRespond(withStatus(HttpStatus.NOT_MODIFIED))

		val result = client.fetchPage(cursor)

		assertThat(result).isEqualTo(FeedFetchResult.Unchanged)
		server.verify()
	}

	@Test
	fun `rejects cross-origin continuation url`() {
		val client = NavFeedRestClient(RestClient.builder().build(), pageMapper, TEST_BASE_URL)

		val exception = assertThrows<IllegalArgumentException> {
			client.fetchPage(FeedPageCursor("https://evil.test${NavFeedPaths.FEED_PATH}?next=x"))
		}

		assertThat(exception.message).contains("origin")
	}

	@Test
	fun `rejects continuation url outside feed path`() {
		val client = NavFeedRestClient(RestClient.builder().build(), pageMapper, TEST_BASE_URL)

		val exception = assertThrows<IllegalArgumentException> {
			client.fetchPage(FeedPageCursor("$TEST_BASE_URL$OUTSIDE_FEED_PATH?next=x"))
		}

		assertThat(exception.message).contains(NavFeedPaths.FEED_PATH)
	}

	private fun feedPageJson(nextUrl: String?): String {
		val nextField = nextUrl?.let { "\"next_url\": \"$it\"" } ?: "\"next_url\": null"
		return """
		{
		  "version": "1",
		  "title": "Synthetic feed",
		  "home_page_url": "https://example.test",
		  "feed_url": "https://example.test/feed",
		  "description": "Synthetic feed description",
		  "id": "550e8400-e29b-41d4-a716-446655440000",
		  "items": [
		    {
		      "id": "synthetic-id",
		      "url": "https://example.test/vacancy",
		      "title": "Synthetic role",
		      "content_text": "Synthetic description",
		      "date_modified": "2026-01-02T03:04:05Z",
		      "_feed_entry": {
		        "uuid": "synthetic-entry-id",
		        "status": "ACTIVE",
		        "title": "Synthetic feed title",
		        "businessName": "Synthetic employer",
		        "municipal": "Synthetic municipality",
		        "sistEndret": "2026-01-02T03:04:05Z"
		      }
		    }
		  ],
		  $nextField
		}
		""".trimIndent()
	}

	private companion object {
		const val TEST_BASE_URL = "https://pam-stilling-feed.test"
		const val FEED_URL = "$TEST_BASE_URL${NavFeedPaths.FEED_PATH}"
		const val NEXT_CURSOR_URL = "${NavFeedPaths.FEED_PATH}?next=synthetic-next"
		const val OUTSIDE_FEED_PATH = "/api/v1/other"
	}
}
