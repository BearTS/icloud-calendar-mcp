package org.onekash.mcp.calendar

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpAuthTest {
    @Test
    fun `accepts the exact bearer token`() = assertTrue(isAuthorized("Bearer s3cret", "s3cret"))

    @Test
    fun `rejects missing, wrong and non-bearer headers`() {
        assertFalse(isAuthorized(null, "s3cret"))
        assertFalse(isAuthorized("Bearer nope", "s3cret"))
        assertFalse(isAuthorized("s3cret", "s3cret"))
        assertFalse(isAuthorized("Basic s3cret", "s3cret"))
    }
}
