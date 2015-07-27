package couk.cleverthinking.tof;


// dependedencies downloaded from https://developers.google.com/resources/api-libraries/download/drive/v2/java/

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.gson.Gson;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Main {


    private static final String SCOPE = "https://www.googleapis.com/auth/drive";                                        // the scope used for all operartions
    private static String SPACES = "    ";
    private static JsonFactory JSON_FACTORY;
    private static HttpTransport HTTP_TRANSPORT;
    private static HashMap<String, Drive> cachedDriveservices = new HashMap<>();                                        // cache of Drive Services
    private static int indentLevel = 0;                                                                                 // indents output for formatting

    // The following can be overriden by command line args
    private static String FOLDER_COLOUR = "#FF0000";                                                                    // -c
    private static String CONFIG_FILENAME = "tof.cfg";                                                                  // -f


    public static void main(String[] args) throws GeneralSecurityException, IOException {
        processArgs(args);                                                                                              // deal with command line args
//        enableLogging();                                                                                              // log http traffic

        oauthInit();                                                                                                    // setup OAuth base variables

        Config config;
        try {
            config = loadConfigurationFile(CONFIG_FILENAME);                                                            // load the config file
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            l("[M48] Fatal error. Could not open " + CONFIG_FILENAME);
            return;
        }


        for (DomainConfig domainConfig : config.domains) {                                                              // for each domain in the config file
            l("[M60] Processing domain " + domainConfig.domainName + " ...");
            indentLevel++;
            try {
                processDomain(domainConfig);                                                                            // process it
            } catch (Exception e) {
                e.printStackTrace();
                l("   [M64] Exception '" + e + "' while processing domain " + domainConfig.domainName + " . Continuing with next domain");
            }
            indentLevel--;
        }
    }


    /**
     * process the command line args
     * Currently:-
     * -f config file
     * -c folder colour
     * @param args the args from the command line
     */
    private static void processArgs(String[] args) {
        // TODO args
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
        }
    }


    /**
     * Does the processing required for a single domain from the config file
     *
     * @param domainConfig  the sub-config for this domain
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private static void processDomain(DomainConfig domainConfig) throws IOException, GeneralSecurityException {
        l("[M114] Authorising Drive Service for the Service Account ...");
        // build a drive service for the service account user for this domain
        Drive driveserviceServiceAccount = buildDriveService(domainConfig.serviceAccountEmail, "ServiceAccount", convertDomainName2p12Filename(domainConfig.domainName));

        // display the quota of the service account
//        About about = driveserviceServiceAccount.about().get().execute();
//        l("quote = " + about.getQuotaType() + " " + about.getQuotaBytesByService() + " " + about.getQuotaBytesTotal() + " " + about.getQuotaBytesUsed());

        l("[M111] checking access to 'ALL TOFS' (" + domainConfig.tofsFolderId + ")...");
        try {                                                                                                           // check we can access the TOFS ALL folder, ie it exists and we have permission
            driveserviceServiceAccount.files().get(domainConfig.tofsFolderId).execute();
        } catch (IOException e) {
            throw new IllegalStateException("[M116] Could not access 'ALL TOFS' folder " + e);
        }


        l("[M115] Listing child folders of 'ALL TOFS' (" + domainConfig.tofsFolderId + ")...");
        List<ChildReference> children = driveserviceServiceAccount.children().list(domainConfig.tofsFolderId).execute().getItems();

        for (ChildReference child : children) {                                                                         // for each child of TOFS ALL
//            l("Found "+folder.getId());
            indentLevel++;
            processChild(child, domainConfig);                                                                          // process it
            indentLevel--;
        }
    }


    /**
     * This is where the work gets done. This is called recursively for each file/folder under TOFS ALL
     *
     * @param childRef The child (could be a file or a folder) to process
     * @param domainConfig The current domain configuration object
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private static void processChild(ChildReference childRef, DomainConfig domainConfig) throws GeneralSecurityException, IOException {
        File childFile = buildDriveService(null, "ServiceAccount", null)
                .files().get(childRef.getId()).execute();                                                               // get the file object
        String currentOwner = childFile.getOwners().get(0).getEmailAddress();
        boolean isFolder =  "application/vnd.google-apps.folder".equalsIgnoreCase(childFile.getMimeType());
        l("[M145] Processing child '" + childFile.getTitle() + "', the current owner is "+currentOwner);
        indentLevel++;
//        l("folder has "+folder.getOwners().size()+" owners");
//        l("curro=" + currentOwner + " " + folder.getOwners().get(0).getPermissionId());
        if (!currentOwner.equalsIgnoreCase(domainConfig.targetOwner)) {                                                 // if the current owner is NOT the intended target owner
            try {
                l("[M149] !! changing owner permission to " + domainConfig.targetOwner);
                buildDriveService(domainConfig.serviceAccountEmail, currentOwner, convertDomainName2p12Filename(domainConfig.domainName))
                        .permissions().insert(childFile.getId(), buildNewOwnerPremission(domainConfig.targetOwner)).execute();
                if (isFolder) {                                                                                         // check if this is a folder coz if so it needs its colour changing
                    l("[M153] !! changing colour to " + FOLDER_COLOUR);
                    File colourFile = new File();
                    colourFile.setFolderColorRgb(FOLDER_COLOUR);
                    buildDriveService(domainConfig.serviceAccountEmail, currentOwner, convertDomainName2p12Filename(domainConfig.domainName))
                            .files().patch(childFile.getId(), colourFile).execute();
                }
            } catch (Exception e) {
                l("[M154] Error: Could not add new owner permission " + e);
            }
            try {
                l("[M152] !! changing previous owner permission to writer");
                Permission writerPermission = new Permission();
                writerPermission.setRole("writer");
                buildDriveService(domainConfig.serviceAccountEmail, domainConfig.targetOwner, convertDomainName2p12Filename(domainConfig.domainName))
                        .permissions().patch(childFile.getId(), childFile.getOwners().get(0).getPermissionId(), writerPermission).execute();
            } catch (Exception e) {
                l("[M160] Error: Could not delete old owner permission " + e);
            }
        }

        // if this child is also a folder, then list its children and re-process
        if (isFolder) {
            l("[M174] Listing children of " + childFile.getTitle() + " (" + domainConfig.tofsFolderId + ")...");
            List<ChildReference> folders = buildDriveService(null, "ServiceAccount", null).children().list(childFile.getId()).execute().getItems();

            for (ChildReference folder : folders) {
//            l("Found "+folder.getId());
                indentLevel++;
                processChild(folder, domainConfig);
                indentLevel--;
            }
        }
        indentLevel--;
    }

    private static Permission buildNewOwnerPremission(String newOwnerEmail) {
        Permission newOwnerPermission = new Permission();
        newOwnerPermission.setValue(newOwnerEmail);
        newOwnerPermission.setType("user");
        newOwnerPermission.setRole("owner");
        return newOwnerPermission;
    }


    private static String convertDomainName2p12Filename(String domainName) {
        return domainName.replaceAll("\\.", "") + ".p12";
    }


    /**
     * Read the specified file into a JSON object
     *
     * @param filename the nameofthe tof.cfg file
     * @return a config object of its contents
     * @throws FileNotFoundException
     */
    private static Config loadConfigurationFile(String filename) throws FileNotFoundException {
        java.io.File f = new java.io.File(filename);
        InputStream is = new FileInputStream(f);
        final Gson gson = new Gson();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        return gson.fromJson(reader, Config.class);
    }


    /**
     * This is where most of the OAuth heavy lifting happens. Returns a Drive Service for the given account; one of:-
     * - the Service Account
     * - the original owner account
     * - the new (target) owner account
     *
     * Once created, each Drive Srevice is cached to save redundant calls to Google
     *
     * @param serviceAccountEmailAddress  The email address of the Service Account
     * @param impersonatedAccountEmailAddress The email to impersonate, or "ServiceAccount" to build a Drive Service for the SA
     * @param p12Filename The name of the .p12 file, ie. mydomaincom.p12
     * @return a suitably authorised Drive Service
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static Drive buildDriveService(String serviceAccountEmailAddress, String impersonatedAccountEmailAddress, String p12Filename) throws GeneralSecurityException, IOException {
        // simple service account build

        // if a drrive service for the specified email has already been built and cached, return it
        if (cachedDriveservices.containsKey(impersonatedAccountEmailAddress)) {
            return cachedDriveservices.get(impersonatedAccountEmailAddress);
        }

        java.io.File f;
        try {
            f = new java.io.File(p12Filename);
        } catch (Exception e) {
            throw new IllegalAccessError("[M226] Could not read p12 key file '" + p12Filename + "' " + e);
        }
        if (!f.canRead()) {
            throw new IllegalAccessError("[M229] Could not read p12 key file '" + p12Filename + "'");
        }

        GoogleCredential.Builder builder = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmailAddress)
                .setServiceAccountPrivateKeyFromP12File(f)
                .setServiceAccountScopes(Collections.singleton(SCOPE));
        // if requested, impresonate a domain user
        if (!"ServiceAccount".equals(impersonatedAccountEmailAddress)) {
            builder.setServiceAccountUser(impersonatedAccountEmailAddress);
        }

        // build the Drive service
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
                .setApplicationName("TOF")
                .setHttpRequestInitializer(builder.build()).build();

        cachedDriveservices.put(impersonatedAccountEmailAddress, service);                                              // cache the service for next time

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

    /**
     * Simple console logger that indents based on our depth in the tree. Controlled by "indentLevel"
     *
     * @param l the string to log
     */
    private static void l(String l) {
        int TAB = 4;
        String indent;
        try {
            indent = SPACES.substring(0, TAB * indentLevel);
        } catch (Exception e) {
            SPACES += SPACES;
            indent = SPACES.substring(0, TAB * indentLevel);
        }
        System.out.println(indent + l);
    }


    /**
     * The object that corresponds to the JSON config file tof.cfg
     */
    private static class Config {
        DomainConfig[] domains;
    }

    /**
     * The object that corresponds to each domain within the tof.cfg
     */
    private static class DomainConfig {
        String domainName;                                                                                              // the domain name ewg. mydomain.com
        String serviceAccountEmail;                                                                                     // the service account email from the API console, eg 1a23f87282@googleuseraccount.com
        String tofsFolderId;                                                                                            // the folder ID of the ALL TOFS folder, eg. 0B_1576a87a69678fd9678965f
        String targetOwner;                                                                                             // the admin/archive account that will end up owning everything, eg. admin@mydomain.com
    }
}
