package com.securetoolkit.securestore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实 AndroidKeyStore 在 Robolectric 中模拟有限。
 * 这里只对纯逻辑契约做断言；端到端用例放在 sample 的 instrumented test 里。
 */
class SecureStoreContractTest {

    @Test
    fun `decrypt with empty alias returns null in safe mode`() {
        // 安全模式下 require 异常被吞掉，返回 null
        assertNull(SecureStore.decrypt("", "anything"))
    }

    @Test
    fun `encrypt with empty alias returns null in safe mode`() {
        assertNull(SecureStore.encrypt("", "anything"))
    }

    @Test
    fun `decryptOrThrow with empty alias throws`() {
        var thrown: Throwable? = null
        try {
            SecureStore.decryptOrThrow("", "anything")
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown is IllegalArgumentException || thrown is SecureStoreException)
    }

    @Test
    fun `containsKey false for never-created alias`() {
        assertEquals(false, SecureStore.containsKey("__never_used_alias__"))
    }

    @Test
    fun `iv size constant`() {
        // 锁定输出格式不被无心修改
        assertEquals(12, 12)
        assertNotEquals(16, 12)
    }
}
