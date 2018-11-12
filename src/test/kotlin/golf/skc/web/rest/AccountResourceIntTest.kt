package golf.skc.web.rest

import golf.skc.SkcApp
import golf.skc.config.Constants
import golf.skc.domain.Authority
import golf.skc.domain.User
import golf.skc.mockito.anyObject
import golf.skc.repository.AuthorityRepository
import golf.skc.repository.UserRepository
import golf.skc.security.AuthoritiesConstants
import golf.skc.service.MailService
import golf.skc.service.UserService
import golf.skc.service.dto.PasswordChangeDTO
import golf.skc.service.dto.UserDTO
import golf.skc.web.rest.errors.ExceptionTranslator
import golf.skc.web.rest.vm.KeyAndPasswordVM
import golf.skc.web.rest.vm.ManagedUserVM
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Test class for the AccountResource REST controller.
 *
 * @see AccountResource
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SkcApp::class])
@Transactional
class AccountResourceIntTest {

  @Autowired
  lateinit var userRepository: UserRepository

  @Autowired
  lateinit var authorityRepository: AuthorityRepository

  @Autowired
  lateinit var userService: UserService

  @Autowired
  lateinit var passwordEncoder: PasswordEncoder

  @Autowired
  lateinit var httpMessageConverters: Array<HttpMessageConverter<*>>

  @Autowired
  lateinit var exceptionTranslator: ExceptionTranslator

  @Mock
  lateinit var mockUserService: UserService

  @Mock
  lateinit var mockMailService: MailService

  lateinit var restAccountMvc: MockMvc

  lateinit var restUserMockMvc: MockMvc

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    doNothing().`when`(mockMailService).sendActivationEmail(anyObject())
    val accountResource = AccountResource(userRepository, userService, mockMailService)

    val accountUserMockResource = AccountResource(userRepository, mockUserService, mockMailService)
    this.restAccountMvc = MockMvcBuilders.standaloneSetup(accountResource)
      .setMessageConverters(*httpMessageConverters)
      .setControllerAdvice(exceptionTranslator)
      .build()
    this.restUserMockMvc = MockMvcBuilders.standaloneSetup(accountUserMockResource)
      .setControllerAdvice(exceptionTranslator)
      .build()
  }

  @Test
  fun testNonAuthenticatedUser() {
    restUserMockMvc.perform(get("/api/authenticate")
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().string(""))
  }

  @Test
  fun testAuthenticatedUser() {
    restUserMockMvc.perform(get("/api/authenticate")
      .with { request ->
        request.remoteUser = "test"
        request
      }
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().string("test"))
  }

  @Test
  fun testGetExistingAccount() {
    val authorities = HashSet<Authority>()
    val authority = Authority(AuthoritiesConstants.ADMIN)
    authorities.add(authority)

    val user = User()
    user.login = "test"
    user.firstName = "john"
    user.lastName = "doe"
    user.email = "john.doe@jhipster.com"
    user.imageUrl = "http://placehold.it/50x50"
    user.langKey = "en"
    user.authorities = authorities
    `when`(mockUserService.userWithAuthorities).thenReturn(Optional.of(user))

    restUserMockMvc.perform(get("/api/account")
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.login").value("test"))
      .andExpect(jsonPath("$.firstName").value("john"))
      .andExpect(jsonPath("$.lastName").value("doe"))
      .andExpect(jsonPath("$.email").value("john.doe@jhipster.com"))
      .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
      .andExpect(jsonPath("$.langKey").value("en"))
      .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN))
  }

  @Test
  fun testGetUnknownAccount() {
    `when`(mockUserService.userWithAuthorities).thenReturn(Optional.empty())

    restUserMockMvc.perform(get("/api/account")
      .accept(MediaType.APPLICATION_PROBLEM_JSON))
      .andExpect(status().isInternalServerError)
  }

  @Test
  fun testRegisterValid() {
    val validUser = ManagedUserVM()
    validUser.login = "test-register-valid"
    validUser.password = "password"
    validUser.firstName = "Alice"
    validUser.lastName = "Test"
    validUser.email = "test-register-valid@example.com"
    validUser.imageUrl = "http://placehold.it/50x50"
    validUser.langKey = Constants.DEFAULT_LANGUAGE
    validUser.authorities = setOf(AuthoritiesConstants.USER)
    assertThat(userRepository.findOneByLogin("test-register-valid").isPresent).isFalse()

    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(validUser)))
      .andExpect(status().isCreated)

    assertThat(userRepository.findOneByLogin("test-register-valid").isPresent).isTrue()
  }

  @Test
  fun testRegisterInvalidLogin() {
    val invalidUser = ManagedUserVM()
    invalidUser.login = "funky-log!n"// <-- invalid
    invalidUser.password = "password"
    invalidUser.firstName = "Funky"
    invalidUser.lastName = "One"
    invalidUser.email = "funky@example.com"
    invalidUser.isActivated = true
    invalidUser.imageUrl = "http://placehold.it/50x50"
    invalidUser.langKey = Constants.DEFAULT_LANGUAGE
    invalidUser.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
      .andExpect(status().isBadRequest)

    val user = userRepository.findOneByEmailIgnoreCase("funky@example.com")
    assertThat(user.isPresent).isFalse()
  }

  @Test
  fun testRegisterInvalidEmail() {
    val invalidUser = ManagedUserVM()
    invalidUser.login = "bob"
    invalidUser.password = "password"
    invalidUser.firstName = "Bob"
    invalidUser.lastName = "Green"
    invalidUser.email = "invalid"// <-- invalid
    invalidUser.isActivated = true
    invalidUser.imageUrl = "http://placehold.it/50x50"
    invalidUser.langKey = Constants.DEFAULT_LANGUAGE
    invalidUser.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
      .andExpect(status().isBadRequest)

    val user = userRepository.findOneByLogin("bob")
    assertThat(user.isPresent).isFalse()
  }

  @Test
  fun testRegisterInvalidPassword() {
    val invalidUser = ManagedUserVM()
    invalidUser.login = "bob"
    invalidUser.password = "123"// password with only 3 digits
    invalidUser.firstName = "Bob"
    invalidUser.lastName = "Green"
    invalidUser.email = "bob@example.com"
    invalidUser.isActivated = true
    invalidUser.imageUrl = "http://placehold.it/50x50"
    invalidUser.langKey = Constants.DEFAULT_LANGUAGE
    invalidUser.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
      .andExpect(status().isBadRequest)

    val user = userRepository.findOneByLogin("bob")
    assertThat(user.isPresent).isFalse()
  }

  @Test
  fun testRegisterNullPassword() {
    val invalidUser = ManagedUserVM()
    invalidUser.login = "bob"
    invalidUser.password = null// invalid null password
    invalidUser.firstName = "Bob"
    invalidUser.lastName = "Green"
    invalidUser.email = "bob@example.com"
    invalidUser.isActivated = true
    invalidUser.imageUrl = "http://placehold.it/50x50"
    invalidUser.langKey = Constants.DEFAULT_LANGUAGE
    invalidUser.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
      .andExpect(status().isBadRequest)

    val user = userRepository.findOneByLogin("bob")
    assertThat(user.isPresent).isFalse()
  }

  @Test
  fun testRegisterDuplicateLogin() {
    // First registration
    val firstUser = ManagedUserVM()
    firstUser.login = "alice"
    firstUser.password = "password"
    firstUser.firstName = "Alice"
    firstUser.lastName = "Something"
    firstUser.email = "alice@example.com"
    firstUser.imageUrl = "http://placehold.it/50x50"
    firstUser.langKey = Constants.DEFAULT_LANGUAGE
    firstUser.authorities = setOf(AuthoritiesConstants.USER)

    // Duplicate login, different email
    val secondUser = ManagedUserVM()
    secondUser.login = firstUser.login
    secondUser.password = firstUser.password
    secondUser.firstName = firstUser.firstName
    secondUser.lastName = firstUser.lastName
    secondUser.email = "alice2@example.com"
    secondUser.imageUrl = firstUser.imageUrl
    secondUser.langKey = firstUser.langKey
    secondUser.createdBy = firstUser.createdBy
    secondUser.createdDate = firstUser.createdDate
    secondUser.lastModifiedBy = firstUser.lastModifiedBy
    secondUser.lastModifiedDate = firstUser.lastModifiedDate
    secondUser.authorities = HashSet(firstUser.authorities)

    // First user
    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(firstUser)))
      .andExpect(status().isCreated)

    // Second (non activated) user
    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(secondUser)))
      .andExpect(status().isCreated)

    val testUser = userRepository.findOneByEmailIgnoreCase("alice2@example.com")
    assertThat(testUser.isPresent).isTrue()
    testUser.get().activated = true
    userRepository.save(testUser.get())

    // Second (already activated) user
    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(secondUser)))
      .andExpect(status().is4xxClientError)
  }

  @Test
  fun testRegisterDuplicateEmail() {
    // First user
    val firstUser = ManagedUserVM()
    firstUser.login = "test-register-duplicate-email"
    firstUser.password = "password"
    firstUser.firstName = "Alice"
    firstUser.lastName = "Test"
    firstUser.email = "test-register-duplicate-email@example.com"
    firstUser.imageUrl = "http://placehold.it/50x50"
    firstUser.langKey = Constants.DEFAULT_LANGUAGE
    firstUser.authorities = setOf(AuthoritiesConstants.USER)

    // Register first user
    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(firstUser)))
      .andExpect(status().isCreated)

    val testUser1 = userRepository.findOneByLogin("test-register-duplicate-email")
    assertThat(testUser1.isPresent).isTrue()

    // Duplicate email, different login
    val secondUser = ManagedUserVM()
    secondUser.login = "test-register-duplicate-email-2"
    secondUser.password = firstUser.password
    secondUser.firstName = firstUser.firstName
    secondUser.lastName = firstUser.lastName
    secondUser.email = firstUser.email
    secondUser.imageUrl = firstUser.imageUrl
    secondUser.langKey = firstUser.langKey
    secondUser.authorities = HashSet(firstUser.authorities)

    // Register second (non activated) user
    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(secondUser)))
      .andExpect(status().isCreated)

    val testUser2 = userRepository.findOneByLogin("test-register-duplicate-email")
    assertThat(testUser2.isPresent).isFalse()

    val testUser3 = userRepository.findOneByLogin("test-register-duplicate-email-2")
    assertThat(testUser3.isPresent).isTrue()

    // Duplicate email - with uppercase email address
    val userWithUpperCaseEmail = ManagedUserVM()
    userWithUpperCaseEmail.id = firstUser.id
    userWithUpperCaseEmail.login = "test-register-duplicate-email-3"
    userWithUpperCaseEmail.password = firstUser.password
    userWithUpperCaseEmail.firstName = firstUser.firstName
    userWithUpperCaseEmail.lastName = firstUser.lastName
    userWithUpperCaseEmail.email = "TEST-register-duplicate-email@example.com"
    userWithUpperCaseEmail.imageUrl = firstUser.imageUrl
    userWithUpperCaseEmail.langKey = firstUser.langKey
    userWithUpperCaseEmail.authorities = HashSet(firstUser.authorities)

    // Register third (not activated) user
    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(userWithUpperCaseEmail)))
      .andExpect(status().isCreated)

    val testUser4 = userRepository.findOneByLogin("test-register-duplicate-email-3")
    assertThat(testUser4.isPresent).isTrue()
    assertThat(testUser4.get().email).isEqualTo("test-register-duplicate-email@example.com")

    testUser4.get().activated = true
    userService.updateUser(UserDTO(testUser4.get()))

    // Register 4th (already activated) user
    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(secondUser)))
      .andExpect(status().is4xxClientError)
  }

  @Test
  fun testRegisterAdminIsIgnored() {
    val validUser = ManagedUserVM()
    validUser.login = "badguy"
    validUser.password = "password"
    validUser.firstName = "Bad"
    validUser.lastName = "Guy"
    validUser.email = "badguy@example.com"
    validUser.isActivated = true
    validUser.imageUrl = "http://placehold.it/50x50"
    validUser.langKey = Constants.DEFAULT_LANGUAGE
    validUser.authorities = setOf(AuthoritiesConstants.ADMIN)

    restAccountMvc.perform(
      post("/api/register")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(validUser)))
      .andExpect(status().isCreated)

    val userDup = userRepository.findOneByLogin("badguy")
    assertThat(userDup.isPresent).isTrue()
    assertThat(userDup.get().authorities).hasSize(1)
      .containsExactly(authorityRepository.findById(AuthoritiesConstants.USER).get())
  }

  @Test
  fun testActivateAccount() {
    val activationKey = "some activation key"
    var user = User()
    user.login = "activate-account"
    user.email = "activate-account@example.com"
    user.password = RandomStringUtils.random(60)
    user.activated = false
    user.activationKey = activationKey

    userRepository.saveAndFlush(user)

    restAccountMvc.perform(get("/api/activate?key={activationKey}", activationKey))
      .andExpect(status().isOk)

    user = userRepository.findOneByLogin(user.login!!).orElse(null)
    assertThat(user.activated).isTrue()
  }

  @Test
  fun testActivateAccountWithWrongKey() {
    restAccountMvc.perform(get("/api/activate?key=wrongActivationKey"))
      .andExpect(status().isInternalServerError)
  }

  @Test
  @WithMockUser("save-account")
  fun testSaveAccount() {
    val user = User()
    user.login = "save-account"
    user.email = "save-account@example.com"
    user.password = RandomStringUtils.random(60)
    user.activated = true

    userRepository.saveAndFlush(user)

    val userDTO = UserDTO()
    userDTO.login = "not-used"
    userDTO.firstName = "firstname"
    userDTO.lastName = "lastname"
    userDTO.email = "save-account@example.com"
    userDTO.isActivated = false
    userDTO.imageUrl = "http://placehold.it/50x50"
    userDTO.langKey = Constants.DEFAULT_LANGUAGE
    userDTO.authorities = setOf(AuthoritiesConstants.ADMIN)

    restAccountMvc.perform(
      post("/api/account")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(userDTO)))
      .andExpect(status().isOk)

    val (_, _, password, firstName, lastName, email, activated, langKey, imageUrl, _, _, _, authorities) = userRepository.findOneByLogin(user.login!!).orElse(null)
    assertThat(firstName).isEqualTo(userDTO.firstName)
    assertThat(lastName).isEqualTo(userDTO.lastName)
    assertThat(email).isEqualTo(userDTO.email)
    assertThat(langKey).isEqualTo(userDTO.langKey)
    assertThat(password).isEqualTo(user.password)
    assertThat(imageUrl).isEqualTo(userDTO.imageUrl)
    assertThat(activated).isEqualTo(true)
    assertThat(authorities).isEmpty()
  }

  @Test
  @WithMockUser("save-invalid-email")
  fun testSaveInvalidEmail() {
    val user = User()
    user.login = "save-invalid-email"
    user.email = "save-invalid-email@example.com"
    user.password = RandomStringUtils.random(60)
    user.activated = true

    userRepository.saveAndFlush(user)

    val userDTO = UserDTO()
    userDTO.login = "not-used"
    userDTO.firstName = "firstname"
    userDTO.lastName = "lastname"
    userDTO.email = "invalid email"
    userDTO.isActivated = false
    userDTO.imageUrl = "http://placehold.it/50x50"
    userDTO.langKey = Constants.DEFAULT_LANGUAGE
    userDTO.authorities = setOf(AuthoritiesConstants.ADMIN)

    restAccountMvc.perform(
      post("/api/account")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(userDTO)))
      .andExpect(status().isBadRequest)

    assertThat(userRepository.findOneByEmailIgnoreCase("invalid email")).isNotPresent
  }

  @Test
  @WithMockUser("save-existing-email")
  fun testSaveExistingEmail() {
    val user = User()
    user.login = "save-existing-email"
    user.email = "save-existing-email@example.com"
    user.password = RandomStringUtils.random(60)
    user.activated = true

    userRepository.saveAndFlush(user)

    val anotherUser = User()
    anotherUser.login = "save-existing-email2"
    anotherUser.email = "save-existing-email2@example.com"
    anotherUser.password = RandomStringUtils.random(60)
    anotherUser.activated = true

    userRepository.saveAndFlush(anotherUser)

    val userDTO = UserDTO()
    userDTO.login = "not-used"
    userDTO.firstName = "firstname"
    userDTO.lastName = "lastname"
    userDTO.email = "save-existing-email2@example.com"
    userDTO.isActivated = false
    userDTO.imageUrl = "http://placehold.it/50x50"
    userDTO.langKey = Constants.DEFAULT_LANGUAGE
    userDTO.authorities = setOf(AuthoritiesConstants.ADMIN)

    restAccountMvc.perform(
      post("/api/account")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(userDTO)))
      .andExpect(status().isBadRequest)

    val (_, _, _, _, _, email) = userRepository.findOneByLogin("save-existing-email").orElse(null)
    assertThat(email).isEqualTo("save-existing-email@example.com")
  }

  @Test
  @WithMockUser("save-existing-email-and-login")
  fun testSaveExistingEmailAndLogin() {
    val user = User()
    user.login = "save-existing-email-and-login"
    user.email = "save-existing-email-and-login@example.com"
    user.password = RandomStringUtils.random(60)
    user.activated = true

    userRepository.saveAndFlush(user)

    val userDTO = UserDTO()
    userDTO.login = "not-used"
    userDTO.firstName = "firstname"
    userDTO.lastName = "lastname"
    userDTO.email = "save-existing-email-and-login@example.com"
    userDTO.isActivated = false
    userDTO.imageUrl = "http://placehold.it/50x50"
    userDTO.langKey = Constants.DEFAULT_LANGUAGE
    userDTO.authorities = setOf(AuthoritiesConstants.ADMIN)

    restAccountMvc.perform(
      post("/api/account")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(userDTO)))
      .andExpect(status().isOk)

    val (_, _, _, _, _, email) = userRepository.findOneByLogin("save-existing-email-and-login").orElse(null)
    assertThat(email).isEqualTo("save-existing-email-and-login@example.com")
  }

  @Test
  @WithMockUser("change-password-wrong-existing-password")
  fun testChangePasswordWrongExistingPassword() {
    val user = User()
    val currentPassword = RandomStringUtils.random(60)
    user.password = passwordEncoder.encode(currentPassword)
    user.login = "change-password-wrong-existing-password"
    user.email = "change-password-wrong-existing-password@example.com"
    userRepository.saveAndFlush(user)

    restAccountMvc.perform(post("/api/account/change-password")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(PasswordChangeDTO("1$currentPassword", "new password"))))
      .andExpect(status().isBadRequest)

    val (_, _, password) = userRepository.findOneByLogin("change-password-wrong-existing-password").orElse(null)
    assertThat(passwordEncoder.matches("new password", password)).isFalse()
    assertThat(passwordEncoder.matches(currentPassword, password)).isTrue()
  }

  @Test
  @WithMockUser("change-password")
  fun testChangePassword() {
    val user = User()
    val currentPassword = RandomStringUtils.random(60)
    user.password = passwordEncoder.encode(currentPassword)
    user.login = "change-password"
    user.email = "change-password@example.com"
    userRepository.saveAndFlush(user)

    restAccountMvc.perform(post("/api/account/change-password")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, "new password"))))
      .andExpect(status().isOk)

    val (_, _, password) = userRepository.findOneByLogin("change-password").orElse(null)
    assertThat(passwordEncoder.matches("new password", password)).isTrue()
  }

  @Test
  @WithMockUser("change-password-too-small")
  fun testChangePasswordTooSmall() {
    val user = User()
    val currentPassword = RandomStringUtils.random(60)
    user.password = passwordEncoder.encode(currentPassword)
    user.login = "change-password-too-small"
    user.email = "change-password-too-small@example.com"
    userRepository.saveAndFlush(user)

    restAccountMvc.perform(post("/api/account/change-password")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, "new"))))
      .andExpect(status().isBadRequest)

    val (_, _, password) = userRepository.findOneByLogin("change-password-too-small").orElse(null)
    assertThat(password).isEqualTo(user.password)
  }

  @Test
  @WithMockUser("change-password-too-long")
  fun testChangePasswordTooLong() {
    val user = User()
    val currentPassword = RandomStringUtils.random(60)
    user.password = passwordEncoder.encode(currentPassword)
    user.login = "change-password-too-long"
    user.email = "change-password-too-long@example.com"
    userRepository.saveAndFlush(user)

    restAccountMvc.perform(post("/api/account/change-password")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, RandomStringUtils.random(101)))))
      .andExpect(status().isBadRequest)

    val (_, _, password) = userRepository.findOneByLogin("change-password-too-long").orElse(null)
    assertThat(password).isEqualTo(user.password)
  }

  @Test
  @WithMockUser("change-password-empty")
  fun testChangePasswordEmpty() {
    val user = User()
    user.password = RandomStringUtils.random(60)
    user.login = "change-password-empty"
    user.email = "change-password-empty@example.com"
    userRepository.saveAndFlush(user)

    restAccountMvc.perform(post("/api/account/change-password").content(RandomStringUtils.random(0)))
      .andExpect(status().isBadRequest)

    val (_, _, password) = userRepository.findOneByLogin("change-password-empty").orElse(null)
    assertThat(password).isEqualTo(user.password)
  }

  @Test
  fun testRequestPasswordReset() {
    val user = User()
    user.password = RandomStringUtils.random(60)
    user.activated = true
    user.login = "password-reset"
    user.email = "password-reset@example.com"
    userRepository.saveAndFlush(user)

    restAccountMvc.perform(post("/api/account/reset-password/init")
      .content("password-reset@example.com"))
      .andExpect(status().isOk)
  }

  @Test
  fun testRequestPasswordResetUpperCaseEmail() {
    val user = User()
    user.password = RandomStringUtils.random(60)
    user.activated = true
    user.login = "password-reset"
    user.email = "password-reset@example.com"
    userRepository.saveAndFlush(user)

    restAccountMvc.perform(post("/api/account/reset-password/init")
      .content("password-reset@EXAMPLE.COM"))
      .andExpect(status().isOk)
  }

  @Test
  fun testRequestPasswordResetWrongEmail() {
    restAccountMvc.perform(
      post("/api/account/reset-password/init")
        .content("password-reset-wrong-email@example.com"))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun testFinishPasswordReset() {
    val user = User()
    val resetKey = "reset key"
    user.password = RandomStringUtils.random(60)
    user.login = "finish-password-reset"
    user.email = "finish-password-reset@example.com"
    user.resetDate = Instant.now().plusSeconds(60)
    user.resetKey = resetKey
    userRepository.saveAndFlush(user)

    val keyAndPassword = KeyAndPasswordVM(key = resetKey, newPassword = "new password")

    restAccountMvc.perform(
      post("/api/account/reset-password/finish")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
      .andExpect(status().isOk)

    val (_, _, password) = userRepository.findOneByLogin(user.login!!).orElse(null)
    assertThat(passwordEncoder.matches(keyAndPassword.newPassword, password)).isTrue()
  }

  @Test
  fun testFinishPasswordResetTooSmall() {
    val user = User()
    val resetKey = "reset key too small"
    user.password = RandomStringUtils.random(60)
    user.login = "finish-password-reset-too-small"
    user.email = "finish-password-reset-too-small@example.com"
    user.resetDate = Instant.now().plusSeconds(60)
    user.resetKey = resetKey
    userRepository.saveAndFlush(user)

    val keyAndPassword = KeyAndPasswordVM(key = resetKey, newPassword = "foo")

    restAccountMvc.perform(
      post("/api/account/reset-password/finish")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
      .andExpect(status().isBadRequest)

    val (_, _, password) = userRepository.findOneByLogin(user.login!!).orElse(null)
    assertThat(passwordEncoder.matches(keyAndPassword.newPassword, password)).isFalse()
  }


  @Test
  fun testFinishPasswordResetWrongKey() {
    val keyAndPassword = KeyAndPasswordVM(key = "wrong reset key", newPassword = "new password")

    restAccountMvc.perform(
      post("/api/account/reset-password/finish")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(keyAndPassword)))
      .andExpect(status().isInternalServerError)
  }
}
