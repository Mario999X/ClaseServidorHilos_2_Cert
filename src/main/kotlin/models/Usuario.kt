package models

import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val nombre: String,
    val rol: TipoUser?,
    val password: ByteArray,
    var id: Int = 0
) {
    enum class TipoUser(val rol: String) {
        USER("user"), ADMIN("admin")
    }

    override fun toString(): String {
        return "Usuario(nombre='$nombre', rol=${rol?.rol}, id=$id, password=$password)"
    }
}