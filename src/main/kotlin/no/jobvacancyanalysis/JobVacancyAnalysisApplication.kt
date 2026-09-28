package no.jobvacancyanalysis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JobVacancyAnalysisApplication

fun main(args: Array<String>) {
	runApplication<JobVacancyAnalysisApplication>(*args)
}
