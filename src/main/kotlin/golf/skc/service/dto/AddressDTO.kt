package golf.skc.service.dto

import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * A DTO for the Address entity.
 */
data class AddressDTO(
  var id: Long? = null,

  @field:NotNull
  @field:Size(min = 3, max = 128)
  var line1: String? = null,

  var line2: String? = null,

  @field:NotNull
  var city: String? = null,

  @field:NotNull
  var state: String? = null,

  @field:NotNull
  var zip: String? = null,

  @field:NotNull
  var country: String? = null
)
