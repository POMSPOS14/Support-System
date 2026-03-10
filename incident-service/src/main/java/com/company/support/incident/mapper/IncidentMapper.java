package com.company.support.incident.mapper;

import com.company.support.image.grpc.ImageResponse;
import com.company.support.incident.dto.request.CreateIncidentRequest;
import com.company.support.incident.dto.request.UpdateIncidentRequest;
import com.company.support.incident.dto.response.IncidentDetailResponse;
import com.company.support.incident.dto.response.IncidentResponse;
import com.company.support.incident.entity.Incident;
import com.company.support.user.grpc.UserResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface IncidentMapper {

    IncidentResponse toResponse(Incident incident);

    List<IncidentResponse> toResponseList(List<Incident> incidents);

    IncidentDetailResponse toDetailResponse(Incident incident);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateCreate", ignore = true)
    @Mapping(target = "dateClosed", ignore = true)
    @Mapping(target = "analystId", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "responsibleService", ignore = true)
    Incident toEntity(CreateIncidentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateCreate", ignore = true)
    @Mapping(target = "dateClosed", ignore = true)
    @Mapping(target = "analystId", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "responsibleService", ignore = true)
    void updateEntity(UpdateIncidentRequest request, @MappingTarget Incident incident);

    @Mapping(target = "role", expression = "java(response.getRole())")
    @Mapping(target = "userFullName", expression = "java(response.getUserFullName())")
    @Mapping(target = "userLogin", expression = "java(response.getUserLogin())")
    @Mapping(target = "phoneNumber", expression = "java(response.getPhoneNumber())")
    @Mapping(target = "workplace", expression = "java(response.getWorkplace())")
    com.company.support.incident.dto.response.UserResponse fromUserProto(UserResponse response);

    @Mapping(target = "fileName", expression = "java(response.getFileName())")
    @Mapping(target = "mediaType", expression = "java(response.getMediaType())")
    com.company.support.incident.dto.response.ImageResponse fromImageProto(ImageResponse response);

    List<com.company.support.incident.dto.response.ImageResponse> fromImageProtoList(List<ImageResponse> responses);
}
