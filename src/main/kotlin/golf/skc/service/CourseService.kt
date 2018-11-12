package golf.skc.service

import golf.skc.repository.CourseRepository
import golf.skc.service.dto.CourseDTO
import golf.skc.service.mapper.CourseMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service Implementation for managing Course.
 */
@Service
@Transactional
class CourseService(private val courseRepository: CourseRepository, private val courseMapper: CourseMapper) {

  private val log = LoggerFactory.getLogger(CourseService::class.java)

  /**
   * Save a course.
   *
   * @param courseDTO the entity to save
   * @return the persisted entity
   */
  fun save(courseDTO: CourseDTO): CourseDTO {
    log.debug("Request to save Course : {}", courseDTO)

    var course = courseMapper.toEntity(courseDTO)
    course = courseRepository.save(course)
    return courseMapper.toDto(course)
  }

  /**
   * Get all the courses.
   *
   * @param pageable the pagination information
   * @return the list of entities
   */
  @Transactional(readOnly = true)
  fun findAll(pageable: Pageable): Page<CourseDTO> {
    log.debug("Request to get all Courses")
    return courseRepository.findAll(pageable)
      .map { courseMapper.toDto(it) }
  }


  /**
   * Get one course by id.
   *
   * @param id the id of the entity
   * @return the entity
   */
  @Transactional(readOnly = true)
  fun findOne(id: Long?): Optional<CourseDTO> {
    log.debug("Request to get Course : {}", id)
    return courseRepository.findById(id!!)
      .map { courseMapper.toDto(it) }
  }

  /**
   * Delete the course by id.
   *
   * @param id the id of the entity
   */
  fun delete(id: Long?) {
    log.debug("Request to delete Course : {}", id)
    courseRepository.deleteById(id!!)
  }
}
