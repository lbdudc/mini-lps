/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader;

import es.udc.lbd.gema.lps.component.file_uploader.custom.TemporaryFileJSON;
import es.udc.lbd.gema.lps.component.file_uploader.exceptions.UploadException;
import es.udc.lbd.gema.lps.component.file_uploader.exceptions.UploadFileNotProvidedException;
import es.udc.lbd.gema.lps.component.file_uploader.file_uploaders.FileUploadGeneric;

import java.io.File;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class UploadService {

    @Autowired
    private FileUploadGeneric fileUploader;

    /**
     * Stores the file in a temporary folder. <br>
     */
    public TemporaryFileJSON uploadFile(MultipartFile multipartFile) throws UploadException {
        TemporaryFileJSON result = new TemporaryFileJSON();

        // File is mandatory
        if (multipartFile == null) {
            throw new UploadFileNotProvidedException();
        }


        File tmpFile = fileUploader.saveTemporaryFile(multipartFile);
        result.setTemporaryFile(tmpFile.getName());
        result.setFilename(FileUploadUtils.normaliceFileName(multipartFile.getOriginalFilename()));
        return result;
    }

    public TemporaryFileJSON uploadPermanentFile(
        MultipartFile multipartFile, Long galleryId, Long imageId) throws UploadException {
        TemporaryFileJSON result = new TemporaryFileJSON();

        // File is mandatory
        if (multipartFile == null) {
            throw new UploadFileNotProvidedException();
        }

        String subFolder = "galleries" + File.separator + galleryId;

        File file = fileUploader.savePermanentFile(multipartFile, subFolder, imageId.toString());
        result.setTemporaryFile(file.getName());
        result.setFilename(FileUploadUtils.normaliceFileName(multipartFile.getOriginalFilename()));
        return result;
    }

    public TemporaryFileJSON replacePermanentFile(
        MultipartFile multipartFile, Long galleryId, Long imageId)
        throws UploadFileNotProvidedException {
        TemporaryFileJSON result = new TemporaryFileJSON();

        // File is mandatory
        if (multipartFile == null) {
            throw new UploadFileNotProvidedException();
        }

        String subFolder = "galleries" + File.separator + galleryId;

        File file = fileUploader.replacePermanentFile(multipartFile, subFolder, imageId.toString());
        result.setTemporaryFile(file.getName());
        result.setFilename(FileUploadUtils.normaliceFileName(multipartFile.getOriginalFilename()));
        return result;
    }
}
/*% } %*/
