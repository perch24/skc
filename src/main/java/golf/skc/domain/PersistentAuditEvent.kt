package golf.skc.domain

import javax.persistence.*
import javax.validation.constraints.NotNull
import java.time.Instant
import kotlin.collections.HashMap

/**
 * Persist AuditEvent managed by the Spring Boot actuator.
 *
 * @see org.springframework.boot.actuate.audit.AuditEvent
 */
@Entity
@Table(name = "jhi_persistent_audit_event")
class PersistentAuditEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "event_id")
    var id: Long? = null,

    @NotNull
    @Column(nullable = false)
    var principal: String?,

    @Column(name = "event_date")
    var auditEventDate: Instant?,

    @Column(name = "event_type")
    var auditEventType: String?,

    @ElementCollection
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @CollectionTable(name = "jhi_persistent_audit_evt_data", joinColumns = [JoinColumn(name = "event_id")])
    var data: Map<String, String?> = HashMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersistentAuditEvent

        if (id != other.id) return false
        if (principal != other.principal) return false
        if (auditEventDate != other.auditEventDate) return false
        if (auditEventType != other.auditEventType) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (principal?.hashCode() ?: 0)
        result = 31 * result + (auditEventDate?.hashCode() ?: 0)
        result = 31 * result + (auditEventType?.hashCode() ?: 0)
        if (data != null) {
            result = 31 * result + data.hashCode()
        }
        return result
    }
}
