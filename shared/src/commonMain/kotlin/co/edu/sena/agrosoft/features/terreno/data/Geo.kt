package co.edu.sena.agrosoft.features.terreno.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

private const val METROS_POR_GRADO_LAT = 110_540.0
private const val METROS_POR_GRADO_LNG = 111_320.0

fun centroide(vertices: List<Punto>): Punto {
    if (vertices.isEmpty()) return Punto(0.0, 0.0)
    return Punto(vertices.sumOf { it.lat } / vertices.size, vertices.sumOf { it.lng } / vertices.size)
}

fun areaPoligonoM2(vertices: List<Punto>): Double {
    if (vertices.size < 3) return 0.0
    val origen = centroide(vertices)
    val cosLat = cos(origen.lat * PI / 180)
    val locales = vertices.map {
        (it.lng - origen.lng) * METROS_POR_GRADO_LNG * cosLat to (it.lat - origen.lat) * METROS_POR_GRADO_LAT
    }
    var doble = 0.0
    for (i in locales.indices) {
        val (ax, ay) = locales[i]
        val (bx, by) = locales[(i + 1) % locales.size]
        doble += ax * by - bx * ay
    }
    return abs(doble) / 2
}

fun puntoMedio(a: Punto, b: Punto): Punto = Punto((a.lat + b.lat) / 2, (a.lng + b.lng) / 2)
