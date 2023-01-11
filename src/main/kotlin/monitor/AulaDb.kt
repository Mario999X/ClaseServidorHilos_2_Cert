package monitor

import models.Alumno
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val log = KotlinLogging.logger {}

class AulaDb {

    private val listaAlumnos = mutableMapOf<Int, Alumno>()

    private var contadorId = AtomicInteger(0)

    // Lock
    private val lock = ReentrantLock()
    private val depositarAlumnos = lock.newCondition()
    private val obtenerAlumnos = lock.newCondition()

    private var escritor = false
    private var lector = AtomicInteger(0)

    // Introducimos un alumno con ID fijo y en aumento
    fun add(item: Alumno) {
        lock.withLock {
            while (lector.toInt() > 0) {
                depositarAlumnos.await()
            }
            escritor = true

            contadorId.incrementAndGet()
            item.id = contadorId.toInt()

            listaAlumnos[contadorId.toInt()] = item
            log.debug { "\tAlumno -> $contadorId / $item agregado" }

            escritor = false
            obtenerAlumnos.signalAll()
        }
    }

    // Obtenemos el listado de alumnos
    fun getAll(): Map<Int, Alumno> {
        lock.withLock {
            while (escritor) {
                obtenerAlumnos.await()
            }
            lector.incrementAndGet()

            val mapa = listaAlumnos

            log.debug { "\tSe envia el listado de alumnos..." }

            lector.decrementAndGet()
            depositarAlumnos.signalAll()
            return mapa
        }
    }

    fun getById(id: Int): Alumno? {
        lock.withLock {
            while (escritor) {
                obtenerAlumnos.await()
            }
            lector.incrementAndGet()

            log.debug { "\tBuscando usuario -> $id" }
            var searchedAlumno: Alumno? = null

            if (listaAlumnos.containsKey(id)) {
                searchedAlumno = listaAlumnos[id]
            }

            lector.decrementAndGet()
            depositarAlumnos.signalAll()
            return searchedAlumno
        }
    }

    // Actualizamos a un alumno (Nombre y Nota) segun su ID
    fun update(item: Int, alumno: Alumno): Boolean {
        lock.withLock {
            while (escritor) {
                obtenerAlumnos.await()
            }
            lector.incrementAndGet()

            var existe = false
            if (listaAlumnos.containsKey(item)) {
                log.debug { "\tAlumno -> ${listaAlumnos[item]} antiguo" }
                listaAlumnos[item] = alumno
                log.debug { "\tAlumno -> ${listaAlumnos[item]} actualizado" }
                existe = true
            } else {
                log.debug { "\tID NO EXISTE -> $item" }
            }

            lector.decrementAndGet()
            depositarAlumnos.signalAll()
            return existe
        }
    }

    // Borramos a un alumno segun su ID
    fun delete(item: Int): Boolean {
        lock.withLock {
            while (escritor) {
                obtenerAlumnos.await()
            }
            lector.incrementAndGet()

            var existe = false
            if (listaAlumnos.containsKey(item)) {
                log.debug { "\tAlumno -> ${listaAlumnos[item]} eliminado" }
                listaAlumnos.remove(item)
                existe = true
            } else {
                log.debug { "\tID NO EXISTE -> $item" }
            }

            lector.decrementAndGet()
            depositarAlumnos.signalAll()
            return existe
        }
    }
}