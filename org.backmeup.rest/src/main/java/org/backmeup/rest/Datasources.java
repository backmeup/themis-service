package org.backmeup.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;

import org.backmeup.model.AuthRequest;
import org.backmeup.model.Profile;
import org.backmeup.model.spi.SourceSinkDescribable;
import org.backmeup.rest.data.DatasourceContainer;
import org.backmeup.rest.data.DatasourceContainer.Datasource;
import org.backmeup.rest.data.DatasourceOptionContainer;
import org.backmeup.rest.data.DatasourceProfilesContainer;
import org.backmeup.rest.data.PreAuthContainer;
import org.backmeup.rest.data.ValidationNotesContainer;

/**
 * All datasource specific operation will be handled within this class.
 * 
 * @author fschoeppl
 *
 */
@Path("/datasources")
public class Datasources extends Base {
	
	@GET
	@Produces("application/json")
	public DatasourceContainer getDatasources() {
		List<Datasource> l = new ArrayList<Datasource>();
		List<SourceSinkDescribable> descs = getLogic().getDatasources();
		for (SourceSinkDescribable d : descs) {
			l.add(new Datasource(d.getId(), d.getTitle(), d.getImageURL()));
		}
		return new DatasourceContainer(l);
	}
	
	@GET
  @Path("/{username}/validate/{profileId}")
  @Produces("application/json")
  public ValidationNotesContainer validateProfiles(
      @PathParam("username") String username, 
      @PathParam("profileId") String profileId) {
    return new ValidationNotesContainer(getLogic().validateProfile(username,
        Long.parseLong(profileId)));
  }

	@GET
	@Path("/{username}/profiles")
	@Produces("application/json")
	public DatasourceProfilesContainer getDatasourceProfiles(
			@PathParam("username") String username) {
		List<Profile> profiles = getLogic().getDatasourceProfiles(username);
		return new DatasourceProfilesContainer(profiles);
	}

	@DELETE
	@Path("/{username}/profiles/{profileId}")
	public void deleteProfile(@PathParam("username") String username,
			@PathParam("profileId") Long profileId) {
		getLogic().deleteProfile(username, profileId);
	}
	
	@PUT
	@Path("/{username}/profiles/{profileId}")
	@Produces("application/json")
	public void updateProfile(@PathParam("username") String username, @PathParam("profileId") Long profileId, @FormParam("sourceOptions") List<String> sourceOptions) {
		getLogic().changeProfile(profileId, sourceOptions);		
	}

	@POST
	// @Path("/{datasourceId}/options")
	@Path("/{username}/profiles/{profileId}/options")
	@Produces("application/json")
	public DatasourceOptionContainer getDatasourceOptions(
			@PathParam("username") String username,
			@PathParam("profileId") Long profileId,
			@FormParam("keyRing") String keyRingPassword) {
		return new DatasourceOptionContainer(getLogic().getDatasourceOptions(username, profileId, keyRingPassword));
	}
	
	/*
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadPlugin(@FormDataParam("file") InputStream data, @FormDataParam("file") FormDataContentDisposition fileDetail ) throws IOException {
		getLogic().uploadDatasourcePlugin(fileDetail.getFileName(), data);
		return Response.status(200).build();
	}*/
	
	@DELETE
	@Path("/{datasourceId}")
	public void deletePlugin(@PathParam("datasourceId") String source) {
		getLogic().deleteDatasourcePlugin(source);
	}
	
	@POST
	@Path("/{username}/{datasourceId}/auth")
	@Produces("application/json")
	public PreAuthContainer preAuthenticate(
			@PathParam("datasourceId") String datasourceId,
			@PathParam("username") String username,
			@FormParam("profileName") String profileName,
			@FormParam("keyRing") String keyRing) {
		AuthRequest ar = getLogic().preAuth(username,
				datasourceId, profileName, keyRing);
		return new PreAuthContainer(Long.toString(ar.getProfile()
				.getProfileId()), ar.getRedirectURL() == null ? "Input"
				: "OAuth", ar.getRequiredInputs(), ar.getTypeMapping(),
				ar.getRedirectURL(), true);
	}

	@POST
	@Path("/{username}/{profileId}/auth/post")
	@Produces("application/json")
	public void postAuthenticate(@PathParam("profileId") Long profileId,
			@PathParam("username") String username,
			@FormParam("keyRing") String keyRing,
			MultivaluedMap<String, String> formParams) {
		Properties p = new Properties();
		for (Entry<String, List<String>> o : formParams.entrySet()) {
			String paramName = o.getKey();
			if (!"profileId".equals(paramName) && !"keyRing".equals(paramName)) {
				p.setProperty(paramName, o.getValue().get(0));
			}
		}

		getLogic().postAuth(profileId, p, keyRing);
	}
}
