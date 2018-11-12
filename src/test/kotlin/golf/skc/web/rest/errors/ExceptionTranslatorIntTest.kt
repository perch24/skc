package golf.skc.web.rest.errors

import golf.skc.SkcApp
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Test class for the ExceptionTranslator controller advice.
 *
 * @see ExceptionTranslator
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SkcApp::class])
class ExceptionTranslatorIntTest {

  @Autowired
  lateinit var controller: ExceptionTranslatorTestController

  @Autowired
  lateinit var exceptionTranslator: ExceptionTranslator

  @Autowired
  lateinit var jacksonMessageConverter: MappingJackson2HttpMessageConverter

  lateinit var mockMvc: MockMvc

  @Before
  fun setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
      .setControllerAdvice(exceptionTranslator)
      .setMessageConverters(jacksonMessageConverter)
      .build()
  }

  @Test
  fun testConcurrencyFailure() {
    mockMvc.perform(get("/test/concurrency-failure"))
      .andExpect(status().isConflict)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_CONCURRENCY_FAILURE))
  }

  @Test
  fun testMethodArgumentNotValid() {
    mockMvc.perform(post("/test/method-argument").content("{}").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value(ErrorConstants.ERR_VALIDATION))
      .andExpect(jsonPath("$.fieldErrors.[0].objectName").value("testDTO"))
      .andExpect(jsonPath("$.fieldErrors.[0].field").value("test"))
      .andExpect(jsonPath("$.fieldErrors.[0].message").value("NotNull"))
  }

  @Test
  fun testParameterizedError() {
    mockMvc.perform(get("/test/parameterized-error"))
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("test parameterized error"))
      .andExpect(jsonPath("$.params.param0").value("param0_value"))
      .andExpect(jsonPath("$.params.param1").value("param1_value"))
  }

  @Test
  fun testParameterizedError2() {
    mockMvc.perform(get("/test/parameterized-error2"))
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("test parameterized error"))
      .andExpect(jsonPath("$.params.foo").value("foo_value"))
      .andExpect(jsonPath("$.params.bar").value("bar_value"))
  }

  @Test
  fun testMissingServletRequestPartException() {
    mockMvc.perform(get("/test/missing-servlet-request-part"))
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("error.http.400"))
  }

  @Test
  fun testMissingServletRequestParameterException() {
    mockMvc.perform(get("/test/missing-servlet-request-parameter"))
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("error.http.400"))
  }

  @Test
  fun testAccessDenied() {
    mockMvc.perform(get("/test/access-denied"))
      .andExpect(status().isForbidden)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("error.http.403"))
      .andExpect(jsonPath("$.detail").value("test access denied!"))
  }

  @Test
  fun testUnauthorized() {
    mockMvc.perform(get("/test/unauthorized"))
      .andExpect(status().isUnauthorized)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("error.http.401"))
      .andExpect(jsonPath("$.path").value("/test/unauthorized"))
      .andExpect(jsonPath("$.detail").value("test authentication failed!"))
  }

  @Test
  fun testMethodNotSupported() {
    mockMvc.perform(post("/test/access-denied"))
      .andExpect(status().isMethodNotAllowed)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("error.http.405"))
      .andExpect(jsonPath("$.detail").value("Request method 'POST' not supported"))
  }

  @Test
  fun testExceptionWithResponseStatus() {
    mockMvc.perform(get("/test/response-status"))
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("error.http.400"))
      .andExpect(jsonPath("$.title").value("test response status"))
  }

  @Test
  fun testInternalServerError() {
    mockMvc.perform(get("/test/internal-server-error"))
      .andExpect(status().isInternalServerError)
      .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(jsonPath("$.message").value("error.http.500"))
      .andExpect(jsonPath("$.title").value("Internal Server Error"))
  }
}
