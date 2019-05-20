import org.junit.AfterClass
import org.junit.Test

import org.junit.Assert.*
import org.junit.BeforeClass
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class GuuDebuggerTest {
    val debugger = GuuDebugger()
    var file: Path = Paths.get("Test.guu")

    @Test
    fun parse() {
        debugger.parse(file.toString())
        debugger.process()
    }

    @Test
    fun printVariables() {

    }

}