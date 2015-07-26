package couk.cleverthinking.tof;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Main {

    private static final String SCOPE = "https://www.googleapis.com/auth/drive";
    // downloaded from API Console
    private static final String DEFAULT_P12_FILENAME = "MyProject.p12";
    // copy paste from API Console
    private static final String SERVICE_ACCOUNT_EMAIL_ADDRESS = "498909596280-hg2d77klsgjm5g7dall0mno71gmtfr2b@developer.gserviceaccount.com";

    public static void main(String[] args) throws GeneralSecurityException, IOException {


        enableLogging();


        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(SERVICE_ACCOUNT_EMAIL_ADDRESS)
                .setServiceAccountPrivateKeyFromP12File(new java.io.File(DEFAULT_P12_FILENAME))
                .setServiceAccountScopes(Collections.singleton(SCOPE))
                .build();

        System.out.println("hw");
        Drive service = new Drive.Builder(httpTransport, JSON_FACTORY, null)
                .setHttpRequestInitializer(credential).build();
        About a = service.about().get().execute();
        File body = new File();
        body.setTitle("t");
        body.setMimeType("text/plain");

        AbstractInputStreamContent b;
        File file = service.files().insert(body, new ByteArrayContent(null, "foo".getBytes())).execute();


        System.out.println(a.getKind());
        System.out.println(file.getId());
    }

    /**
     * enables logging of google api calls
     */
    private static void enableLogging() {
//        Level level = Level.CONFIG;                                                                                   // log all but secrets
        Level level = Level.ALL;                                                                                        // log all
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(level);
        Logger logger = Logger.getLogger("com.google");
        logger.setLevel(level);
        logger.addHandler(handler);
    }
}
