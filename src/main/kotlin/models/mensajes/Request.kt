package models.mensajes

import kotlinx.serialization.Serializable

@Serializable
data class Request<T>(
    val token: String?, // Token para el control de roles y el control de acciones del usuario
    val content: T?,
    val content2: T?, // Para el paso de password en el login como String
    val type: Type

) {
    enum class Type {
        GET_TOKEN, ADD, DELETE, UPDATE, CONSULT// User solo podra enviar CONSULT y EXIT, los demas seran denegados.
    }
}