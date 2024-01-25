/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CronTasks {

    @Autowired
    private FileUtil fileUtil;

    @Value("${server.temporaryUploads}")
    public String temporaryPathRaw;

    private final static Logger logger = LoggerFactory.getLogger(CronTasks.class);

    public CronTasks() {
    }

    public void removeOldTemporaryFiles() throws IOException {

        logger.debug("Removing old temporary files");

        File f = fileUtil.getAbsolutePath(temporaryPathRaw);

        if (f.exists()) {
            if (f.isDirectory()) {
                logger.info("[Clean temporary folder] Cleaning: '" + f.getAbsolutePath() + "' folder.");

                // Set age of files to be deleted
                Calendar deleteOlderThan = Calendar.getInstance();
                deleteOlderThan.add(Calendar.DAY_OF_MONTH, -1);

                int deletedItems = removeFilesAndFolders(f, deleteOlderThan);
                logger.info("[Clean temporary folder] Old temporary files/folders deleted: " + deletedItems);
            } else {
                logger.info("[Clean temporary folder] '" + f.getAbsolutePath() + "' is not a folder.");
            }
        } else {
            logger.info("[Clean temporary folder] Folder: '" + f.getAbsolutePath() + "' not found.");
        }
    }

    private int removeFilesAndFolders(File folder, Calendar deleteOlderThan) throws IOException {
        int deletedItems = 0;

        for (String itemName : folder.list()) {

            File item = new File(folder + File.separator + itemName);

            if (item.isDirectory()) {
               logger.debug("    - '" + item.getAbsolutePath() + "' is a directory");
                deletedItems += removeFilesAndFolders(item, deleteOlderThan);
                if (item.list().length == 0) {
                   logger.debug("    - '" + item.getAbsolutePath() + "' is an empty directory => Deleting");
                    FileUploadUtils.removeFileOrFolder(item);
                    deletedItems++;
                    logger.debug("    - '" + item.getAbsolutePath() + "' deleted.");
                }
            } else {
               logger.debug("    - '" + item.getAbsolutePath() + "' is a file");
                BasicFileAttributes attrs = Files.readAttributes(item.toPath(), BasicFileAttributes.class);
                Calendar itemCreationTime = Calendar.getInstance();
                itemCreationTime.setTimeInMillis(attrs.creationTime().toMillis());
                if (itemCreationTime.before(deleteOlderThan)) {
                   logger.debug("    - '" + item.getAbsolutePath() + "' was created more than one day before today => Deleting");
                    FileUploadUtils.removeFileOrFolder(item.getAbsoluteFile());
                    deletedItems++;
                    logger.debug("    - '" + item.getAbsolutePath() + "' deleted.");
                } else {
                   logger.debug("    - '" + item.getAbsolutePath() + "' is a recent file, preserving it.");
                }
            }
        }
        return deletedItems;
    }

}
/*% } %*/
