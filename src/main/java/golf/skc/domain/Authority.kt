package golf.skc.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Column
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import java.io.Serializable

/**
 * An authority (a security role) used by Spring Security.
 */
@Entity
@Table(name = "jhi_authority")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
data class Authority(
    @NotNull
    @Size(max = 50)
    @Id
    @Column(length = 50)
    var name: String? = null
)
