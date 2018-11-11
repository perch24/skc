package golf.skc.web.rest

import golf.skc.SkcApp
import golf.skc.domain.User
import golf.skc.repository.UserRepository
import golf.skc.security.jwt.TokenProvider
import golf.skc.web.rest.errors.ExceptionTranslator
import golf.skc.web.rest.vm.LoginVM
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional

/**
 * Test class for the UserJWTController REST controller.
 *
 * @see UserJWTController
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = arrayOf(SkcApp::class))
class UserJWTControllerIntTest {

  @Autowired
  lateinit var tokenProvider: TokenProvider

  @Autowired
  lateinit var authenticationManager: AuthenticationManager

  @Autowired
  lateinit var userRepository: UserRepository

  @Autowired
  lateinit var passwordEncoder: PasswordEncoder

  @Autowired
  lateinit var exceptionTranslator: ExceptionTranslator

  private var mockMvc: MockMvc? = null

  @Before
  fun setup() {
    val userJWTController = UserJWTController(tokenProvider, authenticationManager)
    this.mockMvc = MockMvcBuilders.standaloneSetup(userJWTController)
      .setControllerAdvice(exceptionTranslator)
      .build()
  }

  @Test
  @Transactional
  @Throws(Exception::class)
  fun testAuthorize() {
    val user = User()
    user.login = "user-jwt-controller"
    user.email = "user-jwt-controller@example.com"
    user.activated = true
    user.password = passwordEncoder.encode("test")

    userRepository.saveAndFlush(user)

    val login = LoginVM()
    login.username = "user-jwt-controller"
    login.password = "test"
    mockMvc!!.perform(post("/api/authenticate")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(login)))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id_token").isString)
      .andExpect(jsonPath("$.id_token").isNotEmpty)
      .andExpect(header().string("Authorization", not(nullValue())))
      .andExpect(header().string("Authorization", not(isEmptyString())))
  }

  @Test
  @Transactional
  @Throws(Exception::class)
  fun testAuthorizeWithRememberMe() {
    val user = User()
    user.login = "user-jwt-controller-remember-me"
    user.email = "user-jwt-controller-remember-me@example.com"
    user.activated = true
    user.password = passwordEncoder.encode("test")

    userRepository.saveAndFlush(user)

    val login = LoginVM()
    login.username = "user-jwt-controller-remember-me"
    login.password = "test"
    login.isRememberMe = true
    mockMvc!!.perform(post("/api/authenticate")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(login)))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id_token").isString)
      .andExpect(jsonPath("$.id_token").isNotEmpty)
      .andExpect(header().string("Authorization", not(nullValue())))
      .andExpect(header().string("Authorization", not(isEmptyString())))
  }

  @Test
  @Transactional
  @Throws(Exception::class)
  fun testAuthorizeFails() {
    val login = LoginVM()
    login.username = "wrong-user"
    login.password = "wrong password"
    mockMvc!!.perform(post("/api/authenticate")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(login)))
      .andExpect(status().isUnauthorized)
      .andExpect(jsonPath("$.id_token").doesNotExist())
      .andExpect(header().doesNotExist("Authorization"))
  }
}
