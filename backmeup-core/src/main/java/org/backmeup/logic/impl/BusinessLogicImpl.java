package org.backmeup.logic.impl;

import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.configuration.cdi.Configuration;
import org.backmeup.dal.Connection;
import org.backmeup.index.model.SearchResponse;
import org.backmeup.job.JobManager;
import org.backmeup.logic.AuthorizationLogic;
import org.backmeup.logic.BackupLogic;
import org.backmeup.logic.BusinessLogic;
import org.backmeup.logic.PluginsLogic;
import org.backmeup.logic.ProfileLogic;
import org.backmeup.logic.SearchLogic;
import org.backmeup.logic.UserRegistration;
import org.backmeup.model.AuthData;
import org.backmeup.model.BackMeUpUser;
import org.backmeup.model.BackupJob;
import org.backmeup.model.PluginConfigInfo;
import org.backmeup.model.Profile;
import org.backmeup.model.ProtocolDetails;
import org.backmeup.model.ProtocolOverview;
import org.backmeup.model.ValidationNotes;
import org.backmeup.model.constants.DelayTimes;
import org.backmeup.model.dto.JobProtocolDTO;
import org.backmeup.model.exceptions.BackMeUpException;
import org.backmeup.model.exceptions.PluginException;
import org.backmeup.model.exceptions.ValidationException;
import org.backmeup.model.spi.PluginDescribable;
import org.backmeup.model.spi.ValidationExceptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the BusinessLogic interface by delegating most operations to
 * following layers: - DataAccessLayer - JobManager - PluginLayer
 * 
 * If an error occurs within a method an exception will be thrown that must be
 * handled by the client of the business logic.
 * 
 * @author fschoeppl
 */
@ApplicationScoped 
public class BusinessLogicImpl implements BusinessLogic {

    private static final String SHUTTING_DOWN_BUSINESS_LOGIC = "org.backmeup.logic.impl.BusinessLogicImpl.SHUTTING_DOWN_BUSINESS_LOGIC";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // There seems to be a problem with weld (can't find resource bundle 
    // with getClass().getSimpleName()). Therefore use class name. 
    private final ResourceBundle textBundle = ResourceBundle.getBundle("BusinessLogicImpl");

    @Inject
    private JobManager jobManager;

    @Inject
    private Connection conn;

    @Inject
    private UserRegistration registration;

    @Inject
    private AuthorizationLogic authorization;

    @Inject
    private SearchLogic search;

    @Inject
    private ProfileLogic profiles;

    @Inject
    private BackupLogic backupJobs;

    @Inject
    private PluginsLogic plugins; 

    @Inject
    @Configuration(key = "backmeup.autoVerifyUser")
    private Boolean autoVerifyUser;

    // ========================================================================

    // CDI lifecycle methods --------------------------------------------------

    @PreDestroy
    public void shutdown() {
        logger.debug(textBundle.getString(SHUTTING_DOWN_BUSINESS_LOGIC));
    }

    // ========================================================================

    // Authentication ---------------------------------------------------------

    @Override
    public BackMeUpUser authorize(final String username, final String password) {
        return conn.txJoinReadOnly(new Callable<BackMeUpUser>() {
            @Override public BackMeUpUser call() {

                BackMeUpUser user = registration.getUserByUsername(username, true);
                authorization.authorize(user, password);
                return user;
            }
        }); 
    }

    // ========================================================================

    // User operations --------------------------------------------------------

    @Override
    public BackMeUpUser getUserByUsername(final String username) {
        return conn.txJoinReadOnly(new Callable<BackMeUpUser>() {
            @Override public BackMeUpUser call() {

                return registration.getUserByUsername(username, true);
                
            }
        });
    }

    @Override
    public BackMeUpUser getUserByUserId(final Long userId) {
        return conn.txJoinReadOnly(new Callable<BackMeUpUser>() {
            @Override public BackMeUpUser call() {

                return registration.getUserByUserId(userId, true);

            }
        });
    }

    @Override
    public BackMeUpUser deleteUser(final Long userId) {
        BackMeUpUser user = conn.txNew(new Callable<BackMeUpUser>() {
            @Override public BackMeUpUser call() {

                BackMeUpUser u = registration.getUserByUserId(userId);
                authorization.unregister(u);
                backupJobs.deleteJobsOf(u.getUserId());
                profiles.deleteProfilesOf(u.getUserId());
                registration.delete(u); 
                return u;

            }
        });

        return user;
    }

    @Override
    public BackMeUpUser updateUser(final BackMeUpUser user) {
        return conn.txNew(new Callable<BackMeUpUser>() {
            @Override public BackMeUpUser call() {

                registration.getUserByUsername(user.getUsername(), true);
                registration.update(user);
                return user;

            }
        });
    }

    @Override
    public BackMeUpUser addUser(final BackMeUpUser newUser) {
        return conn.txNew(new Callable<BackMeUpUser>() {
            @Override public BackMeUpUser call() {
                BackMeUpUser user = registration.register(newUser);
                authorization.register(user);
                if(autoVerifyUser) {
                    registration.activateUserFor(user.getVerificationKey());
                } else {
                    registration.sendVerificationEmailFor(user);
                }
                return user;

            }
        });
    }

    // ========================================================================

    // Plugin operations ------------------------------------------------------

    @Override
    public boolean isPluginAvailable(String pluginId) {
        return plugins.isPluginAvailable(pluginId);
    }

    @Override
    public List<PluginDescribable> getDatasources() {
        return plugins.getDatasources();
    }

    @Override
    public List<PluginDescribable> getDatasinks() {
        return plugins.getDatasinks();
    }

    @Override
    public List<PluginDescribable> getActions() {
        return plugins.getActions();
    }

    @Override
    public PluginDescribable getPluginDescribable(String pluginId) {
        return plugins.getPluginDescribableById(pluginId);
    }

    @Override
    public Profile getPluginProfile(final Long profileId) {
        return conn.txNewReadOnly(new Callable<Profile>() {
            @Override public Profile call() {

                return profiles.getProfile(profileId);

            }
        });
    }

    @Override
    public void deleteProfile(final Long profileId) {
        conn.txJoin(new Runnable() {
            @Override public void run() {

                profiles.deleteProfile(profileId);

            }
        });
    }

    @Override
    public PluginConfigInfo getPluginConfiguration(final String pluginId) {
        return conn.txNew(new Callable<PluginConfigInfo>() {

            @Override public PluginConfigInfo call() {
                PluginConfigInfo pluginConfigInfo = plugins.getPluginConfigInfo(pluginId);
                return pluginConfigInfo;

            }
        });
    }
    
    public PluginConfigInfo getPluginConfiguration(final String pluginId, final AuthData authData) {
        return conn.txNew(new Callable<PluginConfigInfo>() {

            @Override public PluginConfigInfo call() {
                PluginConfigInfo pluginConfigInfo = plugins.getPluginConfigInfo(pluginId, authData);
                return pluginConfigInfo;

            }
        });
    }

    @Override
    public Profile addPluginProfile(final Profile profile) {

        return conn.txNew(new Callable<Profile>() {
            @Override public Profile call() {
                // Check if plugin authorization data is required and still valid
                if((profile.getAuthData() != null) && (profile.getAuthData().getId() != null)) {
                    AuthData authData = profiles.getAuthData(profile.getAuthData().getId());
                    profile.setAuthData(authData);

                    String identification = plugins.authorizePlugin(profile.getAuthData());
                    profile.getAuthData().setIdentification(identification);
                }			

                // Check if plugin validation is required and properties and options are valid
                if (plugins.requiresValidation(profile.getPluginId())) {
                    ValidationNotes notes = plugins.validatePlugin(profile.getPluginId(), profile.getProperties(), profile.getOptions());
                    if (!notes.getValidationEntries().isEmpty()) {
                        throw new ValidationException(ValidationExceptionType.ConfigException, notes);
                    }

                }

                // Everything is in place and valid, now we can store the new profile
                Profile p = profiles.save(profile);

                return p;
            }
        });

    }

    public ValidationNotes validateProfile(final Profile profile) {
        return conn.txJoinReadOnly(new Callable<ValidationNotes>() {
            @Override public ValidationNotes call() {
                ValidationNotes notes = new ValidationNotes();

                try {
                    // Check if plugin authorization data is required and still valid
                    if((profile.getAuthData() != null) && (profile.getAuthData().getId() != null)) {
                        AuthData authData = profiles.getAuthData(profile.getAuthData().getId());
                        profile.setAuthData(authData);

                        String identification = plugins.authorizePlugin(profile.getAuthData());
                        profile.getAuthData().setIdentification(identification);
                    }			

                    // Check if plugin validation is required and properties and options are valid
                    if (plugins.requiresValidation(profile.getPluginId())) {
                        notes.addAll(plugins.validatePlugin(profile.getPluginId(), profile.getProperties(), profile.getOptions()));
                    }
                    return notes;

                } catch (Exception e) {
                    notes.addValidationEntry(ValidationExceptionType.Error, profile.getPluginId(), e);
                    return notes;
                }
            }

        });
    }

    @Override
    public Profile updatePluginProfile(final Profile profile) {
        return conn.txNew(new Callable<Profile>() {
            @Override public Profile call() {
                // TODO: Refactor (see addPluginProfile method); put validation logic in own method
                // Check if plugin authorization data is required and still valid
                if((profile.getAuthData() != null) && (profile.getAuthData().getId() != null)) {
                    AuthData authData = profiles.getAuthData(profile.getAuthData().getId());
                    profile.setAuthData(authData);

                    String identification = plugins.authorizePlugin(profile.getAuthData());
                    profile.getAuthData().setIdentification(identification);
                }			

                // Check if plugin validation is required and properties and options are valid
                if (plugins.requiresValidation(profile.getPluginId())) {
                    ValidationNotes notes = plugins.validatePlugin(profile.getPluginId(), profile.getProperties(), profile.getOptions());
                    if (!notes.getValidationEntries().isEmpty()) {
                        throw new ValidationException(ValidationExceptionType.ConfigException, notes);
                    }
                }

                return profiles.updateProfile(profile);

            }
        });
    }

    // ========================================================================
    
    // Profile operations -----------------------------------------------------
    @Override
    public AuthData addPluginAuthData(final AuthData authData) {
        return conn.txNew(new Callable<AuthData>() {
            @Override public AuthData call() {

                if (authData.getUser() == null) {
                    throw new IllegalArgumentException("User must not be null");
                }

                if (!plugins.requiresAuthorization(authData.getPluginId())) {
                    throw new PluginException(authData.getPluginId(), "AuthData is not required for this plugin");
                }

                // The following statement calls the authorize method of the plugin authorizable
                // It checks if the authentication data is required and valid
                plugins.authorizePlugin(authData);
                return profiles.addAuthData(authData);

            }
        });

    }

    @Override
    public AuthData getPluginAuthData(final Long authDataId) {
        return conn.txNewReadOnly(new Callable<AuthData>() {
            @Override public AuthData call() {

                return profiles.getAuthData(authDataId);

            }
        });
    }

    @Override
    public List<AuthData> listPluginAuthData(final Long userId) {
        return conn.txNewReadOnly(new Callable<List<AuthData>>() {
            @Override public List<AuthData> call() {

                return profiles.getAuthDataOf(userId);

            }
        });
    }

    @Override
    public AuthData updatePluginAuthData(final AuthData authData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deletePluginAuthData(final Long authDataId) {
        conn.txNew(new Runnable() {
            @Override public void run() {

                profiles.deleteAuthData(authDataId);

            }
        });

    }
    
    // ========================================================================

    // BackupJob operations ---------------------------------------------------
    
    @Override
    public BackupJob createBackupJob(BackupJob backupJob) {
        try {
            validateBackupJob(backupJob);
            BackupJob job = jobManager.createBackupJob(backupJob);
            return job;
        } finally {
            //            conn.rollback();
        }
    }

    @Override
    public BackupJob getBackupJob(final Long jobId) {
        return conn.txNewReadOnly(new Callable<BackupJob>() {
            @Override public BackupJob call() {

                return backupJobs.getExistingJob(jobId);

            }
        });
    }

    @Override
    public BackupJob updateBackupJob(final Long userId, final BackupJob backupJob) {
        if (backupJob.getId() == null) {
            throw new IllegalArgumentException("JobId must not be null!");
        }

        BackupJob job = conn.txNew(new Callable<BackupJob>() {
            @Override public BackupJob call() {

                BackupJob persistentJob = backupJobs.getExistingUserJob(backupJob.getId(), userId);
                backupJobs.updateJob(persistentJob, backupJob);
                return persistentJob;

            }
        });

        return job;
    }

    @Override
    public List<BackupJob> getBackupJobs(final Long userId) {
        return conn.txNewReadOnly(new Callable<List<BackupJob>>() {
            @Override public List<BackupJob> call() {

                registration.getUserByUserId(userId, true);
                return backupJobs.getBackupJobsOf(userId);

            }
        });
    }

    @Override
    public void deleteBackupJob(final Long userId, final Long jobId) {
        conn.txNew(new Runnable() {
            @Override public void run() {

                registration.getUserByUserId(userId, true);
                backupJobs.deleteJob(userId, jobId);

            }
        });
    }

    @Override
    public ProtocolDetails getProtocolDetails(Long userId, String fileId) {
        return search.getProtocolDetails(userId, fileId);
    }

    @Override
    public ProtocolOverview getProtocolOverview(final Long userId, final String duration) {
        return conn.txNewReadOnly(new Callable<ProtocolOverview>() {
            @Override public ProtocolOverview call() {

                BackMeUpUser user = registration.getUserByUserId(userId, true);

                Date to = new Date();
                Date from = duration.equals("month") ? new Date(to.getTime() - DelayTimes.DELAY_MONTHLY) :
                    new Date(to.getTime() - DelayTimes.DELAY_WEEKLY);

                return backupJobs.getProtocolOverview(user, from, to);

            }
        });
    }

    @Override
    public void updateJobProtocol(final Long userId, final Long jobId, final JobProtocolDTO jobProtocol) {
        conn.txNew(new Runnable() {
            @Override public void run() {

                BackMeUpUser user = registration.getUserByUserId(userId, true);
                BackupJob job = backupJobs.getExistingUserJob(jobId, userId);
                backupJobs.createJobProtocol(user, job, jobProtocol);

            }
        });
    }

    @Override
    public void deleteJobProtocols(final Long userId) {
        conn.txNew(new Runnable() {
            @Override public void run() {

                registration.getUserByUserId(userId, true);
                backupJobs.deleteProtocolsOf(userId);

            }
        });
    }
    
    private void validateBackupJob(final BackupJob backupJob) {
        conn.txNewReadOnly(new Runnable() {
            @Override public void run() {
                ValidationNotes notes = new ValidationNotes();
                try {
                    notes.addAll(validateProfile(backupJob.getSourceProfile()));
                    notes.addAll(validateProfile(backupJob.getSinkProfile()));

                    for(Profile actionProfile : backupJob.getActionProfiles()) {
                        notes.addAll(validateProfile(actionProfile));
                    }

                } catch (BackMeUpException bme) {
                    notes.addValidationEntry(ValidationExceptionType.Error, bme);
                }
                
                if (!notes.getValidationEntries().isEmpty()) {
                    throw new ValidationException(ValidationExceptionType.ConfigException, notes);
                }
            }
        });
    }
    
    // ========================================================================

    // search operations ------------------------------------------------------

    @Override
    public SearchResponse queryBackup(final Long userId, final String query, final String source, final String type, final String job) {
        return conn.txNewReadOnly(new Callable<SearchResponse>() {
            @Override public SearchResponse call() {

                BackMeUpUser user = registration.getUserByUserId(userId, true);
                return search.runSearch(user, query, source, type, job);

            }
        });
    }
    
    // ========================================================================

}
