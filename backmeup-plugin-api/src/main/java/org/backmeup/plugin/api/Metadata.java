package org.backmeup.plugin.api;

public class Metadata {
    
    private Metadata() {
        // Utility classes should not have public constructor
    }
    
    /**
     * Valid values are: DAILY, MONTHLY, WEEKLY
     */
    public static final String BACKUP_FREQUENCY = "META_BACKUP_FREQUENCY";

    /**
     * The file size limit in MB (e.g. "100")
     */
    public static final String FILE_SIZE_LIMIT = "META_FILE_SIZE_LIMIT";

    /**
     * The users quota that is currently stored on a service in MB (e.g. "234.55")
     */
    public static final String QUOTA = "META_QUOTA";

    /**
     * The maximal amount of data that can be stored on a service in MB (e.g. "2000").
     */
    public static final String QUOTA_LIMIT = "META_QUOTA_LIMIT";

    /**
     * Declares if a storage location is 'always on' i.e. the data accessible on demand. boolean flag True or False
     */
    public static final String STORAGE_ALWAYS_ACCESSIBLE = "META_STORAGE_ALWAYS_ACCESSIBLE";
    
    /**
     * Host and common base path for datasinks that support on demand accessible data.
     */
    public static final String DOWNLOAD_BASE = "META_DOWNLOAD_BASE";
    
    /**
     * Indicates that a plugin provides user account specific options. Boolean flag True or False
     */
    public static final String DYNAMIC_OPTIONS = "META_DYNAMIC_OPTIONS";
    
}
