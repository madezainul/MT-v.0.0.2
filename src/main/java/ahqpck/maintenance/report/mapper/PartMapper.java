package ahqpck.maintenance.report.mapper;

import ahqpck.maintenance.report.dto.PartDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Part;
import ahqpck.maintenance.report.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PartMapper {

    @Mapping(target = "equipmentCount", expression = "java(calculateEquipmentCount(part))")
    PartDTO toDTO(Part part);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "equipmentBOMs", ignore = true)
    Part toEntity(PartDTO dto);

    @Named("mapUserToUserDTO")
    default UserDTO mapUserToUserDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setEmployeeId(user.getEmployeeId());
        return dto;
    }

    default int calculateEquipmentCount(Part part) {
        return part.getEquipmentBOMs() != null ? part.getEquipmentBOMs().size() : 0;
    }
}