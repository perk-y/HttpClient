package com.lattice.TestFileUpload;

import org.apache.commons.httpclient.methods.PostMethod;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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

        long time = System.currentTimeMillis();
       File file = new File("C:\\Users\\Sakshi\\Videos\\Goblin\\Gob2.mp4");
     	//	File file = new File("C:\\Users\\Sakshi\\Videos\\Frozen (2013) [1080p]\\Frozen.2013.1080p.BluRay.x264.YIFY.mp4");

        byte[] fileContent = Files.readAllBytes(file.toPath());
        CloseableHttpClient client = HttpClients.createDefault();

        URIBuilder uribuilder = new URIBuilder("http://localhost:8090/Sangath/forms/files/fileExists");
        uribuilder.setParameter("filename", file.getName()).setParameter("fileSize", Long.toString(file.length()));

        HttpGet post = new HttpGet(uribuilder.build());
        CloseableHttpResponse fileSizeResponse = client.execute(post);
        long bytesWritten = 0;
        String fileResponseString = new BasicResponseHandler().handleResponse(fileSizeResponse);
        System.out.println(fileResponseString);
        if(fileSizeResponse.getStatusLine().getStatusCode() == 206) {

            JSONObject json = new JSONObject(fileResponseString);
            bytesWritten = (Integer) json.get("bytes");
        }
        // check if file already exists on server

        if(fileSizeResponse.getStatusLine().getStatusCode() != 200) {

            HttpPost httpPost = new HttpPost("http://localhost:8090/Sangath/forms/files/inPartsUploadStream");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);

            if (bytesWritten != 0) {
                System.out.println("Number of bytes Skipped");
                System.out.println(inputStream.skip(bytesWritten));
            }
            builder.addBinaryBody("file", inputStream, ContentType.APPLICATION_OCTET_STREAM
                    , "Gob2.mp4");
//           		builder.addBinaryBody("file", inputStream, ContentType.APPLICATION_OCTET_STREAM
//				, "Frozen.2013.1080p.BluRay.x264.YIFY.mp4");


//		File file = new File("C:\\Users\\Sakshi\\Videos\\Frozen (2013) [1080p]\\Frozen.2013.1080p.BluRay.x264.YIFY.mp4");
//		builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM
//				, "Frozen.2013.1080p.BluRay.x264.YIFY.mp4");

            HttpEntity multipart = builder.build();

            // Progress listener - updates task's progress
            BytesTransferred bytesTransferred = new BytesTransferred();
            ProgressEntityWrapper.ProgressListener progressListener =
                    progress -> {
                        System.out.println(progress);
                        bytesTransferred.setBytesTransferred(progress);
                    };

            httpPost.setEntity(new ProgressEntityWrapper(multipart, progressListener));
            httpPost.addHeader("filename", file.getName());
            httpPost.addHeader("fileSize", Long.toString(file.length()));
            CloseableHttpResponse response = null;
            try {
                response = client.execute(httpPost);
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
                System.out.println("Converted value "  +  contentRange);
                System.out.println("Accessing builder response handler");
                String responseString = new BasicResponseHandler().handleResponse(response);
                System.out.println(responseString);
                client.close();
                System.out.println("Time Taken :" + (System.currentTimeMillis() - time));
            }
        }
        else {

            System.out.println("Complete file exists on server");
        }
    }


}
