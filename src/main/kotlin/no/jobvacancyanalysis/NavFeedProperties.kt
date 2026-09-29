package no.jobvacancyanalysis

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("nav.feed")
class NavFeedProperties {
	var probe: Boolean = false
	var lookbackDays: Long = 1
	var apiToken: String? = null

	fun requireValidLookbackDays(): Long = lookbackDays.also {
		require(it > 0) { "nav.feed.lookback-days must be greater than zero" }
	}

	fun requireApiToken(): String {
		val token = apiToken
			?.takeIf(String::isNotBlank)
			?: error("nav.feed.api-token is required when nav.feed.probe=true")
		require(token == token.trim()) { "nav.feed.api-token must not have surrounding whitespace" }
		return token
	}
}
