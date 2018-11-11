package golf.skc.mockito

import org.mockito.Mockito

fun <T> anyObject(): T {
    return Mockito.any<T>()
}
