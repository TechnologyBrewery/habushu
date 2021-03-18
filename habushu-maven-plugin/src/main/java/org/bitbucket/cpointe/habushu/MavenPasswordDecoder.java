package org.bitbucket.cpointe.habushu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * A simple Maven password decoding tool adapted from open-source code.
 */
public class MavenPasswordDecoder {
	
	private MavenPasswordDecoder() {}
	
	/**
	 * The settings.xml file for the current Maven user.
	 */
	private static final File ORIGINAL_SETTINGS_FILE = new File(System.getProperty("user.home"), ".m2/settings.xml");
	
	/**
	 * The settings-security.xml file for the current Maven user.
	 */
	private static final File ORIGINAL_SETTINGS_SECURITY_FILE = new File(System.getProperty("user.home"), ".m2/settings-security.xml");
	
	private static String decodePassword(String encodedPassword, String key) throws PlexusCipherException {
		DefaultPlexusCipher cipher = new DefaultPlexusCipher();
		return cipher.decryptDecorated(encodedPassword, key);
	}

	private static String decodeMasterPassword(String encodedMasterPassword) throws PlexusCipherException {
		return decodePassword(encodedMasterPassword, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
	}

	private static SettingsSecurity readSettingsSecurity(File file) throws SecDispatcherException {
		return SecUtil.read(file.getAbsolutePath(), true);
	}

	private static Settings readSettings(File file) throws IOException, XmlPullParserException {
		SettingsXpp3Reader reader = new SettingsXpp3Reader();
		return reader.read(new FileInputStream(file));
	}

	public static String decryptPasswordForServer(String serverId)
			throws IOException, XmlPullParserException, SecDispatcherException, PlexusCipherException {

		Settings settings = readSettings(ORIGINAL_SETTINGS_FILE);
		SettingsSecurity settingsSecurity = readSettingsSecurity(ORIGINAL_SETTINGS_SECURITY_FILE);

		String encodedMasterPassword = settingsSecurity.getMaster();
		String plainTextMasterPassword = decodeMasterPassword(encodedMasterPassword);

		List<Server> servers = settings.getServers();

		for (Server server : servers) {
			if (serverId.equals(server.getId())) {
				String encodedServerPassword = server.getPassword();
				String plainTextServerPassword = decodePassword(encodedServerPassword, plainTextMasterPassword);
				
				return plainTextServerPassword;
			}
		}

		return null;
	}
}
