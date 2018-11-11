package golf.skc.service

import golf.skc.config.Constants
import golf.skc.domain.Authority
import golf.skc.domain.User
import golf.skc.repository.AuthorityRepository
import golf.skc.repository.UserRepository
import golf.skc.security.AuthoritiesConstants
import golf.skc.security.SecurityUtils
import golf.skc.service.dto.UserDTO
import golf.skc.service.util.RandomUtil
import golf.skc.web.rest.errors.*

import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.HashSet

/**
 * Service class for managing users.
 */
@Service
@Transactional
class UserService(private val userRepository: UserRepository, private val passwordEncoder: PasswordEncoder, private val authorityRepository: AuthorityRepository, private val cacheManager: CacheManager) {

    private val log = LoggerFactory.getLogger(UserService::class.java)

    val userWithAuthorities: Optional<User>
        @Transactional(readOnly = true)
        get() = SecurityUtils.currentUserLogin.flatMap { userRepository.findOneWithAuthoritiesByLogin(it) }

    /**
     * @return a list of all the authorities
     */
    val authorities: List<String>
        get() = authorityRepository.findAll().map { it.name }

    fun activateRegistration(key: String): Optional<User> {
        log.debug("Activating user for activation key {}", key)
        return userRepository.findOneByActivationKey(key)
            .map { user ->
                // activate given user for the registration key.
                user.activated = true
                user.activationKey = null
                this.clearUserCaches(user)
                log.debug("Activated user: {}", user)
                user
            }
    }

    fun completePasswordReset(newPassword: String, key: String): Optional<User> {
        log.debug("Reset user password for reset key {}", key)
        return userRepository.findOneByResetKey(key)
            .filter { (_, _, _, _, _, _, _, _, _, _, _, resetDate) -> resetDate!!.isAfter(Instant.now().minusSeconds(86400)) }
            .map { user ->
                user.password = passwordEncoder.encode(newPassword)
                user.resetKey = null
                user.resetDate = null
                this.clearUserCaches(user)
                user
            }
    }

    fun requestPasswordReset(mail: String): Optional<User> {
        return userRepository.findOneByEmailIgnoreCase(mail)
            .filter { it.activated }
            .map { user ->
                user.resetKey = RandomUtil.generateResetKey()
                user.resetDate = Instant.now()
                this.clearUserCaches(user)
                user
            }
    }

    fun registerUser(userDTO: UserDTO, password: String): User {
        userRepository.findOneByLogin(userDTO.login!!.toLowerCase()).ifPresent { existingUser ->
            val removed = removeNonActivatedUser(existingUser)
            if (!removed) {
                throw LoginAlreadyUsedException()
            }
        }
        userRepository.findOneByEmailIgnoreCase(userDTO.email!!).ifPresent { existingUser ->
            val removed = removeNonActivatedUser(existingUser)
            if (!removed) {
                throw EmailAlreadyUsedException()
            }
        }
        val newUser = User()
        val encryptedPassword = passwordEncoder.encode(password)
        newUser.login = userDTO.login!!.toLowerCase()
        // new user gets initially a generated password
        newUser.password = encryptedPassword
        newUser.firstName = userDTO.firstName
        newUser.lastName = userDTO.lastName
        newUser.email = userDTO.email?.toLowerCase()
        newUser.imageUrl = userDTO.imageUrl
        newUser.langKey = userDTO.langKey
        // new user is not active
        newUser.activated = false
        // new user gets registration key
        newUser.activationKey = RandomUtil.generateActivationKey()
        val authorities = HashSet<Authority>()
        authorityRepository.findById(AuthoritiesConstants.USER).ifPresent { authorities.add(it) }
        newUser.authorities = authorities
        userRepository.save(newUser)
        this.clearUserCaches(newUser)
        log.debug("Created Information for User: {}", newUser)
        return newUser
    }

    private fun removeNonActivatedUser(existingUser: User): Boolean {
        if (existingUser.activated) {
            return false
        }
        userRepository.delete(existingUser)
        userRepository.flush()
        this.clearUserCaches(existingUser)
        return true
    }

    fun createUser(userDTO: UserDTO): User {
        val user = User()
        user.login = userDTO.login?.toLowerCase()
        user.firstName = userDTO.firstName
        user.lastName = userDTO.lastName
        user.email = userDTO.email?.toLowerCase()
        user.imageUrl = userDTO.imageUrl
        if (userDTO.langKey == null) {
            user.langKey = Constants.DEFAULT_LANGUAGE // default language
        } else {
            user.langKey = userDTO.langKey
        }
        val encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword())
        user.password = encryptedPassword
        user.resetKey = RandomUtil.generateResetKey()
        user.resetDate = Instant.now()
        user.activated = true
        val authorities = userDTO.authorities
            .map { authorityRepository.findById(it) }
            .filter { it.isPresent }
            .map { it.get() }
        user.authorities = HashSet(authorities)
        userRepository.save(user)
        this.clearUserCaches(user)
        log.debug("Created Information for User: {}", user)
        return user
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user
     * @param lastName last name of user
     * @param email email id of user
     * @param langKey language key
     * @param imageUrl image URL of user
     */
    fun updateUser(firstName: String, lastName: String, email: String, langKey: String, imageUrl: String) {
        SecurityUtils.currentUserLogin
            .flatMap { userRepository.findOneByLogin(it) }
            .ifPresent { user ->
                user.firstName = firstName
                user.lastName = lastName
                user.email = email.toLowerCase()
                user.langKey = langKey
                user.imageUrl = imageUrl
                this.clearUserCaches(user)
                log.debug("Changed Information for User: {}", user)
            }
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update
     * @return updated user
     */
    fun updateUser(userDTO: UserDTO): Optional<UserDTO> {
        return Optional.of(userRepository
            .findById(userDTO.id!!))
            .filter { it.isPresent }
            .map { it.get() }
            .map { user ->
                this.clearUserCaches(user)
                user.login = userDTO.login?.toLowerCase()
                user.firstName = userDTO.firstName
                user.lastName = userDTO.lastName
                user.email = userDTO.email?.toLowerCase()
                user.imageUrl = userDTO.imageUrl
                user.activated = userDTO.isActivated
                user.langKey = userDTO.langKey
                val managedAuthorities = user.authorities
                managedAuthorities.clear()
                userDTO.authorities.stream()
                    .map<Optional<Authority>> { authorityRepository.findById(it) }
                    .filter { it.isPresent }
                    .map { it.get() }
                    .forEach { managedAuthorities.add(it) }
                this.clearUserCaches(user)
                log.debug("Changed Information for User: {}", user)
                user
            }
            .map { UserDTO(it) }
    }

    fun deleteUser(login: String) {
        userRepository.findOneByLogin(login).ifPresent { user ->
            userRepository.delete(user)
            this.clearUserCaches(user)
            log.debug("Deleted User: {}", user)
        }
    }

    fun changePassword(currentClearTextPassword: String, newPassword: String) {
        SecurityUtils.currentUserLogin
            .flatMap { userRepository.findOneByLogin(it) }
            .ifPresent { user ->
                val currentEncryptedPassword = user.password
                if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                    throw InvalidPasswordException()
                }
                val encryptedPassword = passwordEncoder.encode(newPassword)
                user.password = encryptedPassword
                this.clearUserCaches(user)
                log.debug("Changed password for User: {}", user)
            }
    }

    @Transactional(readOnly = true)
    fun getAllManagedUsers(pageable: Pageable): Page<UserDTO> {
        return userRepository.findAllByLoginNot(pageable, Constants.ANONYMOUS_USER).map { UserDTO(it) }
    }

    @Transactional(readOnly = true)
    fun getUserWithAuthoritiesByLogin(login: String): Optional<User> {
        return userRepository.findOneWithAuthoritiesByLogin(login)
    }

    @Transactional(readOnly = true)
    fun getUserWithAuthorities(id: Long?): Optional<User> {
        return userRepository.findOneWithAuthoritiesById(id)
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     *
     *
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun removeNotActivatedUsers() {
        userRepository
            .findAllByActivatedIsFalseAndCreatedDateBefore(Instant.now().minus(3, ChronoUnit.DAYS))
            .forEach { user ->
                log.debug("Deleting not activated user {}", user.login)
                userRepository.delete(user)
                this.clearUserCaches(user)
            }
    }

    private fun clearUserCaches(user: User) {
        cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)?.evict(user.login!!)
        cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)?.evict(user.email!!)
    }
}
