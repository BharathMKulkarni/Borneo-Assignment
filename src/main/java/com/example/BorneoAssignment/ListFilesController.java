//package com.example.BorneoAssignment;
//
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.services.drive.Drive;
//import com.google.api.services.drive.model.File;
//import com.google.api.services.drive.model.FileList;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//import com.example.BorneoAssignment.DriveQuickstart;
//
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//import java.util.List;
//
//@RestController
//public class ListFilesController {
//
//    /** Application name. */
//    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
//    /** Global instance of the JSON factory. */
//    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//
//    @GetMapping("/")
//    public static String SayHello(){
//        return "Hey there!";
//    }
//
//    @GetMapping("/listFiles")
//    public static void ListFiles() throws IOException, GeneralSecurityException {
//        // Build a new authorized API client service.
//
//        DriveQuickstart driver = new DriveQuickstart();
//
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, driver.getCredentials(HTTP_TRANSPORT))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//
//        // Print the names and IDs for up to 10 files.
//        FileList result = service.files().list()
//                .setPageSize(10)
//                .setFields("nextPageToken, files(id, name)")
//                .execute();
//        List<File> files = result.getFiles();
//        if (files == null || files.isEmpty()) {
//            System.out.println("No files found.");
//        } else {
//            System.out.println("Files:");
//            for (File file : files) {
//                System.out.printf("%s (%s)\n", file.getName(), file.getId());
//            }
//        }
//    }
//}
