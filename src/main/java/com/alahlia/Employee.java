package com.alahlia;

import java.io.Serializable;
import java.util.Date;

/**
 * Legacy Employee stub — matches the serialized field layout in employees.dat
 * Only used for deserialization during migration.
 */
public class Employee implements Serializable {
    private static final long serialVersionUID = 2L;

    public String name;
    public String email;
    public String department;
    public String activeDirectory;
    public String role;
    public String adObjectGuid;
    public String adDistinguishedName;
    public Date lastAdSyncTime;
    public boolean isAdSynced;
    public boolean isGroup;
}
