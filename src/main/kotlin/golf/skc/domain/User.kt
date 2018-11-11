package golf.skc.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import golf.skc.config.Constants
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.Instant
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Email
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

/**
 * A user.
 */
@Entity
@Table(name = "jhi_user")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
data class User(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @SequenceGenerator(name = "sequenceGenerator")
  var id: Long? = null,

  // Lowercase the login before saving it in database
  @NotNull
  @Pattern(regexp = Constants.LOGIN_REGEX)
  @Size(min = 1, max = 50)
  @Column(length = 50, unique = true, nullable = false)
  var login: String? = null,

  @JsonIgnore
  @NotNull
  @Size(min = 60, max = 60)
  @Column(name = "password_hash", length = 60, nullable = false)
  var password: String? = null,

  @Size(max = 50)
  @Column(name = "first_name", length = 50)
  var firstName: String? = null,

  @Size(max = 50)
  @Column(name = "last_name", length = 50)
  var lastName: String? = null,

  @Email
  @Size(min = 5, max = 254)
  @Column(length = 254, unique = true)
  var email: String? = null,

  @NotNull
  @Column(nullable = false)
  var activated: Boolean = false,

  @Size(min = 2, max = 6)
  @Column(name = "lang_key", length = 6)
  var langKey: String? = null,

  @Size(max = 256)
  @Column(name = "image_url", length = 256)
  var imageUrl: String? = null,

  @Size(max = 20)
  @Column(name = "activation_key", length = 20)
  @JsonIgnore
  var activationKey: String? = null,

  @Size(max = 20)
  @Column(name = "reset_key", length = 20)
  @JsonIgnore
  var resetKey: String? = null,

  @Column(name = "reset_date")
  var resetDate: Instant? = null,

  @JsonIgnore
  @ManyToMany
  @JoinTable(name = "jhi_user_authority", joinColumns = [JoinColumn(name = "user_id", referencedColumnName = "id")],
    inverseJoinColumns = [JoinColumn(name = "authority_name", referencedColumnName = "name")])
  @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
  @BatchSize(size = 20)
  var authorities: MutableSet<Authority> = HashSet()
) : AbstractAuditingEntity() {
  @PrePersist
  @PreUpdate
  fun beforePersist() {
    this.login = this.login?.toLowerCase()
    this.email = this.email?.toLowerCase()
  }
}
