  /*% if (feature.DM_DI_DF_GeoTIFF) { %*/

package es.udc.lbd.gema.lps.component.standard_data_importer;

import es.udc.lbd.gema.lps.component.file_uploader.file_uploaders.FileUploadImport;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public abstract class AbstractFileImportService {

  protected final FileUploadImport fileUploadImport;

  @Autowired
  public AbstractFileImportService(FileUploadImport fileUploadImport) {
    this.fileUploadImport = fileUploadImport;
  }

  public final void importFile(MultipartFile multipartFile) throws IOException {
    validateFile(multipartFile);
    File file = saveTemporaryFile(multipartFile);
    processFile(file, multipartFile.getOriginalFilename());
    cleanup(file);
  }

  protected abstract void validateFile(MultipartFile file) throws IOException;

  private File saveTemporaryFile(MultipartFile multipartFile) {
    return fileUploadImport.saveTemporaryFile(multipartFile);
  }

  protected abstract void processFile(File file, String filename) throws IOException;

  private void cleanup(File file) {
    fileUploadImport.deleteTemporaryFile(FilenameUtils.getBaseName(file.getAbsolutePath()));
  }
}


  /*% } %*/
