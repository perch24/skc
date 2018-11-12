package golf.skc.service.mapper

import golf.skc.domain.Course
import golf.skc.service.dto.CourseDTO
import org.mapstruct.Mapper
import org.springframework.stereotype.Service


/**
 * Mapper for the entity Course and its DTO CourseDTO.
 */
@Mapper(componentModel = "spring", uses = [AddressMapper::class])
@Service
interface CourseMapper : EntityMapper<CourseDTO, Course> {
  override fun toDto(entity: Course): CourseDTO

  override fun toEntity(dto: CourseDTO): Course

  companion object {
    fun fromId(id: Long?): Course? {
      if (id == null) {
        return null
      }
      val course = Course()
      course.id = id
      return course
    }
  }
}
