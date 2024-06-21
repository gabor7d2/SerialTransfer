import java.io.File

enum class FlagsState(val state: Int) {
    ANY(256),
    NOT_ZERO(128),
    NOT_CARRY(64),
    NOT_OVERFLOW(32),
    NOT_NEGATIVE(16),
    ZERO(8),
    CARRY(4),
    OVERFLOW(2),
    NEGATIVE(1),
    NONE(0);

    fun matchFlags(flags: Int): Boolean {
        val z = flags.extractBits(1, 0)
        val c = flags.extractBits(1, 1)
        val o = flags.extractBits(1, 2)
        val n = flags.extractBits(1, 3)
        return when (this) {
            ANY -> true
            NOT_ZERO -> z == 0
            NOT_CARRY -> c == 0
            NOT_OVERFLOW -> o == 0
            NOT_NEGATIVE -> n == 0
            ZERO -> z == 1
            CARRY -> c == 1
            OVERFLOW -> o == 1
            NEGATIVE -> n == 1
            NONE -> flags == 0
        }
    }
}

val signals = Array<Long>(32768) {
    when (it.extractBits(3, 8)) {
        // PC -> MAR (low-byte)
        0 -> 0x3738FFEB38
        // PC -> MAR (high-byte)
        1 -> 0x3B38FFEE38
        // M -> IR
        2 -> 0x2F30FFFF38
        // Timer Reset
        3 -> 0x3D38FFEF38
        else -> 0x3F38FFEF38
    }
}

fun setupInstructions() {
    // Addressing modes:
    // not specified: implied (no operand or extra data for the instruction)
    // imm: immediate (1-byte data right after instruction)
    // abs: absolute (2-byte addr)
    // abs-c: absolute with page set to C register's contents (1-byte addr)
    // abs-pg: absolute within current page (1-byte addr, high byte doesn't change)
    // idx-abs-pg: indexed absolute within current page (1-byte addr + B register's contents, high byte doesn't change)
    // idx-abs-c
    // idx-abs
    // rel: relative (offset in current page)
    addInstruction(1, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, AI)) // LDA - imm
    addInstruction(2, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, BI)) // LDB - imm
    addInstruction(3, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, CI)) // LDC - imm

    addInstruction(10, FlagsState.ANY, listOf(ALS0, ALS1, ALC, AO, ALW, FW), listOf(ALO, AI)) // INA
    addInstruction(11, FlagsState.ANY, listOf(ALS0, ALS1, ALC, BO, ALW, FW), listOf(ALO, BI)) // INB

    addInstruction(20, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, PCHI), listOf(PCLO, MALI), listOf(ME, PCLI)) // JMP - abs
    //addInstruction(21, FlagsState.CARRY, listOf(PCLO, MALI), listOf(PCC, ME, BI), listOf(ALS0, ALS1, ALB, PCLO, ALW), listOf(ALO, PCLI)) // BCS - rel
    //addInstruction(22, FlagsState.NOT_CARRY, listOf(PCLO, MALI), listOf(PCC, ME, BI), listOf(ALS0, ALS1, ALB, PCLO, ALW), listOf(ALO, PCLI)) // BCC - rel
    addInstruction(21, FlagsState.CARRY, listOf(PCLO, MALI), listOf(ME, PCLI)) // BCS - abs-pg
    addInstruction(21, FlagsState.NOT_CARRY, listOf(PCC)) // BCS - abs-pg
    addInstruction(22, FlagsState.NOT_CARRY, listOf(PCLO, MALI), listOf(ME, PCLI)) // BCC - abs-pg
    addInstruction(22, FlagsState.CARRY, listOf(PCC)) // BCC - abs-pg
    addInstruction(23, FlagsState.ZERO, listOf(PCLO, MALI), listOf(ME, PCLI)) // BZS - abs-pg
    addInstruction(23, FlagsState.NOT_ZERO, listOf(PCC)) // BZS - abs-pg
    addInstruction(24, FlagsState.NOT_ZERO, listOf(PCLO, MALI), listOf(ME, PCLI)) // BZC - abs-pg
    addInstruction(24, FlagsState.ZERO, listOf(PCC)) // BZC - abs-pg

    addInstruction(30, FlagsState.CARRY, listOf(PCLO, MALI), listOf(PCC, ALS0, ALS1, ALB, ALC, ME, ALW, FW)) // ADC - imm
    addInstruction(30, FlagsState.NOT_CARRY, listOf(PCLO, MALI), listOf(PCC, ALS0, ALS1, ALB, ME, ALW, FW)) // ADC - imm
    addInstruction(31, FlagsState.CARRY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(CO, MAHI), listOf(ALS0, ALS1, ALB, ALC, ME, ALW, FW)) // ADC - abs-c
    addInstruction(31, FlagsState.NOT_CARRY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(CO, MAHI), listOf(ALS0, ALS1, ALB, ME, ALW, FW)) // ADC - abs-c
    addInstruction(32, FlagsState.CARRY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(ALS0, ALS1, ALB, ALC, ME, ALW, FW)) // ADC - abs-pg
    addInstruction(32, FlagsState.NOT_CARRY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(ALS0, ALS1, ALB, ME, ALW, FW)) // ADC - abs-pg

    addInstruction(33, FlagsState.CARRY, listOf(PCLO, MALI), listOf(PCC, ALS0, ALB, ME, ALW, FW)) // SBC - imm
    addInstruction(33, FlagsState.NOT_CARRY, listOf(PCLO, MALI), listOf(PCC, ALS0, ALB, ALC, ME, ALW, FW)) // SBC - imm
    addInstruction(34, FlagsState.CARRY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(CO, MAHI), listOf(ALS0, ALB, ME, ALW, FW)) // SBC - abs-c
    addInstruction(34, FlagsState.NOT_CARRY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(CO, MAHI), listOf(ALS0, ALB, ALC, ME, ALW, FW)) // SBC - abs-c
    addInstruction(35, FlagsState.CARRY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(ALS0, ALB, ME, ALW, FW)) // SBC - abs-pg
    addInstruction(35, FlagsState.NOT_CARRY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(ALS0, ALB, ALC, ME, ALW, FW)) // SBC - abs-pg

    addInstruction(40, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(CO, MAHI), listOf(AO, ME, MW)) // STA - abs-c
    addInstruction(41, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(CO, MAHI), listOf(BO, ME, MW)) // STB - abs-c
    addInstruction(42, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(AO, ME, MW)) // STA - abs-pg
    addInstruction(43, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, MALI), listOf(BO, ME, MW)) // STB - abs-pg

    addInstruction(50, FlagsState.ANY, listOf(AO, BI)) // TAB (transfer A -> B)
    addInstruction(51, FlagsState.ANY, listOf(BO, AI)) // TBA (transfer B -> A)

    addInstruction(250, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, DI)) // DPI (display instruction) - imm
    addInstruction(251, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ALS0, ALS1, ALB, ME, ALW, FW), listOf(ALO, MALI), listOf(ME, DRS, DI)) // DPD b (display data) - idx-abs-pg
    addInstruction(252, FlagsState.ANY, listOf(PCLO, MALI), listOf(PCC, ME, DRS, DI), listOf(ALO, DRS, DI)) // DPD (display data) - imm
}

fun addInstruction(opcode: Int, flagsState: FlagsState, vararg controlSignals: List<ControlSignal>) {
    for ((i, signalList) in controlSignals.withIndex()) {
        for (flag in 0..15) {
            if (!flagsState.matchFlags(flag)) continue
            val address: Int = (flag shl 11) + ((i + 3) shl 8) + opcode
            if (i == 0) signals[address] = 0x3F38FFEF38
            addMicroInstruction(address, *signalList.toTypedArray())
            if (i == controlSignals.size - 1) addMicroInstruction(address, TR)
            //if (opcode == 22) println("Data at address $address (${address.toString(2).padStart(15, '0')}) (flags: ${flag.toString(2).padStart(4, '0')}, timer: ${i + 3}) is ${signals[address].toString(16)} (${signals[address].toString(2).padStart(40, '0').substring(24..31)})")
        }
    }
}

fun addMicroInstruction(address: Int, vararg controlSignals: ControlSignal) {
    var signal = signals[address]
    controlSignals.forEach {
        signal = if (it.inverted) signal xor it.value else signal or it.value
    }
    signals[address] = signal
}

fun generateFile(romIndex: Int) {
    println("Generating microcode file for EEPROM #$romIndex...")
    var values = ""
    signals.forEach {
        //values += it.extractBits(8, romIndex * 8).toString(16)
        values += String.format("%02x", it.extractBits(8, romIndex * 8))
    }

    val file = File("microcode$romIndex")
    if (file.exists() && file.readText() == values) {
        println("Microcode for EEPROM #$romIndex didn't change")
    } else file.writeText(values)
}

/**
 * Function which extracts k bits from p position
 * and returns the extracted value as integer
 */
fun Int.extractBits(k: Int, p: Int = 0): Int {
    return ((1 shl k) - 1) and (this shr p)
}

/**
 * Function which extracts k bits from p position
 * and returns the extracted value as long
 */
fun Long.extractBits(k: Int, p: Int = 0): Long {
    return ((1L shl k) - 1) and (this shr p)
}