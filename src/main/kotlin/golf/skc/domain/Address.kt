package golf.skc.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name="address")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
data class Address(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @SequenceGenerator(name = "sequenceGenerator")
  var id: Long? = null,

  @NotNull
  @Size(min = 3, max = 128)
  @Column(name = "line_1", length = 128, nullable = false)
  var line1: String? = null,

  @Column(name = "line_2")
  var line2: String? = null,

  @NotNull
  @Column(name = "city", nullable = false)
  var city: String? = null,

  @NotNull
  @Column(name = "state", nullable = false)
  var state: String? = null,

  @NotNull
  @Column(name = "zip", nullable = false)
  var zip: String? = null,

  @NotNull
  @Column(name = "country", nullable = false)
  var country: String? = null
)
