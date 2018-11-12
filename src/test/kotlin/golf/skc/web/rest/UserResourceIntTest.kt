package golf.skc.web.rest

import golf.skc.SkcApp
import golf.skc.domain.Authority
import golf.skc.domain.User
import golf.skc.repository.UserRepository
import golf.skc.security.AuthoritiesConstants
import golf.skc.service.MailService
import golf.skc.service.UserService
import golf.skc.service.dto.UserDTO
import golf.skc.service.mapper.UserMapper
import golf.skc.web.rest.errors.ExceptionTranslator
import golf.skc.web.rest.vm.ManagedUserVM
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasItems
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import javax.persistence.EntityManager

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserResource
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SkcApp::class])
@Transactional
class UserResourceIntTest {

  @Autowired
  lateinit var userRepository: UserRepository

  @Autowired
  lateinit var mailService: MailService

  @Autowired
  lateinit var userService: UserService

  @Autowired
  lateinit var userMapper: UserMapper

  @Autowired
  lateinit var jacksonMessageConverter: MappingJackson2HttpMessageConverter

  @Autowired
  lateinit var pageableArgumentResolver: PageableHandlerMethodArgumentResolver

  @Autowired
  lateinit var exceptionTranslator: ExceptionTranslator

  @Autowired
  lateinit var cacheManager: CacheManager

  lateinit var restUserMockMvc: MockMvc

  lateinit var user: User

  @Before
  fun setup() {
    cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)?.clear()
    cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)?.clear()
    val userResource = UserResource(userService, userRepository, mailService)

    this.restUserMockMvc = MockMvcBuilders.standaloneSetup(userResource)
      .setCustomArgumentResolvers(pageableArgumentResolver)
      .setControllerAdvice(exceptionTranslator)
      .setMessageConverters(jacksonMessageConverter)
      .build()
  }

  @Before
  fun initTest() {
    user = createEntity()
    user.login = DEFAULT_LOGIN
    user.email = DEFAULT_EMAIL
  }

  @Test
  fun createUser() {
    val databaseSizeBeforeCreate = userRepository.findAll().size

    // Create the User
    val managedUserVM = ManagedUserVM()
    managedUserVM.login = DEFAULT_LOGIN
    managedUserVM.password = DEFAULT_PASSWORD
    managedUserVM.firstName = DEFAULT_FIRSTNAME
    managedUserVM.lastName = DEFAULT_LASTNAME
    managedUserVM.email = DEFAULT_EMAIL
    managedUserVM.isActivated = true
    managedUserVM.imageUrl = DEFAULT_IMAGEURL
    managedUserVM.langKey = DEFAULT_LANGKEY
    managedUserVM.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(post("/api/users")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
      .andExpect(status().isCreated)

    // Validate the User in the database
    val userList = userRepository.findAll()
    assertThat(userList).hasSize(databaseSizeBeforeCreate + 1)
    val (_, login, _, firstName, lastName, email, _, langKey, imageUrl) = userList[userList.size - 1]
    assertThat(login).isEqualTo(DEFAULT_LOGIN)
    assertThat(firstName).isEqualTo(DEFAULT_FIRSTNAME)
    assertThat(lastName).isEqualTo(DEFAULT_LASTNAME)
    assertThat(email).isEqualTo(DEFAULT_EMAIL)
    assertThat(imageUrl).isEqualTo(DEFAULT_IMAGEURL)
    assertThat(langKey).isEqualTo(DEFAULT_LANGKEY)
  }

  @Test
  fun createUserWithExistingId() {
    val databaseSizeBeforeCreate = userRepository.findAll().size

    val managedUserVM = ManagedUserVM()
    managedUserVM.id = 1L
    managedUserVM.login = DEFAULT_LOGIN
    managedUserVM.password = DEFAULT_PASSWORD
    managedUserVM.firstName = DEFAULT_FIRSTNAME
    managedUserVM.lastName = DEFAULT_LASTNAME
    managedUserVM.email = DEFAULT_EMAIL
    managedUserVM.isActivated = true
    managedUserVM.imageUrl = DEFAULT_IMAGEURL
    managedUserVM.langKey = DEFAULT_LANGKEY
    managedUserVM.authorities = setOf(AuthoritiesConstants.USER)

    // An entity with an existing ID cannot be created, so this API call must fail
    restUserMockMvc.perform(post("/api/users")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
      .andExpect(status().isBadRequest)

    // Validate the User in the database
    val userList = userRepository.findAll()
    assertThat(userList).hasSize(databaseSizeBeforeCreate)
  }

  @Test
  fun createUserWithExistingLogin() {
    // Initialize the database
    userRepository.saveAndFlush(user)
    val databaseSizeBeforeCreate = userRepository.findAll().size

    val managedUserVM = ManagedUserVM()
    managedUserVM.login = DEFAULT_LOGIN// this login should already be used
    managedUserVM.password = DEFAULT_PASSWORD
    managedUserVM.firstName = DEFAULT_FIRSTNAME
    managedUserVM.lastName = DEFAULT_LASTNAME
    managedUserVM.email = "anothermail@localhost"
    managedUserVM.isActivated = true
    managedUserVM.imageUrl = DEFAULT_IMAGEURL
    managedUserVM.langKey = DEFAULT_LANGKEY
    managedUserVM.authorities = setOf(AuthoritiesConstants.USER)

    // Create the User
    restUserMockMvc.perform(post("/api/users")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
      .andExpect(status().isBadRequest)

    // Validate the User in the database
    val userList = userRepository.findAll()
    assertThat(userList).hasSize(databaseSizeBeforeCreate)
  }

  @Test
  fun createUserWithExistingEmail() {
    // Initialize the database
    userRepository.saveAndFlush(user)
    val databaseSizeBeforeCreate = userRepository.findAll().size

    val managedUserVM = ManagedUserVM()
    managedUserVM.login = "anotherlogin"
    managedUserVM.password = DEFAULT_PASSWORD
    managedUserVM.firstName = DEFAULT_FIRSTNAME
    managedUserVM.lastName = DEFAULT_LASTNAME
    managedUserVM.email = DEFAULT_EMAIL// this email should already be used
    managedUserVM.isActivated = true
    managedUserVM.imageUrl = DEFAULT_IMAGEURL
    managedUserVM.langKey = DEFAULT_LANGKEY
    managedUserVM.authorities = setOf(AuthoritiesConstants.USER)

    // Create the User
    restUserMockMvc.perform(post("/api/users")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
      .andExpect(status().isBadRequest)

    // Validate the User in the database
    val userList = userRepository.findAll()
    assertThat(userList).hasSize(databaseSizeBeforeCreate)
  }

  @Test
  fun getAllUsers() {
    // Initialize the database
    userRepository.saveAndFlush(user)

    // Get all the users
    restUserMockMvc.perform(get("/api/users?sort=id,desc")
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
      .andExpect(jsonPath("$.[*].firstName").value(hasItem(DEFAULT_FIRSTNAME)))
      .andExpect(jsonPath("$.[*].lastName").value(hasItem(DEFAULT_LASTNAME)))
      .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
      .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGEURL)))
      .andExpect(jsonPath("$.[*].langKey").value(hasItem(DEFAULT_LANGKEY)))
  }

  @Test
  fun getUser() {
    // Initialize the database
    userRepository.saveAndFlush(user)

    assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)!!.get(user.login!!)).isNull()

    // Get the user
    restUserMockMvc.perform(get("/api/users/{login}", user.login!!))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.login").value(user.login!!))
      .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
      .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
      .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
      .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGEURL))
      .andExpect(jsonPath("$.langKey").value(DEFAULT_LANGKEY))

    assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)?.get(user.login!!)).isNotNull
  }

  @Test
  fun getNonExistingUser() {
    restUserMockMvc.perform(get("/api/users/unknown"))
      .andExpect(status().isNotFound)
  }

  @Test
  fun updateUser() {
    // Initialize the database
    userRepository.saveAndFlush(user)
    val databaseSizeBeforeUpdate = userRepository.findAll().size

    // Update the user
    val updatedUser = userRepository.findById(user.id!!).get()

    val managedUserVM = ManagedUserVM()
    managedUserVM.id = updatedUser.id
    managedUserVM.login = updatedUser.login
    managedUserVM.password = UPDATED_PASSWORD
    managedUserVM.firstName = UPDATED_FIRSTNAME
    managedUserVM.lastName = UPDATED_LASTNAME
    managedUserVM.email = UPDATED_EMAIL
    managedUserVM.isActivated = updatedUser.activated
    managedUserVM.imageUrl = UPDATED_IMAGEURL
    managedUserVM.langKey = UPDATED_LANGKEY
    managedUserVM.createdBy = updatedUser.createdBy
    managedUserVM.createdDate = updatedUser.createdDate
    managedUserVM.lastModifiedBy = updatedUser.lastModifiedBy
    managedUserVM.lastModifiedDate = updatedUser.lastModifiedDate
    managedUserVM.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(put("/api/users")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
      .andExpect(status().isOk)

    // Validate the User in the database
    val userList = userRepository.findAll()
    assertThat(userList).hasSize(databaseSizeBeforeUpdate)
    val (_, _, _, firstName, lastName, email, _, langKey, imageUrl) = userList[userList.size - 1]
    assertThat(firstName).isEqualTo(UPDATED_FIRSTNAME)
    assertThat(lastName).isEqualTo(UPDATED_LASTNAME)
    assertThat(email).isEqualTo(UPDATED_EMAIL)
    assertThat(imageUrl).isEqualTo(UPDATED_IMAGEURL)
    assertThat(langKey).isEqualTo(UPDATED_LANGKEY)
  }

  @Test
  fun updateUserLogin() {
    // Initialize the database
    userRepository.saveAndFlush(user)
    val databaseSizeBeforeUpdate = userRepository.findAll().size

    // Update the user
    val updatedUser = userRepository.findById(user.id!!).get()

    val managedUserVM = ManagedUserVM()
    managedUserVM.id = updatedUser.id
    managedUserVM.login = UPDATED_LOGIN
    managedUserVM.password = UPDATED_PASSWORD
    managedUserVM.firstName = UPDATED_FIRSTNAME
    managedUserVM.lastName = UPDATED_LASTNAME
    managedUserVM.email = UPDATED_EMAIL
    managedUserVM.isActivated = updatedUser.activated
    managedUserVM.imageUrl = UPDATED_IMAGEURL
    managedUserVM.langKey = UPDATED_LANGKEY
    managedUserVM.createdBy = updatedUser.createdBy
    managedUserVM.createdDate = updatedUser.createdDate
    managedUserVM.lastModifiedBy = updatedUser.lastModifiedBy
    managedUserVM.lastModifiedDate = updatedUser.lastModifiedDate
    managedUserVM.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(put("/api/users")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
      .andExpect(status().isOk)

    // Validate the User in the database
    val userList = userRepository.findAll()
    assertThat(userList).hasSize(databaseSizeBeforeUpdate)
    val (_, login, _, firstName, lastName, email, _, langKey, imageUrl) = userList[userList.size - 1]
    assertThat(login).isEqualTo(UPDATED_LOGIN)
    assertThat(firstName).isEqualTo(UPDATED_FIRSTNAME)
    assertThat(lastName).isEqualTo(UPDATED_LASTNAME)
    assertThat(email).isEqualTo(UPDATED_EMAIL)
    assertThat(imageUrl).isEqualTo(UPDATED_IMAGEURL)
    assertThat(langKey).isEqualTo(UPDATED_LANGKEY)
  }

  @Test
  fun updateUserExistingEmail() {
    // Initialize the database with 2 users
    userRepository.saveAndFlush(user)

    val anotherUser = User()
    anotherUser.login = "jhipster"
    anotherUser.password = RandomStringUtils.random(60)
    anotherUser.activated = true
    anotherUser.email = "jhipster@localhost"
    anotherUser.firstName = "java"
    anotherUser.lastName = "hipster"
    anotherUser.imageUrl = ""
    anotherUser.langKey = "en"
    userRepository.saveAndFlush(anotherUser)

    // Update the user
    val updatedUser = userRepository.findById(user.id!!).get()

    val managedUserVM = ManagedUserVM()
    managedUserVM.id = updatedUser.id
    managedUserVM.login = updatedUser.login
    managedUserVM.password = updatedUser.password
    managedUserVM.firstName = updatedUser.firstName
    managedUserVM.lastName = updatedUser.lastName
    managedUserVM.email = "jhipster@localhost"// this email should already be used by anotherUser
    managedUserVM.isActivated = updatedUser.activated
    managedUserVM.imageUrl = updatedUser.imageUrl
    managedUserVM.langKey = updatedUser.langKey
    managedUserVM.createdBy = updatedUser.createdBy
    managedUserVM.createdDate = updatedUser.createdDate
    managedUserVM.lastModifiedBy = updatedUser.lastModifiedBy
    managedUserVM.lastModifiedDate = updatedUser.lastModifiedDate
    managedUserVM.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(put("/api/users")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun updateUserExistingLogin() {
    // Initialize the database
    userRepository.saveAndFlush(user)

    val anotherUser = User()
    anotherUser.login = "jhipster"
    anotherUser.password = RandomStringUtils.random(60)
    anotherUser.activated = true
    anotherUser.email = "jhipster@localhost"
    anotherUser.firstName = "java"
    anotherUser.lastName = "hipster"
    anotherUser.imageUrl = ""
    anotherUser.langKey = "en"
    userRepository.saveAndFlush(anotherUser)

    // Update the user
    val updatedUser = userRepository.findById(user.id!!).get()

    val managedUserVM = ManagedUserVM()
    managedUserVM.id = updatedUser.id
    managedUserVM.login = "jhipster"// this login should already be used by anotherUser
    managedUserVM.password = updatedUser.password
    managedUserVM.firstName = updatedUser.firstName
    managedUserVM.lastName = updatedUser.lastName
    managedUserVM.email = updatedUser.email
    managedUserVM.isActivated = updatedUser.activated
    managedUserVM.imageUrl = updatedUser.imageUrl
    managedUserVM.langKey = updatedUser.langKey
    managedUserVM.createdBy = updatedUser.createdBy
    managedUserVM.createdDate = updatedUser.createdDate
    managedUserVM.lastModifiedBy = updatedUser.lastModifiedBy
    managedUserVM.lastModifiedDate = updatedUser.lastModifiedDate
    managedUserVM.authorities = setOf(AuthoritiesConstants.USER)

    restUserMockMvc.perform(put("/api/users")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(managedUserVM)))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun deleteUser() {
    // Initialize the database
    userRepository.saveAndFlush(user)
    val databaseSizeBeforeDelete = userRepository.findAll().size

    // Delete the user
    restUserMockMvc.perform(delete("/api/users/{login}", user.login!!)
      .accept(TestUtil.APPLICATION_JSON_UTF8))
      .andExpect(status().isOk)

    assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)?.get(user.login!!)).isNull()

    // Validate the database is empty
    val userList = userRepository.findAll()
    assertThat(userList).hasSize(databaseSizeBeforeDelete - 1)
  }

  @Test
  fun getAllAuthorities() {
    restUserMockMvc.perform(get("/api/users/authorities")
      .accept(TestUtil.APPLICATION_JSON_UTF8)
      .contentType(TestUtil.APPLICATION_JSON_UTF8))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$").value(hasItems(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN)))
  }

  @Test
  fun testUserEquals() {
    TestUtil.equalsVerifier(User::class.java)
    val user1 = User()
    user1.id = 1L
    val user2 = User()
    user2.id = user1.id
    assertThat(user1).isEqualTo(user2)
    user2.id = 2L
    assertThat(user1).isNotEqualTo(user2)
    user1.id = null
    assertThat(user1).isNotEqualTo(user2)
  }

  @Test
  fun testUserFromId() {
    assertThat(userMapper.userFromId(DEFAULT_ID)!!.id).isEqualTo(DEFAULT_ID)
    assertThat(userMapper.userFromId(null)).isNull()
  }

  @Test
  fun testUserDTOtoUser() {
    val userDTO = UserDTO()
    userDTO.id = DEFAULT_ID
    userDTO.login = DEFAULT_LOGIN
    userDTO.firstName = DEFAULT_FIRSTNAME
    userDTO.lastName = DEFAULT_LASTNAME
    userDTO.email = DEFAULT_EMAIL
    userDTO.isActivated = true
    userDTO.imageUrl = DEFAULT_IMAGEURL
    userDTO.langKey = DEFAULT_LANGKEY
    userDTO.createdBy = DEFAULT_LOGIN
    userDTO.lastModifiedBy = DEFAULT_LOGIN
    userDTO.authorities = setOf(AuthoritiesConstants.USER)

    val user = userMapper.userDTOToUser(userDTO)
    assertThat(user.id).isEqualTo(DEFAULT_ID)
    assertThat(user.login).isEqualTo(DEFAULT_LOGIN)
    assertThat(user.firstName).isEqualTo(DEFAULT_FIRSTNAME)
    assertThat(user.lastName).isEqualTo(DEFAULT_LASTNAME)
    assertThat(user.email).isEqualTo(DEFAULT_EMAIL)
    assertThat(user.activated).isEqualTo(true)
    assertThat(user.imageUrl).isEqualTo(DEFAULT_IMAGEURL)
    assertThat(user.langKey).isEqualTo(DEFAULT_LANGKEY)
    assertThat(user.createdBy).isNull()
    assertThat(user.createdDate).isNotNull()
    assertThat(user.lastModifiedBy).isNull()
    assertThat(user.lastModifiedDate).isNotNull()
    assertThat(user.authorities).extracting("name").containsExactly(AuthoritiesConstants.USER)
  }

  @Test
  fun testUserToUserDTO() {
    user.id = DEFAULT_ID
    user.createdBy = DEFAULT_LOGIN
    user.createdDate = Instant.now()
    user.lastModifiedBy = DEFAULT_LOGIN
    user.lastModifiedDate = Instant.now()
    val authorities = HashSet<Authority>()
    val authority = Authority(AuthoritiesConstants.USER)
    authorities.add(authority)
    user.authorities = authorities

    val userDTO = userMapper.userToUserDTO(user)

    assertThat(userDTO.id).isEqualTo(DEFAULT_ID)
    assertThat(userDTO.login).isEqualTo(DEFAULT_LOGIN)
    assertThat(userDTO.firstName).isEqualTo(DEFAULT_FIRSTNAME)
    assertThat(userDTO.lastName).isEqualTo(DEFAULT_LASTNAME)
    assertThat(userDTO.email).isEqualTo(DEFAULT_EMAIL)
    assertThat(userDTO.isActivated).isEqualTo(true)
    assertThat(userDTO.imageUrl).isEqualTo(DEFAULT_IMAGEURL)
    assertThat(userDTO.langKey).isEqualTo(DEFAULT_LANGKEY)
    assertThat(userDTO.createdBy).isEqualTo(DEFAULT_LOGIN)
    assertThat(userDTO.createdDate).isEqualTo(user.createdDate)
    assertThat(userDTO.lastModifiedBy).isEqualTo(DEFAULT_LOGIN)
    assertThat(userDTO.lastModifiedDate).isEqualTo(user.lastModifiedDate)
    assertThat(userDTO.authorities).containsExactly(AuthoritiesConstants.USER)
    assertThat(userDTO.toString()).isNotNull()
  }

  @Test
  fun testAuthorityEquals() {
    val authorityA = Authority("test")
    assertThat(authorityA).isEqualTo(authorityA)
    assertThat(authorityA).isNotEqualTo(null)
    assertThat(authorityA).isNotEqualTo(Any())
    assertThat(authorityA.toString()).isNotNull()

    val authorityB = Authority("test")
    assertThat(authorityA).isEqualTo(authorityB)

    authorityB.name = AuthoritiesConstants.ADMIN
    assertThat(authorityA).isNotEqualTo(authorityB)

    authorityA.name = AuthoritiesConstants.USER
    assertThat(authorityA).isNotEqualTo(authorityB)

    authorityB.name = AuthoritiesConstants.USER
    assertThat(authorityA).isEqualTo(authorityB)
    assertThat(authorityA.hashCode()).isEqualTo(authorityB.hashCode())
  }

  companion object {

    private const val DEFAULT_LOGIN = "johndoe"
    private const val UPDATED_LOGIN = "jhipster"

    private const val DEFAULT_ID = 1L

    private const val DEFAULT_PASSWORD = "passjohndoe"
    private const val UPDATED_PASSWORD = "passjhipster"

    private const val DEFAULT_EMAIL = "johndoe@localhost"
    private const val UPDATED_EMAIL = "jhipster@localhost"

    private const val DEFAULT_FIRSTNAME = "john"
    private const val UPDATED_FIRSTNAME = "jhipsterFirstName"

    private const val DEFAULT_LASTNAME = "doe"
    private const val UPDATED_LASTNAME = "jhipsterLastName"

    private const val DEFAULT_IMAGEURL = "http://placehold.it/50x50"
    private const val UPDATED_IMAGEURL = "http://placehold.it/40x40"

    private const val DEFAULT_LANGKEY = "en"
    private const val UPDATED_LANGKEY = "fr"

    /**
     * Create a User.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which has a required relationship to the User entity.
     */
    fun createEntity(): User {
      val user = User()
      user.login = DEFAULT_LOGIN + RandomStringUtils.randomAlphabetic(5)
      user.password = RandomStringUtils.random(60)
      user.activated = true
      user.email = RandomStringUtils.randomAlphabetic(5) + DEFAULT_EMAIL
      user.firstName = DEFAULT_FIRSTNAME
      user.lastName = DEFAULT_LASTNAME
      user.imageUrl = DEFAULT_IMAGEURL
      user.langKey = DEFAULT_LANGKEY
      return user
    }
  }
}
