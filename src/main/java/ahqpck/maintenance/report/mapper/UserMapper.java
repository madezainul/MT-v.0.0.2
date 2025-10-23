package ahqpck.maintenance.report.mapper;

import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "activationToken", ignore = true)
    @Mapping(target = "roleNames", ignore = true)
    UserDTO toDTO(User user);
}