package server

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Alumno
import models.Usuario
import models.mensajes.Request
import models.mensajes.Response
import monitor.AulaDb
import monitor.UsersDb
import mu.KotlinLogging
import security.ManejadorTokens
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.net.ssl.SSLSocket

private val log = KotlinLogging.logger {}
private val json = Json

class GestorClientes(private val cliente: SSLSocket, private val usersDb: UsersDb, private val aulaDb: AulaDb) :
    Runnable {

    // Preparamos los canales de entrada-salida
    private val salida = DataOutputStream(cliente.outputStream)
    private val entrada = DataInputStream(cliente.inputStream)

    // Preparo un Boolean que se volvera True si el Token ha caducado, lo que enviara el response adecuado al usuario
    private var tokenExpired = false

    override fun run() {
        val request = lecturaRequest() // Leemos el request y actuamos segun el tipo que sea
        val permisos = comprobarToken(request) // Comprobamos el tipo se usuario con el token

        if (!tokenExpired) {
            when (request.type) {
                Request.Type.GET_TOKEN -> {
                    enviarToken(request)
                }

                Request.Type.ADD -> {
                    agregarAlumno(request, permisos)
                }

                Request.Type.UPDATE -> {
                    modificarAlumno(request, permisos)
                }

                Request.Type.DELETE -> {
                    eliminarAlumno(request, permisos)
                }

                Request.Type.CONSULT -> {
                    consultarAlumnos() // El unico que no necesita permisos
                }
            }
        } else tokenExpiredSignal()

        cliente.close()
    }

    private fun consultarAlumnos() {
        log.debug { "\tConsultando alumnos" }

        val response = Response(aulaDb.getAll().values.toList().toString(), Response.Type.OK)
        salida.writeUTF(json.encodeToString(response) + "\n")
    }

    private fun eliminarAlumno(request: Request<Alumno>, permisos: Boolean) {
        log.debug { "\tEliminando alumno" }

        val response = if (!permisos) {
            log.debug { "No tiene permisos para esta operacion" }

            Response("Operacion NO Realizada, no tiene permisos", Response.Type.ERROR)
        } else {
            if (request.content?.let { aulaDb.delete(request.content.id) } == true) {
                Response("Operacion Realizada", Response.Type.OK)
            } else {
                Response("Operacion NO Realizada, alumno no existe", Response.Type.ERROR)
            }
        }
        salida.writeUTF(json.encodeToString(response) + "\n")
    }

    private fun modificarAlumno(request: Request<Alumno>, permisos: Boolean) {
        log.debug { "\tActualizando alumno" }

        if (!permisos) {
            log.debug { "No tiene permisos para esta operacion" }

            val response = Response("Operacion NO Realizada, no tiene permisos", Response.Type.ERROR)
            salida.writeUTF(json.encodeToString(response) + "\n")
        } else {
            val existe = request.content?.id.let { aulaDb.update(request.content!!.id, request.content) }
            val response = if (!existe) {
                Response("Alumno no existe", Response.Type.OK)
            } else Response("Operacion Realizada", Response.Type.OK)

            salida.writeUTF(json.encodeToString(response) + "\n")
        }
    }

    private fun agregarAlumno(request: Request<Alumno>, permisos: Boolean) {
        log.debug { "\tProcesando alumno" }

        val response = if (!permisos) {
            log.debug { "No tiene permisos para esta operacion" }

            Response("Operacion NO Realizada, no tiene permisos", Response.Type.ERROR)
        } else {
            request.content?.let { aulaDb.add(it) }

            Response("Operacion Realizada", Response.Type.OK)
        }
        salida.writeUTF(json.encodeToString(response) + "\n")
    }

    private fun tokenExpiredSignal() {
        log.debug { "Token caducado" }

        val response = Response("Token caducado, inice sesion de nuevo", Response.Type.TOKEN_EXPIRED)
        salida.writeUTF(json.encodeToString(response) + "\n")
    }

    private fun comprobarToken(request: Request<Alumno>): Boolean {
        log.debug { "Comprobando token..." }

        var funcionDisponible = true

        val token = request.token?.let { ManejadorTokens.decodeToken(it) }

        if (token != null) {
            //println(token?.getClaim("rol"))
            //println(Usuario.TipoUser.USER.rol)
            if (token.getClaim("rol").toString().contains(Usuario.TipoUser.USER.rol)) {
                funcionDisponible = false
            }
        } else if (request.type != Request.Type.GET_TOKEN) {
            tokenExpired = true
        }

        return funcionDisponible
    }

    private fun enviarToken(request: Request<Alumno>) {
        log.debug { "Procesando token..." }

        val user = usersDb.login(request.content!!.nombre, request.content2!!)

        val responseToken = if (user == null) {

            println("User not found")
            Response(null, Response.Type.ERROR)
        } else {

            val token = ManejadorTokens.createToken(user.rol!!.rol)
            Response(token, Response.Type.OK)
        }

        salida.writeUTF(json.encodeToString(responseToken) + "\n")
    }

    private fun lecturaRequest(): Request<Alumno> {
        log.debug { "Procesando request..." }
        return json.decodeFromString(entrada.readUTF())
    }
}