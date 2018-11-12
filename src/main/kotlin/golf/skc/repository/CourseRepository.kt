package golf.skc.repository

import golf.skc.domain.Course
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for the Address entity.
 */
@Repository
interface CourseRepository : JpaRepository<Course, Long>, JpaSpecificationExecutor<Course>
