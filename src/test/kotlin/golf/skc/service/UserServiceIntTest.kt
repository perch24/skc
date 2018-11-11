package golf.skc.service

import golf.skc.SkcApp
import golf.skc.config.Constants
import golf.skc.domain.User
import golf.skc.repository.UserRepository
import golf.skc.service.util.RandomUtil
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.auditing.AuditingHandler
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserService
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = arrayOf(SkcApp::class))
@Transactional
class UserServiceIntTest {

  @Autowired
  lateinit var userRepository: UserRepository

  @Autowired
  lateinit var userService: UserService

  @Autowired
  lateinit var auditingHandler: AuditingHandler

  @Mock
  lateinit var dateTimeProvider: DateTimeProvider

  private var user: User? = null

  @Before
  fun init() {
    user = User()
    user!!.login = "johndoe"
    user!!.password = RandomStringUtils.random(60)
    user!!.activated = true
    user!!.email = "johndoe@localhost"
    user!!.firstName = "john"
    user!!.lastName = "doe"
    user!!.imageUrl = "http://placehold.it/50x50"
    user!!.langKey = "en"

    `when`(dateTimeProvider.now).thenReturn(Optional.of(LocalDateTime.now()))
    auditingHandler.setDateTimeProvider(dateTimeProvider)
  }

  @Test
  @Transactional
  fun assertThatUserMustExistToResetPassword() {
    userRepository.saveAndFlush(user!!)
    var maybeUser = userService.requestPasswordReset("invalid.login@localhost")
    assertThat(maybeUser).isNotPresent

    maybeUser = userService.requestPasswordReset(user!!.email!!)
    assertThat(maybeUser).isPresent
    assertThat(maybeUser.orElse(null).email).isEqualTo(user!!.email)
    assertThat(maybeUser.orElse(null).resetDate).isNotNull()
    assertThat(maybeUser.orElse(null).resetKey).isNotNull()
  }

  @Test
  @Transactional
  fun assertThatOnlyActivatedUserCanRequestPasswordReset() {
    user!!.activated = false
    userRepository.saveAndFlush(user!!)

    val maybeUser = userService.requestPasswordReset(user!!.login!!)
    assertThat(maybeUser).isNotPresent
    userRepository.delete(user!!)
  }

  @Test
  @Transactional
  fun assertThatResetKeyMustNotBeOlderThan24Hours() {
    val daysAgo = Instant.now().minus(25, ChronoUnit.HOURS)
    val resetKey = RandomUtil.generateResetKey()
    user!!.activated = true
    user!!.resetDate = daysAgo
    user!!.resetKey = resetKey
    userRepository.saveAndFlush(user!!)

    val maybeUser = userService.completePasswordReset("johndoe2", user!!.resetKey!!)
    assertThat(maybeUser).isNotPresent
    userRepository.delete(user!!)
  }

  @Test
  @Transactional
  fun assertThatResetKeyMustBeValid() {
    val daysAgo = Instant.now().minus(25, ChronoUnit.HOURS)
    user!!.activated = true
    user!!.resetDate = daysAgo
    user!!.resetKey = "1234"
    userRepository.saveAndFlush(user!!)

    val maybeUser = userService.completePasswordReset("johndoe2", user!!.resetKey!!)
    assertThat(maybeUser).isNotPresent
    userRepository.delete(user!!)
  }

  @Test
  @Transactional
  fun assertThatUserCanResetPassword() {
    val oldPassword = user!!.password
    val daysAgo = Instant.now().minus(2, ChronoUnit.HOURS)
    val resetKey = RandomUtil.generateResetKey()
    user!!.activated = true
    user!!.resetDate = daysAgo
    user!!.resetKey = resetKey
    userRepository.saveAndFlush(user!!)

    val maybeUser = userService.completePasswordReset("johndoe2", user!!.resetKey!!)
    assertThat(maybeUser).isPresent
    assertThat(maybeUser.orElse(null).resetDate).isNull()
    assertThat(maybeUser.orElse(null).resetKey).isNull()
    assertThat(maybeUser.orElse(null).password).isNotEqualTo(oldPassword)

    userRepository.delete(user!!)
  }

  @Test
  @Transactional
  fun testFindNotActivatedUsersByCreationDateBefore() {
    val now = Instant.now()
    `when`(dateTimeProvider.now).thenReturn(Optional.of(now.minus(4, ChronoUnit.DAYS)))
    user!!.activated = false
    val dbUser = userRepository.saveAndFlush(user!!)
    dbUser.createdDate = now.minus(4, ChronoUnit.DAYS)
    userRepository.saveAndFlush(user!!)
    var users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS))
    assertThat(users).isNotEmpty
    userService.removeNotActivatedUsers()
    users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS))
    assertThat(users).isEmpty()
  }

  @Test
  @Transactional
  fun assertThatAnonymousUserIsNotGet() {
    user!!.login = Constants.ANONYMOUS_USER
    if (!userRepository.findOneByLogin(Constants.ANONYMOUS_USER).isPresent) {
      userRepository.saveAndFlush(user!!)
    }
    val pageable = PageRequest.of(0, userRepository.count().toInt())
    val allManagedUsers = userService.getAllManagedUsers(pageable)
    assertThat(allManagedUsers.content.stream()
      .noneMatch { user -> Constants.ANONYMOUS_USER == user.login })
      .isTrue()
  }


  @Test
  @Transactional
  fun testRemoveNotActivatedUsers() {
    // custom "now" for audit to use as creation date
    `when`(dateTimeProvider.now).thenReturn(Optional.of(Instant.now().minus(30, ChronoUnit.DAYS)))

    user!!.activated = false
    userRepository.saveAndFlush(user!!)

    assertThat(userRepository.findOneByLogin("johndoe")).isPresent
    userService.removeNotActivatedUsers()
    assertThat(userRepository.findOneByLogin("johndoe")).isNotPresent
  }

}
