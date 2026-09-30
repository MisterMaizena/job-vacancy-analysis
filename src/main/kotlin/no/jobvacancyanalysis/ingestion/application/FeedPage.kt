package no.jobvacancyanalysis.ingestion.application

data class FeedPage(
	val items: List<FeedItem>,
	val nextUrl: String?,
)

data class FeedItem(
	val id: String?,
	val url: String?,
	val title: String?,
	val contentText: String?,
	val dateModified: String?,
)
