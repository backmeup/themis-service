package org.backmeup.plugin.api.connectors;

import java.util.List;
import java.util.Map;

import org.backmeup.model.dto.BackupJobDTO;
import org.backmeup.plugin.api.storage.Storage;

public interface Action {

    public void doAction(Map<String, String> authData, Map<String, String> properties, List<String> options,
            Storage storage, BackupJobDTO job, Progressable progressor)
            throws ActionException;
}
