package no.jobvacancyanalysis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

// TODO: Add a PostgreSQL/Testcontainers integration test for the normal
// DataSource/Flyway setup.
@SpringBootTest(
	properties = [
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
		"spring.flyway.enabled=false",
		"nav.feed.base-url=https://example.test",
		"nav.feed.token=synthetic-token",
	],
)
class JobVacancyAnalysisApplicationTests(
	@Autowired private val context: ApplicationContext,
) {

	@Test
	fun contextLoads() {
		assertThat(context.getBean(JobVacancyAnalysisApplication::class.java)).isNotNull()
	}

}
