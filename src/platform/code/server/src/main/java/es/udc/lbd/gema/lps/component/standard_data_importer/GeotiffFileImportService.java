  /*% if (feature.DM_DI_DF_GeoTIFF) { %*/

package es.udc.lbd.gema.lps.component.standard_data_importer;

import es.udc.lbd.gema.lps.component.file_uploader.file_uploaders.FileUploadImport;
import es.udc.lbd.gema.lps.config.GeoServerProperties;
import it.geosolutions.geoserver.rest.HTTPUtils;
import java.io.File;
import java.io.IOException;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class GeotiffFileImportService extends AbstractFileImportService {

  GeoServerProperties geoServerProperties;

  private static final Logger logger = LoggerFactory.getLogger(GeotiffFileImportService.class);

  public GeotiffFileImportService(
      FileUploadImport fileUploadImport, GeoServerProperties geoServerProperties) {
    super(fileUploadImport);
    this.geoServerProperties = geoServerProperties;
  }

  @Override
  protected void validateFile(MultipartFile file) throws IllegalArgumentException {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("The file is empty.");
    }

    String filename = file.getOriginalFilename();
    if (filename == null) {
      throw new IllegalArgumentException(
          "Unsupported file format. Only .tif and .zip are accepted.");
    }
  }

  @Override
  protected void processFile(File file, String filename)
      throws IOException, FileUploadException, IllegalArgumentException {
    if (filename.endsWith(".tif")) {
      handleGeoTiffFile(file, filename);
    } else if (filename.endsWith(".zip")) {
      handleZipFile(file, filename);
    } else {
      throw new IllegalArgumentException(
          "Unsupported file format. Only .tif and .zip are accepted.");
    }
  }

  private void handleGeoTiffFile(File geoTiffFile, String filename)
      throws IOException, FileUploadException {

    String cleanFilename = "t_" + filename.replaceAll("(?i)\\.tif$", "").toLowerCase();
    String url =
        geoServerProperties.getUrl()
            + "/rest/workspaces/"
            + geoServerProperties.getWorkspace()
            + "/coveragestores/"
            + cleanFilename
            + "/file.geotiff";

    String response =
        HTTPUtils.put(
            url,
            geoTiffFile,
            "image/tiff",
            geoServerProperties.getUser(),
            geoServerProperties.getPassword());
    if (response != null) {
      logger.info("GeoTIFF store '{}' successfully created ", cleanFilename);
    } else {
      logger.warn("Failed to create GeoTIFF store '{}'", cleanFilename);
      throw new FileUploadException("GeoTIFF store creation failed for: " + cleanFilename);
    }
  }

  private void handleZipFile(File zipFile, String zipName) throws IOException, FileUploadException {

    String cleanFilename = "t_" + zipName.replaceAll("(?i)\\.zip$", "").toLowerCase();

    String url =
        geoServerProperties.getUrl()
            + "/rest/workspaces/"
            + geoServerProperties.getWorkspace()
            + "/coveragestores/"
            + cleanFilename
            + "/file.imagemosaic";

    String response =
        HTTPUtils.put(
            url,
            zipFile,
            "application/zip",
            geoServerProperties.getUser(),
            geoServerProperties.getPassword());
    if (response != null) {
      logger.info("GeoTIFF store '{}' successfully created ", cleanFilename);
    } else {
      logger.warn("Failed to create GeoTIFF store '{}'", cleanFilename);
      throw new FileUploadException("GeoTIFF store creation failed for: " + cleanFilename);
    }
  }
}

  /*% } %*/
