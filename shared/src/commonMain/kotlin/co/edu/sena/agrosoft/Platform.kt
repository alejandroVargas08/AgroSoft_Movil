package co.edu.sena.agrosoft

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform