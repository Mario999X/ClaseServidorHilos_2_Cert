package models

import kotlinx.serialization.Serializable

@Serializable
data class Alumno(
    val nombre: String = "",
    val nota: Int = 0,
    var id: Int = 0
) {
    override fun toString(): String {
        return "Alumno(nombre='$nombre', nota=$nota, id=$id)"
    }
}