package golf.skc.service.mapper

import golf.skc.domain.Address
import golf.skc.service.dto.AddressDTO
import org.mapstruct.Mapper
import org.springframework.stereotype.Service

/**
 * Mapper for the entity Course and its DTO CourseDTO.
 */
@Mapper(componentModel = "spring")
@Service
interface AddressMapper : EntityMapper<AddressDTO, Address> {
    override fun toDto(entity: Address): AddressDTO

    override fun toEntity(dto: AddressDTO): Address
}
