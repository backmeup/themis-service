package org.backmeup.keyserver.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.backmeup.keyserver.client.AuthDataResult;
import org.backmeup.keyserver.client.AuthOAuth;
import org.backmeup.keyserver.client.AuthUsrPwd;
import org.backmeup.keyserver.client.Token;
import org.backmeup.keyserver.client.TokenRequest;
import org.backmeup.model.exceptions.BackMeUpException;

import com.google.gson.Gson;

@ApplicationScoped
public class Keyserver implements org.backmeup.keyserver.client.Keyserver {

  private static class Result {
    public HttpResponse response;
    public String content;
  }

  private enum ReqType {
    GET, DELETE, PUT, POST
  }

  @Inject
  @Named("keyserver.scheme")
  private String scheme;

  @Inject
  @Named("keyserver.host")
  private String host;

  @Inject
  @Named("keyserver.path")
  private String path;
  
  @Inject
  @Named("keyserver.keystore")
  private String keystore;
  
  @Inject
  @Named("keyserver.keystoreType")
  private String keystoreType;
  
  @Inject
  @Named("keyserver.keystorePwd")
  private String keystorePwd;
  
  @Inject
  @Named("keyserver.truststore")
  private String truststore;
  
  @Inject
  @Named("keyserver.truststoreType")
  private String truststoreType;
  
  @Inject
  @Named("keyserver.truststorePwd")
  private String truststorePwd;
  
  @Inject
  @Named("keyserver.allowAllHostnames")
  private boolean allowAllHostnames;

  public Keyserver() {

  }
  
  private SchemeRegistry schemeRegistry;
  
  private DefaultHttpClient createClient() {
    if (scheme.equals("http")) {
      return new DefaultHttpClient();
    } else if (schemeRegistry == null) {   
      try {
        KeyStore keystore = KeyStore.getInstance(keystoreType != null ? keystoreType : KeyStore.getDefaultType());
        InputStream keystoreInput = getClass().getClassLoader().getResourceAsStream(this.keystore);
        keystore.load(keystoreInput, keystorePwd != null ? keystorePwd.toCharArray() : null);
        keystoreInput.close();
        
        // load the truststore, leave it null to rely on cacerts distributed with the JVM
        KeyStore truststore = null;
        if (this.truststore != null) {
          truststore = KeyStore.getInstance(truststoreType != null ? truststoreType : KeyStore.getDefaultType());          
          InputStream truststoreInput = getClass().getClassLoader().getResourceAsStream(this.truststore);    
          truststore.load(truststoreInput, truststorePwd != null ? truststorePwd.toCharArray() : null);
          truststoreInput.close();
        }
        schemeRegistry = new SchemeRegistry();      
        SSLSocketFactory lSchemeSocketFactory = new SSLSocketFactory(SSLSocketFactory.TLS, keystore, keystorePwd, truststore, (SecureRandom)null, allowAllHostnames ? SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER : SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);        
        schemeRegistry.register(new Scheme("https", 443, lSchemeSocketFactory));
      } catch (Exception e) {
        throw new BackMeUpException(e);
      }
    }
    final HttpParams httpParams = new BasicHttpParams();
    return new DefaultHttpClient(new SingleClientConnManager(schemeRegistry), httpParams);
  }

  private Result execute(String path, ReqType type) {
    return execute(path, type, null);
  }

  private Result execute(String path, ReqType type, String jsonParams) {
    
    HttpClient client = createClient();

    try {
      URI registerUri = new URI(scheme, host, path, null);
      HttpUriRequest request;
      switch (type) {
      case PUT:
        request = new HttpPut(registerUri);
        break;
      case DELETE:
        request = new HttpDelete(registerUri);
        break;
      case GET:
        request = new HttpGet(registerUri);
        break;
      default:
        HttpPost post;
        request = post = new HttpPost(registerUri);
        if (jsonParams != null) {
          StringEntity entity = new StringEntity(jsonParams);
          BasicHeader header = new BasicHeader(HTTP.CONTENT_TYPE,
              "application/json");
          entity.setContentType(header);
          post.setEntity(entity);
        }
        break;
      }
      HttpResponse response = client.execute(request);
      Result r = new Result();
      r.response = response;
      if (response.getEntity() != null) {
        try {
          r.content = new Scanner(response.getEntity().getContent())
              .useDelimiter("\\A").next();
        } catch (NoSuchElementException nee) {
        }
      }
      return r;
    } catch (URISyntaxException e) {
      throw new BackMeUpException(e);
    } catch (ClientProtocolException e) {
      throw new BackMeUpException(e);
    } catch (IOException e) {
      throw new BackMeUpException(e);
    }
  }

  // User Operations
  @Override
  public void registerUser(Long userId, String password) {
    Result response = execute(path + "/users/" + userId + "/" + password + "/register",
        ReqType.POST);
    if (response.response.getStatusLine().getStatusCode() != 204) {
      throw new BackMeUpException("Error during user creation: error code was "
          + response.response.getStatusLine().getStatusCode() + "; message: "
          + response.response.getStatusLine().getReasonPhrase() + "; Data: " + response.content);
    }
  }

  @Override
  public void deleteUser(Long userId) {
    Result response = execute(path + "/users/" + userId, ReqType.DELETE);
    if (response.response.getStatusLine().getStatusCode() != 204) {
      throw new BackMeUpException("Failed to delete user " + userId + " code: "
          + response.response.getStatusLine().getStatusCode() + "; message: "
          + response.response.getStatusLine().getReasonPhrase());
    }
  }

  @Override
  public boolean isUserRegistered(Long userId) {
    Result response = execute(path + "/users/" + userId, ReqType.GET);
    return response.response.getStatusLine().getStatusCode() == 200;
  }
  
  @Override
  public boolean validateUser(Long userId, String password) {
	Result response = execute(path + "/users/" + userId + "/" + password + "/validate", ReqType.GET);
	if (response.response.getStatusLine().getStatusCode() != 204) {
		//TODO: Log response here!
		return false;
	}
  	return true;
  }
  
  // Service Operations
  @Override
  public void addService(Long serviceId) {
    Result response = execute(path + "/services/" + serviceId + "/register",
        ReqType.POST);
    if (response.response.getStatusLine().getStatusCode() != 204) {
      throw new BackMeUpException("Failed to add service " + serviceId
          + "; Data: " + response.content);
    }
  }

  @Override
  public boolean isServiceRegistered(Long serviceId) {
    Result response = execute(path + "/services/" + serviceId, ReqType.GET);
    return response.response.getStatusLine().getStatusCode() == 200;
  }

  @Override
  public void deleteService(Long serviceId) {
    Result response = execute(path + "/services/" + serviceId, ReqType.DELETE);
    if (response.response.getStatusLine().getStatusCode() != 204) {
      throw new BackMeUpException("Failed to delete service " + serviceId
          + "; Data: " + response.content);
    }
  }

  // Authentication Operations
  @Override
  public void addAuthInfo(Long userId, String userPwd, Long serviceId,
      Long authInfoId, String ai_username, String ai_password) {
    Gson g = new Gson();
    String json = g.toJson(new AuthUsrPwd(userId, userPwd, serviceId, authInfoId,
        ai_username, ai_password));
    Result response = execute(path + "/authinfos/add/userpwd", ReqType.POST,
        json);
    if (response.response.getStatusLine().getStatusCode() != 204) {
      throw new BackMeUpException("Failed to addAuthInfo! Data: " + response.content);
    }
  }

  @Override
  public void addAuthInfo(Long userId, String userPwd, Long serviceId,
      Long authInfoId, String ai_oauth) {
    Gson g = new Gson();
    String json = g.toJson(new AuthOAuth(userId, userPwd, serviceId,
        authInfoId, ai_oauth));
    Result response = execute(path + "/authinfos/add/oauth", ReqType.POST,
        json);
    if (response.response.getStatusLine().getStatusCode() != 204) {
      throw new BackMeUpException("Failed to addAuthInfo! Data: " + response.content);
    }
  }

  @Override
  public boolean isAuthInformationAvailable(Long authInfoId, Long userId,
      Long serviceId, String userPwd) {
    // TODO: Determine if query parameters will be encrypted with SSL
    Result r = execute(path + "/authinfos/" + authInfoId + "/" + userId + "/" + serviceId + "/" + userPwd, ReqType.GET);
    return r.response.getStatusLine().getStatusCode() == 200;
  }

  @Override
  public void deleteAuthInfo(Long authInfoId) {    
    Result r = execute(path + "/authinfos/" + authInfoId, ReqType.DELETE);
    if (r.response.getStatusLine().getStatusCode() != 204)
      throw new BackMeUpException("Failed to delete authinfo " + authInfoId + ". Data: " + r.content);
  }

  @Override
  public Token getToken(Long userId, String userPwd, Long[] services,
      Long[] authinfos, Long backupdate) {
    Gson g = new Gson();
    String json = g.toJson(new TokenRequest(userId, userPwd, services, authinfos, backupdate));
    Result r = execute(path + "/tokens/token", ReqType.POST, json);
    if (r.response.getStatusLine().getStatusCode() == 200) {
      return g.fromJson(r.content, Token.class);
    }
    throw new BackMeUpException("Failed to retrieve a token: " + r.content);
  }

  @Override
  public AuthDataResult getData(Token token) {
    Gson g = new Gson();
    String json = g.toJson(token);
    Result r = execute(path + "/tokens/data", ReqType.POST, json);
    if (r.response.getStatusLine().getStatusCode() == 200) {
      return g.fromJson(r.content, AuthDataResult.class);
    }
    throw new BackMeUpException("Failed to retrieve token data: " + r.content);
  }
}
