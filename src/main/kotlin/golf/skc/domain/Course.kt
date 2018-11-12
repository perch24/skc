package golf.skc.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name="course")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
data class Course(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @SequenceGenerator(name = "sequenceGenerator")
  var id: Long? = null,

  @NotNull
  @Size(min = 3, max = 128)
  @Column(name = "name", length = 128, nullable = false)
  var name: String? = null,

  @Column(name = "description")
  var description: String? = null,

  @OneToOne(optional = false, cascade = [CascadeType.ALL])
  @NotNull
  @JoinColumn(name = "address_id", unique = true)
  var address: Address? = null
)
