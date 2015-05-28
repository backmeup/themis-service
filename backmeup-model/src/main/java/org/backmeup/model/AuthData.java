package org.backmeup.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.backmeup.model.exceptions.BackMeUpException;

/**
 * 
 * The AuthData class represents authentication information for a profile. The
 * plugin of the respective profile specifies how this auth data has to be
 * structured and what it should contain.
 * 
 * The class has been annotated with JPA specific annotations.
 */
@Entity
public class AuthData {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private String pluginId;
	
    // The username that has been used for a certain profile, 
    // e.g. the dropbox username or facebook username
    private String identification;
	
	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
	private BackMeUpUser user;

	@Temporal(TemporalType.TIMESTAMP)
	private Date created;

	@Temporal(TemporalType.TIMESTAMP)
	private Date modified;

	@ElementCollection(fetch = FetchType.EAGER)
	@MapKeyColumn(name = "authdata_key")
	@Column(name = "authdata_value", columnDefinition="text")
	@CollectionTable(name = "AuthDataProperties", joinColumns = @JoinColumn(name = "id"))
	private Map<String, String> properties;

	public AuthData() {
		this.created = new Date();
		this.modified = this.created;
	}

	public AuthData(String name, String pluginId, BackMeUpUser user) {
		this(name, pluginId, user, new HashMap<String, String>());
	}

	public AuthData(String name, String pluginId, BackMeUpUser user, Map<String, String> properties) {
		super();
		this.name = name;
		this.pluginId = pluginId;
		this.user = user;
		this.properties = properties;
		this.created = new Date();
		this.modified = this.created;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
		this.modified = new Date();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.modified = new Date();
	}

	public String getPluginId() {
		return pluginId;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
		this.modified = new Date();
	}

	public BackMeUpUser getUser() {
		return user;
	}

	public void setUser(BackMeUpUser user) {
		this.user = user;
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
	
	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

    public void addProperty(String key, String value) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(key, value);
    }
    
    public String getPropertiesAsEncodedString() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(properties);
            so.flush();
            return Base64.getEncoder().encodeToString(bo.toByteArray());
        } catch (Exception e) {
            throw new BackMeUpException("Cannot serialize auth data properties", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void setPropertiesFromEncodedString(String properpies) {
        try {
            byte b[] = Base64.getDecoder().decode(properpies);
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            this.properties = (HashMap<String, String>) si.readObject();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
