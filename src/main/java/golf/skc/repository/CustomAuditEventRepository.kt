package golf.skc.repository

import golf.skc.config.Constants
import golf.skc.config.audit.AuditEventConverter
import golf.skc.domain.PersistentAuditEvent

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.audit.AuditEvent
import org.springframework.boot.actuate.audit.AuditEventRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import java.time.Instant
import java.util.*

/**
 * An implementation of Spring Boot's AuditEventRepository.
 */
@Repository
open class CustomAuditEventRepository(private val persistenceAuditEventRepository: PersistenceAuditEventRepository,
                                 private val auditEventConverter: AuditEventConverter) : AuditEventRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun find(principal: String, after: Instant, type: String): List<AuditEvent> {
        val persistentAuditEvents = persistenceAuditEventRepository.findByPrincipalAndAuditEventDateAfterAndAuditEventType(principal, after, type)
        return auditEventConverter.convertToAuditEvent(persistentAuditEvents)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun add(event: AuditEvent) {
        if (AUTHORIZATION_FAILURE != event.type && Constants.ANONYMOUS_USER != event.principal) {

            val eventData = truncate(auditEventConverter.convertDataToStrings(event.data))

            val persistentAuditEvent = PersistentAuditEvent(
                    principal = event.principal,
                    auditEventType = event.type,
                    auditEventDate = event.timestamp,
                    data = eventData)
            persistenceAuditEventRepository.save(persistentAuditEvent)
        }
    }

    /**
     * Truncate event data that might exceed column length.
     */
    private fun truncate(data: Map<String, String>?): Map<String, String?> {
        val results = HashMap<String, String?>()

        if (data != null) {
            for (entry in data.entries) {
                var value: String? = entry.value
                if (value != null) {
                    val length = value.length
                    if (length > EVENT_DATA_COLUMN_MAX_LENGTH) {
                        value = value.substring(0, EVENT_DATA_COLUMN_MAX_LENGTH)
                        log.warn("Event data for {} too long ({}) has been truncated to {}. Consider increasing column width.",
                                entry.key, length, EVENT_DATA_COLUMN_MAX_LENGTH)
                    }
                }
                results[entry.key] = value
            }
        }
        return results
    }

    companion object {
        private const val AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE"

        /**
         * Should be the same as in Liquibase migration.
         */
        const val EVENT_DATA_COLUMN_MAX_LENGTH = 255
    }
}
