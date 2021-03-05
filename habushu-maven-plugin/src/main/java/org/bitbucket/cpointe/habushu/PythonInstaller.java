package org.bitbucket.cpointe.habushu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.bitbucket.cpointe.habushu.pythondownload.FileDownLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PythonInstaller {
	
	/**
	 * The version we are installing of python.
	 */
	private final String VERSION = "3.9.2";
	
	/**
	 * The download location of python to be download.
	 */
	private final String DEFAULT_PYTHON_DOWNLOAD_ROOT = "https://www.python.org/ftp/python/3.9.2/python-3.9.2-macos11.pkg";
	
	/**
	 * Object to lock the execution
	 */
	private static final Object LOCK = new Object();
	
	/**
	 * Standard logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(PythonInstaller.class);
	
	/**
	 * The directory to where python will be downloaded to.
	 */
	private File pythonDownload = new File(System.getProperty("user.home")+"/Downloads/python-3.9.2-macos11.pkg");
	
	/**
	 * This will download the package for python.
	 */
	private final FileDownLoad fileDownLoader;
	
	/**
	 * Regex to determine if the correct version of python is installed, need to be at least 3.x.x
	 */
	private final String checkVersion = "Python 3[.]\\d[.]\\d";
	
	/**
	 * @param fileDownLoader
	 */
	public PythonInstaller(FileDownLoad fileDownLoader) {
		super();
		this.fileDownLoader = fileDownLoader;
	}

	/**
	 * This will check if python is installed correctly and then install python if necessary.
	 */
	public void checkInstallPython() {
		synchronized (LOCK) {
			try {
				String cmd = "python --version";
				Process proc = Runtime.getRuntime().exec(cmd);
				BufferedReader buff = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				proc = Runtime.getRuntime().exec(cmd);
				String version;
				boolean pythonInstall = false;
				while((version=buff.readLine()) != null) {
					if(version.matches(checkVersion))
						pythonInstall=true;
				}
				if(pythonInstall) {
					logger.info("Python 3 already installed.");
				}	
				else	{
					if(!pythonDownload.exists())	{
						this.fileDownLoader.download(this.DEFAULT_PYTHON_DOWNLOAD_ROOT, pythonDownload.getPath());
					}
					install();
				}
			}	catch (IOException e) {
				logger.error("Failded to check the version of python correctly.");
				throw new HabushuException("Failed to check version of python.", e);
			}
		}
	}	
	
	/**
	 * The purpose of this is to install python once the package has been successfully downloaded.
	 * 
	 */
	private void install() {
		synchronized (LOCK) {
			try {
		        logger.info("Installing python version {}", this.VERSION);
		        String command = "open " + pythonDownload;
		        Process child = Runtime.getRuntime().exec(command);
		        int result = child.waitFor();
		        if(result != 0) {
		        	throw new HabushuException("Child proccess failed to execute correctly; exited with " + result);
		        }
		    } catch (IOException e) {
		    	logger.error("Failed to install python");
		        throw new HabushuException("Failed to install python.", e);
		    } catch (InterruptedException e) {
		    	logger.error("Failed to wait for the install to complete.");
		    	throw new HabushuException("Failed to wait for the install to complete.", e);
		    }
		}
	}
}
