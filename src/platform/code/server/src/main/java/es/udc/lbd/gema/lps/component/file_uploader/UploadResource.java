/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader;

import es.udc.lbd.gema.lps.component.file_uploader.custom.TemporaryFileJSON;
import es.udc.lbd.gema.lps.component.file_uploader.exceptions.UploadException;
import es.udc.lbd.gema.lps.component.file_uploader.exceptions.UploadFileNotSupportedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(UploadResource.UPLOAD_URL)
public class UploadResource {
    static final String UPLOAD_URL = "/api/upload";

    @Autowired
    private UploadService uploadService;

    /**
     * Uploads the file to a temporary folder
     *
     * @param request Two fields required <br>
     *                <ul>
     *                <li>'file' - {@link MultipartFile} multipartFile</li>
     *                <li>'extensions' - Valid extensions. String separated by commas. Example: ".pdf,.xls,.doc"</li>
     *                </ul>
     * @return Temporary file name (Unique)
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TemporaryFileJSON uploadFile(MultipartHttpServletRequest request) throws UploadException {

        MultipartFile multipartFile = request.getFile("file");
        String fileExtension = "." + StringUtils.substringAfterLast(multipartFile.getOriginalFilename(), ".").toLowerCase();

        List<String> extensions = Arrays.asList(StringUtils.split(request.getParameter("formats"), ','));
        List<String> validExtensions = extensions.stream()
            .map(String::trim)
            .map(String::toLowerCase)
            .collect(Collectors.toList());

        if (!validExtensions.isEmpty() && !validExtensions.contains(fileExtension)) {
            throw new UploadFileNotSupportedException(fileExtension);
        }

        return uploadService.uploadFile(multipartFile);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        path = "/gallery/{galleryId}/{imageId}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TemporaryFileJSON uploadGalleryFile(
        MultipartHttpServletRequest request, @PathVariable Long galleryId, @PathVariable Long imageId)
        throws UploadException {

        MultipartFile multipartFile = request.getFile("file");
        String fileExtension =
            "."
                + StringUtils.substringAfterLast(multipartFile.getOriginalFilename(), ".")
                    .toLowerCase();

        List<String> extensions =
            Arrays.asList(StringUtils.split(request.getParameter("formats"), ','));
        List<String> validExtensions =
            extensions.stream().map(String::trim).map(String::toLowerCase).collect(Collectors.toList());

        if (!validExtensions.isEmpty() && !validExtensions.contains(fileExtension)) {
            throw new UploadFileNotSupportedException(fileExtension);
        }

        return uploadService.uploadPermanentFile(multipartFile, galleryId, imageId);
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        path = "/gallery/{galleryId}/{imageId}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TemporaryFileJSON replaceGalleryFile(
        MultipartHttpServletRequest request, @PathVariable Long galleryId, @PathVariable Long imageId)
        throws UploadException {

        MultipartFile multipartFile = request.getFile("file");
        String fileExtension =
            "."
                + StringUtils.substringAfterLast(multipartFile.getOriginalFilename(), ".")
                    .toLowerCase();

        List<String> extensions =
            Arrays.asList(StringUtils.split(request.getParameter("formats"), ','));
        List<String> validExtensions =
            extensions.stream().map(String::trim).map(String::toLowerCase).collect(Collectors.toList());

        if (!validExtensions.isEmpty() && !validExtensions.contains(fileExtension)) {
            throw new UploadFileNotSupportedException(fileExtension);
        }

        return uploadService.replacePermanentFile(multipartFile, galleryId, imageId);
    }
}
/*% } %*/
