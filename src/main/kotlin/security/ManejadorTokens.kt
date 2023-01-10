package security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*

object ManejadorTokens {

    // Algoritmo
    private val algoritmo: Algorithm = Algorithm.HMAC256("123456")

    fun createToken(rol: String): String {
        val jwtToken: String = JWT.create()
            .withIssuer("Login")
            .withClaim("rol", rol)
            .withExpiresAt(Date(System.currentTimeMillis() + 100000)) // alrededor de 2 minutos
            .sign(algoritmo)

        println(jwtToken)
        return jwtToken
    }

    fun decodeToken(jwtToken: String): DecodedJWT {
        val verifier = JWT.require(algoritmo)
            //.withIssuer("XX") // Quien lo emite  y solo validamos para este tipo de emisor
            .build()

        return verifier.verify(jwtToken)
    }
}
