package com.documenthub.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Defines actions permitted for a specific role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAccess {

    private String role;           // e.g., "admin", "customer", "agent", "backOffice", "system"
    private List<String> actions;  // e.g., ["View", "Update", "Delete", "Download"]
}
