package no.jobvacancyanalysis

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
@ConditionalOnProperty(prefix = "nav.feed", name = ["probe"], havingValue = "true")
class NavFeedProbe(
	private val objectMapper: ObjectMapper,
	private val navFeedProperties: NavFeedProperties,
) : ApplicationRunner {
	private val restClient = RestClient.create("https://pam-stilling-feed.nav.no")
	private val lookbackDays = navFeedProperties.requireValidLookbackDays()
	private val token = navFeedProperties.requireApiToken()

	override fun run(args: ApplicationArguments) {
		val ifModifiedSince = ZonedDateTime.now(ZoneOffset.UTC)
			.minusDays(lookbackDays)
			.format(DateTimeFormatter.RFC_1123_DATE_TIME)

		val response = restClient.get()
			.uri("/api/v1/feed")
			.accept(MediaType.APPLICATION_JSON)
			.headers {
				it.setBearerAuth(token)
				it.set("If-Modified-Since", ifModifiedSince)
			}
			.exchange { _, clientResponse ->
				val body = clientResponse.body.readBytes().toString(Charsets.UTF_8)
				Triple(clientResponse.statusCode, clientResponse.headers, body)
			}

		println("Response body:")
		println(prettyPrint(response.third))
		println()
		println("--- NAV feed response summary ---")
		println("NAV GET /api/v1/feed -> ${response.first}")
		println("If-Modified-Since: $ifModifiedSince (${lookbackDays}d lookback)")
		response.second.eTag?.let { println("ETag: $it") }
		response.second.lastModified.takeIf { it > 0 }?.let {
			println("Last-Modified: ${response.second.getFirst("Last-Modified")}")
		}
		printSummary(response.third)
	}

	private fun printSummary(body: String) {
		val root = try {
			objectMapper.readTree(body)
		} catch (_: Exception) {
			println("Could not parse response body as JSON.")
			return
		}
		val itemsNode = root.path("items")
		val items = if (itemsNode.isArray) itemsNode.toList() else emptyList()
		val statuses = items.map { it.path("_feed_entry").path("status").toString().trim('"') }
		println("Page id: ${root.path("id")}; next id: ${root.path("next_id")}; next URL: ${root.path("next_url")}")
		println("Items: ${items.size}; active: ${statuses.count { it == "ACTIVE" }}; inactive: ${statuses.count { it == "INACTIVE" }}")
	}

	private fun prettyPrint(body: String): String = try {
		objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(body))
	} catch (_: Exception) {
		body
	}
}
