@file:OptIn(ExperimentalTime::class)

package co.edu.sena.agrosoft.core.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object Fechas {
    private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val zona: TimeZone get() = TimeZone.currentSystemDefault()

    fun instante(texto: String?): Instant? {
        if (texto.isNullOrBlank()) return null
        runCatching { Instant.parse(texto) }.getOrNull()?.let { return it }
        runCatching { LocalDateTime.parse(texto.take(19)).toInstant(zona) }.getOrNull()?.let { return it }
        return runCatching { LocalDate.parse(texto.take(10)).atTime(23, 59).toInstant(zona) }.getOrNull()
    }

    fun horasHasta(texto: String?): Long? =
        instante(texto)?.let { (it - Clock.System.now()).inWholeHours }

    fun etiquetaLimite(texto: String?): String? {
        val inst = instante(texto) ?: return null
        val ahora = Clock.System.now()
        val local = inst.toLocalDateTime(zona)
        val hoy = ahora.toLocalDateTime(zona).date
        val hora = if (texto!!.length > 10) " · " + hora12(local) else ""
        return when {
            inst < ahora -> "Vencida"
            local.date == hoy -> "Hoy$hora"
            local.date == hoy.plus(1, DateTimeUnit.DAY) -> "Mañana$hora"
            else -> diaMes(local.date)
        }
    }

    fun haceCuanto(texto: String?): String? {
        val inst = instante(texto) ?: return null
        val minutos = (Clock.System.now() - inst).inWholeMinutes
        return when {
            minutos < 1 -> "Ahora"
            minutos < 60 -> "Hace $minutos min"
            minutos < 1440 -> "Hace ${minutos / 60} h"
            else -> "Hace ${minutos / 1440} d"
        }
    }

    fun hora12(f: LocalDateTime): String {
        val h = f.hour % 12
        val minutos = f.minute.toString().padStart(2, '0')
        return "${if (h == 0) 12 else h}:$minutos ${if (f.hour < 12) "AM" else "PM"}"
    }

    fun diaMes(d: LocalDate): String {
        val iso = d.toString()
        return "${iso.substring(8, 10).toInt()} ${meses[iso.substring(5, 7).toInt() - 1]}"
    }
}

fun decimal(valor: Double): String {
    val redondeado = (valor * 10).roundToLong() / 10.0
    val texto = if (redondeado == redondeado.toLong().toDouble()) redondeado.toLong().toString() else redondeado.toString()
    return texto.replace('.', ',')
}
