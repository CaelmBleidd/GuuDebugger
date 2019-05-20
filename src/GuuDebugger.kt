import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Paths
import java.util.*

class GuuDebugger {

    private val stack = Stack<Function>()
    private val variables = mutableMapOf<String, Int>()
    private val nameToFunction = mutableMapOf<String, Function>()
    private lateinit var reader: BufferedReader


    fun parse(path: String) {
        var counter = 0
        lateinit var tmpFunction: Function

        reader = FileReader(path).buffered()

        reader.use { reader ->
            var nextLine = reader.readLine()
            while (nextLine != null) {
                val line = nextLine.split("\\p{Space}+".toRegex()).filter { it != "" }

                counter++

                if (line.size < 2) {
                    nextLine = reader.readLine()
                    continue
                }//TODO()

                when (line[0]) {
                    "sub" -> {
                        tmpFunction = Function(line[1], counter, mutableListOf())
                        nameToFunction.put(tmpFunction.name, tmpFunction)
                    }
                    "set" -> {
                        tmpFunction.instructions.add(Operator(line[0], mutableListOf(line[1], line[2])))
                    }
                    "call", "print" -> {
                        tmpFunction.instructions.add(Operator(line[0], mutableListOf(line[1])))
                    }
                }

                nextLine = reader.readLine()
            }
        }
    }

    fun process() {
        val main = nameToFunction["main"]
        if (main == null) {
            print("ERROR")
            return
        }

        reader = System.`in`.bufferedReader()

        reader.use {
            execFunction(main, false)
        }
    }

    private fun execFunction(function: Function, hidden: Boolean) {
        stack.push(function)

        nextLine@ for (line in function.instructions) {
            var newHidden = true

            if (!hidden) {
                commandWaiting@ while (true) {
                    when (reader.readLine()) {
                        "i" -> {
                            if (line.name == "call")
                                newHidden = false
                            break@commandWaiting
                        }
                        "o" -> break@commandWaiting
                        "trace" -> printStackTrace()
                        "var" -> printVariables()
                        else -> println("Unknown symbol found")
                    }
                }
            }

            when (line.name) {
                "call" -> {
                    val nextFunction = nameToFunction[line.args.first()]!!
                    if (hidden || newHidden)
                        execFunction(nextFunction, true)
                    else
                        execFunction(nextFunction, false)
                }
                "set" -> variables[line.args[0]] = line.args[1].toInt()
                "print" -> println(variables[line.args.first()])
            }
        }

        stack.pop()
    }

    private fun printStackTrace(): Unit = stack.forEach { func -> println("${func.startLineNumber}: ${func.name}") }

    private fun printVariables(): Unit = variables.forEach { println("${it.key}: ${it.value}") }

    data class Function(val name: String, val startLineNumber: Int, val instructions: MutableList<Operator>)
    data class Operator(val name: String, val args: MutableList<String>)

}


fun main() {
    val file = "Test.guu"
    val debugger = GuuDebugger()
    debugger.parse(file)
    debugger.process()
}