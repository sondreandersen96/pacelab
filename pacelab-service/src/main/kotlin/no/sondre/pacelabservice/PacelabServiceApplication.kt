package no.sondre.pacelabservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PacelabServiceApplication

fun main(args: Array<String>) {
    runApplication<PacelabServiceApplication>(*args)
}
