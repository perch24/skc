package golf.skc.web.rest

import com.codahale.metrics.annotation.Timed
import golf.skc.service.CourseQueryService
import golf.skc.service.CourseService
import golf.skc.service.dto.CourseCriteria
import golf.skc.service.dto.CourseDTO
import golf.skc.web.rest.errors.BadRequestAlertException
import golf.skc.web.rest.util.HeaderUtil
import golf.skc.web.rest.util.PaginationUtil
import io.github.jhipster.web.util.ResponseUtil
import org.hibernate.id.IdentifierGenerator.ENTITY_NAME
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException
import javax.validation.Valid


/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
class CourseResource(private val courseService: CourseService, private val courseQueryService: CourseQueryService) {
  private val log = LoggerFactory.getLogger(CourseResource::class.java)


  /**
   * POST  /courses : Create a new course.
   *
   * @param courseDTO the courseDTO to create
   * @return the ResponseEntity with status 201 (Created) and with body the new courseDTO, or with status 400 (Bad Request) if the course has already an ID
   * @throws URISyntaxException if the Location URI syntax is incorrect
   */
  @PostMapping("/courses")
  @Timed
  @Throws(URISyntaxException::class)
  fun createCourse(@Valid @RequestBody courseDTO: CourseDTO): ResponseEntity<CourseDTO> {
    log.debug("REST request to save Course : {}", courseDTO)
    if (courseDTO.id != null) {
      throw BadRequestAlertException("A new course cannot already have an ID", ENTITY_NAME, "idexists")
    }
    val result = courseService.save(courseDTO)
    return ResponseEntity.created(URI("/api/courses/" + result.id!!))
      .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.id!!.toString()))
      .body(result)
  }

  /**
   * PUT  /courses : Updates an existing course.
   *
   * @param courseDTO the courseDTO to update
   * @return the ResponseEntity with status 200 (OK) and with body the updated courseDTO,
   * or with status 400 (Bad Request) if the courseDTO is not valid,
   * or with status 500 (Internal Server Error) if the courseDTO couldn't be updated
   * @throws URISyntaxException if the Location URI syntax is incorrect
   */
  @PutMapping("/courses")
  @Timed
  @Throws(URISyntaxException::class)
  fun updateCourse(@Valid @RequestBody courseDTO: CourseDTO): ResponseEntity<CourseDTO> {
    log.debug("REST request to update Course : {}", courseDTO)
    if (courseDTO.id == null) {
      throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
    }
    val result = courseService.save(courseDTO)
    return ResponseEntity.ok()
      .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, courseDTO.id!!.toString()))
      .body(result)
  }

  /**
   * GET  /courses : get all the courses.
   *
   * @param pageable the pagination information
   * @param criteria the criterias which the requested entities should match
   * @return the ResponseEntity with status 200 (OK) and the list of courses in body
   */
  @GetMapping("/courses")
  @Timed
  fun getAllCourses(criteria: CourseCriteria, pageable: Pageable): ResponseEntity<List<CourseDTO>> {
    log.debug("REST request to get Courses by criteria: {}", criteria)
    val page = courseQueryService.findByCriteria(criteria, pageable)
    val headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/courses")
    return ResponseEntity.ok().headers(headers).body(page.content)
  }

  /**
   * GET  /courses/count : count all the courses.
   *
   * @param criteria the criterias which the requested entities should match
   * @return the ResponseEntity with status 200 (OK) and the count in body
   */
  @GetMapping("/courses/count")
  @Timed
  fun countCourses(criteria: CourseCriteria): ResponseEntity<Long> {
    log.debug("REST request to count Courses by criteria: {}", criteria)
    return ResponseEntity.ok().body(courseQueryService.countByCriteria(criteria))
  }

  /**
   * GET  /courses/:id : get the "id" course.
   *
   * @param id the id of the courseDTO to retrieve
   * @return the ResponseEntity with status 200 (OK) and with body the courseDTO, or with status 404 (Not Found)
   */
  @GetMapping("/courses/{id}")
  @Timed
  fun getCourse(@PathVariable id: Long?): ResponseEntity<CourseDTO> {
    log.debug("REST request to get Course : {}", id)
    val courseDTO = courseService.findOne(id)
    return ResponseUtil.wrapOrNotFound(courseDTO)
  }

  /**
   * DELETE  /courses/:id : delete the "id" course.
   *
   * @param id the id of the courseDTO to delete
   * @return the ResponseEntity with status 200 (OK)
   */
  @DeleteMapping("/courses/{id}")
  @Timed
  fun deleteCourse(@PathVariable id: Long?): ResponseEntity<Void> {
    log.debug("REST request to delete Course : {}", id)
    courseService.delete(id)
    return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id!!.toString())).build()
  }
}
