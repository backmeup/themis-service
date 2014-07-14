package org.backmeup.rest.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.backmeup.model.ActionProfile;
import org.backmeup.model.BackMeUpUser;
import org.backmeup.model.BackupJob;
import org.backmeup.model.Profile;
import org.backmeup.model.ProfileOptions;
import org.backmeup.model.ValidationNotes;
import org.backmeup.model.constants.BackupJobStatus;
import org.backmeup.model.constants.DelayTimes;
import org.backmeup.model.dto.BackupJobCreationDTO;
import org.backmeup.model.dto.BackupJobDTO;
import org.backmeup.model.dto.PluginProfileDTO;
import org.backmeup.model.dto.BackupJobDTO.JobFrequency;
import org.backmeup.model.dto.BackupJobDTO.JobStatus;
import org.backmeup.rest.auth.BackmeupPrincipal;
import org.backmeup.rest.filters.SecurityInterceptor;


@Path("/backupjobs")
public class BackupJobs extends Base {	
	@Context
    private SecurityContext securityContext;
	
	@RolesAllowed("user")
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public List<BackupJobDTO> listBackupJobs(@QueryParam("jobStatus") JobStatus jobStatus) {		
		BackMeUpUser activeUser = ((BackmeupPrincipal)securityContext.getUserPrincipal()).getUser();
		
		BackupJobStatus status = getMapper().map(jobStatus, BackupJobStatus.class);
		
		List<BackupJob> allJobsOfUser = getLogic().getJobs(activeUser.getUsername());
		List<BackupJobDTO> jobList = new ArrayList<>();
		
		for(BackupJob job : allJobsOfUser) {
			if ((jobStatus == null) || (status == job.getStatus())) {
				BackupJobDTO jobDTO = getMapper().map(job, BackupJobDTO.class);
				jobList.add(jobDTO);
			}
		}

		return jobList;
	}
	
	@RolesAllowed("user")
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BackupJobDTO createBackupJob(BackupJobCreationDTO backupJob) {		
//		return  DummyDataManager.getBackupJobDTO(expandUser, expandToken, expandProfiles, expandProtocol);
		BackMeUpUser activeUser = ((BackmeupPrincipal)securityContext.getUserPrincipal()).getUser();
		
		Profile sourceProfile = getLogic().getPluginProfile(backupJob.getSource());
		Set<ProfileOptions> sourceProfiles = new HashSet<>();
		sourceProfiles.add(new ProfileOptions(sourceProfile, new String[0]));
		
		Profile sinkProfile = getLogic().getPluginProfile(backupJob.getSink());
		
		// TODO: actions ignored
		List<ActionProfile> actionProfiles = new ArrayList<>();
		
		long delay = DelayTimes.DELAY_MONTHLY;
		boolean reschedule = false;
		String timeExpression = "monthly";
		
		if (backupJob.getSchedule().equals(JobFrequency.daily)) {
			delay = DelayTimes.DELAY_DAILY;
			timeExpression = "daily";
			reschedule = true;
		} else if (backupJob.getSchedule().equals(JobFrequency.weekly)) {
			delay = DelayTimes.DELAY_WEEKLY;
			timeExpression = "weekly";
			reschedule = true;
			
		} else if (backupJob.getSchedule().equals(JobFrequency.montly)) {
			delay = DelayTimes.DELAY_MONTHLY;
			timeExpression = "monthly";
			reschedule = true;
			
		} else if (backupJob.getSchedule().equals(JobFrequency.onece)) {
			delay = DelayTimes.DELAY_REALTIME;
			timeExpression = "realtime";
			reschedule = false;
			
		} 
		
		BackupJob job = new BackupJob(activeUser, sourceProfiles, sinkProfile, actionProfiles, backupJob.getStart(), delay, backupJob.getJobTitle(), reschedule);
		job.setTimeExpression(timeExpression);
		ValidationNotes vn = getLogic().createBackupJob(job);
		
		if(vn.getValidationEntries().size() > 0) {
//			List<ValidationEntry> entries = vn.getValidationEntries();
//			throw new WebApplicationException("Validation threw " + entries.size() + " errors", Status.INTERNAL_SERVER_ERROR);
		} 
		
		job = vn.getJob();
		return getMapper().map(job, BackupJobDTO.class);
		
	}
	
	@RolesAllowed({"user", "worker"})
	@GET
	@Path("/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public BackupJobDTO getBackupJob(
			@PathParam("jobId") String jobId, 
			@QueryParam("expandUser") @DefaultValue("false") boolean expandUser,
			@QueryParam("expandToken") @DefaultValue("false") boolean expandToken,
			@QueryParam("expandProfiles") @DefaultValue("false") boolean expandProfiles,
			@QueryParam("expandProtocol") @DefaultValue("false") boolean expandProtocol) {
//		return DummyDataManager.getBackupJobDTO(expandUser, expandToken, expandProfiles, expandProtocol);
		
		BackMeUpUser activeUser = ((BackmeupPrincipal)securityContext.getUserPrincipal()).getUser();
		
		BackupJob job = getLogic().getBackupJobFull("", Long.parseLong(jobId));
		if ((activeUser.getUserId() != job.getUser().getUserId()) && (!activeUser.getUsername().equals(SecurityInterceptor.BACKMEUP_WORKER_NAME))) {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		BackupJobDTO jobDTO = getMapper().map(job, BackupJobDTO.class);
		
		if(!expandUser) {
			jobDTO.setUser(null);
		}
		
		if(!expandToken) {
			jobDTO.setToken(null);
		}
		
		if(expandProfiles) {
			// get source profile
			ProfileOptions sourceProfileOptions = job.getSourceProfiles().iterator().next();
			Profile sourceProfile = getLogic().getPluginProfile(sourceProfileOptions.getProfile().getProfileId());
			PluginProfileDTO sourceProfileDTO = getMapper().map(sourceProfile, PluginProfileDTO.class);
			sourceProfileDTO.setPluginId(sourceProfile.getDescription());
			jobDTO.setSource(sourceProfileDTO);

			// get sink profile
			Profile sinkProfile = getLogic().getPluginProfile(job.getSinkProfile().getProfileId());		
			PluginProfileDTO sinkProfileDTO = getMapper().map(sinkProfile, PluginProfileDTO.class);
			sinkProfileDTO.setPluginId(sinkProfile.getDescription());
			jobDTO.setSink(sinkProfileDTO);
			
			// get action profiles
			for(ActionProfile action : job.getRequiredActions()) {
				Profile actionProfile = getLogic().getPluginProfile(action.getId());
				PluginProfileDTO actionProfileDTO = getMapper().map(actionProfile, PluginProfileDTO.class);
				actionProfileDTO.setPluginId(actionProfile.getDescription());
				jobDTO.setSink(actionProfileDTO);
			}
		}
		
		return jobDTO;
	}
	
	@RolesAllowed({"user", "worker"})
	@PUT
	@Path("/{jobId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BackupJobDTO updateBackupJob(@PathParam("jobId") String jobId, BackupJobDTO backupjob) {
		if(Long.parseLong(jobId) != backupjob.getJobId()) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		BackMeUpUser activeUser = ((BackmeupPrincipal)securityContext.getUserPrincipal()).getUser();
		
		BackupJob job = getLogic().getBackupJobFull("", backupjob.getJobId());
		if ((activeUser.getUserId() != job.getUser().getUserId()) && (!activeUser.getUsername().equals(SecurityInterceptor.BACKMEUP_WORKER_NAME))) {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
//		job.getToken().setTokenId(backupjob.getToken().getTokenId());
		job.getToken().setToken(backupjob.getToken().getToken());
		job.getToken().setBackupdate(backupjob.getToken().getValidity());
		
		BackupJobStatus jobStatus = getMapper().map(backupjob.getJobStatus(), BackupJobStatus.class);
		job.setStatus(jobStatus);
		
		// TODO: Job protocol
		
		getLogic().updateBackupJob(job.getUser().getUsername(), job);
		
		return backupjob;
		
	}
	
	@RolesAllowed("user")
	@DELETE
	@Path("/{jobId}")
	public void deleteBackupJob(@PathParam("jobId") String jobId) {
		BackMeUpUser activeUser = ((BackmeupPrincipal)securityContext.getUserPrincipal()).getUser();
		
		BackupJob job = getLogic().getBackupJobFull("", Long.parseLong(jobId));
		if (job.getUser().getUserId() != activeUser.getUserId()) {
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		getLogic().deleteJob(activeUser.getUsername(), Long.parseLong(jobId));
	}
}
