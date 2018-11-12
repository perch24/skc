package golf.skc.web.rest


import golf.skc.SkcApp
import golf.skc.domain.Address
import golf.skc.domain.Course
import golf.skc.repository.CourseRepository
import golf.skc.service.CourseQueryService
import golf.skc.service.CourseService
import golf.skc.service.dto.CourseDTO
import golf.skc.service.mapper.CourseMapper
import golf.skc.web.rest.errors.ExceptionTranslator
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

/**
 * Test class for the CourseResource REST controller.
 *
 * @see CourseResource
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SkcApp::class])
@Transactional
class CourseResourceIntTest {
  @Autowired
  private lateinit var courseRepository: CourseRepository

  @Autowired
  private lateinit var courseMapper: CourseMapper

  @Autowired
  private lateinit var courseService: CourseService

  @Autowired
  private lateinit var courseQueryService: CourseQueryService

  @Autowired
  private lateinit var jacksonMessageConverter: MappingJackson2HttpMessageConverter

  @Autowired
  private lateinit var pageableArgumentResolver: PageableHandlerMethodArgumentResolver

  @Autowired
  private lateinit var exceptionTranslator: ExceptionTranslator

  @Autowired
  private lateinit var em: EntityManager

  private lateinit var restCourseMockMvc: MockMvc

  private lateinit var course: Course

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    val courseResource = CourseResource(courseService, courseQueryService)
    this.restCourseMockMvc = MockMvcBuilders.standaloneSetup(courseResource)
      .setCustomArgumentResolvers(pageableArgumentResolver)
      .setControllerAdvice(exceptionTranslator)
      .setConversionService(createFormattingConversionService())
      .setMessageConverters(jacksonMessageConverter).build()
  }

  @Before
  fun initTest() {
    course = createEntity(em)
  }

  @Test
  fun createCourse() {
    val databaseSizeBeforeCreate = courseRepository.findAll().size

    // Create the Course
    val courseDTO = courseMapper.toDto(course)
    restCourseMockMvc.perform(post("/api/courses")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(courseDTO)))
      .andExpect(status().isCreated)

    // Validate the Course in the database
    val courseList = courseRepository.findAll()
    assertThat(courseList).hasSize(databaseSizeBeforeCreate + 1)
    val (_, name) = courseList[courseList.size - 1]
    assertThat(name).isEqualTo(DEFAULT_NAME)
  }

  @Test
  fun createCourseWithExistingId() {
    val databaseSizeBeforeCreate = courseRepository.findAll().size

    // Create the Course with an existing ID
    course.id = 1L
    val courseDTO = courseMapper.toDto(course)

    // An entity with an existing ID cannot be created, so this API call must fail
    restCourseMockMvc.perform(post("/api/courses")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(courseDTO)))
      .andExpect(status().isBadRequest)

    // Validate the Course in the database
    val courseList = courseRepository.findAll()
    assertThat(courseList).hasSize(databaseSizeBeforeCreate)
  }

  @Test
  fun checkNameIsRequired() {
    val databaseSizeBeforeTest = courseRepository.findAll().size
    // set the field null
    course.name = null

    // Create the Course, which fails.
    val courseDTO = courseMapper.toDto(course)

    restCourseMockMvc.perform(post("/api/courses")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(courseDTO)))
      .andExpect(status().isBadRequest)

    val courseList = courseRepository.findAll()
    assertThat(courseList).hasSize(databaseSizeBeforeTest)
  }

  @Test
  fun getAllCourses() {
    // Initialize the database
    courseRepository.saveAndFlush(course)

    // Get all the courseList
    restCourseMockMvc.perform(get("/api/courses?sort=id,desc"))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.[*].id").value(hasItem(course.id!!.toInt())))
      .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
  }

  @Test
  fun getCourse() {
    // Initialize the database
    courseRepository.saveAndFlush(course)

    // Get the course
    restCourseMockMvc.perform(get("/api/courses/{id}", course.id))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.id").value(course.id!!.toInt()))
      .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
  }

  @Test
  fun getAllCoursesByNameIsEqualToSomething() {
    // Initialize the database
    courseRepository.saveAndFlush(course)

    // Get all the courseList where name equals to DEFAULT_NAME
    defaultCourseShouldBeFound("name.equals=$DEFAULT_NAME")

    // Get all the courseList where name equals to UPDATED_NAME
    defaultCourseShouldNotBeFound("name.equals=$UPDATED_NAME")
  }

  @Test
  fun getAllCoursesByNameIsInShouldWork() {
    // Initialize the database
    courseRepository.saveAndFlush(course)

    // Get all the courseList where name in DEFAULT_NAME or UPDATED_NAME
    defaultCourseShouldBeFound("name.in=$DEFAULT_NAME,$UPDATED_NAME")

    // Get all the courseList where name equals to UPDATED_NAME
    defaultCourseShouldNotBeFound("name.in=$UPDATED_NAME")
  }

  @Test
  fun getAllCoursesByNameIsNullOrNotNull() {
    // Initialize the database
    courseRepository.saveAndFlush(course)

    // Get all the courseList where name is not null
    defaultCourseShouldBeFound("name.specified=true")

    // Get all the courseList where name is null
    defaultCourseShouldNotBeFound("name.specified=false")
  }

  @Test
  fun getAllCoursesByAddressIsEqualToSomething() {
    // Initialize the database
    courseRepository.saveAndFlush(course)
    val addressId = course.address!!.id!!

    // Get all the courseList where address equals to addressId
    defaultCourseShouldBeFound("addressId.equals=$addressId")

    // Get all the courseList where address equals to addressId + 1
    defaultCourseShouldNotBeFound("addressId.equals=${addressId + 1}")
  }

  /**
   * Executes the search, and checks that the default entity is returned
   */
  private fun defaultCourseShouldBeFound(filter: String) {
    restCourseMockMvc.perform(get("/api/courses?sort=id,desc&$filter"))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.[*].id").value(hasItem(course.id!!.toInt())))
      .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))

    // Check, that the count call also returns 1
    restCourseMockMvc.perform(get("/api/courses/count?sort=id,desc&$filter"))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(content().string("1"))
  }

  /**
   * Executes the search, and checks that the default entity is not returned
   */
  private fun defaultCourseShouldNotBeFound(filter: String) {
    restCourseMockMvc.perform(get("/api/courses?sort=id,desc&$filter"))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$").isEmpty)

    // Check, that the count call also returns 0
    restCourseMockMvc.perform(get("/api/courses/count?sort=id,desc&$filter"))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(content().string("0"))
  }


  @Test
  fun getNonExistingCourse() {
    // Get the course
    restCourseMockMvc.perform(get("/api/courses/{id}", java.lang.Long.MAX_VALUE))
      .andExpect(status().isNotFound)
  }

  @Test
  fun updateCourse() {
    // Initialize the database
    courseRepository.saveAndFlush(course)

    val databaseSizeBeforeUpdate = courseRepository.findAll().size

    // Update the course
    val updatedCourse = courseRepository.findById(course.id!!).get()
    // Disconnect from session so that the updates on updatedCourse are not directly saved in db
    em.detach(updatedCourse)
    updatedCourse.name = UPDATED_NAME
    val courseDTO = courseMapper.toDto(updatedCourse)

    restCourseMockMvc.perform(put("/api/courses")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(courseDTO)))
      .andExpect(status().isOk)

    // Validate the Course in the database
    val courseList = courseRepository.findAll()
    assertThat(courseList).hasSize(databaseSizeBeforeUpdate)
    val (_, name) = courseList[courseList.size - 1]
    assertThat(name).isEqualTo(UPDATED_NAME)
  }

  @Test
  fun updateNonExistingCourse() {
    val databaseSizeBeforeUpdate = courseRepository.findAll().size

    // Create the Course
    val courseDTO = courseMapper.toDto(course)

    // If the entity doesn't have an ID, it will throw BadRequestAlertException
    restCourseMockMvc.perform(put("/api/courses")
      .contentType(TestUtil.APPLICATION_JSON_UTF8)
      .content(TestUtil.convertObjectToJsonBytes(courseDTO)))
      .andExpect(status().isBadRequest)

    // Validate the Course in the database
    val courseList = courseRepository.findAll()
    assertThat(courseList).hasSize(databaseSizeBeforeUpdate)
  }

  @Test
  fun deleteCourse() {
    // Initialize the database
    courseRepository.saveAndFlush(course)

    val databaseSizeBeforeDelete = courseRepository.findAll().size

    // Get the course
    restCourseMockMvc.perform(delete("/api/courses/{id}", course.id)
      .accept(TestUtil.APPLICATION_JSON_UTF8))
      .andExpect(status().isOk)

    // Validate the database is empty
    val courseList = courseRepository.findAll()
    assertThat(courseList).hasSize(databaseSizeBeforeDelete - 1)
  }

  @Test
  fun equalsVerifier() {
    TestUtil.equalsVerifier(Course::class.java)
    val course1 = Course()
    course1.id = 1L
    val course2 = Course()
    course2.id = course1.id
    assertThat(course1).isEqualTo(course2)
    course2.id = 2L
    assertThat(course1).isNotEqualTo(course2)
    course1.id = null
    assertThat(course1).isNotEqualTo(course2)
  }

  @Test
  fun dtoEqualsVerifier() {
    TestUtil.equalsVerifier(CourseDTO::class.java)
    val courseDTO1 = CourseDTO()
    courseDTO1.id = 1L
    val courseDTO2 = CourseDTO()
    assertThat(courseDTO1).isNotEqualTo(courseDTO2)
    courseDTO2.id = courseDTO1.id
    assertThat(courseDTO1).isEqualTo(courseDTO2)
    courseDTO2.id = 2L
    assertThat(courseDTO1).isNotEqualTo(courseDTO2)
    courseDTO1.id = null
    assertThat(courseDTO1).isNotEqualTo(courseDTO2)
  }

  @Test
  fun testEntityFromId() {
    assertThat(CourseMapper.fromId(42L)!!.id!!).isEqualTo(42)
    assertThat(CourseMapper.fromId(null)).isNull()
  }

  companion object {
    private const val DEFAULT_NAME = "AAAAAAAAAA"
    private const val DEFAULT_DESCRIPTION = "This course is pretty nice"
    private const val UPDATED_NAME = "BBBBBBBBBB"
    private const val UPDATED_DESCRIPTION = "This course sucks"

    private const val ADDRESS_LINE1 = "123 Foo St"
    private const val ADDRESS_LINE2 = "Line 2 Info"
    private const val ADDRESS_CITY = "Some City"
    private const val ADDRESS_STATE = "CA"
    private const val ADDRESS_ZIP = "12345-1242"
    private const val ADDRESS_COUNTRY = "USA"

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    fun createEntity(em: EntityManager): Course {
      val address = Address(
        line1 = ADDRESS_LINE1,
        line2 = ADDRESS_LINE2,
        city = ADDRESS_CITY,
        state = ADDRESS_STATE,
        zip = ADDRESS_ZIP,
        country = ADDRESS_COUNTRY)

      return Course(name = DEFAULT_NAME,
        description = DEFAULT_DESCRIPTION,
        address = address)
    }
  }
}
