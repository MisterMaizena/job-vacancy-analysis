package no.jobvacancyanalysis.ingestion.application.nav

import java.time.OffsetDateTime

data class FeedPage(
	val id: String,
	val items: List<FeedItem>,
	val nextUrl: String?,
	val nextId: String?,
)

data class FeedItem(
	val id: String,
	val url: String,
	val title: String,
	val contentText: String,
	val dateModified: OffsetDateTime?,
	val feedEntry: FeedEntry,
)

data class FeedEntry(
	val uuid: String,
	val status: String,
	val title: String,
	val businessName: String,
	val municipal: String,
	val sistEndret: OffsetDateTime,
)
