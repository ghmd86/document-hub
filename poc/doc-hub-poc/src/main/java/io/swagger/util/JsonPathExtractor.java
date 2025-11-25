package io.swagger.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JsonPathExtractor {

    private static final Configuration CONFIG = Configuration.builder()
        .options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS)
        .build();

    public Object extract(String json, String jsonPathExpression) {
        try {
            return JsonPath.using(CONFIG).parse(json).read(jsonPathExpression);
        } catch (Exception e) {
            log.warn("Failed to extract using JSONPath: {}", jsonPathExpression, e);
            return null;
        }
    }
}
