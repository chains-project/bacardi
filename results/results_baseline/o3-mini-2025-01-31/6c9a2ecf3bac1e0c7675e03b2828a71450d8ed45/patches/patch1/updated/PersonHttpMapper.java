package io.github.wesleyosantos91.api.v1.mapper;

import io.github.wesleyosantos91.api.v1.request.PersonRequest;
import io.github.wesleyosantos91.api.v1.response.PersonResponse;
import io.github.wesleyosantos91.core.domain.PersonDomain;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public abstract class PersonHttpMapper {

    public static final PersonHttpMapper INSTANCE = Mappers.getMapper(PersonHttpMapper.class);

    public abstract PersonDomain toDomain(PersonRequest request);
    public abstract PersonRequest toRequest(PersonDomain domain);

    public abstract PersonDomain toDomain(PersonResponse response);
    public abstract PersonResponse toResponse(PersonDomain domain);

    public List<PersonResponse> toListResponse(List<PersonDomain> domains) {
        List<PersonResponse> list = new ArrayList<>();
        domains.forEach(d -> list.add(toResponse(d)));
        return list;
    }
}