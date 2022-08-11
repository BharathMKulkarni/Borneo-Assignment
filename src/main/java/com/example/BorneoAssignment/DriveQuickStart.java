package com.example.BorneoAssignment;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class DriveQuickStart {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER";

//    @Value("${google.oauth.callback.uri}")
    private String CALLBACK_URI = "https://127.0.0.1.nip.io:8443/oauth";

//    @Value("${google.secret.key.path}")
    private String gdSecretKeys = "/home/bharathmkulkarni/BHARATH/Academics/Programming/Borneo/BorneoAssignment/src/main/resources/Keys/credentials.json";

//    @Value("${google.credentials.folder.path}")
    private String credentialsFolder = "/home/bharathmkulkarni/BHARATH/Academics/Programming/Borneo/BorneoAssignment/src/main/resources/CrediantialsStorage";

    private GoogleAuthorizationCodeFlow flow;

    public DriveQuickStart() throws FileNotFoundException {
    }

    @PostConstruct
    public void init() throws IOException {

        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(
                new FileInputStream(gdSecretKeys)
        ));

        flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(credentialsFolder)))
                .build();

    }

    @GetMapping("/")
    public String showHomePage() throws IOException {

        boolean isUserAuthenticated = false;

        Credential credential = flow.loadCredential(USER_IDENTIFIER_KEY);
        if(credential!=null){
            boolean tokenValid = credential.refreshToken();
            if(tokenValid){
                isUserAuthenticated = true;
            }
        }

        return isUserAuthenticated? "dashboard.html" :  "index.html";
    }

    @GetMapping("/googlesignin")
    public void doGoogleSignIn(HttpServletResponse response) throws IOException {

        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        String redirectURL = url.setRedirectUri(CALLBACK_URI).setAccessType("offline").build();

        response.sendRedirect(redirectURL);
    }

    @GetMapping("/oauth")
    public String saveAuthorizationCode (@RequestParam String code) throws IOException {

//        String code = request.getParameter("code");
        System.out.println(code);
        if(code!=null){
            saveToken(code);
            return "dashboard.html";
        } else {
            return "index.html";
        }
    }

    private void saveToken(String code) throws IOException {
        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(CALLBACK_URI).execute();
        flow.createAndStoreCredential(response, USER_IDENTIFIER_KEY);
    }


    @GetMapping(value = {"/listFiles"}, produces = {"application/json"})
    public @ResponseBody List<FileItemDTO> listFiles() throws IOException {

        Credential credentials = flow.loadCredential(USER_IDENTIFIER_KEY);

        // Build a new authorized API client service.
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
                .setApplicationName("BorneoAssignmentApplication").build();

        List<com.google.api.services.drive.model.File> files = new ArrayList<>();
        List<FileItemDTO> resultList = new ArrayList<>();
        String pageToken = null;

            FileList result = service.files().list()
                    .setQ("fullText contains 'Borneo Cosmos'")
                    .setFields("*")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();


            for (com.google.api.services.drive.model.File file : result.getFiles()) {
                FileItemDTO fileItemDTO = new FileItemDTO();

                fileItemDTO.setId(file.getId());
                fileItemDTO.setName(file.getName());

                getFileContent(file.getId());
                downloadFileById(file.getId());
                resultList.add(fileItemDTO);

            }

            files.addAll(result.getFiles());

            pageToken = result.getNextPageToken();

        return resultList;
    }

    @GetMapping(value = {"/search"}, produces = {"application/json"})
    public @ResponseBody List<FileItemDTO> searchFilesWithQuery(@RequestParam("q") String q) throws IOException {

        Credential credentials = flow.loadCredential(USER_IDENTIFIER_KEY);

        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
                .setApplicationName("BorneoAssignmentApplication").build();

        List<FileItemDTO> resultList = new ArrayList<>();

        // BUILD THE SEARCH QUERY
        String searchQuery = "fullText contains " + "'" + q + "'";
        System.out.println("searchQuery: " + searchQuery);

        FileList result = service.files().list()
                .setQ(searchQuery)
                .setFields("*")
                .execute();

        for (com.google.api.services.drive.model.File file : result.getFiles()) {
            FileItemDTO fileItemDTO = new FileItemDTO();

            fileItemDTO.setId(file.getId());
            fileItemDTO.setName(file.getName());
            fileItemDTO.setWebContentLink(file.getWebContentLink());
            resultList.add(fileItemDTO);
        }
        return resultList;
    }


    FileOutputStream fileOutputStream = new FileOutputStream("temp.txt");
    private void downloadFileById(String id) throws IOException, GoogleJsonResponseException {

        Credential credentials = flow.loadCredential(USER_IDENTIFIER_KEY);

        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
                .setApplicationName("BorneoAssignmentApplication").build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        service.files().get(id).executeMediaAndDownloadTo(outputStream);
        parseFromTika(outputStream.toByteArray());
        System.out.println(outputStream.toString());

    }

    private void getFileContent(String id) throws IOException {

        Credential credentials = flow.loadCredential(USER_IDENTIFIER_KEY);
//        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        // Build a new authorized API client service.
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
                .setApplicationName("BorneoAssignmentApplication").build();

        com.google.api.services.drive.model.File file = service.files().get(id).execute();
        String downloadURL = file.getWebContentLink();

        System.out.println(file.toString());
        System.out.println("\n\n\n\n");
    }


    private void parseFromTika(byte[] bytes) throws IOException {

        // INITIALIZE TIKA PARSER
        final Parser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(-1);
        ParseContext context = new ParseContext();

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

            // EXTRACT USING TIKA
            parser.parse(inputStream,handler,metadata, context);

            System.out.println("FILE CONTENTS : " + handler.toString());

        } catch (GoogleJsonResponseException e) {
            System.err.println("Unable to move file: " + e.getDetails());
            throw e;
        } catch (TikaException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }

    }

}
