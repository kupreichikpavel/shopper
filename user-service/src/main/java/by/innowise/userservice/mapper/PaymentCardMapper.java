package by.innowise.userservice.mapper;

import by.innowise.userservice.dto.paymentcard.PaymentCardCreateDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardUpdateDto;
import by.innowise.userservice.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentCardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    PaymentCard toEntity(PaymentCardCreateDto dto);

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponseDto toDto(PaymentCard paymentCard);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntity(PaymentCardUpdateDto dto, @MappingTarget PaymentCard paymentCard);
}
