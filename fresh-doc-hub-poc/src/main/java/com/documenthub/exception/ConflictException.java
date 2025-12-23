package com.documenthub.exception;

/**
 * Exception thrown when a conflict is detected (e.g., duplicate entries, date overlaps).
 */
public class ConflictException extends RuntimeException {

    private final String conflictType;

    public ConflictException(String message) {
        super(message);
        this.conflictType = "GENERAL";
    }

    public ConflictException(String conflictType, String message) {
        super(message);
        this.conflictType = conflictType;
    }

    public String getConflictType() {
        return conflictType;
    }
}
