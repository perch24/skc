package golf.skc.web.rest.vm

import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * View Model object for storing a user's credentials.
 */
data class LoginVM(

  @NotNull
  @Size(min = 1, max = 50)
  var username: String? = null,

  @NotNull
  @Size(min = ManagedUserVM.PASSWORD_MIN_LENGTH, max = ManagedUserVM.PASSWORD_MAX_LENGTH)
  var password: String? = null,

  var isRememberMe: Boolean = false
)
