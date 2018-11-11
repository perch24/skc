package golf.skc.service.dto

import golf.skc.config.Constants
import golf.skc.domain.User

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

import javax.validation.constraints.*
import java.time.Instant

/**
 * A DTO representing a user, with his authorities.
 */
open class UserDTO(
    var id: Long? = null,

    @field:NotBlank
    @field:Pattern(regexp = Constants.LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    var login: String? = null,

    @field:Size(max = 50)
    var firstName: String? = null,

    @field:Size(max = 50)
    var lastName: String? = null,

    @field:Email
    @field:Size(min = 5, max = 254)
    var email: String? = null,

    @field:Size(max = 256)
    var imageUrl: String? = null,

    var isActivated: Boolean = false,

    @field:Size(min = 2, max = 6)
    var langKey: String? = null,

    var createdBy: String? = null,

    var createdDate: Instant? = null,

    var lastModifiedBy: String? = null,

    var lastModifiedDate: Instant? = null,

    var authorities: Set<String> = HashSet()
) {
    constructor(user: User) : this(
        id = user.id,
        login = user.login,
        firstName = user.firstName,
        lastName = user.lastName,
        email = user.email,
        imageUrl = user.imageUrl,
        isActivated = user.activated,
        langKey = user.langKey,
        createdBy = user.createdBy,
        createdDate = user.createdDate,
        lastModifiedBy = user.lastModifiedBy,
        lastModifiedDate = user.lastModifiedDate,
        authorities = HashSet(user.authorities.map { it.name })
    )

    override fun toString(): String {
        return "UserDTO(id=$id, login=$login, firstName=$firstName, lastName=$lastName, email=$email, imageUrl=$imageUrl, isActivated=$isActivated, langKey=$langKey, createdBy=$createdBy, createdDate=$createdDate, lastModifiedBy=$lastModifiedBy, lastModifiedDate=$lastModifiedDate, authorities=$authorities)"
    }
}
