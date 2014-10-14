package org.backmeup.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.backmeup.model.spi.PluginDescribable.PluginType;

/**
 * 
 * The Profile class represents a configuration for a certain plugin
 * (datasource, -sink or action).
 * 
 * The class has been annotated with JPA specific annotations.
 * 
 * @author fschoeppl
 * 
 */
@Entity
public class Profile {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER, optional = false)
	private BackMeUpUser user;

	private String name;

	private String description;

	@Temporal(TemporalType.TIMESTAMP)
	private Date created;

	@Temporal(TemporalType.TIMESTAMP)
	private Date modified;

	// The username that has been used for a certain profile, e.g. the dropbox
	// username or facebook username
	private String identification;

	@Enumerated(EnumType.STRING)
	private PluginType pluginType;

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER, optional = false)
	private AuthData authData;

	@ElementCollection
	private Map<String, String> properties;

	@ElementCollection
	private List<String> options;

	public Profile() {

	}

	public Profile(BackMeUpUser user, String profileName, String desc,
			PluginType source) {
		this(null, user, profileName, desc, source);
	}

	public Profile(Long id, BackMeUpUser user, String profileName, String desc,
			PluginType pluginType) {
		this.id = id;
		this.user = user;
		this.name = profileName;
		this.pluginType = pluginType;
		this.description = desc;
		this.created = new Date();
		this.modified = this.created;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long profileId) {
		this.id = profileId;
		this.modified = new Date();
	}

	public BackMeUpUser getUser() {
		return user;
	}

	public void setUser(BackMeUpUser user) {
		this.user = user;
		this.modified = new Date();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.modified = new Date();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String desc) {
		this.description = desc;
		this.modified = new Date();
	}

	public PluginType getType() {
		return pluginType;
	}

	public void setType(PluginType pluginType) {
		this.modified = new Date();
		this.pluginType = pluginType;
	}

	public Date getCreated() {
		return created;
	}

	public Date getModified() {
		return modified;
	}

	public String getIdentification() {
		return identification;
	}

	public void setIdentification(String identification) {
		this.identification = identification;
	}

	public AuthData getAuthData() {
		return authData;
	}

	public void setAuthData(AuthData authData) {
		this.authData = authData;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
	public void addProperty(String key, String value) {
		if(this.properties == null) {
			this.properties = new HashMap<String, String>();
		}
		this.properties.put(key, value);
	}

	public List<String> getOptions() {
		return options;
	}

	public void setOptions(List<String> options) {
		this.options = options;
	}
	
	public void addOption(String option) {
		if(this.options == null) {
			this.options = new ArrayList<>();
		}
		this.options.add(option);
	}
}
