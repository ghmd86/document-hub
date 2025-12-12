package com.documenthub.service.extraction;

import com.documenthub.model.extraction.DataSourceConfig;
import com.documenthub.model.extraction.FieldSourceConfig;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an ordered execution plan for data extraction.
 *
 * <p><b>What:</b> A container for an ordered list of API calls that need
 * to be executed to extract required fields.</p>
 *
 * <p><b>Why:</b> The extraction process involves multiple API calls that
 * may need to be executed in a specific order (due to dependencies) or
 * in parallel (for performance). This class encapsulates the planned
 * API calls and their execution order.</p>
 *
 * <p><b>How:</b> Built by {@link ExtractionPlanBuilder} which analyzes
 * field dependencies and determines the correct order. The plan is then
 * executed by {@link ApiCallExecutor} which handles the actual HTTP calls.</p>
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * ExtractionPlan plan = planBuilder.buildPlan(config, context);
 * if (!plan.isEmpty()) {
 *     return apiCallExecutor.executeSequential(plan, context);
 * }
 * </pre>
 * </p>
 *
 * @see ExtractionPlanBuilder
 * @see ApiCallExecutor
 * @see ApiCall
 */
@Getter
public class ExtractionPlan {

    /**
     * The ordered list of API calls to execute.
     * Order matters for sequential execution; parallel execution ignores order.
     */
    private final List<ApiCall> apiCalls = new ArrayList<>();

    /**
     * Adds an API call to the execution plan.
     *
     * <p><b>What:</b> Creates and adds a new ApiCall to the plan.</p>
     *
     * <p><b>Why:</b> The plan builder needs to add API calls as it
     * determines the execution order based on field dependencies.</p>
     *
     * <p><b>How:</b> Creates an ApiCall with the provided configuration
     * and appends it to the list (preserving insertion order).</p>
     *
     * @param apiId Unique identifier for the API (e.g., "account-api")
     * @param dataSource Configuration for the API endpoint
     * @param fieldSources Map of field names to their extraction configurations
     */
    public void addApiCall(
            String apiId,
            DataSourceConfig dataSource,
            Map<String, FieldSourceConfig> fieldSources) {

        apiCalls.add(new ApiCall(apiId, dataSource, fieldSources));
    }

    /**
     * Checks if the plan has any API calls.
     *
     * <p><b>What:</b> Returns true if no API calls are needed.</p>
     *
     * <p><b>Why:</b> Some templates may not require external API calls
     * (all data comes from the request). Checking for empty plans
     * avoids unnecessary processing.</p>
     *
     * @return true if the plan contains no API calls
     */
    public boolean isEmpty() {
        return apiCalls.isEmpty();
    }

    /**
     * Returns the number of API calls in the plan.
     *
     * <p><b>What:</b> Count of API calls to execute.</p>
     *
     * <p><b>Why:</b> Useful for logging and monitoring to understand
     * the complexity of the extraction process.</p>
     *
     * @return Number of API calls in the plan
     */
    public int size() {
        return apiCalls.size();
    }
}
