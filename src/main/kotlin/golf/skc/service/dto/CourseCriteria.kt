package golf.skc.service.dto

import io.github.jhipster.service.filter.Filter
import io.github.jhipster.service.filter.LongFilter
import io.github.jhipster.service.filter.StringFilter

/**
 * Criteria class for the Course entity. This class is used in CourseResource to
 * receive all the possible filtering options from the Http GET request parameters.
 * For example the following could be a valid requests:
 * ` /courses?id.greaterThan=5&attr1.contains=something&attr2.specified=false`
 * As Spring is unable to properly convert the types, unless specific [Filter] class are used, we need to use
 * fix type specific filters.
 */
data class CourseCriteria(
  var id: LongFilter? = null,
  var name: StringFilter? = null,
  var addressId: LongFilter? = null
)
