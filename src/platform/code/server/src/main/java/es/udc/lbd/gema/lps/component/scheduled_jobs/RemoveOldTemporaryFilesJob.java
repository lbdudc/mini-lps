/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.scheduled_jobs;

import java.io.IOException;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import es.udc.lbd.gema.lps.component.file_uploader.CronTasks;
import org.springframework.stereotype.Component;

@Component
public class RemoveOldTemporaryFilesJob {

  @Inject private CronTasks cronTasks;

  private final Logger log = LoggerFactory.getLogger(RemoveOldTemporaryFilesJob.class);

  public void execute() {
    try {
      cronTasks.removeOldTemporaryFiles();
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }
}
/*% } %*/
