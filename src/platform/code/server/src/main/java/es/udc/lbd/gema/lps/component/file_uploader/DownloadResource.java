/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader;

import es.udc.lbd.gema.lps.component.file_uploader.exceptions.FileNotFoundException;

import java.io.File;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by sfernandez on 22/11/2016.
 */
@RestController
@RequestMapping(DownloadResource.DOWNLOAD_URL)
public class DownloadResource {
    public static final String DOWNLOAD_URL = "/api/download";

    @Autowired
    private DownloadService downloadService;

    @Value("${server.uploads}")
    private String uploadPath;
    @RequestMapping(value = "/gallery/{galleryId}/{imageId}", method = RequestMethod.GET)
    public void downloadGalleryFile(
      @PathVariable Long galleryId, @PathVariable Long imageId, HttpServletResponse response)
      throws IOException, FileNotFoundException {
      downloadService.downloadIGImage(
        response, uploadPath + "galleries" + File.separator + galleryId, imageId.toString());
    }

}
/*% } %*/
