import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.*
import kotlin.NoSuchElementException

class GuuDebugger {

    private val stack = Stack<Function>()
    private val variables = mutableMapOf<String, Int>()
    private val nameToFunction = mutableMapOf<String, Function>()
    private lateinit var reader: BufferedReader
    private var savedStack = Stack<Function>()


    fun parse(path: String) {
        var counter = 0
        lateinit var tmpFunction: Function

        try {
            reader = FileReader(path).buffered()
        } catch (exc: FileNotFoundException) {
            throw exc
        }

        reader.use { reader ->
            var nextLine: String?
            nextLine = reader.readLine()
            while (nextLine != null) {
                val line = nextLine.split("\\p{Space}+".toRegex()).filter { it != "" }
                counter++

                nextLine = reader.readLine()

                if (line.isEmpty()) {
                    continue
                }

                if (line.size < 2) {
                    throw IllegalArgumentException("There's no command with ${line.size} words: $line ($counter line)")
                }

                when (line[0]) {
                    "sub" -> {
                        tmpFunction = Function(line[1], counter, mutableListOf())
                        nameToFunction[tmpFunction.name] = tmpFunction
                    }
                    "set" -> {
                        tmpFunction.instructions.add(Operator(line[0], mutableListOf(line[1], line[2])))
                    }
                    "call", "print" -> {
                        tmpFunction.instructions.add(Operator(line[0], mutableListOf(line[1])))
                    }
                    else -> {
                        throw NumberFormatException("Unexpected keyword \"${line[0]}\" found at $counter line.")
                    }
                }

            }
        }
    }

    fun process() {
        val main = nameToFunction["main"] ?: throw NoSuchMethodException("Can't find main-function in the source code")

        reader = System.`in`.bufferedReader()

        reader.use {
            try {
                execFunction(main, false)
            } catch (exc: Throwable) {
                throw exc
            }
        }
    }

    private fun execFunction(function: Function, hidden: Boolean) {
        stack.push(function)
        var count = 0

        for (line in function.instructions) {
            var newHidden = true
            count++

            if (!hidden) {
                var commandWaiting = true
                while (commandWaiting) {
                    when (reader.readLine()) {
                        "i" -> {
                            if (line.name == "call")
                                newHidden = false
                            commandWaiting = false
                        }
                        "o" -> commandWaiting = false
                        "trace" -> {
                            println("Now you're stay on ${count + function.startLineNumber} line")
                            printStackTrace(stack)
                        }
                        "var" -> {
                            println("There're ${variables.size} variables:")
                            printVariables()
                        }
                        "save" -> {
                            savedStack.clear()
                            savedStack.addAll(stack)
                        }
                        else -> {
                            throw IllegalArgumentException("Unknown symbol found")
                        }
                    }
                }
            }

            when (line.name) {
                "call" -> {
                    val nextFunction = nameToFunction[line.args.first()]
                            ?: throw NoSuchMethodException("Can't find function ${line.args.first()}")
                    try {
                        if (hidden || newHidden)
                            execFunction(nextFunction, true)
                        else
                            execFunction(nextFunction, false)
                    } catch (exc: StackOverflowError) {
                        throw StackOverflowError("Oops, stack overflowed")
                    }
                }
                "set" -> {
                    var variable: Int
                    try {
                        variable = line.args[1].toInt()
                    } catch (exc: NumberFormatException) {
                        throw java.lang.NumberFormatException("${line.args[1]} is not a number, parsing failed")
                    }
                    variables[line.args.first()] = variable
                }
                "print" -> {
                    val variableName = line.args.first()
                    if (variables.containsKey(variableName))
                        println(variableName)
                    else {
                        throw NoSuchElementException("Variable $variableName doesn't exist")
                    }
                }
            }
        }

        stack.pop()
    }

    fun getSavedStackForTest(): Stack<Function> = savedStack

    fun getNameToFunctionsForTest(): MutableMap<String, Function> = nameToFunction

    private fun printStackTrace(stack: Stack<Function>): Unit = stack.toList()
            .asReversed()
            .forEach { func -> println("${func.startLineNumber}: ${func.name}") }

    private fun printVariables(): Unit = variables.forEach { println("${it.key}: ${it.value}") }

    fun getVariablesForTest(variable: String): Int? {
        return variables[variable]
    }

    data class Function(val name: String, val startLineNumber: Int, val instructions: MutableList<Operator>)
    data class Operator(val name: String, val args: MutableList<String>)

}


fun main(args: Array<String>) {

    if (args.size != 1) {
        System.err.println("Expected the only one argument, found ${args.size}")
        return
    }

    val file = args[0]

    val debugger = GuuDebugger()

    try {
        debugger.parse(file)
    } catch (exc: IOException) {
        System.err.println("An error occurred during reading the file")
    } catch (exc: FileNotFoundException) {
        System.err.println("File $file doesn't exist")
    } catch (exc: IllegalArgumentException) {
        System.err.println(exc.message)
    } catch (exc: NumberFormatException) {
        System.err.println(exc.message)
    }

    try {
        debugger.process()
    } catch (exc: Throwable) {
        System.err.println(exc.message)
    }


}