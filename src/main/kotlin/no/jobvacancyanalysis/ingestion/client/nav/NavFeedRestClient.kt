package no.jobvacancyanalysis.ingestion.client.nav

import java.net.URI
import no.jobvacancyanalysis.ingestion.application.FeedFetchResult
import no.jobvacancyanalysis.ingestion.application.FeedPageCursor
import no.jobvacancyanalysis.ingestion.client.NavFeedClient
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient

class NavFeedRestClient(
	private val restClient: RestClient,
	private val mapper: NavFeedPageMapper,
	baseUrl: String,
) : NavFeedClient {

	private val baseUri: URI = parseBaseUrl(baseUrl)

	override fun fetchPage(cursor: FeedPageCursor?): FeedFetchResult {
		val targetUrl = cursor?.let { validateFeedUrl(it.url) } ?: initialFeedUrl()

		val spec = restClient.get().uri(targetUrl)
		cursor?.etag?.let { spec.header("If-None-Match", it) }
		cursor?.lastModified?.let { spec.header("If-Modified-Since", it) }

		val response = spec.retrieve()
			.onStatus({ it.isError }) { _, clientResponse ->
				throw NavFeedException("NAV feed request failed with status ${clientResponse.statusCode}")
			}
			.toEntity(String::class.java)

		return when {
			response.statusCode.isSameCodeAs(HttpStatus.NOT_MODIFIED) -> FeedFetchResult.Unchanged
			response.statusCode.isSameCodeAs(HttpStatus.OK) -> {
				val body = response.body
					?: throw NavFeedException("NAV feed response body missing for $targetUrl")
				val page = try {
					mapper.mapFeedPage(body)
				} catch (exception: IllegalArgumentException) {
					throw NavFeedException("NAV feed page could not be mapped: ${exception.message}", exception)
				}
				FeedFetchResult.Page(
					page = page,
					cursor = FeedPageCursor(
						url = targetUrl,
						etag = response.headers.getFirst("ETag"),
						lastModified = response.headers.getFirst("Last-Modified"),
					),
					nextCursor = page.nextUrl?.let { FeedPageCursor(resolveContinuationUrl(it)) },
				)
			}
			else -> throw NavFeedException("Unexpected NAV feed response status ${response.statusCode} for $targetUrl")
		}
	}

	private fun initialFeedUrl(): String =
		validateFeedUrl(baseUri.resolve("/api/v1/feed").toString())

	private fun validateFeedUrl(url: String): String {
		val uri = URI.create(url)
		require(uri.scheme == baseUri.scheme && uri.host == baseUri.host && uri.port == baseUri.port) {
			"NAV feed URL must target the configured origin, got: $url"
		}
		require(uri.fragment == null) {
			"NAV feed URL must not contain a fragment, got: $url"
		}
		require(uri.path?.startsWith("/api/v1/feed") == true) {
			"NAV feed URL path must start with /api/v1/feed, got: $url"
		}
		return uri.normalize().toString()
	}

	private fun resolveContinuationUrl(nextUrl: String): String {
		val nextUri = URI.create(nextUrl)
		require(nextUri.scheme == null && nextUri.host == null) {
			"NAV continuation URL must be relative, got: $nextUrl"
		}
		require(nextUri.fragment == null) {
			"NAV continuation URL must not contain a fragment, got: $nextUrl"
		}
		require(nextUri.path?.startsWith("/api/v1/feed") == true) {
			"NAV continuation URL path must start with /api/v1/feed, got: $nextUrl"
		}
		val resolved = baseUri.resolve(nextUri).normalize()
		return validateFeedUrl(resolved.toString())
	}

	private fun parseBaseUrl(baseUrl: String): URI {
		require(baseUrl.isNotBlank()) { "NAV feed base URL must not be blank" }
		val uri = try {
			URI.create(baseUrl)
		} catch (exception: IllegalArgumentException) {
			throw NavFeedException("NAV feed base URL is invalid: $baseUrl", exception)
		}
		require(uri.scheme == "https" || uri.scheme == "http") {
			"NAV feed base URL must use http or https, got: $baseUrl"
		}
		require(uri.host?.isNotBlank() == true) {
			"NAV feed base URL must have a host: $baseUrl"
		}
		return uri
	}
}
