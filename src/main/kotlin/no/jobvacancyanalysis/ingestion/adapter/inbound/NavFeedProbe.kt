package no.jobvacancyanalysis.ingestion.adapter.inbound

import no.jobvacancyanalysis.ingestion.adapter.nav.NavFeedClient
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnProperty(prefix = "nav.feed", name = ["probe"], havingValue = "true")
class NavFeedProbe(
	private val objectMapper: ObjectMapper,
	private val navFeedClient: NavFeedClient,
) : ApplicationRunner {
	override fun run(args: ApplicationArguments) {
		val response = navFeedClient.fetch()

		println("Response body:")
		println(prettyPrint(response.body))
		println()
		println("--- NAV feed response summary ---")
		println("NAV GET /api/v1/feed -> ${response.status}")
		println("If-Modified-Since: ${response.ifModifiedSince} (${response.lookbackDays}d lookback)")
		response.eTag?.let { println("ETag: $it") }
		response.lastModified?.let { println("Last-Modified: $it") }
		printSummary(response.body)
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
