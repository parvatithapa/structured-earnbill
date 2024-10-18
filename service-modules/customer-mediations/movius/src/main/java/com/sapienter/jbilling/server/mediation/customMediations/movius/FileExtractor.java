package com.sapienter.jbilling.server.mediation.customMediations.movius;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FilenameUtils;

public enum FileExtractor {

    ZIP(".zip") {
        @Override
        public String decompress(File file) throws IOException {
            try (ZipInputStream gzis = new ZipInputStream(new FileInputStream(file.getAbsoluteFile()))) {
                return writeIntoFile(gzis, file.getName());
            }
        }

    },
    GZ(".gz") {
        @Override
        public String decompress(File file)  throws IOException {
            try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(file.getAbsoluteFile()))) {
                return writeIntoFile(gzis, file.getName());
            }
        }
    }; 

    public abstract String decompress(File file)  throws IOException ;

    private final String fileExtention;

    private FileExtractor(String fileExtension) {
        this.fileExtention = fileExtension;
    }

    public String getFileExtention() {
        return fileExtention;
    }

    public String writeIntoFile(InputStream input, String fileName) throws IOException {
        byte[] buffer = new byte[1024];
        File tempFile = File.createTempFile(FilenameUtils.removeExtension(fileName), ".tmp");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            int len;
            while ((len = input.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
        return tempFile.getAbsolutePath();
    }

}
