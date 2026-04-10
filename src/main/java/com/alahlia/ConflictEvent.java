package com.alahlia;

import java.io.Serializable;
import java.util.*;

/**
 * Legacy ConflictEvent stub — matches the serialized field layout.
 * Only used for deserialization during migration.
 */
public class ConflictEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public Date detectedAt;
    public Map<String, String> conflictingStatuses;
    public String severity;
    public String resolutionMethod;
    public String resolvedBy;
    public String resolvedStatus;
    public String note;
}
