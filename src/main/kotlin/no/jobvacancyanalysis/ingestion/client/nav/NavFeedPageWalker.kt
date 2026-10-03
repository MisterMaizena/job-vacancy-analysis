package no.jobvacancyanalysis.ingestion.client.nav

import no.jobvacancyanalysis.ingestion.application.nav.FeedFetchResult
import no.jobvacancyanalysis.ingestion.application.nav.FeedPageCursor

class NavFeedPageWalker(
	private val client: NavFeedClient,
) {
	fun walk(
		initialCursor: FeedPageCursor? = null,
		onPage: (FeedFetchResult.Page) -> Unit,
	): NavFeedPageWalkResult {
		val visitedPageUrls = mutableSetOf<String>()
		var cursor = initialCursor
		var lastCursor: FeedPageCursor? = null

		while (true) {
			when (val result = client.fetchPage(cursor)) {
				FeedFetchResult.Unchanged ->
					return NavFeedPageWalkResult(
						outcome = NavFeedPageWalkOutcome.UNCHANGED,
						lastCursor = lastCursor ?: cursor,
					)

				is FeedFetchResult.Page -> {
					if (!visitedPageUrls.add(result.cursor.url)) {
						throw NavFeedException("NAV feed continuation cycle detected")
					}

					onPage(result)
					lastCursor = result.cursor

					val nextCursor = result.nextCursor
						?: return NavFeedPageWalkResult(
							outcome = NavFeedPageWalkOutcome.REACHED_TAIL,
							lastCursor = lastCursor,
						)

					if (nextCursor.url in visitedPageUrls) {
						throw NavFeedException("NAV feed continuation cycle detected")
					}
					cursor = nextCursor
				}
			}
		}
	}
}

data class NavFeedPageWalkResult(
	val outcome: NavFeedPageWalkOutcome,
	val lastCursor: FeedPageCursor?,
)

enum class NavFeedPageWalkOutcome {
	REACHED_TAIL,
	UNCHANGED,
}
