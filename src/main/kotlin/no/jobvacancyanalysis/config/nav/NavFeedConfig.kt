package no.jobvacancyanalysis.config.nav

import no.jobvacancyanalysis.ingestion.client.nav.NavFeedClient
import no.jobvacancyanalysis.ingestion.client.nav.NavFeedPageMapper
import no.jobvacancyanalysis.ingestion.client.nav.NavFeedRestClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(NavFeedProperties::class)
class NavFeedConfig {

	@Bean
	fun navFeedClient(
		mapper: NavFeedPageMapper,
		properties: NavFeedProperties,
	): NavFeedClient {
		require(properties.token.isNotBlank()) { "nav.feed.token must not be blank" }

		val restClient = RestClient.builder()
			.defaultHeader("Authorization", "Bearer ${properties.token}")
			.build()

		return NavFeedRestClient(restClient, mapper, properties.baseUrl)
	}
}
