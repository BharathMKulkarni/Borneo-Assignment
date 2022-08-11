package com.example.BorneoAssignment.Controller;

import com.example.BorneoAssignment.Model.FileItemDTO;
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
import javax.jdo.annotations.Value;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class DriveController {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER3";
    private String CALLBACK_URI = "https://127.0.0.1.nip.io:8443/oauth";
    private String gdSecretKeys = "/home/bharathmkulkarni/BHARATH/Academics/Programming/Borneo/BorneoAssignment/src/main/resources/Keys/credentials.json";
    private String credentialsFolder = "/home/bharathmkulkarni/BHARATH/Academics/Programming/Borneo/BorneoAssignment/src/main/resources/CrediantialsStorage";
    private GoogleAuthorizationCodeFlow flow;
    private Credential credentials ;
    private Drive service ;

    public DriveController() throws FileNotFoundException {
    }

    @PostConstruct
    public void init() throws IOException {
        GoogleClientSecrets secrets = GoogleClientSecrets.load(
                JSON_FACTORY, new InputStreamReader(
                        new FileInputStream(gdSecretKeys)
                ));

        flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(credentialsFolder)))
                .build();

        credentials = flow.loadCredential(USER_IDENTIFIER_KEY);

        service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
                .setApplicationName("BorneoAssignmentApplication").build();
    }

    @GetMapping("/")
    public String showHomePage() throws IOException {
        boolean isUserAuthenticated = false;

        Credential credential = flow.loadCredential(USER_IDENTIFIER_KEY);
        if (credential != null) {
            boolean tokenValid = credential.refreshToken();
            if (tokenValid) {
                isUserAuthenticated = true;
            }
        }

        return isUserAuthenticated ? "dashboard.html" : "index.html";
    }

    @GetMapping("/googlesignin")
    public void doGoogleSignIn(HttpServletResponse response) throws IOException {
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        String redirectURL = url.setRedirectUri(CALLBACK_URI).setAccessType("offline").build();

        response.sendRedirect(redirectURL);
    }

    @GetMapping("/oauth")
    public String saveAuthorizationCode(@RequestParam String code) throws IOException {
        if (code != null) {
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

    /**
     * Lists all the files in a User's Google Drive account
     *
     * @return List of FileDTO objects that will be rendered by the frontend.
     * @throws IOException If files().list() fails
     */
    @GetMapping(value = {"/listFiles"}, produces = {"application/json"})
    public @ResponseBody List<FileItemDTO> listFiles() throws IOException {

        List<FileItemDTO> resultList = new ArrayList<>();

        FileList result = service.files().list()
                .setFields("*")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        for (com.google.api.services.drive.model.File file : result.getFiles()) {
            FileItemDTO fileItemDTO = new FileItemDTO();

            fileItemDTO.setId(file.getId());
            fileItemDTO.setName(file.getName());

            resultList.add(fileItemDTO);
        }

        return resultList;
    }


    /**
     * Searches for the presence of the input query string in all the files
     *
     * @return List of FileDTO objects that contains the input query string
     * @throws IOException If files().list() fails
     * @throws TikaException, SAXException if TIKA parser fails
     */
    @GetMapping(value = {"/search"}, produces = {"application/json"})
    public @ResponseBody List<FileItemDTO> searchFilesWithQuery(@RequestParam("q") String q) throws IOException, TikaException, SAXException {

        List<FileItemDTO> resultList = new ArrayList<>();

        FileList result = service.files().list()
                .setFields("*")
                .execute();

        for (com.google.api.services.drive.model.File file : result.getFiles()) {

            FileItemDTO fileItemDTO = new FileItemDTO();

            fileItemDTO.setId(file.getId());
            fileItemDTO.setName(file.getName());
            fileItemDTO.setWebContentLink(file.getWebContentLink());

            boolean present = false;
            present = searchString(file.getId(), q);

            if (present && file.getName() != null && file.getWebContentLink() != null)
                resultList.add(fileItemDTO);
        }
        return resultList;
    }


    /**
     * Searches for a given string in a file given by the fileID
     *
     * @return true if the string is present, false otherwise
     * @throws IOException If files().get() fails
     * @throws TikaException, SAXException if TIKA parser fails
     */
    private boolean searchString(String id, String q) throws IOException, TikaException, SAXException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.files().get(id).executeMediaAndDownloadTo(outputStream);

        // INITIALIZE TIKA PARSER
        final Parser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(-1);
        ParseContext context = new ParseContext();

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        // EXTRACT USING TIKA
        parser.parse(inputStream,handler,metadata, context);

        return outputStream.toString().contains(q);
    }

}



