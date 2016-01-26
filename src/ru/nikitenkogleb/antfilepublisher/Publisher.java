/*
 *	Publisher.java
 *	AntFilePublisher
 *
 *  Copyright (c) 2016 Nikitenko Gleb.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package ru.nikitenkogleb.antfilepublisher;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.samczsun.skype4j.Skype;
import com.samczsun.skype4j.SkypeBuilder;
import com.samczsun.skype4j.exceptions.ChatNotFoundException;
import com.samczsun.skype4j.exceptions.ConnectionException;
import com.samczsun.skype4j.exceptions.InvalidCredentialsException;
import com.samczsun.skype4j.exceptions.ParseException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.tools.ant.Task;

/**
 * Ant File Publisher.
 * 
 * This implementation means a simple way to upload your build files to 
 * <b>Google Drive</b>, and send a notification about it to your partner (a colleague, 
 * project manager, etc...) via <b>Skype</b>.</br>
 * 
 * Your notification optional can contains a url-link to file, version of release 
 * and/or timestamp of assembly.
 *
 * @author Gleb Nikitenko
 * @version 1.0
 * @since Jan 20, 2016
 */
public final class Publisher extends Task {
    
     /** Application name. */
    private static final String APPLICATION_NAME = "Google Drive Publisher";
    
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /** The name of system-java property defined user-home directory. */
    private static final String SYSJAVAPROP_USER_HOME = "user.home";
    /** The path to data-store file.*/
    private static final String DATA_STORE_FILE = ".credentials/google-drive-publisher";
    /** Directory to store user data for this application. */
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(System.getProperty(SYSJAVAPROP_USER_HOME), DATA_STORE_FILE);
    /** The type of access to data-store file.*/
    private static final String DATA_STORE_ACCESS_TYPE = "offline";
    /** Global instance of the scopes required by this application.. */
    private static final List<String> SCOPES =
            Arrays.asList(new String[]{DriveScopes.DRIVE_FILE});
    /** The authorization user id. */
    private static final String AUTHORIZE_USER = "user";
    /** The name of current time-zone. */
    private static final String TIME_ZONE_NAME = "UTC";
    /** CUrrent time zone . */
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(TIME_ZONE_NAME);
    
    /** The mime type of shared file. */
    private static final String MIME_TYPE = "application/zip";
    
    /** The format of log-message. */
    private static final String SKYPE_NOTIFICATION_LINK_FORMAT =
            "\nhttps://drive.google.com/uc?export=download&id=%s";

    
    /** The date time format. */
    private static final String DATE_TIME_FORMAT = "yyyy-MMM-dd HH:mm:ss";
    /** The uploaded file time format. */
    private static final String FILE_NAME_TIMESTAMP_FORMAT = "dd-MMM-yyyy--HH-mm-ss";
    /** The format of log-message. */
    private static final String DRIVE_LOG_MESSAGE_FORMAT =
            "%s uploaded. File ID: %s";
    /** The format of log-message. */
    @SuppressWarnings("unused")
    private static final String DRIVE_LOG_MESSAGE_FORMAT_2 =
            "%s updated. File ID: %s";

    /** The format of log-message. */
    private static final String SKYPE_LOG_MESSAGE_FORMAT =
            "Link was sent on %s";

    /** The upload file arguments. */
    private String mFileName, mDescription, mClientSecretFile, mFolderId;
    /** The send notification arguments. */
    private String mLogin, mPassword, mUserName, mMessage;

    /** Constructs a new Publisher by default. */
    public Publisher() {}
    
    /**
     * Constructs a new Publisher with full arguments-set.
     *
     * @param fileName          the name of file (<i>Required</i>)
     * @param description       file description (<i>Required</i>)
     * @param clientSecretFile  app client secret credentials file (<i>Required</i>)
     * @param folderId          parent folder id (<i>Not Required</i>)
     * @param login             your skype-login
     * @param password          your skype-password
     * @param userName          your skype-friend
     * @param message           your message
     */
    public Publisher(String fileName, String description,
            String clientSecretFile, String folderId, String login, String password,
            String userName, String message) {
        mFileName = fileName;
        mDescription = description;
        mFolderId = folderId;
        mClientSecretFile = clientSecretFile;
        mLogin = login;
        mPassword = password;
        mUserName = userName;
        mMessage = message;
    }
    
    /** @param fileName to set {@link #mFileName} */
    @SuppressWarnings("javadoc")
    public void setFileName(String fileName) {mFileName = fileName;}
    /** @param description to set {@link #mDescription} */
    @SuppressWarnings("javadoc")
    public void setDescription(String description) {mDescription = description;}
    /** @param clientSecretFile to set {@link #mClientSecretFile} */
    @SuppressWarnings("javadoc")
    public void setClientSecretFile(String clientSecretFile) {mClientSecretFile = clientSecretFile;}
    /** @param folderId to set {@link #mFolderId} */
    @SuppressWarnings("javadoc")
    public void setFolderId(String folderId) {mFolderId = folderId;}
    /** @param login to set {@link #mLogin} */
    @SuppressWarnings("javadoc")
    public void setLogin(String login) {mLogin = login;}
    /** @param password to set {@link #mPassword} */
    @SuppressWarnings("javadoc")
    public void setPassword(String password) {mPassword = password;}
    /** @param userName to set {@link #mUserName} */
    @SuppressWarnings("javadoc")
    public void setUserName(String userName) {mUserName = userName;}
    /** @param message to set {@link #mMessage} */
    @SuppressWarnings("javadoc")
    public void setMessage(String message) {mMessage = message;}

    /**
     * A Command-line handler.
     *  
     * @param args arguments
     */
    public static final void main(String[] args) {
        String fileName = null, description = null,
                clientSecretFile = null, folderId = null, login = null, password = null,
                userName = null, message = null;
        
        for (int i = 0; i < args.length; i++)
            switch (i) {
                case 0: fileName            = args[i]; break;
                case 1: description         = args[i]; break;
                case 2: clientSecretFile    = args[i]; break;
                case 3: folderId            = args[i]; break;
                case 4: login               = args[i]; break;
                case 5: password            = args[i]; break;
                case 6: userName            = args[i]; break;
                case 7: message             = args[i]; break;
                default: break;
            }
        
        final Publisher publisher = new Publisher(fileName, description, clientSecretFile,
                folderId, login, password, userName, message);
        publisher.execute();
    }
    
    /** {@inheritDoc} */
    public final void execute() {
        String message = mMessage;
        final Locale locale = Locale.ENGLISH;
        String fileId = null;
        
        Drive service = null;
        
        if (mFileName != null && !mFileName.isEmpty() &&
            mDescription != null && !mDescription.isEmpty() &&
            mClientSecretFile != null && !mClientSecretFile.isEmpty()) {
                
                // Build a new authorized API client service.
                service = getDriveService(mClientSecretFile);
                if (service != null)
                    fileId = uploadFile(service, mFileName, mDescription, mFolderId);
                if (fileId != null && !fileId.isEmpty()) {
                    log(String.format(locale, DRIVE_LOG_MESSAGE_FORMAT, mFileName, fileId));
                    message = message + String.format(locale, SKYPE_NOTIFICATION_LINK_FORMAT, fileId);
                }
        }
        
        if (mLogin != null && !mLogin.isEmpty() &&
                mPassword != null && !mPassword.isEmpty() &&
                mUserName != null && !mUserName.isEmpty() && service != null) {
            final long sentTime = send(mLogin, mPassword, mUserName, message);
            if (sentTime != -1) {
                updateFile(service, mFileName, fileId, sentTime);
                //log(String.format(locale, DRIVE_LOG_MESSAGE_FORMAT_2, mFileName, updatedFileId));
                final String dateTime = new SimpleDateFormat(DATE_TIME_FORMAT, locale)
                        .format(sentTime);        
                log(String.format(locale, SKYPE_LOG_MESSAGE_FORMAT, dateTime));
            }
        }
        
    }
    
    
    /**
     * Build and return an authorized Drive client service.
     * 
     * @return an authorized Drive client service
     */
    private static final Drive getDriveService(String clientSecretFile) {
        Drive result = null;
        
        HttpTransport httpTransport = null;
        try {httpTransport = GoogleNetHttpTransport.newTrustedTransport();}
        catch (GeneralSecurityException | IOException e) {e.printStackTrace();}
        if (httpTransport != null) {
            final Credential credential = authorize(httpTransport, clientSecretFile);
            if (credential != null)
                result = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME).build();
        }
        
        return result;
    }
    
    /** @return an authorized Credential object. */
    private static final Credential authorize(HttpTransport httpTransport, String clientSecretFile) {
        Credential result = null;
        
        // Open credentials-file's input stream
        //GoogleDrivePublisher.class.getResourceAsStream(clientSecretFile);
        InputStream inputStream = null;
        try {inputStream = new FileInputStream(clientSecretFile);}
        catch (FileNotFoundException e) {e.printStackTrace();}
        if (inputStream != null) {
             // Open credentials-file's input stream reader
            final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            
            GoogleClientSecrets googleClientSecrets = null;
            try {googleClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, inputStreamReader);}
            catch (IOException e) {e.printStackTrace();}
            if (googleClientSecrets != null) {
                    FileDataStoreFactory fileDataStoreFactory = null;
                    try {fileDataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);}
                    catch (IOException e) {e.printStackTrace();}
                    if (fileDataStoreFactory != null) {
                        // Build flow and trigger user authorization request.
                        GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow = null;
                        try {googleAuthorizationCodeFlow =
                                new GoogleAuthorizationCodeFlow.Builder(httpTransport,
                                    JSON_FACTORY, googleClientSecrets, SCOPES)
                            .setDataStoreFactory(fileDataStoreFactory)
                            .setAccessType(DATA_STORE_ACCESS_TYPE)
                            .build();}
                        catch (IOException e) {e.printStackTrace();}
                        if (googleAuthorizationCodeFlow != null)
                            try {result = new AuthorizationCodeInstalledApp (
                                    googleAuthorizationCodeFlow, new LocalServerReceiver())
                                    .authorize(AUTHORIZE_USER);}
                            catch (IOException e) {e.printStackTrace();}
                    }
                
            }
                    
            // Close credentials-file's input stream reader
            try {inputStreamReader.close();} catch (IOException e) {e.printStackTrace();}
            // Close credentials-file's input stream
            try {inputStream.close();} catch (IOException e) {e.printStackTrace();}
         }
        
        return result;
    }

    /**
     * Upload file.
     * 
     * @param service Google Drive Service-Instance
     * 
     * @return Id of inserted file metadata if successful, {@code null} otherwise.
     */
    private static final String uploadFile(Drive service,
            String fileName, String description, String folderId) {
        String result = null;
        
        final DateTime dateTime = new DateTime(System.currentTimeMillis());
        // File's content.
        final java.io.File fileContent = new java.io.File(fileName);
        
        // File's metadata.
        File body = new File()
                .setName(fileContent.getName())
                .setDescription(description)
                .setMimeType(MIME_TYPE)
                .setCreatedTime(dateTime)
                .setModifiedTime(dateTime);
        
        if (folderId != null && !folderId.isEmpty())
            body.setParents(Arrays.asList(new String[] {folderId}));
       
        final FileContent mediaContent = new FileContent(MIME_TYPE, fileContent);
        File file = null;
        try {file = service.files().create(body, mediaContent).execute();}
        catch (IOException e) {e.printStackTrace();}
        if (file != null) result = file.getId();
        return result;
    }
    
    /**
     * Update file.
     * 
     * @param service Google Drive Service-Instance
     * 
     * @return Id of inserted file metadata if successful, {@code null} otherwise.
     */
    private static final String updateFile(Drive service, String oldName,
            String fileId, long timeStamp) {
        String result = null;
        
        final Date javaDate = new Date(timeStamp);
        final DateTime dateTime = new DateTime(javaDate, TIME_ZONE);
        final String dateTimePreffix = new SimpleDateFormat(FILE_NAME_TIMESTAMP_FORMAT,
                Locale.ENGLISH).format(javaDate) + ".zip";
        final String fileName = new java.io.File(oldName).getName()
                .replace(".zip", dateTimePreffix);
        
        // File's metadata.
        File body = new File()
                .setName(fileName)
                //.setCreatedTime(dateTime)
                .setModifiedTime(dateTime);
       
        File file = null;
        try {file = service.files().update(fileId, body).execute();}
        catch (IOException e) {e.printStackTrace();}
        if (file != null) result = file.getId();
        return result;
    }

    /**
     * Send Skype Message.
     * 
     * @param login    your skype-login
     * @param password your skype-password
     * @param friend   your skype-friend
     * @param message  your message
     * 
     * @return         timestamp of outgoing message
     */
    public static final long send(String login, String password, String friend, String message) {
        long result = -1;
        final Skype skype = new SkypeBuilder(login, password).withAllResources().build();
        
        try {
            skype.login();
            result = skype.getContact(friend).getPrivateConversation()
                    .sendMessage(message).getSentTime();
            skype.logout();
        } catch (ParseException | InvalidCredentialsException |
                ConnectionException | ChatNotFoundException e) {e.printStackTrace();}

        return result;
    }

}
