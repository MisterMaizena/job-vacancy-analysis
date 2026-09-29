package no.jobvacancyanalysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class NavFeedPropertiesTests {
	@Test
	fun `probe settings do not require a token unless probe validation is requested`() {
		val properties = NavFeedProperties()

		assertEquals(1L, properties.requireValidLookbackDays())
	}

	@ParameterizedTest
	@NullSource
	@EmptySource
	@ValueSource(strings = [" ", "  "])
	fun `probe requires a nonblank token`(token: String?) {
		val properties = NavFeedProperties().apply { apiToken = token }

		val error = assertThrows(IllegalStateException::class.java) {
			properties.requireApiToken()
		}

		assertEquals("nav.feed.api-token is required when nav.feed.probe=true", error.message)
	}

	@Test
	fun `probe rejects token with surrounding whitespace`() {
		val properties = NavFeedProperties().apply { apiToken = " secret-value " }

		val error = assertThrows(IllegalArgumentException::class.java) {
			properties.requireApiToken()
		}

		assertEquals("nav.feed.api-token must not have surrounding whitespace", error.message)
	}

	@Test
	fun `probe rejects nonpositive lookback`() {
		val properties = NavFeedProperties().apply { lookbackDays = 0 }

		val error = assertThrows(IllegalArgumentException::class.java) {
			properties.requireValidLookbackDays()
		}

		assertEquals("nav.feed.lookback-days must be greater than zero", error.message)
	}
}
