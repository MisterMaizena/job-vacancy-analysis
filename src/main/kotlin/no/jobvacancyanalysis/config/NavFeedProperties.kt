package no.jobvacancyanalysis.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("nav.feed")
data class NavFeedProperties(
	val baseUrl: String,
	val token: String,
)
