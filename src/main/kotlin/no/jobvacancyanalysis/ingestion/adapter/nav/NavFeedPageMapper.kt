package no.jobvacancyanalysis.ingestion.adapter.nav

import no.jobvacancyanalysis.ingestion.application.FeedItem
import no.jobvacancyanalysis.ingestion.application.FeedPage
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class NavFeedPageMapper(
	private val objectMapper: ObjectMapper,
) {
	fun mapFeedPage(json: String): FeedPage {
		val root = objectMapper.readTree(json)
		require(root?.isObject == true) { "NAV feed page must be a JSON object" }

		val itemsNode = root.get("items")
		require(itemsNode != null && itemsNode.isArray) {
			"NAV feed page must contain an items array"
		}

		val nextUrlNode = root.get("next_url")
		require(nextUrlNode == null || nextUrlNode.isNull || nextUrlNode.isString) {
			"NAV feed next_url must be a string or null"
		}

		return FeedPage(
			items = itemsNode.toList().map(::mapFeedPageItem),
			nextUrl = nextUrlNode?.takeUnless(JsonNode::isNull)?.asString(),
		)
	}

	private fun mapFeedPageItem(node: JsonNode): FeedItem {
		require(node.isObject) { "NAV feed items must be JSON objects" }

		return FeedItem(
			id = optionalText(node, "id"),
			url = optionalText(node, "url"),
			title = optionalText(node, "title"),
			contentText = optionalText(node, "content_text"),
			dateModified = optionalText(node, "date_modified"),
		)
	}

	private fun optionalText(node: JsonNode, field: String): String? {
		val value = node.get(field)
		require(value == null || value.isNull || value.isString) {
			"NAV feed item field $field must be a string or null"
		}
		return value?.takeUnless(JsonNode::isNull)?.asString()
	}
}
