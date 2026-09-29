package no.jobvacancyanalysis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class JobVacancyAnalysisApplication

fun main(args: Array<String>) {
	runApplication<JobVacancyAnalysisApplication>(*args)
}
