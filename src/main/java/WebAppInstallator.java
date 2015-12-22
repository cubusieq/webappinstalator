

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppInstallator {
	static Logger logger = LoggerFactory.getLogger(WebAppInstallator.class);

	public WebAppInstallator(CommandLine cl) {
		logger.debug(cl.getOptionValue("dst"));
		boolean forceInstall = false;
		if (cl.hasOption('f')) {
			forceInstall = true;
		}
		if(this.extractWar(cl.getOptionValue("dst"), forceInstall)){
			logger.info("Application installed successfully in "+ cl.getOptionValue("dst"));
		}
	}

	private boolean extractWar(String destinationPath, boolean forceInstall) {

		Path destination = Paths.get(destinationPath);
		if (Files.notExists(destination)) {
			destination.toFile().mkdir();
		}

		if (destination.toFile().list().length > 0 && forceInstall == false) { //Directory not empty and flag not set.
			logger.info(
					"Provided directory is not empty! Check the directory and rerun installer with flag '-f' to overwrite files.");
			return false;
		} else if (destination.toFile().list().length > 0 && forceInstall == true) { //Directory not empty and flag set.
			try {
				FileUtils.cleanDirectory(destination.toFile());
			} catch (IOException ex) {
				logger.error("Cannot delete directory " + destinationPath + ". Check if directory is locked.");
				logger.error(ex.getMessage(), ex);
			}
		}
		
		if (Files.isDirectory(destination)) {
			File warFile = new File("target.war");
			try {
			
				OutputStream outputStream = new FileOutputStream(warFile);
				IOUtils.copy( this.getClass().getResourceAsStream("target.war"),outputStream );
				JarFile targetWar = new JarFile(warFile);
				Enumeration<JarEntry> enumEntries = targetWar.entries();
			
				while (enumEntries.hasMoreElements()) {
					JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
					logger.info(file.getName());
					File f = new File(destinationPath + java.io.File.separator + file.getName());
					if (file.isDirectory()) {
						f.mkdir();
						continue;
					}
					InputStream is = targetWar.getInputStream(file);
					FileOutputStream fos = new FileOutputStream(f);
					while (is.available() > 0) {
						fos.write(is.read());
					}
					fos.close();
					is.close();
				}
				targetWar.close();
				outputStream.close();
				warFile.delete();
				return true;
			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
				return false;
			}finally
			{
				warFile.delete();
			}
		} else {
			logger.error("Provided destination path is not available/is not a directory!");
			return false;
		}
	}

	public static void main(String[] args) {
		try {
			final CommandLineParser parser = new DefaultParser();
			Options options = new Options();
			Option dst = Option.builder("dst").hasArg().required().argName("dst").desc("Path where app will be extracted.")
					.build();
			Option f = Option.builder("f").hasArg(false)
					.desc("Force unpacking. True if overwrite files in dst directory.").build();
			options.addOption(dst);
			options.addOption(f);
			CommandLine parsedCl = parser.parse(options, args);
			if(parsedCl.hasOption("dst")){
			new WebAppInstallator(parsedCl);}else{
				logger.error("Provide option -dst which should contain path where application will be installed.");
			}
		} catch (ParseException ex) {
			logger.error(ex.getMessage(), ex);
			logger.error("Check -dst option, and try again.");
		}

	}

}
