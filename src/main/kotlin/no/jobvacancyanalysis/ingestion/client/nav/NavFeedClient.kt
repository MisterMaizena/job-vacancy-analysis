package no.jobvacancyanalysis.ingestion.client.nav

import no.jobvacancyanalysis.ingestion.application.nav.FeedFetchResult
import no.jobvacancyanalysis.ingestion.application.nav.FeedPageCursor

interface NavFeedClient {
	fun fetchPage(cursor: FeedPageCursor? = null): FeedFetchResult
}
