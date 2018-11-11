package golf.skc.web.rest.errors

import org.springframework.dao.ConcurrencyFailureException
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.*

import javax.validation.Valid
import javax.validation.constraints.NotNull
import java.util.HashMap

@RestController
class ExceptionTranslatorTestController {

    @GetMapping("/test/concurrency-failure")
    fun concurrencyFailure() {
        throw ConcurrencyFailureException("test concurrency failure")
    }

    @PostMapping("/test/method-argument")
    fun methodArgument(@Valid @RequestBody testDTO: TestDTO) {
    }

    @GetMapping("/test/parameterized-error")
    fun parameterizedError() {
        throw CustomParameterizedException("test parameterized error", "param0_value", "param1_value")
    }

    @GetMapping("/test/parameterized-error2")
    fun parameterizedError2() {
        val params = HashMap<String, Any>()
        params["foo"] = "foo_value"
        params["bar"] = "bar_value"
        throw CustomParameterizedException("test parameterized error", params)
    }

    @GetMapping("/test/missing-servlet-request-part")
    fun missingServletRequestPartException(@RequestPart part: String) {
    }

    @GetMapping("/test/missing-servlet-request-parameter")
    fun missingServletRequestParameterException(@RequestParam param: String) {
    }

    @GetMapping("/test/access-denied")
    fun accessdenied() {
        throw AccessDeniedException("test access denied!")
    }

    @GetMapping("/test/unauthorized")
    fun unauthorized() {
        throw BadCredentialsException("test authentication failed!")
    }

    @GetMapping("/test/response-status")
    fun exceptionWithReponseStatus() {
        throw TestResponseStatusException()
    }

    @GetMapping("/test/internal-server-error")
    fun internalServerError() {
        throw RuntimeException()
    }

    class TestDTO {
        @NotNull
        var test: String? = null
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "test response status")
    class TestResponseStatusException : RuntimeException()

}
