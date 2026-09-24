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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class GeotiffFileImportService extends AbstractFileImportService {

  GeoServerProperties geoServerProperties;

  /* the GeoServer layer name to publish under, or null to derive it from the file name */
  private final String layerName;

  private static final Logger logger = LoggerFactory.getLogger(GeotiffFileImportService.class);

  @Autowired
  public GeotiffFileImportService(
      FileUploadImport fileUploadImport, GeoServerProperties geoServerProperties) {
    this(fileUploadImport, geoServerProperties, null);
  }

  public GeotiffFileImportService(
      FileUploadImport fileUploadImport,
      GeoServerProperties geoServerProperties,
      String layerName) {
    super(fileUploadImport);
    this.geoServerProperties = geoServerProperties;
    this.layerName = layerName;
  }

  /**
   * The name of the coverage store, and of the coverage and layer GeoServer creates in it. An
   * explicit layer name is used as it is: it is the one the generated client asks GeoServer for.
   * Without one it is derived from the file name (t_ + the lowercase file name).
   */
  private String storeName(String filename, String extensionRegex) {
    return layerName != null
        ? layerName
        : "t_" + filename.replaceAll(extensionRegex, "").toLowerCase();
  }

  private String coverageParams() {
    return layerName != null ? "?coverageName=" + layerName + "&configure=first" : "";
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
    String lowerName = filename.toLowerCase();
    if (lowerName.endsWith(".tif") || lowerName.endsWith(".tiff")) {
      handleGeoTiffFile(file, filename);
    } else if (lowerName.endsWith(".zip")) {
      handleZipFile(file, filename);
    } else {
      throw new IllegalArgumentException(
          "Unsupported file format. Only .tif and .zip are accepted.");
    }
  }

  private void handleGeoTiffFile(File geoTiffFile, String filename)
      throws IOException, FileUploadException {

    String cleanFilename = storeName(filename, "(?i)\\.tiff?$");
    String url =
        geoServerProperties.getUrl()
            + "/rest/workspaces/"
            + geoServerProperties.getWorkspace()
            + "/coveragestores/"
            + cleanFilename
            + "/file.geotiff"
            + coverageParams();

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

    String cleanFilename = storeName(zipName, "(?i)\\.zip$");

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
