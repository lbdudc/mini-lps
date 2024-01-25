/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader;

import es.udc.lbd.gema.lps.component.file_uploader.exceptions.FileNotFoundException;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

@Service
@Transactional
public class DownloadService {

    private final Logger logger = LoggerFactory.getLogger(DownloadService.class);

    @Autowired
    private FileUtil fileUtil;

    public void downloadIGImage(HttpServletResponse response, String uri, String filename)
      throws IOException, FileNotFoundException {

      File galleryDirectory = fileUtil.getAbsolutePath(uri);
      File downloadFile = null;

      if (galleryDirectory == null || !galleryDirectory.exists()) {
        throw new FileNotFoundException(
          "Gallery \"" + uri + "\" does not exist" , HttpStatus.NOT_FOUND);
      }

      for (File file : galleryDirectory.listFiles()) {
        if (FilenameUtils.removeExtension(file.getName()).equals(filename)) {
          downloadFile = file;
        }
      }

      if (downloadFile != null && downloadFile.exists() && !downloadFile.isDirectory()) {
        OutputStream outStream;
        try (FileInputStream inputStream = new FileInputStream(downloadFile)) {

          // get MIME type of the file
          String mimeType = Files.probeContentType(downloadFile.toPath());
          if (mimeType == null) {
            // set to binary type if MIME mapping not found
            mimeType = "application/octet-stream";
          }

          // set content attributes for the response
          response.setContentType(mimeType);
          response.setContentLength((int) downloadFile.length());

          // set headers for the response
          String headerKey = "Content-Disposition";
          String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
          response.setHeader(headerKey, headerValue);

          // get output stream of the response
          outStream = response.getOutputStream();

          byte[] buffer = new byte[4096];
          int bytesRead = -1;

          // write bytes read from the input stream into the output stream
          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
          }

          inputStream.close();
        }
        outStream.close();
      } else {
        throw new FileNotFoundException("File \"" + filename + "\" not found", HttpStatus.NOT_FOUND);
      }
    }
}
/*% } %*/
