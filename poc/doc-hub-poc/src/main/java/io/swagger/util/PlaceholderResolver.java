package io.swagger.util;

import io.swagger.model.context.ExtractionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PlaceholderResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public String resolve(String template, ExtractionContext context) {
        if (template == null) {
            return null;
        }

        String resolved = template;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object value = context.getVariables().get(placeholder);

            if (value != null) {
                resolved = resolved.replace("${" + placeholder + "}", value.toString());
                log.debug("Resolved placeholder {} to {}", placeholder, value);
            } else {
                log.warn("Placeholder {} not found in context", placeholder);
            }
        }

        return resolved;
    }

    public Map<String, Object> resolveMap(
        Map<String, String> templateMap,
        ExtractionContext context
    ) {
        Map<String, Object> resolved = new HashMap<>();

        templateMap.forEach((key, template) -> {
            String resolvedValue = resolve(template, context);
            resolved.put(key, resolvedValue);
        });

        return resolved;
    }
}
