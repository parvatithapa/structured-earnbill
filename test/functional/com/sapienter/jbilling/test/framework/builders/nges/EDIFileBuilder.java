package com.sapienter.jbilling.test.framework.builders.nges;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.builders.AbstractBuilder;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by neeraj on 22/4/16.
 */
public class EDIFileBuilder extends AbstractBuilder {

    String source;
    String name;
    String destination;
    Map<String, String> replaceData=new HashMap<String, String>();


    public EDIFileBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public EDIFileBuilder withSource(String source){
        this.source = source;
        return this;
    }

    public EDIFileBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * If destination is provided then it will put the file there else consider it EDI inbound file and call the api
     * uploadEdiFile().
     */
    public EDIFileBuilder withDestination(String destination){
        this.destination = destination;
        return this;
    }

    public EDIFileBuilder replace(String name, String value){
        replaceData.put(name, value);
        return this;
    }


    public void build(){

        try{
            BufferedReader inputStream = new BufferedReader(new FileReader(source));
            File target = new File(System.getProperty("java.io.tmpdir")+File.separator+name);
            // if File doesnt exists, then create it
            FileWriter filewriter = new FileWriter(target.getAbsoluteFile());
            BufferedWriter outputStream= new BufferedWriter(filewriter);
            String line;
            while ((line = inputStream.readLine()) != null) {
                for(String key:replaceData.keySet()){
                    line=line.replace(key, replaceData.get(key));
                }
                outputStream.write(line);
                outputStream.write("\n");
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            if (destination != null) {
                buildFileAtDestination(target);
            } else {
                uploadFileAtServer(target);
            }

        }catch (Exception e){
            new SessionInternalError( String.format("Creating temp file from sample file's content : %s", source), e);
        }

    }

    private void buildFileAtDestination(File file) {
        File destinationDir = new File(destination);

        if (!destinationDir.exists()) {
            if (destinationDir != null && ! destinationDir.exists()) {
                destinationDir.mkdirs();
            }
        }

        // Move file to destination folder.
        File destinationFile = new File(destinationDir + File.separator + name);
        try {
            Files.move(file.toPath(), destinationFile.toPath());
        }catch (IOException ex) {
            new SessionInternalError( String.format("Unable to move file : %s to %s", file.getAbsolutePath(), destinationFile.getAbsolutePath()), ex);
        }
        file.renameTo(new File(destinationDir+File.separator+name));
    }

    private void uploadFileAtServer(File file) {
        api.uploadEDIFile(file);
    }
}
