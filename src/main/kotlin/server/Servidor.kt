package server

import models.Alumno
import models.Usuario
import monitor.AulaDb
import monitor.UsersDb
import mu.KotlinLogging
import security.Cifrador
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Executors
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocket

private val log = KotlinLogging.logger {}

private const val PUERTO = 6969

private lateinit var serverFactory: SSLServerSocketFactory
private lateinit var servidor: SSLServerSocket
fun main() {

    // Datos del servidor
    var cliente: SSLSocket

    // Pool de hilos
    val pool = Executors.newFixedThreadPool(10)

    // Preparamos la DB para Usuarios
    val userDb = UsersDb()
    // Usuarios
    val users = listOf(
        Usuario("Mario", Usuario.TipoUser.ADMIN, Cifrador.codifyPassword("Hola1")),
        Usuario("Alysys", Usuario.TipoUser.USER, Cifrador.codifyPassword("Hola2"))
    )
    // Introducimos a los usuarios
    repeat(users.size) {
        userDb.add(users[it])
    }

    // Preparamos la DB para Alumnos
    val aulaDb = AulaDb()
    // Alumnos
    val alumnos = listOf(
        Alumno("L", 10),
        Alumno("Doraemon", 4),
        Alumno("Kira", 8)
    )
    // Introducimos a los alumnos
    repeat(alumnos.size) {
        aulaDb.add(alumnos[it])
    }

    // Arrancamos servidor
    log.debug { "Arrancando servidor..." }

    prepararConexion()

    try {
        log.debug { "\t--Servidor esperando..." }
        while (true) {
            cliente = servidor.accept() as SSLSocket
            log.debug { "Peticion de cliente -> " + cliente.inetAddress + " --- " + cliente.port }

            val gc = GestorClientes(cliente, userDb, aulaDb)
            pool.execute(gc)
        }

    } catch (e: IllegalStateException) {
        e.printStackTrace()
    }

}

private fun prepararConexion() {
    // Fichero de donde se sacan los datos
    val workingDir = System.getProperty("user.dir")
    val fichero = Paths.get(workingDir + File.separator + "cert" + File.separator + "llaveropsp.jks").toString()
    System.setProperty("javax.net.ssl.keyStore", fichero)
    System.setProperty("javax.net.ssl.keyStorePassword", "123456")

    serverFactory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    servidor = serverFactory.createServerSocket(PUERTO) as SSLServerSocket
}