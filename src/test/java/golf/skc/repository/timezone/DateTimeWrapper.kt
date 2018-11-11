package golf.skc.repository.timezone

import java.time.*
import javax.persistence.*

@Entity
@Table(name = "jhi_date_time_wrapper")
data class DateTimeWrapper(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @SequenceGenerator(name = "sequenceGenerator")
  var id: Long? = null,

  @Column(name = "instant")
  var instant: Instant? = null,

  @Column(name = "local_date_time")
  var localDateTime: LocalDateTime? = null,

  @Column(name = "offset_date_time")
  var offsetDateTime: OffsetDateTime? = null,

  @Column(name = "zoned_date_time")
  var zonedDateTime: ZonedDateTime? = null,

  @Column(name = "local_time")
  var localTime: LocalTime? = null,

  @Column(name = "offset_time")
  var offsetTime: OffsetTime? = null,

  @Column(name = "local_date")
  var localDate: LocalDate? = null
)
