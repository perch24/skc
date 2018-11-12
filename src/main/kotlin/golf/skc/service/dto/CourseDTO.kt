package golf.skc.service.dto

import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * A DTO for the Course entity.
 */
data class CourseDTO(

  var id: Long? = null,

  @field:NotNull
  @field:Size(min = 3, max = 128)
  var name: String? = null,

  @field:Size(max = 1024)
  var description: String? = null,

  var address: AddressDTO? = null
)
