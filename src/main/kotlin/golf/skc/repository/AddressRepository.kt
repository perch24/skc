package golf.skc.repository

import golf.skc.domain.Address
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for the Address entity.
 */
@Repository
interface AddressRepository : JpaRepository<Address, Long>
