import junit.framework.TestCase.assertNull
import org.junit.*
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import java.util.*
import kotlin.NoSuchElementException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuuDebuggerTest {
    private var debugger = GuuDebugger()
    private var caught = false

    @Before
    fun before() {
        caught = false
        debugger = GuuDebugger()
    }

    @After
    fun after() {
        System.setIn(System.`in`)
        System.setOut(System.out)
    }

    @Test
    fun parse() {
        debugger.parse("test/testResources/parseTest.guu")
        val expected = mutableSetOf<GuuDebugger.Function>()
        expected.add(GuuDebugger.Function("main", 1, mutableListOf(
                GuuDebugger.Operator("call", mutableListOf("foo"))
        )))


        expected.add(GuuDebugger.Function("foo", 4, mutableListOf(
                GuuDebugger.Operator("set", mutableListOf("a", "15")),
                GuuDebugger.Operator("call", mutableListOf("b"))
        )))

        expected.add(GuuDebugger.Function("b", 21, mutableListOf(
                GuuDebugger.Operator("print", mutableListOf("a"))
        )))

        val debuggerMap = debugger.getNameToFunctionsForTest()
        assertEquals(expected.size, debuggerMap.size)
        for (elem in expected) {
            assertEquals(elem, debuggerMap[elem.name])
        }
    }


    @Test
    fun process() {
        debugger.parse("test/testResources/simpleProgram.guu")
        System.setIn("i\ni\ni\ni\ni\ni\ni\ni\ni".byteInputStream())
        debugger.process()
        assertEquals(debugger.getVariablesForTest("a"), 4)
    }

    @Test
    fun processWithRec() {
        System.setIn("o".byteInputStream())

        debugger.parse("test/testResources/recTest.guu")
        try {
            debugger.process()
        } catch (exc: StackOverflowError) {
            println("Stack overflowed")
            caught = true
        }
        assertTrue(caught)
    }

    @Test
    fun fileDoesntExist() {
        try {
            debugger.parse("asflkjsandflkjasnflkjsndlfkjnaslkjfn")
        } catch (exc: FileNotFoundException) {
            println("File \"asflkjsandflkjasnflkjsndlfkjnaslkjfn\" not found")
            caught = true
        }
        assertTrue(caught)
    }

    @Test
    fun parseWrongCommand() {
        try {
            debugger.parse("test/testResources/wrongCommand.guu")
        } catch (exc: IllegalArgumentException) {
            caught = true
        }
        assertTrue(caught)
    }

    @Test
    fun parseUnknownKeyword() {
        try {
            debugger.parse("test/testResources/unknownKeyword.guu")
        } catch (exc: NumberFormatException) {
            caught = true
        }
        assertTrue(caught)
    }

    @Test
    fun withoutMain() {
        debugger.parse("test/testResources/withoutMain.guu")
        try {
            debugger.process()
        } catch (exc: NoSuchMethodException) {
            caught = true
        }
        assertTrue { caught }
    }

    @Test
    fun printAbsentVariable() {
        debugger.parse("test/testResources/printAbsentVariables.guu")
        try {
            System.setIn("o".byteInputStream())
            debugger.process()
        } catch (exc: NoSuchElementException) {
            caught = true
        }
        assertTrue { caught }
    }

    @Test
    fun callAbsentFunction() {
        debugger.parse("test/testResources/callAbsentFunction.guu")
        try {
            System.setIn("o".byteInputStream())
            debugger.process()
        } catch (exc: NoSuchMethodException) {
            caught = true
        }
        assertTrue { caught }
    }

    @Test
    fun getVariables() {
        debugger.parse("test/testResources/variables.guu")
        System.setIn("o".byteInputStream())
        debugger.process()
        assertEquals(5, debugger.getVariablesForTest("a"))
        assertEquals(4, debugger.getVariablesForTest("b"))
        assertEquals(3, debugger.getVariablesForTest("c"))
        assertEquals(2, debugger.getVariablesForTest("d"))
        assertEquals(1, debugger.getVariablesForTest("e"))
        assertNull(debugger.getVariablesForTest("qwe"))
    }

    @Test
    fun checkStackTrace() {
        debugger.parse("test/testResources/simpleProgram.guu")
        System.setIn("i\ni\ni\nsave\ni\ni".byteInputStream())
        debugger.process()
        val stack = debugger.getSavedStackForTest()
        val expectedStack = Stack<GuuDebugger.Function>()
        expectedStack.add(GuuDebugger.Function("main", 1, mutableListOf(
                GuuDebugger.Operator("set", mutableListOf("a", "3")),
                GuuDebugger.Operator("print", mutableListOf("a")),
                GuuDebugger.Operator("call", mutableListOf("foo")),
                GuuDebugger.Operator("print", mutableListOf("a"))
        )))
        expectedStack.add(GuuDebugger.Function("foo", 7, mutableListOf(
                GuuDebugger.Operator("set", mutableListOf("a", "4"))
        )))
        assertEquals(expectedStack, stack)
    }

}