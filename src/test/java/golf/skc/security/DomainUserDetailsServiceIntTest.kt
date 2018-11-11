package golf.skc.security

import golf.skc.SkcApp
import golf.skc.domain.User
import golf.skc.repository.UserRepository

import org.apache.commons.lang3.RandomStringUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

import java.util.Locale

import org.assertj.core.api.Assertions.assertThat

/**
 * Test class for DomainUserDetailsService.
 *
 * @see DomainUserDetailsService
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = arrayOf(SkcApp::class))
@Transactional
class DomainUserDetailsServiceIntTest {

    @Autowired
    private val userRepository: UserRepository? = null

    @Autowired
    private val domainUserDetailsService: UserDetailsService? = null

    private var userOne: User? = null
    private var userTwo: User? = null
    private var userThree: User? = null

    @Before
    fun init() {
        userOne = User()
        userOne!!.login = USER_ONE_LOGIN
        userOne!!.password = RandomStringUtils.random(60)
        userOne!!.activated = true
        userOne!!.email = USER_ONE_EMAIL
        userOne!!.firstName = "userOne"
        userOne!!.lastName = "doe"
        userOne!!.langKey = "en"
        userRepository!!.save(userOne!!)

        userTwo = User()
        userTwo!!.login = USER_TWO_LOGIN
        userTwo!!.password = RandomStringUtils.random(60)
        userTwo!!.activated = true
        userTwo!!.email = USER_TWO_EMAIL
        userTwo!!.firstName = "userTwo"
        userTwo!!.lastName = "doe"
        userTwo!!.langKey = "en"
        userRepository.save(userTwo!!)

        userThree = User()
        userThree!!.login = USER_THREE_LOGIN
        userThree!!.password = RandomStringUtils.random(60)
        userThree!!.activated = false
        userThree!!.email = USER_THREE_EMAIL
        userThree!!.firstName = "userThree"
        userThree!!.lastName = "doe"
        userThree!!.langKey = "en"
        userRepository.save(userThree!!)
    }

    @Test
    @Transactional
    fun assertThatUserCanBeFoundByLogin() {
        val userDetails = domainUserDetailsService!!.loadUserByUsername(USER_ONE_LOGIN)
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_ONE_LOGIN)
    }

    @Test
    @Transactional
    fun assertThatUserCanBeFoundByLoginIgnoreCase() {
        val userDetails = domainUserDetailsService!!.loadUserByUsername(USER_ONE_LOGIN.toUpperCase(Locale.ENGLISH))
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_ONE_LOGIN)
    }

    @Test
    @Transactional
    fun assertThatUserCanBeFoundByEmail() {
        val userDetails = domainUserDetailsService!!.loadUserByUsername(USER_TWO_EMAIL)
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_TWO_LOGIN)
    }

    @Test(expected = UsernameNotFoundException::class)
    @Transactional
    fun assertThatUserCanNotBeFoundByEmailIgnoreCase() {
        domainUserDetailsService!!.loadUserByUsername(USER_TWO_EMAIL.toUpperCase(Locale.ENGLISH))
    }

    @Test
    @Transactional
    fun assertThatEmailIsPrioritizedOverLogin() {
        val userDetails = domainUserDetailsService!!.loadUserByUsername(USER_ONE_EMAIL)
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_ONE_LOGIN)
    }

    @Test(expected = UserNotActivatedException::class)
    @Transactional
    fun assertThatUserNotActivatedExceptionIsThrownForNotActivatedUsers() {
        domainUserDetailsService!!.loadUserByUsername(USER_THREE_LOGIN)
    }

    companion object {

        private const val USER_ONE_LOGIN = "test-user-one"
        private const val USER_ONE_EMAIL = "test-user-one@localhost"
        private const val USER_TWO_LOGIN = "test-user-two"
        private const val USER_TWO_EMAIL = "test-user-two@localhost"
        private const val USER_THREE_LOGIN = "test-user-three"
        private const val USER_THREE_EMAIL = "test-user-three@localhost"
    }

}
