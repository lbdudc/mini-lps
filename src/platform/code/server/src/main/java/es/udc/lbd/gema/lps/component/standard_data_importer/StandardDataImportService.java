/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer;

/*% if (feature.T_GIS) { %*/
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
/*% } %*/
import es.udc.lbd.gema.lps.component.file_uploader.file_uploaders.FileUploadImport;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.ImportColumnJSON;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.ImportFileJSON;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.ParseFormatJSON;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.FileNotSupportedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.TypeNotSupportedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportColumnDuplicatedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportCustomErrorException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportDateFormatCustomNotProvidedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportDateFormatNotProvidedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportDateUnparseableException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportEmptyValueException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportEntityPrimaryKeyExistsException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportFieldPointPropertiesNotProvidedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportFileInUrlNotExistException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportFileNotProvidedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportInvalidDatePatternException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportInvalidForeignKeyException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportNoColumnsException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportUrlNotProvidedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportUrlNotValidException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportValueNotValidException;
import es.udc.lbd.gema.lps.config.LocaleMessageInterpolator;
import es.udc.lbd.gema.lps.config.Properties;
import es.udc.lbd.gema.lps.model.domain._entityDomain;
import es.udc.lbd.gema.lps.model.service.exceptions.AppRuntimeException;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.Id;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@Service
abstract class StandardDataImportService {

    private static final Logger logger = LoggerFactory.getLogger(StandardDataImportService.class);

    @Inject
    FileUploadImport fileUploadImport;

    @Inject
    ListableBeanFactory beanFactory;

    @Inject
    private Properties properties;

    @Resource(name ="convertUtilsWithExceptions")
    private ConvertUtilsBean convertUtils;

    private static final String localDateType = "java.time.LocalDate";
    private static final String localTimeType = "java.time.LocalTime";
    private static final String localDateTimeType = "java.time.LocalDateTime";
    /*% if (feature.T_GIS) { %*/
    private static final String pointType = "org.locationtech.jts.geom.Point";
    private static final String lineStringType = "org.locationtech.jts.geom.MultiLineString";
    private static final String multiPolygonType = "org.locationtech.jts.geom.MultiPolygon";
    /*% } %*/

    private static final List<String> importCustomTypes = new ArrayList<String>();

    static {
        /*% if (feature.T_GIS) { %*/
        importCustomTypes.add(pointType); // FIXME nuevo point en vez del viejo (geolatte)
        importCustomTypes.add(lineStringType); // FIXME nuevo point en vez del viejo (geolatte)
        importCustomTypes.add(multiPolygonType); // FIXME nuevo point en vez del viejo (geolatte)
        /*% } %*/
        importCustomTypes.add(localDateType);
        importCustomTypes.add(localTimeType);
        importCustomTypes.add(localDateTimeType);
    }

    abstract ImportFileJSON uploadAndProcessHeader(MultipartHttpServletRequest request) throws ImportException, FileNotSupportedException, TypeNotSupportedException;

    abstract File processFile(ParseFormatJSON format) throws ImportException, FileNotSupportedException, TypeNotSupportedException;

    /**
     * Fill object 'instanceObject' with data. <br>
     * Relations between column indexes and data are provided in 'format' <br>
     * Values of read row are provided in 'values'<br>
     */
    Object fillObjectWithData(ImportColumnJSON[] format, Object[] values, Class<?> entityType) throws ImportException {
        Object instanceObject;
        try {
            instanceObject = entityType.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException |
             InvocationTargetException e) {
            throw new AppRuntimeException("Unable to obtain entity instance ", e);
        }

        /*% if (feature.T_GIS) { %*/
        Map<String, Map<String, String>> entityPointValues = new HashMap<String, Map<String, String>>();
        /*% } %*/

        for (int i = 0; i < format.length; i++) {
            try {
                ImportColumnJSON column = format[i];

                // We use column numbers
                if (column != null && i < values.length) {

                    Object value = values[i];

                    String entityPackage = _entityDomain.class.getPackage().getName();
                    boolean isEnumerated = Enum.class.isAssignableFrom(Class.forName(column.getType()));

                  if (isNullOrStringBlank(value)) {
                    // Do nothing
                  } else if (!isEnumerated && column.getType().contains(entityPackage)) {
                        // Case: This field is a relationship with another entity

                        // Obtain related entity from DB
                        Object relatedEntity;
                        String propName;
                        if (column.getName().contains("Custom.")) {
                          String fieldName = StringUtils.substringAfter(column.getName(), ".");
                          propName = StringUtils.substringBefore(column.getName(), "Custom");
                          relatedEntity = getEntityByField(Class.forName(column.getType()), value, fieldName);
                        } else {
                          propName = column.getName();
                          relatedEntity = getEntityByPk(Class.forName(column.getType()), value);
                        }

                        if (relatedEntity == null) {
                            throw new ImportInvalidForeignKeyException(values.toString());
                        }

                        setValueInField(instanceObject, propName, column.getType(), relatedEntity);
                    } else if (importCustomTypes.contains(column.getType())) {
                        /*% if (feature.T_GIS) { %*/
                        // Case: This field is a custom type (Geographic, dates, ... )
                        String propName = StringUtils.substringBefore(column.getName(), "Custom");
                        if (StringUtils.substringBefore(column.getName(), ".").equals(propName + "Custom")) {
                            // Only Points supported
                            preprocessTypePoint(column, (String) value, propName, entityPointValues);
                        } else /*% } %*/if (localDateType.equals(column.getType()) || localTimeType.equals(column.getType()) || localDateTimeType.equals(column.getType())) {
                            // Date & Time
                            processDateField(entityType, instanceObject, column, (String) value);
                        } else {
                            // Line & Polygon (Only shapefile importer)
                            setValueInField(instanceObject, column.getName(), column.getType(), value);
                        }
                    } else {
                        // Case: Normal fields

                        // If current property is primary key: The specified id cannot exists en DB
                        if (getPrimaryKeyField(entityType).getName().equals(column.getName())) {
                            if (getEntityByPk(entityType, value) != null) {
                              logger.error(
                                "Entity with ID " + value.toString() + " already exists, skipping line...");
                            }
                        }

                        setValueInField(instanceObject, column.getName(), column.getType(), value);
                    }
                }
            } catch (ImportException e) {
                throw e;
            } catch (ClassNotFoundException e) {
                throw new AppRuntimeException("Unable to retrieve column class", e);
            }
        }

        // Custom Types
        /*% if (feature.T_GIS) { %*/
        for (Map.Entry<String, Map<String, String>> entry : entityPointValues.entrySet()) {
            processTypePoint(instanceObject, entry.getKey(), entry.getValue());
        }
        /*% } %*/

        // Validate object using java validator
        // https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/?v=6.0#section-validator-factory-message-interpolator
        Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new LocaleMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(instanceObject);
        for (ConstraintViolation<Object> cv : constraintViolations) {
            String message = "Property '" + cv.getPropertyPath() + "'. Found: '" + cv.getMessage() + "'.";
            logger.error(message);
            throw new ImportCustomErrorException(message);
        }
        return instanceObject;
    }

    /*% if (feature.T_GIS) { %*/
    /**
     * Looks for properties '&lt;field&gt;Custom.x' and '&lt;field&gt;Custom.y' and adds
     * them to provided Map
     */
    private void preprocessTypePoint(ImportColumnJSON column, String value, String propertyName, Map<String, Map<String, String>> entityPointValues) {
        String[] customName = StringUtils.split(column.getName(), ".");
        String customEntity = customName[0];
        String customProperty = customName[1];

        String subname = StringUtils.substringBefore(customEntity, "Custom");

        Map<String, String> pointValues;
        if (!entityPointValues.containsKey(subname)) {
            pointValues = new HashMap<String, String>();
            entityPointValues.put(subname, pointValues);
        } else {
            pointValues = entityPointValues.get(subname);
        }

        if (customEntity.equals(subname + "Custom")) {
            if (customProperty.equals("x")) {
                pointValues.put("x", value);
            } else if (customProperty.equals("y")) {
                pointValues.put("y", value);
            } else {
                throw new AppRuntimeException("Unexpected position property '" + customProperty + "'");
            }
        }
    }
    /*% } %*/

    void saveEntity(String clazzRepositoryName, Method saveMethod, Object instanceObject){
        try {
            saveMethod.invoke(beanFactory.getBean(StringUtils.uncapitalize(clazzRepositoryName)), instanceObject);
        } catch (BeansException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            if (e instanceof InvocationTargetException && ((InvocationTargetException) e).getTargetException() instanceof DataIntegrityViolationException) {
                logger.error("Entity violates data integrity, skipping line...");
            } else {
                throw new AppRuntimeException("Unable to invoke method", e);
            }

        }
    }

    /**
     * Fills instance field with provided value
     *
     * @param instance  Object to be filled
     * @param fieldName Field name
     * @param fieldType Field type
     * @param valueRaw  Value
     * @throws ImportValueNotValidException Received value cannot be converted to given class
     */
    private void setValueInField(Object instance, String fieldName, String fieldType, Object valueRaw) throws ImportValueNotValidException {
        try {
            Object value;

            if (valueRaw.equals("null") || valueRaw.equals("NaN")) {
              value = null;
            } else {
              value = convertUtils.convert(valueRaw, Class.forName(fieldType));
            }

            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            throw new ImportValueNotValidException(valueRaw.toString());
        }
    }

    /*% if (feature.T_GIS) { %*/
    /**
     * Creates a new Point and adds it to
     */
    private void processTypePoint(Object instanceObject, String propertyName, Map<String, String> pointValues)
        throws ImportException {

        if (!pointValues.isEmpty()) {
            if (pointValues.containsKey("x") && pointValues.containsKey("y")) {
                PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
                int srid = properties.getGis().getDefaultSrid();
                GeometryFactory gf = new GeometryFactory(pm, srid);
                CoordinateSequenceFactory f = gf.getCoordinateSequenceFactory();
                CoordinateSequence cs = f.create(1, 2); // One coordinate with 2 dimensions

                try {

                    Double x = (Double) convertUtils.convert(pointValues.get("x"), Class.forName("java.lang.Double"));
                    if (x == null) {
                        throw new ImportEmptyValueException("x");
                    }

                    Double y = (Double) convertUtils.convert(pointValues.get("y"), Class.forName("java.lang.Double"));
                    if (y == null) {
                        throw new ImportEmptyValueException("y");
                    }

                    cs.setOrdinate(0, 0, x);
                    cs.setOrdinate(0, 1, y);
                } catch (ClassNotFoundException e) {
                    throw new AppRuntimeException("Unable to obtain class ", e);
                }

                Point point = new Point(cs, gf);
                setValueInField(instanceObject, propertyName, pointType, point);

            } else {
                throw new ImportFieldPointPropertiesNotProvidedException();
            }
        }
    }
    /*% } %*/

    private void processDateField(Class<?> entityType, Object instanceObject, ImportColumnJSON column, String value)
        throws ImportException {

        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(column.getRealPattern());
        } catch (IllegalArgumentException e) {
            throw new ImportInvalidDatePatternException();
        }
        Object date;
        try {
            if (localDateType.equals(column.getType())) {
                date = LocalDate.parse(value, formatter);
            } else if (localTimeType.equals(column.getType())) {
                date = LocalTime.parse(value, formatter);
            } else if (localDateTimeType.equals(column.getType())) {
                date = LocalDateTime.parse(value, formatter);
            } else {
                throw new AppRuntimeException("Data type '" + column.getType() + "' not expected");
            }
        } catch (DateTimeParseException e) {
            throw new ImportDateUnparseableException(value);
        }

        setValueInField(instanceObject, column.getName(), column.getType(), date);
    }

    /**
     * Obtains the entity instance with value as primary key
     */
    private Object getEntityByPk(Class<?> entityClass, Object primaryKey) throws ImportException {
        // Obtains id field type
        Class<?> idFieldType = getPrimaryKeyField(entityClass).getType();

        // Obtain entity from DB
        String clazzRepositoryPackage = entityClass.getPackage().getName().replace("domain", "repository");
        String clazzRepositoryName = entityClass.getSimpleName() + "Repository";
        Class<?> repository;
        try {
            repository = Class.forName(clazzRepositoryPackage + "." + clazzRepositoryName);
        } catch (ClassNotFoundException e) {
            throw new AppRuntimeException("Unable to obtain repository class", e);
        }

        // WARNING: We need to declare a .findById method in every Repository
        // class! => Try to avoid this
        Method findByIdMethod;
        try {
          findByIdMethod = repository.getMethod("findById", idFieldType);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new AppRuntimeException("Unable to obtain 'getOne' method for class " + clazzRepositoryName, e);
        }

        // If idFieldType is not the same as received value: Try to convert it
        Object pk = convertUtils.convert(primaryKey, idFieldType);
        Object instance;
        try {
          Optional result =
            (Optional)
              findByIdMethod.invoke(
                  beanFactory.getBean(StringUtils.uncapitalize(clazzRepositoryName)), pk);
          instance = result.orElse(null);
        } catch (BeansException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new AppRuntimeException("Unable invoke getOne method ", e);
        }

        return instance;
    }

    /**
     * Obtains primary id field. <br>
     * No composite keys are supported
     */
    private Field getPrimaryKeyField(Class<?> entityClass) {
        // Obtains id field type
        List<Field> idFieldsNames = FieldUtils.getFieldsListWithAnnotation(entityClass, Id.class);
        if (idFieldsNames.size() == 0) {
            throw new AppRuntimeException("Primary keys not found for '" + entityClass.getSimpleName() + "'");
        } else if (idFieldsNames.size() > 1) {
            throw new AppRuntimeException("Composite primary keys not supported. Entity: '" + entityClass.getSimpleName() + "'");
        }
        return idFieldsNames.iterator().next();
    }

    /** Obtains the entity instance with value as field */
    private Object getEntityByField(Class<?> entityClass, Object value, String fieldName)
        throws ImportException {
        // Obtains id field type
        Class<?> fieldType = getField(entityClass, fieldName).getType();

        // Obtain entity from DB
        String clazzRepositoryPackage = entityClass.getPackage().getName().replace("domain", "repository");
        String clazzRepositoryName = entityClass.getSimpleName() + "Repository";
        Class<?> repository;
        try {
            repository = Class.forName(clazzRepositoryPackage + "." + clazzRepositoryName);
        } catch (ClassNotFoundException e) {
            throw new AppRuntimeException("Unable to obtain repository class", e);
        }

        // WARNING: We need to declare a .findBy${fieldName} method in every Repository
        // class! => Try to avoid this
        Method findByFieldMethod;
        String methodName = "findBy" + StringUtils.capitalize(fieldName);
        try {
            findByFieldMethod = repository.getMethod(methodName, fieldType);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new AppRuntimeException(
                "Unable to obtain '" + methodName + "' method for class " + clazzRepositoryName, e);
        }

        // If fieldType is not the same as received value: Try to convert it
        Object fieldValue = convertUtils.convert(value, fieldType);
        Object instance;
        try {
            Optional result =
                (Optional)
                    findByFieldMethod.invoke(
                        beanFactory.getBean(StringUtils.uncapitalize(clazzRepositoryName)), fieldValue);
            instance = result.orElse(null);
        } catch (BeansException
            | IllegalAccessException
            | IllegalArgumentException
            | InvocationTargetException e) {
            throw new AppRuntimeException("Unable invoke '" + methodName + "' method ", e);
        }

        return instance;
    }

    /** Obtains field of class provided. <br> */
    private Field getField(Class<?> entityClass, String fieldName) {
        Field field = FieldUtils.getField(entityClass, fieldName, true);
        // Obtains id field type
        if (field == null) {
        throw new AppRuntimeException(
            "Field " + fieldName + " not found for '" + entityClass.getSimpleName() + "'");
        }
        return field;
    }

    void validateFormat(ImportColumnJSON[] format) throws ImportException {
        // Check no duplicate columns
        Set<String> columnsFound = new HashSet<String>();
        for (ImportColumnJSON f : format) {
            if (f != null && !columnsFound.add(f.getName())) {
                throw new ImportColumnDuplicatedException(f.getName());
            }
        }

        if (columnsFound.size() == 0) {
            throw new ImportNoColumnsException();
        }

        // Check date format provided
        for (ImportColumnJSON column : format) {
            if (column != null && (localDateType.equals(column.getType()) || localTimeType.equals(column.getType())
                || localDateTimeType.equals(column.getType()))) {
                if (StringUtils.isBlank(column.getPattern())) {
                    throw new ImportDateFormatNotProvidedException();
                } else if (ImportColumnJSON.CUSTOM.equals(column.getPattern())
                    && StringUtils.isBlank(column.getPatternCustom())) {
                    throw new ImportDateFormatCustomNotProvidedException();
                }
            }
        }
    }

    /**
     * Checks if file is provided
     * @param file file
     * @param validExtensions validExtensions
     * @return The file
     * @throws ImportFileNotProvidedException If no file was provided
     * @throws FileNotSupportedException If provided file extension is not in validExtensions
     */
    MultipartFile checkAndGetFile(MultipartFile file, Set<String> validExtensions) throws ImportFileNotProvidedException, FileNotSupportedException {
        // Check file provided
        if (file == null) {
            throw new ImportFileNotProvidedException();
        }

        // Check valid extension
        String fileExt = FilenameUtils.getExtension(file.getOriginalFilename());
        for (String ext : validExtensions) {
            if (ext.equalsIgnoreCase(fileExt)) {
                return file;
            }
        }

        throw new FileNotSupportedException();
    }

    /**
     * Checks if file is provided
     * @param file file as String
     * @param validExtensions validExtensions
     * @return The file
     * @throws ImportFileNotProvidedException If no file was provided
     * @throws FileNotSupportedException If provided file extension is not in validExtensions
     */
    String checkAndGetFile(String file, Set<String> validExtensions) throws ImportFileNotProvidedException, FileNotSupportedException {
        // Check file provided
        if (file == null) {
            throw new ImportFileNotProvidedException();
        }

        // Check valid extension
        String fileExt = FilenameUtils.getExtension(file);
        for (String ext : validExtensions) {
            if (ext.equalsIgnoreCase(fileExt)) {
                return file;
            }
        }

        throw new FileNotSupportedException();
    }

    /**
     * Checks if url is provided and is valid
     * @param url Url to check (As string)
     * @param validExtensions Valid extensions of the file specified in the Url
     * @return Url checked
     * @throws ImportUrlNotProvidedException If no URL vas provided
     * @throws ImportUrlNotValidException If URL is not valid
     * @throws ImportFileInUrlNotExistException If URL does not replies code: 200
     * @throws FileNotSupportedException If provided file extension is not in validExtensions
     */
    String checkAndGetUrl(String url, Set<String> validExtensions) throws ImportUrlNotProvidedException, ImportUrlNotValidException, ImportFileInUrlNotExistException, FileNotSupportedException {
        if (StringUtils.isBlank(url)) {
            throw new ImportUrlNotProvidedException();
        }
        // Check valid URL
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new ImportUrlNotValidException();
        }

        // Check file exists
        if (!fileExists(url)) {
            throw new ImportFileInUrlNotExistException();
        }

        // Check valid extension
        String fileExt = FilenameUtils.getExtension(url);
        for (String ext : validExtensions) {
            if (ext.equalsIgnoreCase(fileExt)) {
                return url;
            }
        }

        throw new FileNotSupportedException();
    }

    /**
     * Check no duplicated values present
     * @param values Values to check
     * @throws ImportColumnDuplicatedException If duplicated value found
     */
    void checkNoValuesRepeated(String[] values) throws ImportColumnDuplicatedException {
        Set<String> valuesFound = new HashSet<String>();
        for (String v : values) {
            if (v != null && !valuesFound.add(v)) {
                throw new ImportColumnDuplicatedException(v);
            }
        }
    }

    /**
     * Check if the URL returns a HttpStatus 200 code.
     * @param url Url to check
     * @return true if file exists, false otherwise
     */
    private boolean fileExists(String url) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            throw new AppRuntimeException(e);
        }
    }

    /**
     * True in object is null or object is a String with values (not blank)
     */
    private boolean isNullOrStringBlank(Object o) {
        if (o == null) {
            return true;
        } else {
            if (isString(o)) {
                return !StringUtils.isNotBlank((String) o);
            } else {
                return false;
            }
        }
    }

    private boolean isString(Object o) {
        return o.getClass().getName().equals("java.lang.String");
    }

}
/*% } %*/
