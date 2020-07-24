package com.lattice.TestFileUpload;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;

@SpringBootTest
class TestFileUploadApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void uploadFileInParts() throws IOException, URISyntaxException, JSONException {
        File file = new File("C:\\Users\\sakshi\\Videos\\Frozen.2013.1080p.BluRay.x264.YIFY.mp4");

        CloseableHttpClient client = HttpClients.createDefault();

        URIBuilder uribuilder = new URIBuilder("http://localhost:8080/fileExists");
        uribuilder.setParameter("filename", file.getName()).setParameter("fileSize", Long.toString(file.length()));
        System.out.println(file.length());

        HttpGet httpGet = new HttpGet(uribuilder.build());
        CloseableHttpResponse fileSizeResponse = client.execute(httpGet);
        long bytesWritten = 0;
        String fileResponseString = new BasicResponseHandler().handleResponse(fileSizeResponse);
        System.out.println(fileResponseString);
        if (fileSizeResponse.getStatusLine().getStatusCode() == 206) {
            System.out.println("Partial Content already exists on server. Initiating partial Upload!!");
            JSONObject json = new JSONObject(fileResponseString);
            bytesWritten = (Integer) json.get("bytes");
        }
        // check if file already exists on server

        if (fileSizeResponse.getStatusLine().getStatusCode() != 200) {

            HttpPost httpPost = new HttpPost("http://localhost:8080/fileUpload");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            InputStream inputStream = new FileInputStream(file);

            if (bytesWritten != 0) {
                System.out.print("Number of bytes Skipped: ");
                System.out.println(inputStream.skip(bytesWritten));
            }
            builder.addBinaryBody("file", inputStream, ContentType.APPLICATION_OCTET_STREAM
                    , "Frozen.2013.1080p.BluRay.x264.YIFY.mp4");


            // Progress listener - updates task's progress
            BytesTransferred bytesTransferred = new BytesTransferred();
            ProgressEntityWrapper.ProgressListener progressListener =
                    progress -> {
                        System.out.println((progress*100)/file.length() + "%" );
                        bytesTransferred.setBytesTransferred(progress);
                    };

            httpPost.setEntity(new ProgressEntityWrapper(builder.build(), progressListener));
            httpPost.addHeader("filename", file.getName());
            httpPost.addHeader("fileSize", Long.toString(file.length()));
            CloseableHttpResponse response = null;
            System.out.println("Starting File Upload!!");
            try {
                long time = System.currentTimeMillis();
                response = client.execute(httpPost);
                System.out.println("Time Taken :" + (System.currentTimeMillis() - time));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            if (response != null) {

                System.out.println(response.getStatusLine().getStatusCode());
                System.out.println("Number of bytes transferred: " + bytesTransferred.getBytesTransferred());
                assert response.getStatusLine().getStatusCode() == 200;
                System.out.println("Accessing content range");
                System.out.println(Arrays.toString(response.getHeaders("Content-Range")));
                Long contentRange = Long.parseLong(response.getFirstHeader("Content-Range").getValue());
                System.out.println("Converted value " + contentRange);
                System.out.println("Accessing builder response handler");
                String responseString = new BasicResponseHandler().handleResponse(response);
                System.out.println(responseString);
                client.close();

            }
        } else {
            System.out.println("Complete file exists on server");
        }
    }


}
