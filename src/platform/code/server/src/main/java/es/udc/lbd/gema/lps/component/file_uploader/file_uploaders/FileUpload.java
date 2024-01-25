/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader.file_uploaders;

import es.udc.lbd.gema.lps.component.file_uploader.FileUploadUtils;
import es.udc.lbd.gema.lps.component.file_uploader.FileUploader;
import es.udc.lbd.gema.lps.model.service.exceptions.AppRuntimeException;

import java.io.File;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public abstract class FileUpload extends FileUploader {

    /**
     * Uploads the file to a temporary file inside temporary import folder
     *
     * @param file File to be saved
     * @return Saved file
     */
    public File saveTemporaryFile(MultipartFile file) {
        try {
            return super.saveTemporaryFile(file, getTemporaryPath());
        } catch (IOException e) {
            throw new AppRuntimeException("Unable to create temporary file ", e);
        }
    }

    /**
     * Retrieves the temporary file in import temporary folder
     *
     * @param fileName Name of temporary file (Without temporary path)
     * @return Temporary file
     */
    public File getTemporaryFile(String fileName) {
        return new File(getTemporaryPath(), fileName);
    }

    public File savePermanentFile(MultipartFile file, String subFolder, String filename) {
        try {
            return super.savePermanentFile(file, getUploadsPath(subFolder), filename);
        } catch (IOException e) {
            throw new AppRuntimeException("Unable to create temporary file ", e);
        }
    }

    public File replacePermanentFile(MultipartFile file, String subFolder, String filename) {
        try {
            return super.replacePermanentFile(file, getUploadsPath(subFolder), filename);
        } catch (IOException e) {
            throw new AppRuntimeException("Unable to create temporary file ", e);
        }
    }

    /**
     * Deletes the file in import temporary folder
     *
     * @param fileName Name of the temporary file
     */
    public void deleteTemporaryFile(String fileName) {
        File file = new File(getTemporaryPath() + File.separator + fileName);
        FileUploadUtils.removeFileOrFolder(file);
    }
}
/*% } %*/
