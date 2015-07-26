package couk.cleverthinking.tof;


// dependedencies downloaded from https://developers.google.com/resources/api-libraries/download/drive/v2/java/

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Main {

    private static final String CONFIG_FILENAME = "tof.cfg";

    private static final String SCOPE = "https://www.googleapis.com/auth/drive";

    private static JsonFactory JSON_FACTORY;
    private static HttpTransport HTTP_TRANSPORT;

    public static void main(String[] args) throws GeneralSecurityException, IOException {


//        enableLogging();

        oauthInit();

        Config config = null;
        try {
            config = loadConfigurationFile(CONFIG_FILENAME);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            l("[M48] Fatal error. Could not open " + CONFIG_FILENAME);
            return;
        }


        for (DomainConfig domainConfig : config.domains) {
            l("[M60] Processing domain "+domainConfig.domainName +" ...");
            try {
                processDomain(domainConfig);
            } catch (Exception e) {
                e.printStackTrace();
                l("[M64] Exception '"+e+"' while processing domain "+domainConfig.domainName+" . Continuing with next domain");
            }
        }
    }


    /**
     * initialise the Google library Oauth objects
     */
    private static void oauthInit() {
        JSON_FACTORY = JacksonFactory.getDefaultInstance();
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            e.printStackTrace();
            l("[M57] Fatal error. Could not create HTTP Transport " + e);
            return;
        }
    }


    private static void processDomain(DomainConfig domainConfig) throws IOException, GeneralSecurityException {
        l("[M114] Authorising Drive Service for the Service Account ...");
        Drive driveserviceServiceAccount = buildDriveService(domainConfig.serviceAccountEmail, null, convertDomainName2p12Filename(domainConfig.domainName) );

        l("[M111] checking access to 'ALL TOFS' ("+domainConfig.tofsFolderId+")...");
        try {
            File f = driveserviceServiceAccount.files().get(domainConfig.tofsFolderId).execute();
        } catch (IOException e) {
            throw new IllegalStateException("[M116] Could not access 'ALL TOFS' folder "+e);
        }


        l("[M115] Listing child folders of 'ALL TOFS' ("+domainConfig.tofsFolderId+")...");
        List<ChildReference> folders = driveserviceServiceAccount.children().list(domainConfig.tofsFolderId).execute().getItems();

        for (ChildReference folder: folders) {
            l("Found "+folder.getId());
        }

        l("[M116] Authorising Drive Service for the current owner account ..."); // todo GET FROM FOLDER AFTER READING SBFOLDERS OF ALLTOF
        Drive driveserviceUser = buildDriveService(domainConfig.serviceAccountEmail, "gwappo@primetext.com", convertDomainName2p12Filename(domainConfig.domainName));
        if (domainConfig.targetOwner != null) {
            l("[M118] Authorising Drive Service for the new owner account ...");
            Drive targetDriveservice = buildDriveService(domainConfig.serviceAccountEmail, "gwappo@primetext.com", convertDomainName2p12Filename(domainConfig.domainName));
        } else {
            Drive targetDriveservice = driveserviceServiceAccount;
        }

//        About a = driveserviceUser.about().get().execute();
        com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
        body.setTitle("ttt");
        body.setMimeType("text/plain");

        AbstractInputStreamContent b;
        com.google.api.services.drive.model.File file1 = driveserviceServiceAccount.files().insert(body, new ByteArrayContent(null, "foo".getBytes())).execute();
        com.google.api.services.drive.model.File file2 = driveserviceUser.files().insert(body, new ByteArrayContent(null, "foo".getBytes())).execute();


//        System.out.println(a.getKind());
        System.out.println(file2.getId());
    }

    private static String convertDomainName2p12Filename (String domainName) {
        return domainName.replaceAll("\\.", "") + ".p12";
    }


    /**
     * Read the specified file into a JSON object
     *
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    private static Config loadConfigurationFile(String filename) throws FileNotFoundException {
        java.io.File f = new java.io.File(filename);
        InputStream is = new FileInputStream(f);
        final Gson gson = new Gson();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        return gson.fromJson(reader, Config.class);
    }


    public static Drive buildDriveService(String serviceAccountEmailAddress, String impersonatedAccountEmailAddress, String p12Filename) throws GeneralSecurityException, IOException {
        // simple service account build
        java.io.File f = new java.io.File(p12Filename);
        if (!f.canRead()) {
            throw new IllegalAccessError("[M150] Could not read p12 key file '"+p12Filename+"'");
        }

        GoogleCredential.Builder builder = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmailAddress)
                .setServiceAccountPrivateKeyFromP12File(f)
                .setServiceAccountScopes(Collections.singleton(SCOPE));
        // if requested, impresonate a domain user
        if (impersonatedAccountEmailAddress != null) {
            builder.setServiceAccountUser(impersonatedAccountEmailAddress);
        }

        // build the Drive service
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
                .setApplicationName("TOF")
                .setHttpRequestInitializer(builder.build()).build();

        return service;
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

    private static void l(String l) {
        System.out.println(l);
    }


    private static class Config {
        String foo;
        DomainConfig[] domains;
    }

    private static class DomainConfig {
        String domainName;
        String serviceAccountEmail;
        String tofsFolderId;
        String targetOwner;
    }
}
