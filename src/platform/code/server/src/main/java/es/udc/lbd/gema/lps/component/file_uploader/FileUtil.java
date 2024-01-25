/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader;

import es.udc.lbd.gema.lps.model.service.exceptions.AppRuntimeException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletContext;

import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

@Component
public class FileUtil implements ServletContextAware {
    final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private ServletContext servletContext;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    /**
     * @return Gets the base real path of the application
     */
    public String getBasePath() {
        return servletContext.getRealPath("/");
    }

    /**
     * Retrieves a file / folder
     *
     * @param uri
     * @return
     */
    public File getFile(String uri) {
        File file = getAbsolutePath(uri);
        file.getParentFile().mkdirs();
        return file;
    }

    /**
     * Retrieves the unique file in the folder
     *
     * @param uri
     * @return
     */
    public File getFileUnique(String uri) {
        File file = getAbsolutePath(uri);
        List<File> filesInFolder = (List<File>) org.apache.commons.io.FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

        // Only one file expected in the folder
        if (filesInFolder.size() > 1) {
            throw new AppRuntimeException("More than one file found in " + uri);
        }

        File downloadFile = filesInFolder.get(0);

        return downloadFile;
    }


    /**
     * Copies a file into a folder. If not targetName is specified, the original file name will be used
     **/
    public String copyFileToFolder(File file, File folder, String targetName) {
        try {
            String nombreNuevoArchivo = targetName != null ? targetName : file.getName();
            String nombreArchivo = FileUploadUtils.normaliceFileName(nombreNuevoArchivo);
            org.apache.commons.io.FileUtils.copyFile(file, new File(folder.getAbsolutePath() + File.separator + nombreArchivo));
            return nombreArchivo;
        } catch (IOException e) {
            logger.error("Error copying the file '" + file.getPath() + "' to the folder '" + folder.getPath() + "'", e);
            return null;
        }
    }

    /**
     * Converts the path to absolute path if necessary
     *
     * @param pathAsString Absolute or application base relative path
     * @return Absolute path
     */
    public File getAbsolutePath(String pathAsString) {
        File path = new File(pathAsString);
        if (path.isAbsolute()) {
            return path;
        }
        return new File(getBasePath() + pathAsString);
    }
}
/*% } %*/
