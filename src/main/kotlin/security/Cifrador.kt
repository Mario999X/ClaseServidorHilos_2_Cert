package security

import com.toxicbakery.bcrypt.Bcrypt

object Cifrador {

    fun codifyPassword(password: String): ByteArray {
        return Bcrypt.hash(password, 16)
    }
}