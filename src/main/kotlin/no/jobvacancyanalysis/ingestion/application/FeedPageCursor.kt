package no.jobvacancyanalysis.ingestion.application

data class FeedPageCursor(
	val url: String,
	val etag: String? = null,
	val lastModified: String? = null,
)
