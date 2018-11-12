package golf.skc.service

import golf.skc.domain.Address
import javax.persistence.criteria.JoinType

import golf.skc.domain.Address_
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import io.github.jhipster.service.QueryService

import golf.skc.domain.Course
import golf.skc.domain.Course_
import golf.skc.repository.CourseRepository
import golf.skc.service.dto.CourseCriteria
import golf.skc.service.dto.CourseDTO
import golf.skc.service.mapper.CourseMapper

/**
 * Service for executing complex queries for Course entities in the database.
 * The main input is a [CourseCriteria] which gets converted to [Specification],
 * in a way that all the filters must apply.
 * It returns a [List] of [CourseDTO] or a [Page] of [CourseDTO] which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
class CourseQueryService(private val courseRepository: CourseRepository, private val courseMapper: CourseMapper) : QueryService<Course>() {

    private val log = LoggerFactory.getLogger(CourseQueryService::class.java)

    /**
     * Return a [List] of [CourseDTO] which matches the criteria from the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    fun findByCriteria(criteria: CourseCriteria): List<CourseDTO> {
        log.debug("find by criteria : {}", criteria)
        val specification = createSpecification(criteria)
        return courseMapper.toDto(courseRepository.findAll(specification))
    }

    /**
     * Return a [Page] of [CourseDTO] which matches the criteria from the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    fun findByCriteria(criteria: CourseCriteria, page: Pageable): Page<CourseDTO> {
        log.debug("find by criteria : {}, page: {}", criteria, page)
        val specification = createSpecification(criteria)
        return courseRepository.findAll(specification, page)
                .map{ courseMapper.toDto(it) }
    }

    /**
     * Return the number of matching entities in the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    fun countByCriteria(criteria: CourseCriteria): Long {
        log.debug("count by criteria : {}", criteria)
        val specification = createSpecification(criteria)
        return courseRepository.count(specification)
    }

    /**
     * Function to convert CourseCriteria to a [Specification]
     */
    private fun createSpecification(criteria: CourseCriteria?): Specification<Course> {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        var specification = Specification.where<Course>(null)
        if (criteria != null) {
            if (criteria.id != null) {
                specification = specification.and(buildSpecification(criteria.id, Course_.id))
            }
            if (criteria.name != null) {
                specification = specification.and(buildStringSpecification(criteria.name, Course_.name))
            }
            if (criteria.addressId != null) {
                specification = specification.and(buildSpecification(criteria.addressId!!
                ) { root -> root.join<Address>(Course_.address, JoinType.LEFT).get(Address_.id) })
            }
        }
        return specification
    }
}
