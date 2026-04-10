package com.alahlia;

import java.awt.Color;
import java.io.Serializable;
import java.util.Map;

/**
 * Legacy ProjectManager stub — matches the serialized field layout.
 * Only used for deserialization during migration.
 *
 * projects.dat contains a Map<String, ProjectManager.Project> directly.
 */
public class ProjectManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public Map<String, Project> projects;
    
    public static class Project implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String id;
        public String name;
        public Color color;
        public String path;
    }
}
