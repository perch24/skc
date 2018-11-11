package golf.skc.service.dto

/**
 * A DTO representing a password change required data - current and new password.
 */
data class PasswordChangeDTO(var currentPassword: String, var newPassword: String)
