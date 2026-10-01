package no.jobvacancyanalysis.ingestion.application

sealed interface FeedFetchResult {
	data class Page(
		val page: FeedPage,
		val cursor: FeedPageCursor,
		val nextCursor: FeedPageCursor?,
	) : FeedFetchResult

	data object Unchanged : FeedFetchResult
}
