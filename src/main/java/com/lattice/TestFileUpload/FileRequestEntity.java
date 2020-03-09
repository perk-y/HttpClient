package com.lattice.TestFileUpload;

import org.apache.commons.httpclient.methods.RequestEntity;

import java.io.*;

public class FileRequestEntity implements RequestEntity {

    private File file;

    public FileRequestEntity(File file) {
        super();
        this.file = file;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void writeRequest(OutputStream out) throws IOException {
        InputStream in = new FileInputStream(this.file);
        try {
            int l;
            byte[] buffer = new byte[1024];
            while ((l = in.read(buffer)) != -1) {
                out.write(buffer, 0, l);
            }
        } finally {
            in.close();
        }

    }

    @Override
    public long getContentLength() {
        return file.length();
    }

    @Override
    public String getContentType() {
        return "text/plain; charset=UTF-8";
    }
}
