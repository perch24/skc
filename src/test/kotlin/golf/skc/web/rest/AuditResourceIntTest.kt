package golf.skc.web.rest

import golf.skc.SkcApp
import golf.skc.config.audit.AuditEventConverter
import golf.skc.domain.PersistentAuditEvent
import golf.skc.repository.PersistenceAuditEventRepository
import golf.skc.service.AuditEventService
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.format.support.FormattingConversionService
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Test class for the AuditResource REST controller.
 *
 * @see AuditResource
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SkcApp::class])
@Transactional
class AuditResourceIntTest {

  @Autowired
  lateinit var auditEventRepository: PersistenceAuditEventRepository

  @Autowired
  lateinit var auditEventConverter: AuditEventConverter

  @Autowired
  lateinit var jacksonMessageConverter: MappingJackson2HttpMessageConverter

  @Autowired
  lateinit var formattingConversionService: FormattingConversionService

  @Autowired
  lateinit var pageableArgumentResolver: PageableHandlerMethodArgumentResolver

  lateinit var auditEvent: PersistentAuditEvent

  lateinit var restAuditMockMvc: MockMvc

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    val auditEventService = AuditEventService(auditEventRepository, auditEventConverter)
    val auditResource = AuditResource(auditEventService)
    this.restAuditMockMvc = MockMvcBuilders.standaloneSetup(auditResource)
      .setCustomArgumentResolvers(pageableArgumentResolver)
      .setConversionService(formattingConversionService)
      .setMessageConverters(jacksonMessageConverter).build()
  }

  @Before
  fun initTest() {
    auditEventRepository.deleteAll()
    auditEvent = PersistentAuditEvent(
      auditEventType = SAMPLE_TYPE,
      principal = SAMPLE_PRINCIPAL,
      auditEventDate = SAMPLE_TIMESTAMP
    )
  }

  @Test
  fun getAllAudits() {
    // Initialize the database
    auditEventRepository.save(auditEvent)

    // Get all the audits
    restAuditMockMvc.perform(get("/management/audits"))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.[*].principal").value(hasItem(SAMPLE_PRINCIPAL)))
  }

  @Test
  fun getAudit() {
    // Initialize the database
    auditEventRepository.save(auditEvent)

    // Get the audit
    restAuditMockMvc.perform(get("/management/audits/{id}", auditEvent.id))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.principal").value(SAMPLE_PRINCIPAL))
  }

  @Test
  fun getAuditsByDate() {
    // Initialize the database
    auditEventRepository.save(auditEvent)

    // Generate dates for selecting audits by date, making sure the period will contain the audit
    val fromDate = SAMPLE_TIMESTAMP.minusSeconds(SECONDS_PER_DAY).toString().substring(0, 10)
    val toDate = SAMPLE_TIMESTAMP.plusSeconds(SECONDS_PER_DAY).toString().substring(0, 10)

    // Get the audit
    restAuditMockMvc.perform(get("/management/audits?fromDate=$fromDate&toDate=$toDate"))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(jsonPath("$.[*].principal").value(hasItem(SAMPLE_PRINCIPAL)))
  }

  @Test
  fun getNonExistingAuditsByDate() {
    // Initialize the database
    auditEventRepository.save(auditEvent)

    // Generate dates for selecting audits by date, making sure the period will not contain the sample audit
    val fromDate = SAMPLE_TIMESTAMP.minusSeconds(2 * SECONDS_PER_DAY).toString().substring(0, 10)
    val toDate = SAMPLE_TIMESTAMP.minusSeconds(SECONDS_PER_DAY).toString().substring(0, 10)

    // Query audits but expect no results
    restAuditMockMvc.perform(get("/management/audits?fromDate=$fromDate&toDate=$toDate"))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
      .andExpect(header().string("X-Total-Count", "0"))
  }

  @Test
  fun getNonExistingAudit() {
    // Get the audit
    restAuditMockMvc.perform(get("/management/audits/{id}", java.lang.Long.MAX_VALUE))
      .andExpect(status().isNotFound)
  }

  @Test
  fun testPersistentAuditEventEquals() {
    TestUtil.equalsVerifier(PersistentAuditEvent::class.java)
    val auditEvent1 = PersistentAuditEvent(
      id = 1L,
      principal = SAMPLE_PRINCIPAL,
      auditEventDate = SAMPLE_TIMESTAMP,
      auditEventType = SAMPLE_TYPE)
    val auditEvent2 = PersistentAuditEvent(
      id = auditEvent1.id,
      principal = auditEvent1.principal,
      auditEventDate = auditEvent1.auditEventDate,
      auditEventType = auditEvent1.auditEventType)
    assertThat(auditEvent1).isEqualTo(auditEvent2)
    auditEvent2.id = 2L
    assertThat(auditEvent1).isNotEqualTo(auditEvent2)
    auditEvent1.id = null
    assertThat(auditEvent1).isNotEqualTo(auditEvent2)
  }

  companion object {
    private const val SAMPLE_PRINCIPAL = "SAMPLE_PRINCIPAL"
    private const val SAMPLE_TYPE = "SAMPLE_TYPE"
    private val SAMPLE_TIMESTAMP = Instant.parse("2015-08-04T10:11:30Z")
    private const val SECONDS_PER_DAY = (60 * 60 * 24).toLong()
  }
}
