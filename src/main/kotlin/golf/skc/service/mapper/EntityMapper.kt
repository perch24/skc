package golf.skc.service.mapper

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param <D> - DTO type parameter.
 * @param <E> - Entity type parameter.
</E></D> */

interface EntityMapper<D, E> {

    fun toEntity(dto: D): E

    fun toDto(entity: E): D

    fun toEntity(dtoList: List<D>): List<E>

    fun toDto(entityList: List<E>): List<D>
}
