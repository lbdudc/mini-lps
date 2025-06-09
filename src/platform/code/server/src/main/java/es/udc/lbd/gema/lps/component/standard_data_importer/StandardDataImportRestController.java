/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer;

import es.udc.lbd.gema.lps.component.file_uploader.file_uploaders.FileUploadImport;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.ImportFileJSON;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.ParseFormatJSON;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.FileNotSupportedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.TypeNotSupportedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportFileNotSupportedException;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;

/*% if (feature.DM_DI_DF_Shapefile) { %*/
import org.apache.commons.io.FilenameUtils;
/*% } %*/
/*% if (feature.DM_DI_DF_GeoTIFF) { %*/
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import es.udc.lbd.gema.lps.config.GeoServerProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
/*% } %*/
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping(StandardDataImportRestController.SDI_REST_URL)
public class StandardDataImportRestController {
  static final String SDI_REST_URL = "/api/import";

  /*% if (feature.DM_DI_DF_Shapefile) { %*/

  @Inject
  private GisImportService gisImportService;
  /*% } %*/
  /*% if (feature.DM_DI_DF_GeoTIFF) { %*/
  @Inject
  GeoServerProperties geoServerProperties;
  /*% } %*/

  @Inject
  private FileUploadImport fileUploadImport;

  /**
   * Read the first line of the file, it can be provided in the body of the
   * request or via an URL
   */
  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImportFileJSON obtainHeader(MultipartHttpServletRequest request)
    throws ImportException {
    Set<StandardDataImportService> importers = new HashSet<>();

    /*% if (feature.DM_DI_DF_Shapefile) { %*/
    importers.add(gisImportService);
    /*% } %*/
    for (StandardDataImportService importer : importers) {
      try {
        return importer.uploadAndProcessHeader(request);
      } catch (FileNotSupportedException | TypeNotSupportedException e) {
        // Try with next importer
      }
    }

    throw new ImportFileNotSupportedException();

  }
  /*% if (feature.DM_DI_DF_GeoTIFF) { %*/
  @RequestMapping(value = "/layer", method = RequestMethod.POST)
  public ResponseEntity<String> importGeotiff(@RequestParam("file") MultipartFile multipartFile) {
    try {
      GeotiffFileImportService geotiffFileImportService =
        new GeotiffFileImportService(fileUploadImport, geoServerProperties);
      geotiffFileImportService.importFile(multipartFile);
      return ResponseEntity.ok("Import process completed successfully.");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("An unexpected error occurred during the import process.");
    }
  }
  /*% } %*/
  /**
   * Imports data of the file, it can be provided in the body of the request
   * or via an URL
   */
  @RequestMapping(method = RequestMethod.PUT)
  public void processFile(@Validated @RequestBody ParseFormatJSON parseFormat)
    throws ImportException {
    Set<StandardDataImportService> importers = new HashSet<>();
    /*% if (feature.DM_DI_DF_Shapefile) { %*/
    importers.add(gisImportService);
    /*% } %*/
    for (StandardDataImportService importer : importers) {
      try {
        File file = importer.processFile(parseFormat);
        if (file != null) {
          // Remove tmp file
          fileUploadImport.deleteTemporaryFile(file.getName());
          /*% if (feature.DM_DI_DF_Shapefile) { %*/
          // Remove tmp folder
          if (parseFormat.getType().equals("gis")) {
            fileUploadImport.deleteTemporaryFile(FilenameUtils.getBaseName(file.getAbsolutePath()));
          }
          /*% } %*/
        }
        return;
      } catch (FileNotSupportedException | TypeNotSupportedException e) {
        // Try with next importer
      }
    }
    throw new ImportFileNotSupportedException();
  }
}
/*% } %*/
