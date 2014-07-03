package org.backmeup.rest.resources.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.backmeup.model.BackMeUpUser;
import org.backmeup.model.Profile;
import org.backmeup.model.dto.PluginDTO;
import org.backmeup.model.dto.PluginDTO.PluginType;
import org.backmeup.model.dto.PluginProfileDTO;
import org.backmeup.model.dto.UserDTO;
import org.backmeup.model.spi.SourceSinkDescribable;
import org.backmeup.model.spi.SourceSinkDescribable.Type;
import org.backmeup.plugin.Plugin;
import org.backmeup.plugin.osgi.PluginImpl;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.junit.Test;

public class MappingTests {
	private static final String DOZER_CUSTOM_CONVERTERS = "dozer-custom-converters.xml";
	private static final String DOZER_USER_MAPPING = "dozer-user-mapping.xml";
	private static final String DOZER_PROFILE_MAPPING = "dozer-profile-mapping.xml";

	@Test
	public void testUserMapping() {
		Long userId = 1L;
		String username = "johndoe";
		String firstname = "John";
		String lastname = "Doe";
		String email = "johndoe@example.com";
		boolean activated = true;
		String verificationKey = "123ABC";
		String password = "john123!#";
		
		BackMeUpUser srcUser = new BackMeUpUser(userId, username, firstname, lastname, email, password);
		srcUser.setActivated(activated);
		srcUser.setVerificationKey(verificationKey);
		
		Mapper mapper = getMapper(DOZER_USER_MAPPING);
		
		UserDTO destUser = mapper.map(srcUser, UserDTO.class);
		
		assertEquals(srcUser.getUserId(), destUser.getUserId());
		assertEquals(srcUser.getUsername(), destUser.getUsername());
		assertEquals(srcUser.getFirstname(), destUser.getFirstname());
		assertEquals(srcUser.getLastname(), destUser.getLastname());
		assertEquals(srcUser.getEmail(), destUser.getEmail());
		assertEquals(srcUser.isActivated(), destUser.isActivated());
	}
	
	@Test
	public void testPluginMapping() {
		String pluginId = "org.backmeup.dropbox";
		
		Plugin plugin = setupPluginInfrastructure();
		SourceSinkDescribable  pluginModel = plugin.getSourceSinkById(pluginId);
		
		Mapper mapper = getMapper(DOZER_PROFILE_MAPPING);
		
		PluginDTO pluginDTO = mapper.map(pluginModel, PluginDTO.class);
		
		assertEquals(pluginModel.getId(), pluginDTO.getPluginId());
		assertEquals(pluginModel.getTitle(), pluginDTO.getTitle());
		assertEquals(pluginModel.getDescription(), pluginDTO.getDescription());
		// TODO metadata
		assertEquals(pluginModel.getImageURL(), pluginDTO.getImageURL());
		
		((PluginImpl) plugin).shutdown();
	}
	
	@Test
	public void testPluginProfileMapping() {
		String username = "johndoe";
		String firstname = "John";
		String lastname = "Doe";
		String email = "johndoe@example.com";
		String password = "john123!#";
		
		
		Long profileId = 1L;
		BackMeUpUser user = new BackMeUpUser(username, firstname, lastname, email, password);
		String profileName = "TestProfile";
		String description = "Description of test profile";
		String identification = "identification";
		Type profileTypeModel = Type.Source;
		PluginType profileTypeDTO = PluginType.source;
		
		Profile profile = new Profile(profileId, user, profileName, description, Type.Source);
		profile.setIdentification(identification);
		
		List<String> configList = new ArrayList<String>();
		configList.add(DOZER_CUSTOM_CONVERTERS);
		configList.add(DOZER_USER_MAPPING);
		configList.add(DOZER_PROFILE_MAPPING);
		
		Mapper mapper = getMapper(configList);
		
		PluginProfileDTO profileDTO = mapper.map(profile, PluginProfileDTO.class);
		
		assertEquals(profile.getProfileId().longValue(), profileDTO.getProfileId());
		assertEquals(profile.getProfileName(), profileDTO.getTitle());
		assertEquals(profile.getType(), profileTypeModel);
		assertEquals(profileDTO.getProfileType(), profileTypeDTO);
	}
	
	@SuppressWarnings("serial")
	private Mapper getMapper(final String mappingFile) {
		List<String> list = new ArrayList<String>() { 
			{add(mappingFile);}
		};
		return getMapper(list);
	}
	
	private Mapper getMapper(final List<String> mappingFiles) {
		return new DozerBeanMapper(mappingFiles);
	}
	
	private Plugin setupPluginInfrastructure() {
		Plugin plugin = new PluginImpl(
				"/data/backmeup-service/autodeploy",
				"/data/backmeup-service/osgi-tmp",
				"org.backmeup.plugin.spi org.backmeup.model org.backmeup.model.spi org.backmeup.plugin.api.connectors org.backmeup.plugin.api.storage com.google.gson org.backmeup.plugin.api");

		plugin.startup();
		((PluginImpl) plugin).waitForInitialStartup();

		try {
			Thread.sleep(2000);
		} catch (Exception e) {

		}
		return plugin;
	}
}
