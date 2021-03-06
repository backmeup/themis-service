package org.backmeup.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * The FriendUser class represents a friend as used within a friendslist. It contains a unique id, an owner which is the
 * related BMU user who 'owns' this friend, a name, description and email address which will be used to lookup the BMU
 * user id if the friend is existant on the platform
 * 
 */
@Entity
public class FriendlistUser {

    public enum FriendListType {
        SHARING, //user for sharing friends list
        HERITAGE; //user for heritage friends list for use case 'vererben'
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long entityId;

    @Column(unique = false, nullable = false)
    private Long ownerId; //user that 'owns' this friend

    @Column(unique = false, nullable = false)
    private String name;

    private String description;

    @Column(unique = false, nullable = false)
    private String email;

    @Column(unique = false, nullable = false)
    private FriendListType friendListType;

    @Transient
    private Long friendsBmuUserId; //the user id if the friend is on bmu. Not persisted, lookup via email if bmu user

    public FriendlistUser() {

    }

    public FriendlistUser(Long ownerId, String name, String description, String email) {
        this(ownerId, name, description, email, null, null);
    }

    public FriendlistUser(Long ownerId, String name, String description, String email, FriendListType friendListType) {
        this(ownerId, name, description, email, null, friendListType);
    }

    public FriendlistUser(Long ownerId, String name, String description, String email, Long friendsBmuUserId,
            FriendListType friendListType) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.email = email;
        this.friendsBmuUserId = friendsBmuUserId;
        if (friendListType == null) {
            this.friendListType = FriendListType.SHARING;
        } else {
            this.friendListType = friendListType;
        }
    }

    public Long getEntityId() {
        return this.entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "entityId: '" + this.entityId + "', name: '" + this.name + "', description: '" + this.description
                + "', email: '" + this.email + "', ownerId: '" + this.ownerId + "', friendsBMUUserId: '"
                + this.friendsBmuUserId + "', friendListType: '" + this.friendListType + "'";
    }

    public Long getFriendsBmuUserId() {
        return this.friendsBmuUserId;
    }

    public void setFriendsBmuUserId(Long friendsBmuUserId) {
        this.friendsBmuUserId = friendsBmuUserId;
    }

    public FriendListType getFriendListType() {
        return this.friendListType;
    }

    public void setFriendListType(FriendListType friendListType) {
        this.friendListType = friendListType;
    }

}
