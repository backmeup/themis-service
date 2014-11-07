package org.backmeup.logic.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.dal.BackupJobDao;
import org.backmeup.dal.DataAccessLayer;
import org.backmeup.dal.JobProtocolDao;
import org.backmeup.dal.StatusDao;
import org.backmeup.keyserver.client.Keyserver;
import org.backmeup.logic.BackupLogic;
import org.backmeup.model.BackMeUpUser;
import org.backmeup.model.BackupJob;
import org.backmeup.model.JobProtocol;
import org.backmeup.model.Token;
import org.backmeup.model.JobProtocol.JobProtocolMember;
import org.backmeup.model.Profile;
import org.backmeup.model.ProtocolOverview;
import org.backmeup.model.ProtocolOverview.Activity;
import org.backmeup.model.ProtocolOverview.Entry;
import org.backmeup.model.Status;
import org.backmeup.model.StatusWithFiles;
import org.backmeup.model.constants.BackupJobStatus;
import org.backmeup.model.dto.JobProtocolDTO;

@ApplicationScoped
public class BackupLogicImpl implements BackupLogic {

    private static final String JOB_USER_MISSMATCH = "org.backmeup.logic.impl.BusinessLogicImpl.JOB_USER_MISSMATCH";
    private static final String NO_SUCH_JOB = "org.backmeup.logic.impl.BusinessLogicImpl.NO_SUCH_JOB";
    private static final String NO_PROFILE_WITHIN_JOB = "org.backmeup.logic.impl.BusinessLogicImpl.NO_PROFILE_WITHIN_JOB";
    
    @Inject
    private DataAccessLayer dal;
    
    @Inject
    private Keyserver keyserverClient;

    private final ResourceBundle textBundle = ResourceBundle.getBundle("BackupLogicImpl");
    
    private BackupJobDao getBackupJobDao() {
        return dal.createBackupJobDao();
    }

    private StatusDao getStatusDao() {
        return dal.createStatusDao();
    }

    private JobProtocolDao createJobProtocolDao() {
        return dal.createJobProtocolDao();
    }
    
    // BackupLogic methods ----------------------------------------------------
    
    @Override
    public BackupJob createJob(BackupJob job) {
		job.setStatus(BackupJobStatus.queued);

		Long firstExecutionDate = job.getStart().getTime() + job.getDelay();
		
		storePluginConfigOnKeyserver(job);

		// reusable=true means, that we can get the data for the token + a new token for the next backup
		Token t = keyserverClient.getToken(job, job.getUser().getPassword(), firstExecutionDate, true, null);
		job.setToken(t);
		
		return getBackupJobDao().save(job);
		
    }
    
    @Override
    public List<BackupJob> getBackupJobsOf(Long userId) {
        return getBackupJobDao().findByUserId(userId);
    }
    
    @Override
    public BackupJob getExistingJob(Long jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("JobId must not be null");
        }
        BackupJob job = getBackupJobDao().findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException(String.format(textBundle.getString(NO_SUCH_JOB), jobId));
        }
        return job;
    }

    @Override
    public BackupJob getExistingUserJob(Long jobId, Long userId) {
        BackupJob job = getExistingJob(jobId);
        if (job.getUser().getUserId() != userId) {
            throw new IllegalArgumentException(String.format(textBundle.getString(JOB_USER_MISSMATCH),
                    jobId, userId));
        }
        return job;
    }
    
    @Override
    public void updateJob(BackupJob persistentJob, BackupJob updatedJob) {
    	persistentJob.getToken().setTokenId(updatedJob.getToken().getTokenId());
    	persistentJob.getToken().setToken(updatedJob.getToken().getToken());
    	persistentJob.getToken().setBackupdate(updatedJob.getToken().getBackupdate());
    	
    	persistentJob.setStatus(updatedJob.getStatus());
    	
    	// TODO: update fields
    }

    @Override
    public void deleteJob(Long userId, Long jobId) {
        BackupJob job = getExistingUserJob(jobId, userId);

        deleteStatuses(job.getId());

        getBackupJobDao().delete(job);
    }
    
    @Override
    public void deleteJobsOf(Long userId) {
        BackupJobDao jobDao = getBackupJobDao();
        StatusDao statusDao = getStatusDao();
        for (BackupJob job : jobDao.findByUserId(userId)) {
            for (Status status : statusDao.findByJobId(job.getId())) {
                statusDao.delete(status);
            }
            jobDao.delete(job);
        }
    }
    
    @Override
    public void createJobProtocol(BackMeUpUser user, BackupJob job, JobProtocolDTO jobProtocol) {
        JobProtocolDao jpd = createJobProtocolDao();
        
        JobProtocol protocol = new JobProtocol();
        protocol.setUser(user);
        protocol.setJob(job);
        protocol.setSuccessful(jobProtocol.isSuccessful());
        
//        for(JobProtocolMemberDTO pm : jobProtocol.getMembers()) {
//            protocol.addMember(new JobProtocolMember(protocol, pm.getTitle(), pm.getSpace()));
//        }
        
        if (protocol.isSuccessful()) {
            job.setLastSuccessful(protocol.getExecutionTime());
            job.setStatus(BackupJobStatus.successful);
        } else {
            job.setLastFailed(protocol.getExecutionTime());
            job.setStatus(BackupJobStatus.error);
        }
        
        jpd.save(protocol);
    }

    @Override
    public ProtocolOverview getProtocolOverview(BackMeUpUser user, Date from, Date to) {
        List<JobProtocol> protocols = createJobProtocolDao().findByUsernameAndDuration(user.getUsername(), from, to);
        ProtocolOverview po = new ProtocolOverview();
        Map<String, Entry> entries = new HashMap<>();
        double totalSize = 0;
        long totalCount = 0;
        for (JobProtocol prot : protocols) {
            totalCount += prot.getTotalStoredEntries();
            for (JobProtocolMember member : prot.getMembers()) {
                Entry entry = entries.get(member.getTitle());
                if (entry == null) {
                    entry = new Entry(member.getTitle(), 0, member.getSpace());
                    entries.put(member.getTitle(), entry);
                } else {
                    entry.setAbsolute(entry.getAbsolute() + member.getSpace());
                }
                totalSize += member.getSpace();
            }
            po.getActivities().add(new Activity(prot.getJob().getJobTitle(), prot.getExecutionTime()));
        }

        for (Entry entry : entries.values()) {
            entry.setPercent(100 * entry.getAbsolute() / totalSize);
            po.getStoredAmount().add(entry);
        }
        po.setTotalCount(totalCount+"");
        // TODO Determine format of bytes (currently MB)
        po.setTotalStored(totalSize / 1024 / 1024 +" MB");
        po.setUser(user.getUserId());
        return po;
    }

    @Override
    public void deleteProtocolsOf(Long userId) {
        createJobProtocolDao().deleteByUserId(userId);
    }
    
    // Helper methods ---------------------------------------------------------
    
    private void deleteStatuses(Long jobId) {
        // Delete Job status records first
        StatusDao statusDao = getStatusDao();
        for (Status status : statusDao.findByJobId(jobId)) {
            statusDao.delete(status);
        }
    }
    
    private void storePluginConfigOnKeyserver(BackupJob job) {
    	// Active user (password is set) is stored in job.getUser()
    	// profile users (job.getXProfile().getUser() password is null!
    	updateProfileOnKeyserver(job.getSourceProfile(), job.getUser().getPassword());
    	updateProfileOnKeyserver(job.getSinkProfile(), job.getUser().getPassword());
		for (Profile actionProfile : job.getActionProfiles()) {
			updateProfileOnKeyserver(actionProfile, job.getUser().getPassword());
		}
    }
    
    private void updateProfileOnKeyserver(Profile profile, String password) {    
    	if(keyserverClient.isServiceRegistered(profile.getId())){
    		keyserverClient.deleteService(profile.getId());
    	}
    	
        if (keyserverClient.isAuthInformationAvailable(profile, password)) {
            keyserverClient.deleteAuthInfo(profile.getId());
        }
        
        // For now, store auth data and props together
        Properties props = new Properties();
        // Otherwise, we cannot retrieve the token later on.
        // If no property is available the keyserver throws internally an IndexOutOfBoundsException
        props.put("dummy", "dummy"); 
		if (profile.getAuthData() != null && profile.getAuthData().getProperties() != null) {
			props.putAll(profile.getAuthData().getProperties());
		}
		if (profile.getProperties() != null) {
			props.putAll(profile.getProperties());
		}

		keyserverClient.addService(profile.getId());
		keyserverClient.addAuthInfo(profile, password, props);
    }
    
    // Deprecated methods -----------------------------------------------------
    
    @Deprecated
    @Override
    public Profile getJobActionOption(String actionId, Long jobId) {
        BackupJob job = getExistingJob(jobId);
        for (Profile action : job.getActionProfiles()) {
            if (action.getId().equals(actionId)) {
                return action;
            }
        }
        throw new IllegalArgumentException(String.format(textBundle.getString(NO_PROFILE_WITHIN_JOB), jobId, actionId));
    }

    @Deprecated
    @Override
    public void updateJobActionOption(String actionId, Long jobId, Map<String, String> actionOptions) {
        BackupJob job = getExistingJob(jobId);
        for (Profile ap : job.getActionProfiles()) {
            if (ap.getId().equals(actionId)) {
                ap.getProperties().clear();
                addActionProperties(ap, actionOptions);
            }
        }
    }

    @Deprecated
    private void addActionProperties(Profile ap, Map<String, String> keyValues) {
            ap.getProperties().putAll(keyValues);
    }
    
    @Deprecated
    @Override
    public BackupJob updateRequestFor(Long jobId) {
        return getExistingJob(jobId);
    }

    @Deprecated
    @Override
    public List<StatusWithFiles> getStatus(Long userId, Long jobId) {
        BackupJobDao jobDao = getBackupJobDao();
        
        if (jobId == null) {
            List<Status> status = new ArrayList<>();
            BackupJob job = jobDao.findLastBackupJob(userId);
            if (job != null) {
                status.addAll(getStatusForJob(job));
            }
            // for (BackupJob job : jobs) {
            //     status.add(getStatusForJob(job));
            // }
            return allowFiles(status);
        }
        
        BackupJob job = getExistingUserJob(jobId, userId);
        List<Status> status = new ArrayList<>();
        status.addAll(getStatusForJob(job));
        return allowFiles(status);
    }

    @Deprecated
    private List<StatusWithFiles> allowFiles(List<Status> statuses) {
        List<StatusWithFiles> list = new ArrayList<>();
        for (Status status : statuses) {
            list.add(new StatusWithFiles(status));
        }
        return list;
    }

    @Deprecated
    private List<Status> getStatusForJob(final BackupJob job) {
        StatusDao sd = dal.createStatusDao();
        List<Status> status = sd.findLastByJob(job.getUser().getUsername(), job.getId());
        return status;
    }
}
