package golf.skc.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

/**
 * Utility class for Spring Security.
 */
object SecurityUtils {

  /**
   * Get the login of the current user.
   *
   * @return the login of the current user
   */
  val currentUserLogin: Optional<String>
    get() {
      val authentication = SecurityContextHolder.getContext().authentication
      if (authentication != null) {
        val principal = authentication.principal
        return when (principal) {
          is UserDetails -> Optional.ofNullable(principal.username)
          is String -> Optional.ofNullable(authentication.principal as String)
          else -> Optional.empty()
        }
      }
      return Optional.empty()
    }

  /**
   * Get the JWT of the current user.
   *
   * @return the JWT of the current user
   */
  val currentUserJWT: Optional<String>
    get() {
      val securityContext = SecurityContextHolder.getContext()
      return Optional.ofNullable(securityContext.authentication)
        .filter { authentication -> authentication.credentials is String }
        .map { authentication -> authentication.credentials as String }
    }

  /**
   * Check if a user is authenticated.
   *
   * @return true if the user is authenticated, false otherwise
   */
  val isAuthenticated: Boolean
    get() {
      val securityContext = SecurityContextHolder.getContext()
      return Optional.ofNullable(securityContext.authentication)
        .map<Boolean> { authentication ->
          authentication.authorities.stream()
            .noneMatch { grantedAuthority -> grantedAuthority.authority == AuthoritiesConstants.ANONYMOUS }
        }
        .orElse(false)
    }

  /**
   * If the current user has a specific authority (security role).
   *
   *
   * The name of this method comes from the isUserInRole() method in the Servlet API
   *
   * @param authority the authority to check
   * @return true if the current user has the authority, false otherwise
   */
  fun isCurrentUserInRole(authority: String): Boolean {
    val securityContext = SecurityContextHolder.getContext()
    return Optional.ofNullable(securityContext.authentication)
      .map<Boolean> { authentication ->
        authentication.authorities.stream()
          .anyMatch { grantedAuthority -> grantedAuthority.authority == authority }
      }
      .orElse(false)
  }
}
