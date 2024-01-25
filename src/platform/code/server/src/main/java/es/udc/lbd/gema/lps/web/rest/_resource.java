/*%@ return data.dataModel.entities
      .filter(function(context){
          return !context.abstract;
      })
      .map(function(en) {
        return {
            fileName: normalize(en.name, true) + 'Resource.java',
            context: en
        };
      });
%*/
/*%
    var geographicPropertyNames = [];
    if (feature.MapViewer) {
        geographicPropertyNames = getEntityPropertyNamesOfGeographicTypes(context);
    }
    var validGeom = (!checkEntityContainsPropertiesOfTypes(context, ["Point", "MultiPoint"]) && geographicPropertyNames.length != 0);
%*/
package es.udc.lbd.gema.lps.web.rest;

/*% if (feature.MV_T_ViewMapAsList) { %*/
import es.udc.lbd.gema.lps.model.domain./*%= normalize(context.name, true) %*/;
/*% } %*/
import es.udc.lbd.gema.lps.model.service.dto./*%= normalize(context.name, true) %*/DTO;
import es.udc.lbd.gema.lps.model.service.dto./*%= normalize(context.name, true) %*/FullDTO;
import es.udc.lbd.gema.lps.model.service./*%= normalize(context.name, true) %*/Service;
import es.udc.lbd.gema.lps.model.views.Views;
/*% if (geographicPropertyNames.length > 0) { %*/
import es.udc.lbd.gema.lps.web.rest.custom.FeatureCollectionJSON;
import es.udc.lbd.gema.lps.web.rest.custom.FeatureJSON;
import java.util.HashMap;
/*% } %*/
/*% if (context.traceable) { %*/
import es.udc.lbd.gema.lps.security.SecurityUtils;
/*% } %*/
import es.udc.lbd.gema.lps.web.rest.util.HeaderUtil;
import es.udc.lbd.gema.lps.web.rest.util.PaginationUtil;
import es.udc.lbd.gema.lps.web.rest.custom.ValidationErrorUtils;
/*% if (feature.MV_T_ViewMapAsList) { %*/
import net.kaczmarzyk.spring.data.jpa.domain.In;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.data.jpa.domain.Specification;
/*% } %*/
import org.apache.commons.lang3.reflect.FieldUtils;
import es.udc.lbd.gema.lps.model.service.exceptions.OperationNotAllowedException;
import es.udc.lbd.gema.lps.model.service.exceptions.NotFoundException;

import es.udc.lbd.gema.lps.web.rest.util.specification_utils.*;
import es.udc.lbd.gema.lps.web.rest.specifications./*%= normalize(context.name, true) %*/Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

/*%
    var pkClass = getEntityProperty(context, 'id').class;
    var pkAutoinc = pkClass.indexOf('autoinc') != -1;
    pkClass = normalize(pkClass.split(' ')[0], true);
    var pkName = normalize(getEntityProperty(context, 'id').name);
%*/
@RestController
@RequestMapping(/*%= normalize(context.name, true) %*/Resource./*%= camelToSnakeCase(normalize(context.name)).toUpperCase() %*/_RESOURCE_URL)
public class /*%= normalize(context.name, true) %*/Resource {
    public final static String /*%= camelToSnakeCase(normalize(context.name)).toUpperCase() %*/_RESOURCE_URL = "/api/entities//*%= pluralize(normalize(context.name)) %*/";

    private static final Logger logger = LoggerFactory.getLogger(/*%= normalize(context.name, true) %*/Resource.class);

    @Inject
    private /*%= normalize(context.name, true) %*/Service /*%= normalize(context.name) %*/Service;

    /*% getOtherRelationshipsNotMultipleAndOwning(data, context).forEach(function(r) { %*/
    @GetMapping("/without//*%= r.property.bidirectional %*/")
    public List</*%= normalize(context.name, true) %*/DTO> getAllWithout/*%= normalize(r.property.bidirectional, true) %*/() {
        return /*%= normalize(context.name) %*/Service.getAllWithout/*%= normalize(r.property.bidirectional, true) %*/();
    }
    /*% }); %*/

    /**
     * Get entities in pages<br>
     * <p>
     * If no <code>pageNum</code> is provided, then all results will be returned in one page
     *
     * @param pageable Contains all information about number page, items per page, order, ...
     * @param search   Contains a text that will be searched in all the properties of the entity
     * @param filters  Static filters to apply
     * @return Paginated entities
     */
    @GetMapping
    public ResponseEntity<Page</*%= normalize(context.name, true) %*/DTO>> getAll (
        @PageableDefault(page = 0, size = 100000, sort = "/*%= context.displayString.substring(1) %*/") Pageable pageable,
        @RequestParam(value = "filters", required = false) List<String> filters,
        @RequestParam(value = "search", required = false) String search/*% if (feature.MV_T_ViewMapAsList) { %*/,
        @Spec(path = "/*%= pkName %*/", params = "ids", paramSeparator = ',', spec = In.class) Specification</*%= normalize(context.name, true) %*/> idsSpec /*% } %*/
    ) {
        Page</*%= normalize(context.name, true) %*/DTO> page = /*%= normalize(context.name) %*/Service.getAll(pageable, filters, search/*% if (feature.MV_T_ViewMapAsList) { %*/, idsSpec /*% } %*/);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, /*%= camelToSnakeCase(normalize(context.name)).toUpperCase() %*/_RESOURCE_URL);
        return new ResponseEntity<>(page, headers, HttpStatus.OK);
    }

    /*% if (!feature.MV_MS_GJ_Cached) { %*/
    /*% geographicPropertyNames.forEach(function(geoPropertyName) {
          geoPropertyName = normalize(geoPropertyName);
      %*/
    @GetMapping("/geom//*%= normalize(geoPropertyName) %*/")
      /*% if (feature.MV_MS_GJ_Paginated && geographicPropertyNames.length != 0) { %*/
    public ResponseEntity<FeatureCollectionJSON> get/*%= normalize(geoPropertyName, true) %*/(
        @RequestParam(value = "xmin", required = false) Double xmin,
        @RequestParam(value = "xmax", required = false) Double xmax,
        @RequestParam(value = "ymin", required = false) Double ymin,
        @RequestParam(value = "ymax", required = false) Double ymax) {
        return new ResponseEntity<>(/*%= normalize(context.name) %*/Service.get/*%= normalize(geoPropertyName, true) %*/(xmin, xmax, ymin, ymax), HttpStatus.OK);
    }
      /*% } else { %*/
    public ResponseEntity<FeatureCollectionJSON> get/*%= normalize(geoPropertyName, true) %*/(
        @RequestParam(value = "properties", defaultValue = "false", required = false)
          Boolean properties,
        @RequestParam(value = "filters", required = false) List<String> filters/*% if (feature.MV_T_F_BasicSearch) { %*/,
        @RequestParam(value = "search", required = false) String search/*% } %*/) {

        FeatureCollectionJSON featureCollection =  /*%= normalize(context.name) %*/Service.get/*%= normalize(geoPropertyName, true) %*/(properties, filters/*% if (feature.MV_T_F_BasicSearch) { %*/, search/*% } %*/);
        return new ResponseEntity<>(featureCollection, HttpStatus.OK);
    }
      /*% } %*/
    /*% }); %*/
    /*% } %*/

    @GetMapping("/{/*%= pkName %*/}")
    public ResponseEntity</*%= normalize(context.name, true) %*/FullDTO> get(@PathVariable /*%= pkClass %*/ /*%= pkName %*/) {
       try{
            return new ResponseEntity<>(/*%= normalize(context.name) %*/Service.get(/*%= pkName %*/), HttpStatus.OK);
        } catch (NotFoundException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<?> create (@Valid @RequestBody /*%= normalize(context.name, true) %*/FullDTO /*%= normalize(context.name) %*/, Errors errors) throws URISyntaxException {

        if (errors.hasErrors()) {
            return ResponseEntity.badRequest().body(ValidationErrorUtils.getValidationErrors(errors));
        }
        try {
            /*%= normalize(context.name, true) %*/FullDTO result = /*%= normalize(context.name) %*/Service.create(/*%= normalize(context.name) %*/);
            return ResponseEntity.created(new URI(String.format("%s/%s", /*%= camelToSnakeCase(normalize(context.name)).toUpperCase() %*/_RESOURCE_URL, result.get/*%= normalize(pkName, true) %*/()))).body(result);
        } catch (OperationNotAllowedException e){
            return ResponseEntity.badRequest().headers(HeaderUtil.createError(e.getMessage(), null)).body(null);
        }

      }

    @PutMapping("/{/*%= pkName %*/}")
    public ResponseEntity<?> update (@PathVariable /*%= pkClass %*/ /*%= pkName %*/, @Valid @RequestBody /*%= normalize(context.name, true) %*/FullDTO /*%= normalize(context.name) %*/, Errors errors){
      if (errors.hasErrors()) {
        return ResponseEntity.badRequest().body(ValidationErrorUtils.getValidationErrors(errors));
      }
      try {
          /*%= normalize(context.name, true) %*/FullDTO result = /*%= normalize(context.name) %*/Service.update(/*%= pkName %*/,/*%= normalize(context.name) %*/);
          return ResponseEntity.ok().body(result);
      } catch (OperationNotAllowedException e){
          return ResponseEntity.badRequest().headers(HeaderUtil.createError(e.getMessage(), null)).body(null);
      }
    }

    @DeleteMapping("/{/*%= pkName %*/}")
    public ResponseEntity<Void> delete(@PathVariable /*%= pkClass %*/ /*%= pkName %*/) {
        try {
            /*%= normalize(context.name) %*/Service.delete(/*%= pkName %*/);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .headers(HeaderUtil.createError("psql.exception", null)).build();
        }

        return ResponseEntity.ok().build();
    }

  /*% if ((feature.MV_MS_GeoServer && !feature.MV_MS_GeoJSON) || (feature.MV_MS_GeoServer && validGeom)) { %*/
    @PutMapping("/geom/restart")
    public ResponseEntity<?> updateGeom() {
      try {
            /*%= normalize(context.name) %*/Service.restartGeom();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    /*% } %*/
}
