/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader;

import es.udc.lbd.gema.lps.model.service.exceptions.AppRuntimeException;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

public abstract class FileUploader {

    final Logger logger = LoggerFactory.getLogger(FileUploader.class);

    public static final String DEFAULT_PREFIX = "TMP"; // Al least 3 characters
    public static final String SPLITTER = "-";

    @Value("${server.uploads}")
    public String uploadsPathRaw;

    @Value("${server.temporaryUploads}")
    public String temporaryPathRaw;

    // Singletone temporary path of each FileUploader instance
    private static File temporaryPath = null;

    @Inject
    protected FileUtil fileUtil;

    /**
     * @return Temporary uploads path
     */
    protected abstract String getFolder();

    /**
     * @return Returns temporary path of file uploader instance. (Singletone implementation)
     */
    protected File getTemporaryPath() {
        if (temporaryPath == null) {
            temporaryPath = getTemporaryPath(StringUtils.isNotBlank(getFolder())? File.separator + getFolder() : "");
        }
        return temporaryPath;
    }

    /**
     * @return File uploads path
     */
    public String getUploadsPath() {
        // TODO: Support absolute and relative folders
        return uploadsPathRaw;
    }

    /**
     * Gets temporary path with 'subfolder'.
     * @param subfolder The subfolder, this can be useful  for different components.
     * @return The path
     */
    protected File getTemporaryPath(String subfolder) {
        File folder = fileUtil.getAbsolutePath(temporaryPathRaw + subfolder);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new AppRuntimeException("Unable to create temporary folder '" + folder.getPath() + "'", e);
        }

        return folder;
    }

    protected File getUploadsPath(String subfolder) {
        File folder = fileUtil.getAbsolutePath(uploadsPathRaw + subfolder);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new AppRuntimeException(
                "Unable to create temporary folder '" + folder.getPath() + "'", e);
        }

        return folder;
    }

    /**
     * Saves a temporary file. <br>
     * Is guaranteed that tThe file denoted by the returned abstract pathname did not exist before this method was invoked.
     *
     * @param file
     *            Original file
     * @param folder
     *            The directory in which the file is to be created, or null if the default temporary-file directory is to be used
     * @return Temporary file created
     * @throws IOException
     */
    protected File saveTemporaryFile(MultipartFile file, File folder) throws IOException {

        File tempFile = null;

        try {
            String fileExtension = "." + FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
            tempFile = File.createTempFile(DEFAULT_PREFIX + SPLITTER, fileExtension, folder);
        } catch (IOException e) {
            throw new AppRuntimeException("Unable to create a file in folder: " + folder.getPath(), e);
        }

        try {
            file.transferTo(tempFile);
        } catch (IllegalStateException | IOException e) {
            logger.error("Error saving multipart file " + file.getName() + " in folder " + folder.getPath(), e);
            throw e;
        }

        return tempFile;
    }

    protected File savePermanentFile(MultipartFile file, File folder, String filename)
        throws IOException {

        File storedFile = null;
        String fileExtension =
            "." + FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();

        storedFile = new File(folder.getAbsolutePath() + File.separator + filename + fileExtension);

        try {
            file.transferTo(storedFile);
        } catch (IllegalStateException | IOException e) {
            logger.error(
                "Error saving multipart file " + file.getName() + " in folder " + folder.getPath(), e);
            throw e;
        }

        return storedFile;
    }

    protected File replacePermanentFile(MultipartFile file, File folder, String filename)
        throws IOException {

        File storedFile = null;
        File newFile = null;
        String fileExtension =
            "." + FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();

        newFile = new File(folder.getAbsolutePath() + File.separator + filename + fileExtension);

        File[] galleryFiles = folder.listFiles();

        for (File galleryFile : galleryFiles) {
            if (FilenameUtils.removeExtension(galleryFile.getName()).equals(filename)) {
                storedFile = galleryFile;
                storedFile.renameTo(newFile);
                break;
            }
        }

        try {
            file.transferTo(newFile);
        } catch (IllegalStateException | IOException e) {
            logger.error(
                "Error saving multipart file " + file.getName() + " in folder " + folder.getPath(), e);
            throw e;
        }

        return storedFile;
  }


}
/*% } %*/
