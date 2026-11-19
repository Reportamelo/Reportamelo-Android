package app.reportamelo.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MsgPackJsonConverterTest {

    @Test
    fun `rejects nesting deeper than the limit instead of overflowing the stack`() {
        // 100k nested single-element arrays (0x91) followed by nil
        val bytes = ByteArray(100_000) { 0x91.toByte() } + 0xc0.toByte()
        assertThrows(IllegalArgumentException::class.java) {
            MsgPackJsonConverter.toJsonElement(bytes)
        }
    }

    @Test
    fun `rejects array and map sizes larger than the remaining payload`() {
        // array32 / map32 declaring 0x7fffffff entries with no data behind them
        val hugeArray = byteArrayOf(0xdd.toByte(), 0x7f, 0xff.toByte(), 0xff.toByte(), 0xff.toByte())
        val hugeMap = byteArrayOf(0xdf.toByte(), 0x7f, 0xff.toByte(), 0xff.toByte(), 0xff.toByte())
        assertThrows(IllegalArgumentException::class.java) { MsgPackJsonConverter.toJsonElement(hugeArray) }
        assertThrows(IllegalArgumentException::class.java) { MsgPackJsonConverter.toJsonElement(hugeMap) }
    }

    @Test
    fun `still decodes nested structures within the limit`() {
        // {"a": [1, 2]}
        val bytes = byteArrayOf(0x81.toByte(), 0xa1.toByte(), 'a'.code.toByte(), 0x92.toByte(), 0x01, 0x02)
        assertEquals("""{"a":[1,2]}""", MsgPackJsonConverter.toJsonElement(bytes).toString())
    }
}
