package io.nuvalence.ds4g.documentmanagement.service.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.ds4g.documentmanagement.service.entity.DocumentProcessorResult;
import io.nuvalence.ds4g.documentmanagement.service.generated.models.DocumentProcessingResultModel;
import io.nuvalence.ds4g.documentmanagement.service.processor.impl.DocumentQualityProcessorResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

class DocumentProcessorResultMapperTest {

    private final DocumentProcessorResultMapper mapper =
            Mappers.getMapper(DocumentProcessorResultMapper.class);

    @Test
    void shouldMapToModel() {
        OffsetDateTime createdTimestamp = OffsetDateTime.now();
        DocumentQualityProcessorResult results = new DocumentQualityProcessorResult();

        DocumentProcessorResult result = new DocumentProcessorResult();
        result.setTimestamp(createdTimestamp);
        result.setResult(results);

        DocumentProcessingResultModel model = mapper.toModel(result);

        assertEquals(
                model.getTimestamp(),
                createdTimestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode expected = objectMapper.valueToTree(results);
        JsonNode actual = (JsonNode) model.getResult();

        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        DocumentProcessorResult result = null;

        DocumentProcessingResultModel model = mapper.toModel(result);

        assertNull(model);
    }

    @Test
    void shouldReturnNullWhenTimestampIsNull() {
        DocumentProcessorResult result = new DocumentProcessorResult();
        result.setTimestamp(null);

        DocumentProcessingResultModel model = mapper.toModel(result);

        assertNull(model.getTimestamp());
    }

    @Test
    void shouldReturnNullWhenResultsIsNull() {
        DocumentProcessorResult result = new DocumentProcessorResult();
        result.setResult(null);

        DocumentProcessingResultModel model = mapper.toModel(result);

        assertNull(model.getResult());
    }
}
