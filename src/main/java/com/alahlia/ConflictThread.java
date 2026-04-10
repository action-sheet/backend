package com.alahlia;

import java.io.Serializable;
import java.util.*;

/**
 * Legacy ConflictThread stub — matches the serialized field layout.
 * Only used for deserialization during migration.
 */
public class ConflictThread implements Serializable {
    private static final long serialVersionUID = 1L;

    public String threadId;
    public String actionSheetId;
    public List<ConflictMessage> messages;
    public Date createdAt;

    public static class ConflictMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        public String senderUserId;
        public String senderRole;
        public int senderHierarchyLevel;
        public String text;
        public Date timestamp;
        public String status;
    }
}
