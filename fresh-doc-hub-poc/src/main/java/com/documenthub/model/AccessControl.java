package com.documenthub.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Role-based access control configuration for document templates.
 * Defines which roles can perform which actions on documents.
 *
 * Example JSON structure (stored as array):
 * <pre>
 * [
 *   { "role": "admin", "actions": ["View", "Update", "Delete", "Download"] },
 *   { "role": "customer", "actions": ["View", "Download"] },
 *   { "role": "agent", "actions": ["View"] }
 * ]
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessControl {

    private List<RoleAccess> roles;

    /**
     * Creates AccessControl from a JSON array of RoleAccess objects
     */
    @JsonCreator
    public static AccessControl fromRoles(List<RoleAccess> roles) {
        return AccessControl.builder().roles(roles).build();
    }

    /**
     * Serializes AccessControl as a JSON array of RoleAccess objects
     */
    @JsonValue
    public List<RoleAccess> toRoles() {
        return roles;
    }

    /**
     * Check if a given role has permission to perform a specific action
     *
     * @param role   the role to check
     * @param action the action to verify (View, Update, Delete, Download)
     * @return true if the role has permission, false otherwise
     */
    public boolean hasPermission(String role, String action) {
        if (roles == null || roles.isEmpty()) {
            return true; // No restrictions means full access
        }

        return roles.stream()
                .filter(ra -> ra.getRole() != null && ra.getRole().equalsIgnoreCase(role))
                .findFirst()
                .map(ra -> ra.getActions() != null &&
                           ra.getActions().stream().anyMatch(a -> a.equalsIgnoreCase(action)))
                .orElse(false);
    }

    /**
     * Get all actions permitted for a specific role
     *
     * @param role the role to look up
     * @return list of permitted actions, or empty list if role not found
     */
    public List<String> getActionsForRole(String role) {
        if (roles == null || roles.isEmpty()) {
            return List.of("View", "Update", "Delete", "Download"); // Full access when no restrictions
        }

        return roles.stream()
                .filter(ra -> ra.getRole() != null && ra.getRole().equalsIgnoreCase(role))
                .findFirst()
                .map(RoleAccess::getActions)
                .orElse(List.of());
    }
}
