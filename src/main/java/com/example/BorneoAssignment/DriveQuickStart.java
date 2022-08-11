package com.example.BorneoAssignment;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

    @GetMapping("/listFiles")
    public void listFiles() throws IOException {

        Credential credentials = flow.loadCredential(USER_IDENTIFIER_KEY);
//        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        // Build a new authorized API client service.
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials).setApplicationName("BorneoAssignmentApplication").build();

        List<com.google.api.services.drive.model.File> files = new ArrayList<>();

        String pageToken = null;
        do {
            FileList result = service.files().list()
                    .setQ("fullText contains 'Borneo Cosmos'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            for (com.google.api.services.drive.model.File file : result.getFiles()) {
                System.out.printf("Found file: %s (%s)\n",
                        file.getName(), file.getId());
            }

            files.addAll(result.getFiles());

            pageToken = result.getNextPageToken();
        } while (pageToken != null);
    }


}
