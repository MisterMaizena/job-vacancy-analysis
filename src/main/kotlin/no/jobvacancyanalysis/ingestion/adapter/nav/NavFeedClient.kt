package no.jobvacancyanalysis.ingestion.adapter.nav

import no.jobvacancyanalysis.config.NavFeedProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
@ConditionalOnProperty(prefix = "nav.feed", name = ["probe"], havingValue = "true")
class NavFeedClient(
	navFeedProperties: NavFeedProperties,
) {
	private val restClient = RestClient.create("https://pam-stilling-feed.nav.no")
	private val lookbackDays = navFeedProperties.requireValidLookbackDays()
	private val token = navFeedProperties.requireApiToken()

	fun fetch(): NavFeedResponse {
		val ifModifiedSince = ZonedDateTime.now(ZoneOffset.UTC)
			.minusDays(lookbackDays)
			.format(DateTimeFormatter.RFC_1123_DATE_TIME)

		return restClient.get()
			.uri("/api/v1/feed")
			.accept(MediaType.APPLICATION_JSON)
			.headers {
				it.setBearerAuth(token)
				it.set("If-Modified-Since", ifModifiedSince)
			}
			.exchange { _, clientResponse ->
				val body = clientResponse.body.readBytes().toString(Charsets.UTF_8)
				NavFeedResponse(
					status = clientResponse.statusCode.value(),
					eTag = clientResponse.headers.eTag,
					lastModified = clientResponse.headers.lastModified
						.takeIf { it > 0 }
						?.let { clientResponse.headers.getFirst("Last-Modified") },
					body = body,
					ifModifiedSince = ifModifiedSince,
					lookbackDays = lookbackDays,
				)
			}
	}
}

data class NavFeedResponse(
	val status: Int,
	val eTag: String?,
	val lastModified: String?,
	val body: String,
	val ifModifiedSince: String,
	val lookbackDays: Long,
)
