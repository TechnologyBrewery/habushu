package org.bitbucket.cpointe.habushu.pythondownload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bitbucket.cpointe.habushu.HabushuException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mspalding
 *	This class is to contain all the code needed to download the Python Package.
 */
public class FileDownLoad {
	
	/**
	 * The standard Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(FileDownLoad.class);

    /**
     * Empty Constructor.
     */
    public FileDownLoad(){
    }

    /**
     * 
     * This will go out and download the pythong package necessary.
     * 
     * @param downloadUrl
     * @param destination
     * @throws HabushuException
     */
    public void download(String downloadUrl, String destination) throws HabushuException {
        System.setProperty("https.protocols", "TLSv1.2");
        String fixedDownloadUrl = downloadUrl;
        try {
            fixedDownloadUrl = FilenameUtils.separatorsToUnix(fixedDownloadUrl);
            URI downloadURI = new URI(fixedDownloadUrl);
            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                FileUtils.copyFile(new File(downloadURI), new File(destination));
            }
            else {
                CloseableHttpResponse response = execute(fixedDownloadUrl);
                int statusCode = response.getStatusLine().getStatusCode();
                if(statusCode != 200){
                    throw new HabushuException("Got error code "+ statusCode +" from the server.");
                }
                new File(FilenameUtils.getFullPathNoEndSeparator(destination)).mkdirs();
                ReadableByteChannel rbc = Channels.newChannel(response.getEntity().getContent());
                FileOutputStream fos = new FileOutputStream(destination);
                try {
	                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }	catch (IOException e) {
                	LOGGER.error("Failed to download Python package.");
                	throw new HabushuException("Failed to download Python.", e);
                }	finally	{
                	fos.close();
                }
            }
        } catch (IOException | URISyntaxException e) {
        	LOGGER.error("Failed to download Python package.");
            throw new HabushuException("Could not download " + fixedDownloadUrl, e);
        }
    }

    /**
     * @param requestUrl
     * @return
     * @throws IOException
     */
    private CloseableHttpResponse execute(String requestUrl) throws IOException {
        CloseableHttpResponse response;
        response = buildHttpClient(null).execute(new HttpGet(requestUrl));
        return response;
    }

    /**
     * @param credentialsProvider
     * @return
     */
    private CloseableHttpClient buildHttpClient(CredentialsProvider credentialsProvider) {
    	return HttpClients.custom()
    			.disableContentCompression()
    			.useSystemProperties()
    			.setDefaultCredentialsProvider(credentialsProvider)
    			.build();
    }
}