package request

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RequestTest {

    private val request = Request()

    @Test
    fun testRequest() {
        val a = runBlocking {
            request.getAllTools()
        }
        assertTrue(a.isNotEmpty())
    }

    @Test
    fun testTools() {
        val a = runBlocking {
            request.invokeTool(Tools.C_MD5, Params("", "123"))
        }
        assertTrue(a.isNotEmpty())
    }
}