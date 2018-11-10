package golf.skc.service.mapper

import golf.skc.domain.Authority
import golf.skc.domain.User
import golf.skc.service.dto.UserDTO

import org.springframework.stereotype.Service

import java.util.*
import java.util.stream.Collectors

/**
 * Mapper for the entity User and its DTO called UserDTO.
 *
 * Normal mappers are generated using MapStruct, this one is hand-coded as MapStruct
 * support is still in beta, and requires a manual step with an IDE.
 */
@Service
open class UserMapper {

    fun userToUserDTO(user: User): UserDTO {
        return UserDTO(user)
    }

    fun usersToUserDTOs(users: List<User>): List<UserDTO> {
        return users
            .map { this.userToUserDTO(it) }
    }

    fun userDTOToUser(userDTO: UserDTO): User {
        return User(id = userDTO.id,
            login = userDTO.login,
            firstName = userDTO.firstName,
            lastName = userDTO.lastName,
            email = userDTO.email,
            imageUrl = userDTO.imageUrl,
            activated = userDTO.isActivated,
            langKey = userDTO.langKey,
            authorities = this.authoritiesFromStrings(userDTO.authorities))
    }

    fun userDTOsToUsers(userDTOs: List<UserDTO>): List<User> {
        return userDTOs.map { this.userDTOToUser(it) }
    }

    fun userFromId(id: Long?): User? {
        if (id == null) {
            return null
        }
        return User(id=id)
    }

    fun authoritiesFromStrings(strings: Set<String>): Set<Authority> {
        return strings.map { Authority(name = it) }.toSet()
    }
}



