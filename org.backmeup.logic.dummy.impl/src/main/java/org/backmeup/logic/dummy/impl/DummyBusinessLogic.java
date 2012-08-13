package org.backmeup.logic.dummy.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.backmeup.logic.BusinessLogic;
import org.backmeup.model.ActionProfile;
import org.backmeup.model.AuthRequest;
import org.backmeup.model.BackupJob;
import org.backmeup.model.FileItem;
import org.backmeup.model.Profile;
import org.backmeup.model.ProfileOptions;
import org.backmeup.model.ProtocolDetails;
import org.backmeup.model.ProtocolDetails.FileInfo;
import org.backmeup.model.ProtocolDetails.Sink;
import org.backmeup.model.ProtocolOverview;
import org.backmeup.model.ProtocolOverview.Entry;
import org.backmeup.model.SearchResponse;
import org.backmeup.model.SearchResponse.CountedEntry;
import org.backmeup.model.SearchResponse.SearchEntry;
import org.backmeup.model.Status;
import org.backmeup.model.User;
import org.backmeup.model.ValidationNotes;
import org.backmeup.model.exceptions.AlreadyRegisteredException;
import org.backmeup.model.exceptions.InvalidCredentialsException;
import org.backmeup.model.exceptions.PluginException;
import org.backmeup.model.exceptions.UnknownUserException;
import org.backmeup.model.exceptions.ValidationException;
import org.backmeup.model.spi.ActionDescribable;
import org.backmeup.model.spi.SourceSinkDescribable;
import org.backmeup.model.spi.SourceSinkDescribable.Type;
import org.backmeup.model.spi.ValidationExceptionType;
import org.backmeup.plugin.api.Metadata;

/**
 * The dummy businness logic stores all data within Lists and Maps (in-memory).
 * It realizes all operations and can be used as mock up to test the rest layer
 * or create a rest client.
 * 
 * @author fschoeppl
 * 
 */
@ApplicationScoped
public class DummyBusinessLogic implements BusinessLogic {
  Long maxId = 3l;

  private List<User> knownUsers;
  private List<SourceSinkDescribable> sources;
  private List<SourceSinkDescribable> sinks;
  private List<Profile> profiles;
  private List<BackupJob> jobs;
  private List<Status> status;
  private Map<String, ActionDescribable> actions;
  private Map<Long, SearchResponse> searches;
  private Map<User, String> passwords;
  
  public DummyBusinessLogic() {
    User u1 = new User(0l, "Sepp", "sepp@mail.at");
    User u2 = new User(1l, "Marion", "marion@mail.at");
    User u3 = new User(2l, "Phil", "em");
    knownUsers = new ArrayList<User>();
    knownUsers.add(u1);
    knownUsers.add(u2);
    knownUsers.add(u3);
    passwords = new HashMap<User, String>();
    passwords.put(u1, "pw");
    passwords.put(u2, "1234");
    passwords.put(u3, "p1");

    sources = new ArrayList<SourceSinkDescribable>();
    sources.add(new SourceSinkDescribable() {

      public String getImageURL() {
        return "http://skydrive.image.png";
      }

      public String getId() {
        return "org.backmeup.skydrive";
      }

      public String getTitle() {
        return "SkyDrive";
      }

      public String getDescription() {
        return "Information";
      }

      public Type getType() {
        return Type.Source;
      }

      @Override
      public Properties getMetadata(Properties accessData) {
        Properties props = new Properties();
        props.setProperty(org.backmeup.plugin.api.Metadata.BACKUP_FREQUENCY,
            "daily");
        return props;
      }
    });

    sources.add(new SourceSinkDescribable() {

      public String getImageURL() {
        return "http://dropbox.image.png";
      }

      public String getId() {
        return "org.backmeup.dropbox";
      }

      public String getTitle() {
        return "Dropbox";
      }

      public String getDescription() {
        return "Information";
      }

      public Type getType() {
        return Type.Source;
      }

      @Override
      public Properties getMetadata(Properties accessData) {
        Properties props = new Properties();
        props.setProperty(org.backmeup.plugin.api.Metadata.BACKUP_FREQUENCY,
            "weekly");
        return props;
      }
    });

    sinks = new ArrayList<SourceSinkDescribable>();
    sinks.add(new SourceSinkDescribable() {

      public String getImageURL() {
        return "http://wuala.image.png";
      }

      public String getId() {
        return "org.backmeup.wuala";
      }

      public String getTitle() {
        return "Wuala";
      }

      public String getDescription() {
        return "Information";
      }

      public Type getType() {
        return Type.Sink;
      }

      @Override
      public Properties getMetadata(Properties accessData) {
        Properties props = new Properties();
        props.setProperty(org.backmeup.plugin.api.Metadata.BACKUP_FREQUENCY,
            "daily");
        props.setProperty(org.backmeup.plugin.api.Metadata.FILE_SIZE_LIMIT,
            "100");
        props.setProperty(org.backmeup.plugin.api.Metadata.QUOTA, "500");
        props.setProperty(org.backmeup.plugin.api.Metadata.QUOTA_LIMIT, "2000");
        return props;
      }
    });

    sinks.add(new SourceSinkDescribable() {
      public String getImageURL() {
        return "http://dvd.image.png";
      }

      public String getId() {
        return "org.backmeup.dvd";
      }

      public String getTitle() {
        return "DVD";
      }

      public String getDescription() {
        return "Information";
      }

      public Type getType() {
        return Type.Sink;
      }

      @Override
      public Properties getMetadata(Properties accessData) {
        Properties props = new Properties();
        props.setProperty(org.backmeup.plugin.api.Metadata.BACKUP_FREQUENCY,
            "daily");
        props.setProperty(org.backmeup.plugin.api.Metadata.FILE_SIZE_LIMIT,
            "700");
        props.setProperty(org.backmeup.plugin.api.Metadata.QUOTA, "500");
        props.setProperty(org.backmeup.plugin.api.Metadata.QUOTA_LIMIT, "700");
        return props;
      }
    });

    profiles = new ArrayList<Profile>();
    profiles.add(new Profile(500l, u1, "Dropbox-Source",
        "org.backmeup.dropbox", Type.Source));
    profiles.add(new Profile(501l, u1, "Wuala-Sink", "org.backmeup.wuala",
        Type.Sink));

    profiles.add(new Profile(502l, u3, "Dropbox-Source",
        "org.backmeup.dropbox", Type.Source));
    profiles.add(new Profile(503l, u3, "Wuala-Sink", "org.backmeup.wuala",
        Type.Sink));
    jobs = new ArrayList<BackupJob>();

    actions = new HashMap<String, ActionDescribable>();
    actions.put("org.backmeup.rsa", new ActionDescribable() {
      public String getTitle() {
        return "Verschluesselung";
      }

      public String getId() {
        return "org.backmeup.rsa";
      }

      public String getDescription() {
        return "Verschluesselt Ihre Daten mit RSA";
      }

      @Override
      public Properties getMetadata(Properties accessData) {
        Properties props = new Properties();
        return props;
      }
    });

    Set<ActionProfile> reqActions = new HashSet<ActionProfile>();
    reqActions.add(new ActionProfile("org.backmeup.rsa" ));
    Set<ProfileOptions> popts = new HashSet<ProfileOptions>();
    popts.add(new ProfileOptions(findProfile(500), null));
    BackupJob aJob = new BackupJob(u1, popts, findProfile(501),
        reqActions, "* * * * *");   
    aJob.setId(maxId++);
    jobs.add(aJob);
    status = new ArrayList<Status>();
    status.add(new Status(aJob, "Der Backup-Job wurde gestartet", "INFO",
        new Date(100)));
    status.add(new Status(aJob, "Der Backup-Job wurde unterbrochen", "WARN",
        new Date(500)));
    status.add(new Status(aJob, "Der Backup-Job wurde erfolgreich beendet",
        "INFO", new Date(1000)));

    Set<ProfileOptions> popts2 = new HashSet<ProfileOptions>();
    popts2.add(new ProfileOptions(findProfile(502), null));
    BackupJob bJob = new BackupJob(u3, popts2, findProfile(502),
        reqActions, "* * * * *");
    bJob.setId(maxId++);
    jobs.add(bJob);
    status.add(new Status(bJob, "Ein Status", "INFO", new Date(100)));
    status.add(new Status(bJob, "Noch ein Status", "INFO", new Date(100)));
    Set<FileItem> files = new HashSet<FileItem>();
    files.add(new FileItem("http://thumbnails.at?url=1234", "sennenhund.jpg",
        new Date(100)));
    status.add(new Status(bJob, "Busy status", "STORE", new Date(100), "BUSY",
        files));
    searches = new HashMap<Long, SearchResponse>();
  }

  // user operations

  public User getUser(String username) {
    User u = findUser(username);
    if (u == null)
      throw new UnknownUserException(username);
    return u;
  }

  public User changeUser(String username, String oldPassword,
      String newPassword, String newKeyRing, String newEmail) {
    User u = findUser(username);
    /*if (!u.getPassword().equals(oldPassword))
      throw new InvalidCredentialsException();
    if (newPassword != null)
      u.setPassword(newPassword);
    if (newKeyRing != null)
      u.setKeyRing(newKeyRing);*/
    if (newEmail != null)
      u.setEmail(newEmail);
    return u;
  }

  public User deleteUser(String username) {
    User u = findUser(username);
    if (u == null)
      throw new UnknownUserException(username);
    knownUsers.remove(u);
    return u;
  }

  public User login(String username, String password) {
    for (User u : knownUsers) {
      if (u.getUsername().equals(username) && passwords.get(u).equals(password)) {
        return u;
      }
    }
    throw new InvalidCredentialsException();
  }

  public User register(String username, String password, String keyRing,
      String email) {
    if (username == null || password == null || keyRing == null
        || email == null)
      throw new IllegalArgumentException("One of the given parameters is null!");
    for (User u : knownUsers) {
      if (u.getUsername().equals(username))
        throw new AlreadyRegisteredException(u.getUsername());
    }
    User user = new User(maxId++, username, email);
    passwords.put(user, password);
    knownUsers.add(user);
    return user;
  }

  // datasource operations

  public Profile deleteProfile(String username, Long profile) {
    Profile p = findProfile(profile);
    if (p.getUser().getUsername().equals(username)) {
      profiles.remove(p);
      return p;
    }
    throw new IllegalArgumentException(String.format(
        "Unknown profile %d (user %s)", username, profile));
  }

  public List<SourceSinkDescribable> getDatasources() {
    return sources;
  }

  public List<Profile> getDatasourceProfiles(String username) {
    List<Profile> cloneDs = new ArrayList<Profile>();
    for (Profile p : profiles) {
      if (findSourceDescribable(p.getDesc()) != null
          && p.getUser().getUsername().equals(username)) {
        cloneDs.add(p);
      }
    }
    return cloneDs;
  }

  public List<String> getDatasourceOptions(String username, Long profileId,
      String keyRingPassword) {
    Profile p = findProfile(profileId);
    if (p == null)
      throw new IllegalArgumentException("Invalid profile " + profileId);

    SourceSinkDescribable ssd = findSourceSinkDescribable(p.getDesc());
    if (ssd == null)
      throw new IllegalArgumentException("No such plug-in for profile "
          + p.getProfileId());

    List<String> folders = new ArrayList<String>();
    if ("Dropbox".equals(ssd.getTitle())) {
      folders.add("/Ordner1");
      folders.add("/My Dropbox");
    } else {
      folders.add("/Ordner1");
      folders.add("/My Skydrive");
    }

    return folders;
  }

  public void changeProfile(Long profileId, List<String> sourceOptions) {
    Profile p = findProfile(profileId);
    if (p == null)
      throw new IllegalArgumentException("Invalid profile");
    // TODO: Profile must contain options, not the job
  }

  public void uploadDatasourcePlugin(String filename, InputStream data) {
    File f = new File("tmp");
    if (!f.exists())
      f.mkdir();
    System.out.println(f.getAbsolutePath());
    File name = new File(filename);
    try {
      FileOutputStream fos = new FileOutputStream(new File("tmp/"
          + name.getName()));

      byte[] buffer = new byte[5 * 1024 * 1024];
      int readBytes;
      while ((readBytes = data.read(buffer, 0, buffer.length)) >= 0) {
        fos.write(buffer, 0, readBytes);
      }
      fos.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void deleteDatasourcePlugin(String name) {
    File plugin = new File("tmp/" + new File(name).getName());
    if (plugin.exists()) {
      plugin.delete();
    }
  }

  // datasink operations

  public List<Profile> getDatasinkProfiles(String username) {
    List<Profile> cloneDs = new ArrayList<Profile>();
    for (Profile p : profiles) {
      if (findSinkDescribable(p.getDesc()) != null
          && p.getUser().getUsername().equals(username)) {
        cloneDs.add(p);
      }
    }
    return cloneDs;
  }

  public List<SourceSinkDescribable> getDatasinks() {
    return sinks;
  }

  public AuthRequest preAuth(String username, String uniqueDescIdentifier,
      String profileName, String keyRing) throws PluginException,
      InvalidCredentialsException {
    SourceSinkDescribable source = findSourceSinkDescribable(uniqueDescIdentifier);
    User u = findUser(username);

    if (source == null)
      throw new IllegalArgumentException("Unknown plugin "
          + uniqueDescIdentifier);
    if (u == null)
      throw new UnknownUserException(username);

    if (!passwords.get(u).equals(keyRing))
      throw new InvalidCredentialsException();

    Profile p = new Profile(maxId++, u, profileName, source.getId(),
        Type.Source);
    profiles.add(p);

    if ("Dropbox".equals(source.getTitle())) {
      String redirectURL = "https://www.dropbox.com/1/oauth/authorize";
      return new AuthRequest(null, null, redirectURL, p);
    }
    List<String> requiredInputs = new ArrayList<String>();
    requiredInputs.add("Username");
    requiredInputs.add("Password");
    Map<String, String> typeMapping = new HashMap<String, String>();
    typeMapping.put("Password", "Password");
    typeMapping.put("Username", "String");
    return new AuthRequest(requiredInputs, typeMapping, null, p);
  }

  public void postAuth(Long profileId, Properties props, String keyRing)
      throws PluginException, ValidationException, InvalidCredentialsException {
    Profile p = findProfile(profileId);
    if (p == null)
      throw new IllegalArgumentException("Unknown profile " + profileId);
    if (!passwords.get(p.getUser()).equals(keyRing))
      throw new InvalidCredentialsException();

    for (Object keyObj : props.keySet()) {
      String key = (String) keyObj;
      String value = props.getProperty(key);
      p.putEntry(key, value);
    }
  }

  public void uploadDatasinkPlugin(String filename, InputStream data) {

  }

  public void deleteDatasinkPlugin(String name) {

  }

  // action operations

  public List<ActionDescribable> getActions() {
    List<ActionDescribable> actions = new ArrayList<ActionDescribable>();
    for (ActionDescribable ac : this.actions.values()) {
      actions.add(ac);
    }
    return actions;
  }

  public List<String> getActionOptions(String actionId) {
    if ("org.backmeup.rsa".equals(actionId)) {
      List<String> results = new ArrayList<String>();
      results.add("512-Bit-Verschluesselung (sonst 256)");
      return results;
    }
    return null;
  }

  public void uploadActionPlugin(String filename, InputStream data) {

  }

  public void deleteActionPlugin(String name) {

  }

  // job operations

  public List<BackupJob> getJobs(String username) {
    List<BackupJob> jobs = new ArrayList<BackupJob>();
    for (BackupJob j : this.jobs) {
      if (j.getUser().getUsername().equals(username))
        jobs.add(j);
    }
    return jobs;
  }

  public BackupJob createBackupJob(String username, List<Long> sourceProfiles,
      Long sinkProfileId, Map<Long, String[]> sourceOptions,
      String[] requiredActions, String cronTime, String keyRing) {

    User user = findUser(username);
    if (user == null)
      throw new UnknownUserException(username);

    Set<ProfileOptions> sources = new HashSet<ProfileOptions>();

    for (long sId : sourceProfiles) {
      Profile sourceProfile = findProfile(sId);

      if (sourceProfile == null)
        throw new IllegalArgumentException("Source-profile not found " + sId);
      String[] opts = sourceOptions.get(sId);
      ProfileOptions po = new ProfileOptions(sourceProfile, opts);
      sources.add(po);
    }

    Profile sinkProfile = findProfile(sinkProfileId);
    if (sinkProfile == null)
      throw new IllegalArgumentException("Sink-profile not found "
          + sinkProfileId);

    if (cronTime == null)
      throw new IllegalArgumentException("Cron expression missing");

    BackupJob job = new BackupJob(user, sources, sinkProfile,
        findActions(requiredActions), cronTime);
    job.setId(maxId++);
    jobs.add(job);
    return job;
  }

  public void deleteJob(String username, Long jobId) {
    BackupJob j = null;
    for (BackupJob job : this.jobs) {
      if (job.getId() == jobId && job.getUser().getUsername().equals(username)) {
        j = job;
        break;
      }
    }
    if (j != null) {
      this.jobs.remove(j);
    }
  }

  public List<Status> getStatus(String username, Long jobId, Date fromDate,
      Date toDate) {
    List<Status> status = new ArrayList<Status>();
    for (Status s : this.status) {
      if (s.getJob().getUser().getUsername().equals(username)) {
        if (jobId == null || jobId == s.getJob().getId()) {
          boolean add = fromDate != null ? s.getTimeStamp().compareTo(fromDate) >= 0
              : true;
          add = add
              && (toDate != null ? s.getTimeStamp().compareTo(toDate) <= 0
                  : true);
          if (add) {
            status.add(s);
          }
        }
      }
    }
    return status;
  }

  public ProtocolDetails getProtocolDetails(String username, Long fileId) {
    ProtocolDetails pd = new ProtocolDetails();
    FileInfo fi = new FileInfo(1231L, "Facebook", new Date(13304123),
        "sennenhund.jpg", "image", "http://thumbnails.at?id=1231");
    List<Sink> sinks = new ArrayList<Sink>();
    sinks.add(new Sink("DVD per Post", new Date(13330403), "839482933"));
    sinks.add(new Sink("Dropbox", new Date(13330403),
        "Facebook/2012/Fotos/sennenhund.jpg"));

    List<FileInfo> similar = new ArrayList<FileInfo>();
    similar.add(new FileInfo(1234L, null, null, "sennenhund2.jpg", null,
        "http://thumbnails.at?id=1234"));
    pd.setFileInfo(fi);
    pd.setSinks(sinks);
    pd.setSimilar(similar);
    return pd;
  }

  public ProtocolOverview getProtocolOverview(String username, String duration) {
    ProtocolOverview po = new ProtocolOverview();
    po.setTotalCount("23456345");
    po.setTotalStored("2.2GB");
    List<Entry> storedAmount = new ArrayList<Entry>();
    storedAmount.add(new Entry("Facebook", 30));
    storedAmount.add(new Entry("Twitter", 10));
    storedAmount.add(new Entry("Moodle", 60));
    po.setStoredAmount(storedAmount);
    List<Entry> sinks = new ArrayList<Entry>();
    sinks.add(new Entry("Dropbox", 26));
    sinks.add(new Entry("DVD per Post", 96));
    po.setDatasinks(sinks);
    return po;
  }

  // search operations
  public long searchBackup(String username, String keyRingPassword, String query) {
    long id = maxId++;
    List<SearchEntry> responses = new ArrayList<SearchEntry>();
    responses.add(new SearchEntry(maxId++, new Date(), "image", "image1.png",
        "http://athumbnail.png"));
    responses.add(new SearchEntry(maxId++, new Date(), "image", "image2.png",
        "http://athumbnail.png"));
    responses.add(new SearchEntry(maxId++, new Date(), "image", "image3.png",
        "http://athumbnail.png"));
    searches.put(id, new SearchResponse(0, 100, responses));

    List<CountedEntry> bySource = new ArrayList<CountedEntry>();
    bySource.add(new CountedEntry("Facebook", 258));
    bySource.add(new CountedEntry("Flick", 54));

    List<CountedEntry> byType = new ArrayList<CountedEntry>();
    byType.add(new CountedEntry("Fotos", 32));
    byType.add(new CountedEntry("Word", 1));
    searches.put(id, new SearchResponse(0, 100, responses, bySource, byType));
    return id;
  }

  public SearchResponse queryBackup(String username, long searchId,
      String filterType, String filterValue) {
    SearchResponse sr = searches.get(searchId);
    if (sr == null)
      throw new IllegalArgumentException("Unknown searchId " + searchId);

    SearchResponse sr2 = new SearchResponse();
    if (filterType == null) {
      sr2.setByType(sr.getByType());
      sr2.setBySource(sr.getBySource());
    } else {
      List<SearchEntry> se = new ArrayList<SearchEntry>();
      for (SearchEntry s : sr.getFiles()) {
        if ("type".equals(filterType) && filterValue.equals(s.getType())) {
          se.add(s);
        } else
          se.add(s);
      }
      sr2.setFiles(se);
    }
    return sr2;
  }

  public void shutdown() {
  }

  // private misc/helper methods
  private SourceSinkDescribable findSourceSinkDescribable(String uniqueId) {
    SourceSinkDescribable sd = findSourceDescribable(uniqueId);
    if (sd == null)
      sd = findSinkDescribable(uniqueId);
    return sd;
  }

  private SourceSinkDescribable findSourceDescribable(String uniqueId) {
    for (SourceSinkDescribable desc : sources) {
      if (desc.getId().equals(uniqueId))
        return desc;
    }
    return null;
  }

  private SourceSinkDescribable findSinkDescribable(String uniqueId) {
    for (SourceSinkDescribable desc : sinks) {
      if (desc.getId().equals(uniqueId))
        return desc;
    }
    return null;
  }

  private User findUser(String username) {
    for (User known : knownUsers) {
      if (known.getUsername().equals(username)) {
        return known;
      }
    }
    return null;
  }

  private Profile findProfile(long profileId) {
    for (Profile p : profiles) {
      if (p.getProfileId() == profileId) {
        return p;
      }
    }
    return null;
  }

  private Set<ActionProfile> findActions(String[] actionIds) {
    Set<ActionProfile> actionDescs = new HashSet<ActionProfile>();
    for (String action : actionIds) {
      ActionDescribable itm = actions.get(action);
      if (itm != null) {
        actionDescs.add(new ActionProfile(itm.getId()));
      }
    }
    return actionDescs;
  }

  private BackupJob findJob(String username, Long jobId) {
    for (BackupJob job : jobs) {
      if (job.getUser().getUsername().equals(username) && job.getId() == jobId) {
        return job;
      }
    }
    return null;
  }

  @Override
  public Properties getMetadata(String username, Long profileId) {
    User u = findUser(username);
    if (u == null)
      throw new UnknownUserException(username);
    Profile p = findProfile(profileId);
    SourceSinkDescribable ssd = findSourceSinkDescribable(p.getDesc());
    if (ssd == null)
      throw new IllegalArgumentException("Unknown source/sink with id: "
          + p.getDesc());

    Properties props = new Properties();
    return ssd.getMetadata(props);
  }

  @Override
  public ValidationNotes validateBackupJob(String username, Long jobId) {
    BackupJob job = findJob(username, jobId);
    if (job == null) {
      throw new IllegalArgumentException("Unknown job with id: " + jobId);
    }

    ValidationNotes notes = new ValidationNotes();

    // plugin-level validation
    double requiredSpace = 0;
    for (ProfileOptions po : job.getSourceProfiles()) {
      // TODO: start validation of profile
      SourceSinkDescribable ssd = findSourceDescribable(po.getProfile().getDesc());      
      if (ssd == null) {
        notes.addValidationEntry(ValidationExceptionType.Error, String.format("No plug-in found with id %s", po.getProfile().getDesc()));
      }
      
      Properties meta = getMetadata(username, po.getProfile().getProfileId());
      String quota = meta.getProperty(Metadata.QUOTA);
      if (quota != null) {
        requiredSpace += Double.parseDouble(meta.getProperty(Metadata.QUOTA));
      } else {
        notes
            .addValidationEntry(
                ValidationExceptionType.Warning,
                String
                    .format(
                        "Cannot compute quota for profile '%s' and plugin '%s'. The required space for a backup could be more than the available space.",
                        po.getProfile().getProfileName(), po.getProfile()
                            .getDesc()));
      }
    }
    // TODO: Add required space for index and encryption

    requiredSpace *= 1.1;

    // TODO: validate sink profile
    Properties meta = getMetadata(username, job.getSinkProfile().getProfileId());
    String sinkQuota = meta.getProperty(Metadata.QUOTA);
    String sinkQuotaLimit = meta.getProperty(Metadata.QUOTA_LIMIT);
    if (sinkQuota != null && sinkQuotaLimit != null) {
      double freeSpace = Double.parseDouble(sinkQuotaLimit)
          - Double.parseDouble(sinkQuota);
      if (freeSpace < requiredSpace) {
        notes
            .addValidationEntry(
                ValidationExceptionType.NotEnoughSpaceException,
                String
                    .format(
                        "Not enough space for backup: Required space for backup was %d. Free space on service was %d. (Profile '%s' and plugin '%s')",
                        requiredSpace, freeSpace, job.getSinkProfile()
                            .getProfileName(), job.getSinkProfile().getDesc()));
      }
    } else {
      notes.addValidationEntry(ValidationExceptionType.Warning, String.format(
          "Cannot compute free space for profile '%s' and plugin '%s'", job
              .getSinkProfile().getProfileName(), job.getSinkProfile()
              .getDesc()));
    }
    return notes;
  }

  @Override
  public ValidationNotes validateProfile(String username, Long profileId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addProfileEntries(Long profileId, Properties entries) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setUserProperty(String username, String key, String value) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteUserProperty(String username, String key) {
    // TODO Auto-generated method stub
    
  }
}
