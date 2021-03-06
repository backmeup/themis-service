package org.backmeup.service.client;

import java.util.List;

import org.backmeup.model.dto.BackupJobDTO;
import org.backmeup.model.dto.BackupJobExecutionDTO;
import org.backmeup.model.dto.WorkerConfigDTO;
import org.backmeup.model.dto.WorkerInfoDTO;
import org.backmeup.model.dto.WorkerMetricDTO;
import org.backmeup.service.client.model.auth.AuthInfo;

public interface BackmeupService {
    AuthInfo authenticate(String username, String password);

    AuthInfo authenticateWorker(String workerId, String workerSecret);

    BackupJobDTO getBackupJob(Long jobId);

    BackupJobDTO updateBackupJob(BackupJobDTO backupJob);

    BackupJobExecutionDTO getBackupJobExecution(Long jobExecId);

    BackupJobExecutionDTO getBackupJobExecution(Long jobExecId, boolean redeemToken);

    BackupJobExecutionDTO updateBackupJobExecution(BackupJobExecutionDTO jobExecution);

    WorkerConfigDTO initializeWorker(WorkerInfoDTO workerInfo);
    
    void addWorkerMetrics(List<WorkerMetricDTO> workerMetrics);
}
