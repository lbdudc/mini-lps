/*%@ return data.dataModel.entities
      .filter(function(context){
        return !context.abstract;
      })
      .map(function(en) {
        return {
            fileName: normalize(en.name, true) + 'ServiceImpl.java',
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
/*%
    var pkClass = getEntityProperty(context, 'id').class;
    var pkAutoinc = pkClass.indexOf('autoinc') != -1;
    pkClass = normalize(pkClass.split(' ')[0], true);
    var pkName = normalize(getEntityProperty(context, 'id').name);
%*/
package es.udc.lbd.gema.lps.model.service;

import java.util.List;
import java.util.stream.Collectors;
import es.udc.lbd.gema.lps.model.domain./*%= normalize(context.name, true) %*/;
import es.udc.lbd.gema.lps.model.service.dto./*%= normalize(context.name, true) %*/DTO;
import es.udc.lbd.gema.lps.model.service.dto./*%= normalize(context.name, true) %*/FullDTO;
import es.udc.lbd.gema.lps.model.repository./*%= normalize(context.name, true) %*/Repository;
import org.springframework.core.io.FileSystemResource;
/*% if (geographicPropertyNames.length > 0) { %*/
import es.udc.lbd.gema.lps.web.rest.custom.FeatureCollectionJSON;
import es.udc.lbd.gema.lps.web.rest.custom.FeatureJSON;
import java.util.HashMap;
/*% } %*/
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import es.udc.lbd.gema.lps.web.rest.util.specification_utils.*;
import es.udc.lbd.gema.lps.web.rest.specifications./*%= normalize(context.name, true) %*/Specification;

import es.udc.lbd.gema.lps.model.service.exceptions.OperationNotAllowedException;
import es.udc.lbd.gema.lps.model.service.exceptions.NotFoundException;
/*% if (feature.MV_MS_GeoServer) { %*/
import es.udc.lbd.gema.lps.model.service.util.GeoServerUtil;
/*% } %*/
import java.io.IOException;

/*% if (feature.MV_T_ViewMapAsList) { %*/

import es.udc.lbd.gema.lps.model.domain./*%= normalize(context.name, true) %*/;
import org.springframework.data.jpa.domain.Specification;
/*% } %*/

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class /*%= normalize(context.name, true) %*/ServiceImpl implements /*%= normalize(context.name, true) %*/Service {

  @Inject
  private /*%= normalize(context.name, true) %*/Repository /*%= normalize(context.name) %*/Repository;

  /*% if ((feature.MV_MS_GeoServer && !feature.MV_MS_GeoJSON) || (feature.MV_MS_GeoServer && validGeom)) { %*/

  @Inject
  private GeoServerUtil gsUtil;
  /*% } %*/

  /*% getOtherRelationshipsNotMultipleAndOwning(data, context).forEach(function(r) { %*/
  public List</*%= normalize(context.name, true) %*/DTO> getAllWithout/*%= normalize(r.property.bidirectional, true) %*/(){
    return /*%= normalize(context.name) %*/Repository./*%= r.property.bidirectional %*/IsNull().stream().map(/*%= normalize(context.name, true) %*/DTO::new).collect(Collectors.toList());
  }
  /*% }); %*/

  public Page</*%= normalize(context.name, true) %*/DTO> getAll(Pageable pageable, List<String> filters, String search/*% if (feature.MV_T_ViewMapAsList) { %*/, Specification</*%= normalize(context.name, true) %*/> idsSpec /*% } %*/){
    Page</*%= normalize(context.name, true) %*/> page;
    /*% if (feature.MV_T_ViewMapAsList) { %*/
    if (idsSpec != null) {
      page= /*%= normalize(context.name) %*/Repository.findAll(idsSpec, pageable);
    } else /*% } %*/    if (search != null && !search.isEmpty()) {
      page= /*%= normalize(context.name) %*/Repository.findAll(
        /*%= normalize(context.name, true) %*/Specification.searchAll(search), pageable);
    } else {
      page= /*%= normalize(context.name) %*/Repository.findAll(SpecificationUtil.getSpecificationFromFilters(filters, false), pageable);
    }
    return page.map(/*%= normalize(context.name, true) %*/DTO::new);
  }

  /*% geographicPropertyNames.forEach(function(geoPropertyName) {
        geoPropertyName = normalize(geoPropertyName);
    %*/
  /*% if (feature.MV_MS_GJ_Paginated) { %*/
  public FeatureCollectionJSON get/*%= normalize(geoPropertyName, true) %*/(Double xmin, Double xmax, Double ymin, Double ymax) {
    List</*%= normalize(context.name, true) %*/> list = /*%= normalize(context.name) %*/Repository.getDataByBoundingBox(xmin, xmax, ymin, ymax);

    List<FeatureJSON> ret =
      list.stream()
        .map(
          e -> {
            FeatureJSON geoJSON = new FeatureJSON();
            geoJSON.setProperties(new HashMap());
  /*% } else { %*/
  public FeatureCollectionJSON get/*%= normalize(geoPropertyName, true) %*/ (Boolean properties, List<String> filters/*% if (feature.MV_T_F_BasicSearch) { %*/, String search/*% } %*/){
    List</*%= normalize(context.name, true) %*/> list =
      /*%= normalize(context.name) %*/Repository.findAll(SpecificationUtil.getSpecificationFromFilters(filters, false));

    /*% if (feature.MV_T_F_BasicSearch) { %*/
    if (search != null && !search.isEmpty()) {
      list = /*%= normalize(context.name) %*/Repository.findAll(/*%= normalize(context.name, true) %*/Specification.searchAll(search));
    }
    /*% } %*/

    List<FeatureJSON> ret =
      list.stream()
        .map(
          e -> {
            FeatureJSON geoJSON = new FeatureJSON();
            if (properties) {
              geoJSON = new FeatureJSON(/*%= normalize(context.name, true) %*/.class, e);
            } else {
              geoJSON.setProperties(new HashMap());
            }
  /*% } %*/
            geoJSON.setId(e.get/*%= normalize(getEntityProperty(context, 'id').name, true) %*/());
            geoJSON.getProperties().put("displayString", /*%= getDisplayStringJava(context, 'e') %*/);
            geoJSON.setGeometry(e.get/*%= normalize(geoPropertyName, true) %*/());
            return geoJSON;
      })
      .filter(e -> e.getGeometry() != null)
      .collect(Collectors.toList());
    return new FeatureCollectionJSON(ret);
  }
  /*% }); %*/

  public /*%= normalize(context.name, true) %*/FullDTO get(/*%= pkClass %*/ /*%= pkName %*/) throws NotFoundException {
    /*%= normalize(context.name, true) %*/ /*%= normalize(context.name) %*/ = findById(/*%= pkName %*/);
    return new /*%= normalize(context.name, true) %*/FullDTO(/*%= normalize(context.name) %*/);
  }

  @Transactional(readOnly = false, rollbackFor = Exception.class)
  public /*%= normalize(context.name, true) %*/FullDTO create(/*%= normalize(context.name, true) %*/FullDTO /*%= normalize(context.name) %*/Dto) throws OperationNotAllowedException {
    /*% if (pkAutoinc) { %*/
    if (/*%= normalize(context.name) %*/Dto.get/*%= normalize(pkName, true) %*/() != null){
      throw new OperationNotAllowedException("/*%= normalize(context.name) %*/.error.id-exists");
    }
    /*% } else { %*/
    if (/*%= normalize(context.name) %*/Dto.get/*%= normalize(pkName, true) %*/() == null){
      throw new OperationNotAllowedException("/*%= normalize(context.name) %*/.error.id-not-exists");
    }
    /*% } %*/
    /*%= normalize(context.name, true) %*/ /*%= normalize(context.name) %*/Entity = /*%= normalize(context.name) %*/Dto.to/*%= normalize(context.name, true) %*/();
     /*%= normalize(context.name, true) %*/ /*%= normalize(context.name) %*/Saved = /*%= normalize(context.name) %*/Repository.save(/*%= normalize(context.name) %*/Entity);
    return new /*%= normalize(context.name, true) %*/FullDTO(/*%= normalize(context.name) %*/Saved);
  }

  @Transactional(readOnly = false, rollbackFor = Exception.class)
  public /*%= normalize(context.name, true) %*/FullDTO update(/*%= pkClass %*/ /*%= pkName %*/, /*%= normalize(context.name, true) %*/FullDTO /*%= normalize(context.name) %*/Dto) throws OperationNotAllowedException {
    if (/*%= normalize(context.name) %*/Dto.get/*%= normalize(pkName, true) %*/() == null){
      throw new OperationNotAllowedException("/*%= normalize(context.name) %*/.error.id-not-exists");
    }
    if (!/*%= pkName %*/.equals(/*%= normalize(context.name) %*/Dto.get/*%= normalize(pkName, true) %*/())){
      throw new OperationNotAllowedException("/*%= normalize(context.name) %*/.error.id-dont-match");
    }
    /*%= normalize(context.name, true) %*/ /*%= normalize(context.name) %*/ = /*%= normalize(context.name) %*/Repository
      .findById(/*%= pkName %*/)
      .orElseThrow(
        () ->
          new OperationNotAllowedException("/*%= normalize(context.name) %*/.error.id-not-exists"));
    /*%= normalize(context.name, true) %*/ /*%= normalize(context.name) %*/ToUpdate =
      /*% if (entityIsSuperclass(context)) { %*/
        /*%= normalize(context.name) %*/;
      /*% } else { %*/
        /*%= normalize(context.name) %*/Dto.to/*%= normalize(context.name, true) %*/();
    /*% } %*/
    /*% if (entityIsSuperclass(context)) { %*/
      /*% context.properties.filter(function(prop) { return !prop.pk; }).forEach(function(prop) { %*/
      /*%
        var propertyClass = prop.class;
        if (isGeographicProperty(propertyClass)) return;

        var propertyIsEntity = data.dataModel.entities.find(function(en) { return en.name == prop.class; }) != null;
      %*/
    /*% if (!propertyIsEntity || prop.owner) { %*/
    /*% if (propertyIsEntity) { %*/
    if (/*%= normalize(context.name) %*/Dto.get/*%= normalize(prop.name, true) %*/() != null) {
      /*% if (prop.multiple) { %*/
      /*%= normalize(context.name) %*/ToUpdate.set/*%= normalize(prop.name, true) %*/(/*%= normalize(context.name) %*/Dto.get/*%= normalize(prop.name, true) %*/().stream().map(/*%= prop.class.split(' ')[0] %*/DTO::to/*%= prop.class %*/).collect(Collectors.toList()));
      /*% } else { %*/
      /*%= normalize(context.name) %*/ToUpdate.set/*%= normalize(prop.name, true) %*/(/*%= normalize(context.name) %*/Dto.get/*%= normalize(prop.name, true) %*/().to/*%= prop.class %*/());
      /*% } %*/
    } else {
      /*%= normalize(context.name) %*/ToUpdate.set/*%= normalize(prop.name, true) %*/(null);
    }
    /*% } else { %*/
    /*%= normalize(context.name) %*/ToUpdate.set/*%= normalize(prop.name, true) %*/(/*%= normalize(context.name) %*/Dto.get/*%= normalize(prop.name, true) %*/());
    /*% } %*/
    /*% } %*/
    /*% }); %*/
    /*% } %*/
    /*%= normalize(context.name, true) %*/ /*%= normalize(context.name) %*/Updated = /*%= normalize(context.name) %*/Repository.save(/*%= normalize(context.name) %*/ToUpdate);
    return new /*%= normalize(context.name, true) %*/FullDTO(/*%= normalize(context.name) %*/Updated);
  }

  @Transactional(readOnly = false, rollbackFor = Exception.class)
  public void delete(/*%= pkClass %*/ /*%= pkName %*/){
    /*%= normalize(context.name) %*/Repository.deleteById(/*%= pkName %*/);
  }

  /*% if ((feature.MV_MS_GeoServer && !feature.MV_MS_GeoJSON) || (feature.MV_MS_GeoServer && validGeom)) { %*/
  public void restartGeom() throws IllegalArgumentException {
    gsUtil.recalculateFeatureTypeBBox("t_/*%= camelToSnakeCase(normalize(context.name)) %*/");
  }
  /*% } %*/

  /** PRIVATE METHODS **/
  private /*%= normalize(context.name, true) %*/ findById(/*%= pkClass %*/ /*%= pkName %*/) throws NotFoundException {
    return /*%= normalize(context.name) %*/Repository
      .findById(/*%= pkName %*/)
      .orElseThrow(
        () ->
          new NotFoundException(
            "Cannot find /*%= normalize(context.name, true) %*/ with id " + /*%= pkName %*/));
  }
}
