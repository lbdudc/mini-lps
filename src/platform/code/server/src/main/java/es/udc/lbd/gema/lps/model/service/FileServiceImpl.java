/*% if (feature.MV_MS_GJ_Cached) { %*/
/*% var entities =  data.dataModel.entities
      .filter(function(context){
          return !context.abstract;
      })
      .map(function(en) {
        return {
            context: en
        };
      });
%*/
package es.udc.lbd.gema.lps.model.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.udc.lbd.gema.lps.web.rest.custom.FeatureCollectionJSON;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {

  private static final String DIRECTORY = "/src/main/resources/spatial/";

  /*% entities.forEach(function(en) { %*/
  /*% if (checkEntityContainsGeographicProperties(en.context)) { %*/
  @Inject private /*%= normalize(en.context.name, true) %*/Service /*%= normalize(en.context.name) %*/Service;
  /*% } %*/
  /*% }); %*/

  @Override
  public void writeSpatialFiles() throws IOException {
    /*% entities.forEach(function(en) {
        var geographicPropertyNames = getEntityPropertyNamesOfGeographicTypes(en.context);
        geographicPropertyNames.forEach(function(geoPropertyName) {
    %*/
    FeatureCollectionJSON /*%= normalize(en.context.name) %*/ = /*%= normalize(en.context.name) %*/Service.get/*%= normalize(geoPropertyName, true) %*/(false, null/*% if (feature.MV_T_F_BasicSearch) { %*/, null/*% } %*/);
    writeDataToFile(/*%= normalize(en.context.name) %*/, "/*%= normalize(en.context.name) %*/.json");

    /*% }); %*/
    /*% }); %*/
  }

  private void writeDataToFile(FeatureCollectionJSON featureCollection, String fileName)
    throws IOException {
    String rootPath = System.getProperty("user.dir");
    File file = new File(rootPath + DIRECTORY + fileName);
    file.createNewFile();
    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(file, featureCollection);
  }
}
/*% } %*/
