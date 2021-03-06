package org.backmeup.rest.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import org.backmeup.model.BackMeUpUser;
import org.backmeup.rest.auth.BackmeupPrincipal;

public class SecureBase extends Base {

    @Context
    private SecurityContext securityContext;

    protected void canOnlyWorkWithMyData(Long userId) {
        BackmeupPrincipal principal = ((BackmeupPrincipal) this.securityContext.getUserPrincipal());
        BackMeUpUser activeUser = principal.getEntity(BackMeUpUser.class);
        if (!activeUser.getUserId().equals(userId)) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

}
