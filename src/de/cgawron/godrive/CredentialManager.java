package de.cgawron.godrive;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.extensions.java7.auth.oauth2.FileCredentialStoreJava7;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

/**
 * Credential manager to get, save, delete user credentials.
 *
 * @author jbd@google.com (Burcu Dogan)
 */
public class CredentialManager {
	static private Logger log = Logger.getLogger(CredentialManager.class
			.getName());
	/**
	 * Client secrets object.
	 */
	private GoogleClientSecrets clientSecrets;

	/**
	 * Transport layer for OAuth2 client.
	 */
	private HttpTransport transport;

	/**
	 * JSON factory for OAuth2 client.
	 */
	private JsonFactory jsonFactory;

	/**
	 * Scopes for which to request access from the user.
	 */
	public static final List<String> SCOPES = Arrays.asList(
			// Required to access and manipulate files.
			"https://www.googleapis.com/auth/drive.file",
			"https://www.googleapis.com/auth/drive.install",
			// Required to identify the user in our data store.
			"https://www.googleapis.com/auth/userinfo.email",
			"https://www.googleapis.com/auth/userinfo.profile");

	/**
	 * Credential store to get, save, delete user credentials.
	 */
	private static CredentialStore credentialStore;

	/**
	 * Credential Manager constructor.
	 * 
	 * @param clientSecrets
	 *            App client secrets to be used during OAuth2 exchanges.
	 * @param transport
	 *            Transportation layer for OAuth2 client.
	 * @param factory
	 *            JSON factory for OAuth2 client.
	 * @throws IOException
	 */
	public CredentialManager(GoogleClientSecrets clientSecrets,
			HttpTransport transport, JsonFactory factory) throws IOException {
		this.clientSecrets = clientSecrets;
		this.transport = transport;
		this.jsonFactory = factory;
		CredentialManager.credentialStore = new FileCredentialStoreJava7(
				new File("/tmp/credentials"), jsonFactory);
	}

	/**
	 * Builds an empty credential object.
	 * 
	 * @return An empty credential object.
	 */
	public Credential buildEmpty() {
		return new GoogleCredential.Builder()
				.setClientSecrets(this.clientSecrets).setTransport(transport)
				.setJsonFactory(jsonFactory).build();
	}

	/**
	 * Returns credentials of the given user, returns null if there are none.
	 * 
	 * @param userId
	 *            The id of the user.
	 * @return A credential object or null.
	 * @throws IOException
	 */
	public Credential get(String userId) throws IOException {
		Credential credential = buildEmpty();
		if (credentialStore.load(userId, credential)) {
			return credential;
		}
		return null;
	}

	/**
	 * Saves credentials of the given user.
	 * 
	 * @param userId
	 *            The id of the user.
	 * @param credential
	 *            A credential object to save.
	 * @throws IOException
	 */
	public void save(String userId, Credential credential) throws IOException {
		credentialStore.store(userId, credential);
	}

	/**
	 * Deletes credentials of the given user.
	 * 
	 * @param userId
	 *            The id of the user.
	 * @throws IOException
	 */
	public void delete(String userId) throws IOException {
		credentialStore.delete(userId, get(userId));
	}

	/**
	 * Generates a consent page url.
	 * 
	 * @return A consent page url string for user redirection.
	 */
	public String getAuthorizationUrl() {
		GoogleAuthorizationCodeRequestUrl urlBuilder = new GoogleAuthorizationCodeRequestUrl(
				clientSecrets.getWeb().getClientId(), clientSecrets.getWeb()
						.getRedirectUris().get(0), SCOPES).setAccessType(
				"offline").setApprovalPrompt("force");
		return urlBuilder.build();
	}

	/**
	 * Retrieves a new access token by exchanging the given code with OAuth2
	 * end-points.
	 * 
	 * @param code
	 *            Exchange code.
	 * @return A credential object.
	 */
	public Credential retrieve(String code) {
		try {
			GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
					transport, jsonFactory, clientSecrets.getWeb()
							.getClientId(), clientSecrets.getWeb()
							.getClientSecret(), code, clientSecrets.getWeb()
							.getRedirectUris().get(0)).execute();
			return buildEmpty().setAccessToken(response.getAccessToken());
		} catch (IOException e) {
			new RuntimeException(
					"An unknown problem occured while retrieving token", e);
		}
		return null;
	}

	public void setRedirectUri(String redirectUri) {
		log.info("Setting redirectUri to " + redirectUri);
		List<String> redirectUris = new ArrayList<String>();
		redirectUris.add(redirectUri);
		clientSecrets.getDetails().setRedirectUris(redirectUris);
	}
	
	public void setRedirectUri(HttpServletRequest req) {
		StringBuffer uri = new StringBuffer();
		uri.append(req.getScheme()).append("://")
				.append(req.getServerName());
		if (req.getServerPort() != 80) {
			uri.append(":").append(req.getServerPort());
		}
		uri.append(req.getContextPath());
		setRedirectUri(uri.toString());
	}
}
