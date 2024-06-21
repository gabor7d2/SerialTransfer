import java.io.File

class MultiplexedDisplay {

    val signals = Array<Byte>(32768) {
        if (it.extractBits(1, 8) == 1) {
            segments(it.extractBits(4, 4))
        } else {
            segments(it.extractBits(4, 0))
        }
    }

    fun segments(num: Int): Byte {
        return when (num) {
            // 0gfabedc
            0 -> 0b00111111
            1 -> 0b00001001
            2 -> 0b01011110
            3 -> 0b01011011
            4 -> 0b01101001
            5 -> 0b01110011
            6 -> 0b01110111
            7 -> 0b00011001
            8 -> 0b01111111
            9 -> 0b01111011
            10 -> 0b01111101
            11 -> 0b01100111
            12 -> 0b00110110
            13 -> 0b01001111
            14 -> 0b01110110
            15 -> 0b01110100
            else -> 0
        }
    }

    fun generateFile() {
        var values = ""
        signals.forEach {
            //values += it.extractBits(8, romIndex * 8).toString(16)
            values += String.format("%02x", it)
        }

        val file = File("multiplexed")
        file.writeText(values)
    }
}