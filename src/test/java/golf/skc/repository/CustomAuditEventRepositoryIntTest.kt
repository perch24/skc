package golf.skc.repository

import golf.skc.SkcApp
import golf.skc.config.Constants
import golf.skc.config.audit.AuditEventConverter
import golf.skc.domain.PersistentAuditEvent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.audit.AuditEvent
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

import javax.servlet.http.HttpSession
import java.time.Instant
import java.util.HashMap

import org.assertj.core.api.Assertions.assertThat
import golf.skc.repository.CustomAuditEventRepository.Companion.EVENT_DATA_COLUMN_MAX_LENGTH

/**
 * Test class for the CustomAuditEventRepository class.
 *
 * @see CustomAuditEventRepository
 */
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SkcApp::class])
@Transactional
open class CustomAuditEventRepositoryIntTest {

    @Autowired
    private val persistenceAuditEventRepository: PersistenceAuditEventRepository? = null

    @Autowired
    private val auditEventConverter: AuditEventConverter? = null

    private var customAuditEventRepository: CustomAuditEventRepository? = null

    private var testUserEvent: PersistentAuditEvent? = null

    private var testOtherUserEvent: PersistentAuditEvent? = null

    private var testOldUserEvent: PersistentAuditEvent? = null

    @Before
    fun setup() {
        customAuditEventRepository = CustomAuditEventRepository(persistenceAuditEventRepository!!, auditEventConverter!!)
        persistenceAuditEventRepository.deleteAll()
        val oneHourAgo = Instant.now().minusSeconds(3600)

        val data = HashMap<String, String>()
        data["test-key"] = "test-value"
        testUserEvent = PersistentAuditEvent(
            principal = "test-user",
            auditEventType = "test-type",
            auditEventDate =  oneHourAgo,
            data = data)

        testOldUserEvent = PersistentAuditEvent(
            principal = "test-user",
            auditEventType = "test-type",
            auditEventDate =  oneHourAgo.minusSeconds(10000))

        testOtherUserEvent = PersistentAuditEvent(
            principal = "other-test-user",
            auditEventType = "test-type",
            auditEventDate =  oneHourAgo)
    }

    @Test
    fun addAuditEvent() {
        val data = HashMap<String, Any>()
        data["test-key"] = "test-value"
        val event = AuditEvent("test-user", "test-type", data)
        customAuditEventRepository!!.add(event)
        val persistentAuditEvents = persistenceAuditEventRepository!!.findAll()
        assertThat(persistentAuditEvents).hasSize(1)
        val persistentAuditEvent = persistentAuditEvents[0]
        assertThat(persistentAuditEvent.principal).isEqualTo(event.principal)
        assertThat(persistentAuditEvent.auditEventType).isEqualTo(event.type)
        assertThat<String, String>(persistentAuditEvent.data).containsKey("test-key")
        assertThat(persistentAuditEvent.data["test-key"]).isEqualTo("test-value")
        assertThat(persistentAuditEvent.auditEventDate).isEqualTo(event.timestamp)
    }

    @Test
    fun addAuditEventTruncateLargeData() {
        val data = HashMap<String, Any>()
        val largeData = StringBuilder()
        for (i in 0 until EVENT_DATA_COLUMN_MAX_LENGTH + 10) {
            largeData.append("a")
        }
        data["test-key"] = largeData
        val event = AuditEvent("test-user", "test-type", data)
        customAuditEventRepository!!.add(event)
        val persistentAuditEvents = persistenceAuditEventRepository!!.findAll()
        assertThat(persistentAuditEvents).hasSize(1)
        val persistentAuditEvent = persistentAuditEvents[0]
        assertThat(persistentAuditEvent.principal).isEqualTo(event.principal)
        assertThat(persistentAuditEvent.auditEventType).isEqualTo(event.type)
        assertThat<String, String>(persistentAuditEvent.data).containsKey("test-key")
        val actualData = persistentAuditEvent.data["test-key"]
        assertThat(actualData?.length).isEqualTo(EVENT_DATA_COLUMN_MAX_LENGTH)
        assertThat(actualData).isSubstringOf(largeData)
        assertThat(persistentAuditEvent.auditEventDate).isEqualTo(event.timestamp)
    }

    @Test
    fun testAddEventWithWebAuthenticationDetails() {
        val session = MockHttpSession(null, "test-session-id")
        val request = MockHttpServletRequest()
        request.setSession(session)
        request.remoteAddr = "1.2.3.4"
        val details = WebAuthenticationDetails(request)
        val data = HashMap<String, Any>()
        data["test-key"] = details
        val event = AuditEvent("test-user", "test-type", data)
        customAuditEventRepository!!.add(event)
        val persistentAuditEvents = persistenceAuditEventRepository!!.findAll()
        assertThat(persistentAuditEvents).hasSize(1)
        val persistentAuditEvent = persistentAuditEvents[0]
        assertThat(persistentAuditEvent.data["remoteAddress"]).isEqualTo("1.2.3.4")
        assertThat(persistentAuditEvent.data["sessionId"]).isEqualTo("test-session-id")
    }

    @Test
    fun testAddEventWithNullData() {
        val data = HashMap<String, Any?>()
        data["test-key"] = null
        val event = AuditEvent("test-user", "test-type", data)
        customAuditEventRepository!!.add(event)
        val persistentAuditEvents = persistenceAuditEventRepository!!.findAll()
        assertThat(persistentAuditEvents).hasSize(1)
        val persistentAuditEvent = persistentAuditEvents[0]
        assertThat(persistentAuditEvent.data["test-key"]).isEqualTo("null")
    }

    @Test
    fun addAuditEventWithAnonymousUser() {
        val data = HashMap<String, Any>()
        data["test-key"] = "test-value"
        val event = AuditEvent(Constants.ANONYMOUS_USER, "test-type", data)
        customAuditEventRepository!!.add(event)
        val persistentAuditEvents = persistenceAuditEventRepository!!.findAll()
        assertThat(persistentAuditEvents).hasSize(0)
    }

    @Test
    fun addAuditEventWithAuthorizationFailureType() {
        val data = HashMap<String, Any>()
        data["test-key"] = "test-value"
        val event = AuditEvent("test-user", "AUTHORIZATION_FAILURE", data)
        customAuditEventRepository!!.add(event)
        val persistentAuditEvents = persistenceAuditEventRepository!!.findAll()
        assertThat(persistentAuditEvents).hasSize(0)
    }

}
