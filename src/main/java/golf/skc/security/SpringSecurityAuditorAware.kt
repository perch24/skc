package golf.skc.security

import golf.skc.config.Constants

import java.util.Optional

import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component

/**
 * Implementation of AuditorAware based on Spring Security.
 */
@Component
class SpringSecurityAuditorAware : AuditorAware<String> {
    override fun getCurrentAuditor(): Optional<String> {
        return Optional.of(SecurityUtils.currentUserLogin.orElse(Constants.SYSTEM_ACCOUNT))
    }
}
