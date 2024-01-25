package es.udc.lbd.gema.lps.config;

import org.springframework.context.annotation.Configuration;
/*% if (feature.T_FileUploader) { %*/
import es.udc.lbd.gema.lps.component.scheduled_jobs.*;
/*% } %*/
import jakarta.inject.Inject;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import java.util.Map;

@Configuration
@EnableScheduling
public class SpringScheduler {

  /*% if (feature.T_FileUploader) { %*/
  private final String DAILY_AT_SIX = "0 0 6 * * *";
  @Inject private RemoveOldTemporaryFilesJob removeOldTemporaryFilesJob;

  /*% } %*/

  /*% if (feature.MV_MS_GJ_Cached) { %*/
  @Inject private GenerateSpatialFilesJob generateSpatialFilesJob;

  /*% } %*/

  /*% if (feature.T_FileUploader) { %*/
  @Scheduled(cron = DAILY_AT_SIX)
  public void removeOldTemporaryFiles() {
    removeOldTemporaryFilesJob.execute();
  }
  /*% } %*/

  /*% if (feature.MV_MS_GJ_Cached) { %*/
  @PostConstruct
  public void generateSpatialFiles() {
    generateSpatialFilesJob.execute();
  }
  /*% } %*/
}
