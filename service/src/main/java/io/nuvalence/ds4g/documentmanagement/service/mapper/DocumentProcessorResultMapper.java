package io.nuvalence.ds4g.documentmanagement.service.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingResultModel;
import lombok.SneakyThrows;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Maps document processing result entities and models.
 */
@Mapper(componentModel = "spring")
public interface DocumentProcessorResultMapper {

    ObjectMapper objectMapper = new ObjectMapper();
    DocumentProcessorResultMapper INSTANCE = Mappers.getMapper(DocumentProcessorResultMapper.class);

    /**
     * Maps a document processing result entity to model.
     *
     * @param result processing result entity.
     * @return the model.
     */
    @Mapping(target = "result", source = "result", qualifiedByName = "mapResults")
    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "mapCreatedTimestamp")
    DocumentProcessingResultModel toModel(DocumentProcessorResult result);

    /**
     * Maps a result object to a string.
     *
     * @param results result to be mapped.
     * @return resulting string.
     */
    @Named("mapResults")
    @SneakyThrows
    default JsonNode mapResults(Object results) {
        return results != null ? objectMapper.valueToTree(results) : null;
    }

    /**
     * Maps OffsetDatetime to string, with required format.
     *
     * @param createdTimestamp date time to be transformed.
     * @return resulting string.
     */
    @Named("mapCreatedTimestamp")
    default String mapCreatedTimestamp(OffsetDateTime createdTimestamp) {
        return createdTimestamp != null
                ? createdTimestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : null;
    }
}
