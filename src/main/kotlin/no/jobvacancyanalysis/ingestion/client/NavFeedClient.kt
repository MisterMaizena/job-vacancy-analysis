package no.jobvacancyanalysis.ingestion.client

import no.jobvacancyanalysis.ingestion.application.FeedFetchResult
import no.jobvacancyanalysis.ingestion.application.FeedPageCursor

interface NavFeedClient {
	fun fetchPage(cursor: FeedPageCursor? = null): FeedFetchResult
}
