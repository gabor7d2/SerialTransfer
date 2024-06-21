import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import java.io.File
import java.io.PrintWriter
import kotlin.system.exitProcess

var portName: String? = null
var baudRate: Int? = null
var genMicrocode = false

lateinit var port: SerialPort
var connected = false

var bigWrite = false
var bigValidate = false
var dump = false
var rawDump = false
lateinit var dumpWriter: PrintWriter
lateinit var pageData: List<String>

fun main(args: Array<String>) {
    for (arg in args) {
        if (arg.matches(Regex("-port:.+"))) portName = arg.split(":")[1]
        if (arg.matches(Regex("-rate:[0-9]{1,7}"))) baudRate = arg.split(":")[1].toInt()
        if (arg == "-genMicrocode") genMicrocode = true
    }

    MultiplexedDisplay().generateFile()

    if (genMicrocode) {
        setupInstructions()
        for (i in 0..4) generateFile(i)
    }

    selectPort()
    setupPort()
    processInput()
}

fun selectPort() {
    if (SerialPort.getCommPorts().isEmpty()) {
        println("No serial port available! Please plug in your Arduino, and use the restart command!")
        return
    }

    if (portName == null) {
        println("Available serial ports:")
        for ((i, port) in SerialPort.getCommPorts().withIndex()) {
            //println("${i + 1}: ${port.systemPortName} (${port.descriptivePortName}) @ ${port.baudRate}")
            println("${i + 1}: ${port.systemPortName} - ${port.descriptivePortName}")
        }

        print("Select port (1-${SerialPort.getCommPorts().size}): ")
        val portNumber = readLine()!!.toInt() - 1
        port = SerialPort.getCommPorts()[portNumber]
    } else {
        var found = false
        for (serialPort in SerialPort.getCommPorts()) {
            if (serialPort.systemPortName == portName) {
                port = serialPort
                found = true
                break
            }
        }
        if (!found) {
            println("Error: Port \"$portName\" not found! Exiting...")
            //Thread.sleep(2000)
            exitProcess(-1)
        }
    }

    if (baudRate == null) {
        print("Enter baud rate: ")
        port.baudRate = readLine()!!.toInt()
    } else port.baudRate = baudRate!!
}

var dataList = ArrayList<String>()
var incompleteData = ""

fun setupPort() {
    println("Connecting to \"${port.systemPortName}\"...\n")
    port.openPort()
    port.inputStream.skip(port.inputStream.available().toLong())
    port.addDataListener(object : SerialPortDataListener {
        override fun getListeningEvents(): Int {
            return SerialPort.LISTENING_EVENT_DATA_RECEIVED
        }

        override fun serialEvent(event: SerialPortEvent) {
            //println("Received incoming message")
            val data = String(event.receivedData).replace("\r", "")
            receiveSerial(data)
        }
    })
}

fun sendSerial(data: String) {
    port.outputStream.write("$data\n".toByteArray())
}

fun receiveSerial(data: String) {
    // Assemble incoming messages
    val split = data.split("\n")
    if (split.size == 1) {
        incompleteData += split.first()
    } else {
        dataList.add(incompleteData + split.first())
        incompleteData = split.last()
        if (split.size > 2) for (i in 1..(split.size - 2)) dataList.add(split[i])
    }

    // Process fully-received messages
    processSerial()
    // Clear already processed messages
    dataList.clear()
}

fun processSerial() {
    for (line in dataList) {
        // Process messages
        if (!connected && line.contains("Waiting for command")) connected = true

        if (line.contains("Waiting for command")) {
            if (!connected) connected = true
            if (bigWrite) bigWrite = false
            if (bigValidate) bigValidate = false
            if (dump) {
                dump = false
                dumpWriter.flush()
                dumpWriter.close()
                println("\b\b\bSuccessfully printed contents to file!")
            }
        } else {
            if (dump) {
                if (rawDump) dumpWriter.print(line.split(":")[1].replace(" ", ""))
                else dumpWriter.println(line)
            }
        }

        if ((bigWrite || bigValidate) && line.contains("Ready for page")) {
            val page = line.split("page ")[1].replace("\n", "").toInt()

            /*val charsToDelete = (if (bigWrite) 16 else 19) + (if (page < 10) 1 else 2)
            if (page == 1) print("\b\b\b")
            else {
                print("1" + "\b".repeat(2 + charsToDelete))
                println(charsToDelete)
            }*/
            print("\b\b\b${if (bigWrite) "Writing" else "Validating"} page $page/16\n> ")

            Thread.sleep(1)
            sendSerial(pageData[page - 1])
        } else if (!dump) print("\b\b\b>> $line\n> ")
    }
}

fun processInput() {
    var input: String
    while (true) {
        print("> ")
        input = readLine()!!
        if (!connected && input != "restart" && input != "exit") {
            println("Not connected to Arduino, use the restart command!")
            continue
        }
        when (input) {
            "bigwrite" -> {
                print("Enter path to data file: ")
                val path = readLine()!!
                bigWrite(path)
            }
            "bigval" -> {
                print("Enter path to data file: ")
                val path = readLine()!!
                bigValidate(path)
            }
            "dump" -> {
                rawDump = false
                print("Enter path to destination file: ")
                val path = readLine()!!
                dump(path)
            }
            "rawdump" -> {
                rawDump = true
                print("Enter path to destination file: ")
                val path = readLine()!!
                dump(path)
            }
            "restart" -> {
                connected = false
                port.closePort()
                selectPort()
                setupPort()
            }
            "exit" -> {
                println("Disconnecting...")
                port.closePort()
                println("Goodbye!")
                exitProcess(0)
            }
            "help" -> {
                println("bigwrite - Writes bytes from the specified file to the EEPROM")
                println("bigval - Validates all bytes of the EEPROM with the data from the specified file")
                println("dump - Dumps the contents of the EEPROM to the specified file, creating it if necessary")
                println("rawdump - Dump without any formatting, result will be 1 a line file containing hex")
                println("restart - Gracefully disconnect from the Arduino and restart")
                println("exit - Gracefully disconnect from the Arduino and exit")
                println("help - Shows this help prompt")
                println()
                println("Notes:")
                println("- Any command/input not listed here gets forwarded to the Arduino")
                println("- Bytes not specified/found in the data input default to 0xff")
                println("- Data will only be written if it is different from what is already stored")
            }
            else -> {
                sendSerial(input)
            }
        }
    }
}

fun bigWrite(path: String) {
    if (readFile(path)) {
        println("Starting write...")
        bigWrite = true
        sendSerial("write")
    }
}

fun bigValidate(path: String) {
    if (readFile(path)) {
        println("Starting validation...")
        bigValidate = true
        sendSerial("validate")
    }
}

fun readFile(path: String): Boolean {
    if (!File(path).exists()) {
        println("The specified file doesn't exist!")
        return false
    }
    pageData = File(path).readText()
        .replace(" ", "").replace("\n", "")
        .replace("\r", "").replace("\t", "").trim().chunked(1024)
    return true
}

fun dump(path: String) {
    println("Starting dump...")
    val file = File(path)
    if (file.exists()) file.delete()
    file.createNewFile()
    dumpWriter = file.printWriter()
    dump = true
    sendSerial("print")
}