package no.jobvacancyanalysis.ingestion.application.nav

data class FeedPageCursor(
	val url: String,
	val etag: String? = null,
	val lastModified: String? = null,
)
