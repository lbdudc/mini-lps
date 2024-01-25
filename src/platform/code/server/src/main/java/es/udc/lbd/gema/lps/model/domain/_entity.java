/*%@ return data.dataModel.entities.map(function(en) {
    return {
        fileName: normalize(en.name, true) + '.java',
        context: en
    };
})
%*/
/*%
    var ipPattern     = 'ipPattern';
    var urlPattern    = 'urlPattern';
    var emailPattern  = 'emailPattern';
    var patternTypes  = [ipPattern, urlPattern, emailPattern];

    function camelToSnake(string) {
       return string.replace(/[\w]([A-Z])/g, function(m) {
           return m[0] + "_" + m[1];
       }).toLowerCase();
    }
 %*/
package es.udc.lbd.gema.lps.model.domain;

import es.udc.lbd.gema.lps.model.views.Views;

import java.util.List;
import java.math.BigDecimal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

/*% if (feature.T_GIS) { %*/
import jakarta.persistence.Column;

import es.udc.lbd.gema.lps.model.util.jackson.CustomGeometrySerializer;
import es.udc.lbd.gema.lps.model.util.jackson.CustomGeometryDeserializer;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
/*% } %*/
/*% if(checkEntityContainsPatternOfTypes(context, patternTypes)) { %*/
import es.udc.lbd.gema.lps.config.Constants;
/*% } %*/
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

/*%
    var pkName = normalize(getEntityProperty(context, 'id').name);
%*/
/*% if (entityIsSuperclass(context) && !context.superclass) { %*/
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
/*% } %*/
@Entity(name = "t_/*%= camelToSnake(normalize(context.name)) %*/")
@Table(name = "t_/*%= camelToSnake(normalize(context.name)) %*/")
public /*% if (context.abstract) { %*/abstract /*% } %*/class /*%= normalize(context.name, true) %*/ /*% if (context.superclass) { %*/extends /*%= normalize(context.superclass, true) %*/
  /*% } else if (context.traceable) { %*/extends AbstractOwnerEntity /*% } %*/{
    /*% context.properties.forEach(function(prop) { %*/
        /*%
            var extraColumn = "";
            var propertyClass = prop.class;
            if (propertyClass == 'Date') propertyClass = 'LocalDate';
            if (propertyClass == 'DateTime') propertyClass = 'LocalDateTime';
            var propertyIsText = false;
            if (propertyClass == 'Text') { propertyClass = 'String'; propertyIsText = true; }
            var propertyIsString = propertyClass.localeCompare('String') == 0;
            var propertyIsAutoinc = propertyClass.indexOf('autoinc') != -1;
            propertyClass = propertyClass.split(' ')[0];
            const propertyIsEnum =
                       data.dataModel.enums.find(function(en) { return en.name == propertyClass; }) != null;
            const propertyIsEntity =
                       data.dataModel.entities.find(function(en) { return en.name == propertyClass; }) != null;

            var relationshipIsManyInOtherDirection = false;
            if (prop.bidirectional) {
                relationshipIsManyInOtherDirection = data.dataModel.entities
                    .find(function(en) { return en.name == propertyClass; })
                    .properties
                        .find(function(p) {
                            return p.name == prop.bidirectional;
                        }).multiple;
            }
            if (prop.multiple) {
              propertyClass = 'List<' + normalize(propertyClass, true) + '>';
            } else {
              propertyClass = normalize(propertyClass, true);
            }
        %*/
        /*% if (prop.pk) { %*/
    @Id
        /*% } %*/
        /*% if (propertyIsAutoinc) { %*/
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "/*%= (normalize(context.name)) %*//*%= normalize(prop.name) %*/")
    @SequenceGenerator(name = "/*%= (normalize(context.name)) %*//*%= normalize(prop.name) %*/", sequenceName = "t_/*%= camelToSnake(normalize(context.name)) %*/_/*%= camelToSnake(normalize(prop.name)) %*/_seq", initialValue = 1, allocationSize = 1)
        /*% } %*/
        /*% if (propertyIsEnum) { %*/
    @Enumerated(EnumType.STRING)
        /*% } %*/
        /*% if (propertyIsEntity) { %*/
            /*% if (prop.multiple) { %*/
                /*% if (relationshipIsManyInOtherDirection) { %*/
                    /*% if (prop.owner) {
                        var relationship = getRelationships(data, getEntity(data, prop.class), function(p) {
                            return p.bidirectional == prop.name;
                        })[0];
                        var auxIdRelationship = getEntityProperty(relationship.class, 'id').name;
                    %*/
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tr_/*%= camelToSnake(normalize(context.name)) %*/_/*%= camelToSnake(normalize(prop.name)) %*/",
        joinColumns = @JoinColumn(name = "/*%= camelToSnake(normalize(relationship.property.name)) %*/_/*%= camelToSnake(normalize(pkName)) %*/", referencedColumnName = "/*%= camelToSnake(normalize(pkName)) %*/"),
        inverseJoinColumns = @JoinColumn(name = "/*%= camelToSnake(normalize(prop.name)) %*/_/*%= camelToSnake(normalize(auxIdRelationship)) %*/", referencedColumnName = "/*%= camelToSnake(normalize(auxIdRelationship)) %*/"))
                    /*% } else { %*/
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "/*%= normalize(prop.bidirectional) %*/")
                    /*% } %*/
                /*% } else { %*/
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "/*%= normalize(prop.bidirectional) %*/")
                /*% } %*/
            /*% } else { %*/
                /*% if (relationshipIsManyInOtherDirection) { %*/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "/*%= camelToSnake(normalize(prop.name)) %*/")
                /*% } else { %*/
                    /*% if (prop.owner || !prop.bidirectional) { %*/
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "/*%= camelToSnake(normalize(prop.name)) %*/")
                    /*% } else { %*/
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "/*%= prop.bidirectional %*/")
                    /*% } %*/
                /*% } %*/
            /*% } %*/
        /*% } %*/
        /*% switch (propertyClass) {
            case 'Point':
                extraColumn = ', columnDefinition="geometry(Point, ' + (data.basicData.SRID || 4326) + ')"';
                break;
            case 'MultiPoint':
                extraColumn = ', columnDefinition="geometry(MultiPoint, ' + (data.basicData.SRID || 4326) + ')"';
                break;
            case 'LineString':
                extraColumn = ', columnDefinition="geometry(LineString, ' + (data.basicData.SRID || 4326) + ')"';
                break;
            case 'MultiLineString':
                extraColumn = ', columnDefinition="geometry(MultiLineString, ' + (data.basicData.SRID || 4326) + ')"';
                break;
            case 'Polygon':
                extraColumn = ', columnDefinition="geometry(Polygon, ' + (data.basicData.SRID || 4326) + ')"';
                break;
            case 'MultiPolygon':
                extraColumn = ', columnDefinition="geometry(MultiPolygon, ' + (data.basicData.SRID || 4326) + ')"';
                break;
            case 'Geometry':
                extraColumn = ', columnDefinition="geometry(Geometry, ' + (data.basicData.SRID || 4326) + ')"';
                break;
        } %*/

        /*% if (prop.required && !prop.pk && prop.name != 'password') { %*/
            /*% if (propertyIsEnum || propertyIsEntity || propertyClass == 'LocalDate' || propertyClass == 'Instant' || propertyClass == 'Long' || propertyClass == 'Integer' || propertyClass == 'BigDecimal' || propertyClass == 'Float' || propertyClass == 'Double' || propertyClass == 'Boolean') { %*/
    @NotNull
            /*% } else { %*/
    @NotBlank
            /*% } %*/
        /*% } %*/
        /*% if (prop.min && propertyIsString || prop.max && propertyIsString) { %*/
    @Size(/*% if (prop.min) { %*/min = /*%= prop.min %*//*% } %*//*% if (prop.min && prop.max) { %*/, /*% } %*//*% if (prop.max) { %*/max = /*%= prop.max %*//*% } %*/)
        /*% } %*/
        /*% if (prop.min && !propertyIsString) { %*/
    @DecimalMin("/*%= prop.min %*/")
        /*% } %*/
        /*% if (prop.max && !propertyIsString) { %*/
    @DecimalMax("/*%= prop.max %*/")
        /*% } %*/
        /*% if (prop.patternType) { %*/
            /*% if (prop.patternType === 'ipPattern') { %*/
    @Pattern(regexp=Constants.IP_REGEX)
            /*% } %*/
            /*% if (prop.patternType === 'urlPattern') { %*/
    @Pattern(regexp=Constants.URL_REGEX)
            /*% } %*/
            /*% if (prop.patternType === 'emailPattern') { %*/
    @Pattern(regexp=Constants.EMAIL_REGEX)
            /*% } %*/
            /*% if (prop.patternType === 'customPattern') { %*/
    @Pattern(regexp="^$|/*%= prop.pattern.replace(/\\/g,'\\\\') %*/")
            /*% } %*/
        /*% } %*/
        /*% if (!propertyIsEntity) { %*/
    @Column(name = "/*%= camelToSnake(normalize(prop.name)) %*/"/*%= extraColumn %*/ /*% if (prop.unique) { %*/ ,unique=true/*% } %*/         /*% if (propertyIsText) { %*/
        ,columnDefinition="TEXT"
        /*% } %*/)
        /*% } %*/
    private /*%= propertyClass %*/ /*%= normalize(prop.name) %*/;

    /*% }); %*/
    public /*%= normalize(context.name, true) %*/() {
    }

    /*% context.properties.forEach(function(prop) { %*/
        /*%
            var propertyClass = prop.class.split(' ')[0];
            if (propertyClass == 'Date') propertyClass = 'LocalDate';
            if (propertyClass == 'DateTime') propertyClass = 'LocalDateTime';
            if (propertyClass == 'Text') propertyClass = 'String';
            if (prop.multiple) {
              propertyClass = 'List<' + normalize(propertyClass, true) + '>';
            } else {
              propertyClass = normalize(propertyClass, true);
            }
        %*/
    public /*%= propertyClass %*/ get/*%= normalize(prop.name, true) %*/() {
        return /*%= normalize(prop.name) %*/;
    }

    public void set/*%= normalize(prop.name, true) %*/(/*%= propertyClass %*/ /*%= normalize(prop.name) %*/) {
        this./*%= normalize(prop.name) %*/ = /*%= normalize(prop.name) %*/;
    }

    /*% }); %*/
}
