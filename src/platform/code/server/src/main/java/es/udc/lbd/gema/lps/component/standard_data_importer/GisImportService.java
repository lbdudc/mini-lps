/*% if (feature.DM_DI_DF_Shapefile) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer;

import org.locationtech.jts.geom.Geometry;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.FormatGis;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.ImportFileJSON;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.ImportParamsGis;
import es.udc.lbd.gema.lps.component.standard_data_importer.custom.ParseFormatJSON;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.FileNotSupportedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.TypeNotSupportedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportEncodingNotProvidedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportFileAndUrlNotProvidedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportGisFileNotProvidedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportGisFileRepeatedException;
import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client.ImportGisMissingFilesException;
import es.udc.lbd.gema.lps.config.Properties;
import es.udc.lbd.gema.lps.model.service.exceptions.AppRuntimeException;
import es.udc.lbd.gema.lps.model.repository._repositoriesBasePackage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class GisImportService extends StandardDataImportService {

    private static final Logger logger = LoggerFactory.getLogger(GisImportService.class);

    @Inject
    private Properties properties;

    private final String IMPORT_TYPE = "shapefile";
    private final String ZIP = "zip";

    /**
     * Valid GIS file formats:
     * <ul>
     * <li><strong>.shp</strong>: Shapes (Mandatory)</li>
     * <li><strong>.dbf</strong>: Attributes (Mandatory)</li>
     * <li><strong>.shx</strong>: Shapes to attributes index</li>
     * <li><strong>.prj</strong>: Projection</li>
     * <li><strong>.qix</strong>: Quadtree spatial index</li>
     * <li><strong>.fix</strong>: Feature id index</li>
     * <li><strong>.sld</strong>: Style-layer-descriptor style xml object</li>
     * <li><strong>.sbn</strong>: attribute index</li>
     * <li><strong>.sbx</strong>: spatial index</li>
     * <li><strong>.lyr</strong>: arcmap-only style object</li>
     * <li><strong>.avl</strong>: arcview style object</li>
     * <li><strong>.shp.xml</strong>: fgdc metadata</li>
     * </ul>
     */
    private static final Set<String> validExtensions = new HashSet<String>(
            Arrays.asList("shp", "shx", "dbf", "prj", "qix", "fix", "sld", "sbn", "sbx", "lyr", "avl", "xml"));

    @Override
    ImportFileJSON uploadAndProcessHeader(MultipartHttpServletRequest request) throws ImportException, FileNotSupportedException, TypeNotSupportedException {
        // Obtain parameters from request
        ImportParamsGis p = getParamsHeader(request);

        ImportFileJSON result = new ImportFileJSON();

        if (p.getFile() != null) {
            // Store the uploaded file in a temporary folder
            // This file will be deleted after a successful import or next day
            // via a cron job
            File zip = fileUploadImport.saveTemporaryFile(p.getFile());
            File shp = extractZip(zip);

            result.setValues(readHeaderFromGis(shp));
            result.setTemporaryFile(zip.getName());
            return result;
        } else {
            throw new ImportFileAndUrlNotProvidedException();
        }

    }

    /**
     * Obtains parameters from request.
     *
     * @param request the request
     * @return Parameters obtained from request
     * @throws ImportException
     *             No file or was provided
     * @throws FileNotSupportedException
     *             File format not supported
     */
    private ImportParamsGis getParamsHeader(MultipartHttpServletRequest request) throws ImportException, FileNotSupportedException, TypeNotSupportedException {

        // Get params form request
        ImportParamsGis p = new ImportParamsGis();

        // Check type
        String type = request.getParameter("type");
        if (!type.equals(IMPORT_TYPE)) {
            throw new TypeNotSupportedException();
        }

        p.setFile(request.getFile("file"));

        // Check if provided fileFormat is supported
        if (p.getFile() == null || !ZIP.equalsIgnoreCase(p.getExtension())) {
            throw new FileNotSupportedException();
        }

        // Check if encoding is provided
        if (StringUtils.isBlank(request.getParameter("encoding"))) {
            throw new ImportEncodingNotProvidedException();
        }
        p.setEncoding(request.getParameter("encoding"));

        return p;
    }

    /**
     * Validates zip file, extracts it and return the shp file
     *
     * @param zip zip file
     * @return the .shp file
     * @throws ImportException if any controlled exception was detected
     */
    private File extractZip(File zip) throws ImportException {
        logger.debug("Received zip file: " + zip.getName());

        try (ZipFile zipFile = new ZipFile(zip)) {

            // Obtain file entries (Only those with valid extensions):
            Predicate<ZipEntry> isFile = e -> !e.isDirectory();
            Predicate<ZipEntry> isGisFile = e -> validExtensions.contains(FilenameUtils.getExtension(e.getName()));
            List<ZipEntry> entries = zipFile.stream().filter(isFile).filter(isGisFile).collect(Collectors.toList());

            // Show files inside zip
            logger.debug("Files inside zip:");
            entries.forEach(r -> logger.debug("=> " + r.getName()));

            checkFilesProvided(entries);

            // Extract zip
            String zipFilename = FilenameUtils.getBaseName(zip.getName());
            File folder;
            try {
                folder = fileUploadImport.getTemporaryFile(zipFilename);
                FileUtils.forceMkdir(folder);
            } catch (IOException e) {
                throw new AppRuntimeException("Unable to obtain temporary folder to extract zip file", e);
            }

            File shpFile = null;

            // For each entry => Extract file into subfolder
            for (ZipEntry zipEntry : entries) {
                String fileName = folder + File.separator + FilenameUtils.getBaseName(zip.getName()) + "."
                        + FilenameUtils.getExtension(zipEntry.getName());
                File unzippedFile = new File(fileName);

                // Extract file
                try (InputStream is = zipFile.getInputStream(zipEntry)) {
                    try (FileOutputStream fos = new FileOutputStream(unzippedFile)) {
                        IOUtils.copy(is, fos);
                    }
                } catch (IOException e) {
                    throw new AppRuntimeException("Unable to extract zip file", e);
                }

                if (FilenameUtils.getExtension(unzippedFile.getName()).equalsIgnoreCase("shp")) {
                    shpFile = unzippedFile;
                }
            }

            return shpFile;
        } catch (IOException e) {
            throw new AppRuntimeException("Unable to process zip file", e);
        }
    }

    /**
     * Reads the first entry of the file
     *
     * @param file
     *            shp file
     * @return Array of columns name. First column renamed to 'GEOM'
     */
    private String[] readHeaderFromGis(File file) {
      SimpleFeatureSource source;
      FileDataStore dataStore;
      try {
        dataStore = FileDataStoreFinder.getDataStore(file);
        source = dataStore.getFeatureSource();
      } catch (IOException e) {
        throw new AppRuntimeException("Unable to read shapefile from " + file.getAbsolutePath(), e);
      }
      SimpleFeatureType schema = source.getSchema();

      // We need only the first entry
      Query query = new Query(schema.getTypeName());
      query.setMaxFeatures(1);

      ArrayList<String> columns = new ArrayList<>();

      FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
      try {
        collection = source.getFeatures(query);
      } catch (IOException e) {
        throw new AppRuntimeException("Unable to obtain features", e);
      }
      try (FeatureIterator<SimpleFeature> features = collection.features()) {
        while (features.hasNext()) {
          SimpleFeature feature = features.next();
          logger.debug("Read line: " + feature.getID());

          for (Property prop : feature.getProperties()) {

            logger.debug("Column \t" + prop.getName() + ":" + prop.getValue());
            if (prop.getName().getLocalPart().equals("the_geom")) {
              columns.add("GEOM");
            } else {
              columns.add(
                StringUtils.capitalize(StringUtils.lowerCase(prop.getName().getLocalPart())));
            }
          }
        }
      } finally {
        dataStore.dispose();
      }

      return columns.toArray(new String[0]);
    }

    @Override
    File processFile(ParseFormatJSON format) throws ImportException, FileNotSupportedException, TypeNotSupportedException {

        FormatGis params = getParamsBody(format);

        // Validate format
        validateFormat(params.getFormat());

        // Obtain target object class and instance
        Class<?> clazzObject;
        try {
            clazzObject = Class.forName(params.getEntityClazz());
        } catch (ClassNotFoundException e) {
            throw new AppRuntimeException("Unable to obtain entity class", e);
        }

        // Obtain repository class and save method
        String clazzRepositoryPackage = StringUtils.removeEnd(_repositoriesBasePackage.class.getName(),
                _repositoriesBasePackage.class.getSimpleName());
        String clazzRepositoryName = clazzObject.getSimpleName() + "Repository";
        Class<?> clazzRepository;
        try {
            clazzRepository = Class.forName(clazzRepositoryPackage + clazzRepositoryName);
        } catch (ClassNotFoundException e) {
            throw new AppRuntimeException("Unable to obtain entity class", e);
        }

        // WARNING: We need to declare a .save method in every Repository class!
        // => Try to avoid this
        Method saveMethod;
        try {
            saveMethod = clazzRepository.getMethod("save", clazzObject);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new AppRuntimeException("Unable to obtain 'save' method for class " + clazzRepositoryName, e);
        }

        this.importFromGisFile(params, clazzObject, clazzRepositoryName, saveMethod);

        return fileUploadImport.getTemporaryFile(params.getTemporaryFileName());
    }

    /**
     * Check format values
     *
     * @param format the format
     * @return Parameters
     * @throws ImportException
     *             No file or url were provided
     * @throws FileNotSupportedException
     *             File format not supported
     */
    private FormatGis getParamsBody(ParseFormatJSON format) throws ImportException, FileNotSupportedException, TypeNotSupportedException {

        // Check type
        if (!format.getType().equals(IMPORT_TYPE)) {
            throw new TypeNotSupportedException();
        }

        FormatGis p = new FormatGis();

        // Check provided file is .zip
        p.setTemporaryFileName(format.getFile());

        // Check if provided fileFormat is supported
        if (p.getTemporaryFileName() == null) {
            throw new ImportGisFileNotProvidedException();
        } else if (!ZIP.equalsIgnoreCase(p.getExtension())) {
            throw new FileNotSupportedException();
        }

        // Retrieve .shp file
        p.setFile(getTemporaryShpFile(p.getTemporaryFileName()));

        // Get rest of parameters
        p.setEntityClazz(format.getEntityName());
        p.setFormat(format.getColumns());

        // Check if encoding is provided
        if (StringUtils.isBlank(format.getEncoding())) {
            throw new ImportEncodingNotProvidedException();
        }
        p.setEncoding(format.getEncoding());

        return p;
    }

    /**
     * Retrieves the .shp file uncrompressed in temporary subfolder
     *
     * @param temporaryZipName filename of temporary zip file
     * @return the file
     */
    private File getTemporaryShpFile(String temporaryZipName) {

        String baseName = StringUtils.substringBeforeLast(temporaryZipName, ".");
        String shpName = baseName + File.separator + baseName + ".shp";

        return fileUploadImport.getTemporaryFile(shpName);
    }

    /**
     * Reads gis file. <br>
     */
    private void importFromGisFile(FormatGis p, Class<?> clazzObject, String clazzRepositoryName, Method saveMethod)
            throws ImportException {
        logger.debug("Received shp file: " + p.getFile().getName());

        SimpleFeatureSource source;
        try {
            ShapefileDataStore sds = (ShapefileDataStore) FileDataStoreFinder.getDataStore(p.getFile());
            source = sds.getFeatureSource();
            sds.setCharset(Charset.forName(p.getEncoding()));
        } catch (IOException e) {
            throw new AppRuntimeException("Unable to read shapefile from " + p.getFile().getAbsolutePath(), e);
        }

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
        try {
            collection = source.getFeatures();
        } catch (IOException e) {
            throw new AppRuntimeException("Unable to read features", e);
        }

        CoordinateReferenceSystem crs = collection.getSchema().getCoordinateReferenceSystem();
        Integer epsg;

        MathTransform transformer;
        Integer targetEpsg;
        try {
            epsg = CRS.lookupEpsgCode(crs, true);
            targetEpsg = properties.getGis().getDefaultSrid();

            // Note: To reverse X & Y properties
            CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
            CoordinateReferenceSystem targetCrs = factory.createCoordinateReferenceSystem("EPSG:" + targetEpsg);

            transformer = CRS.findMathTransform(crs, targetCrs);

        } catch (FactoryException e) {
            throw new AppRuntimeException("Unable to obtain EPSG ", e);
        }
        try (FeatureIterator<SimpleFeature> features = collection.features()) {

            // For each entry we read all values (Geom and attributes)
            while (features.hasNext()) {
                ArrayList<Object> values = new ArrayList<Object>();

                try {
                    SimpleFeature feature = features.next();
                    for (Property attribute : feature.getProperties()) {
                        Object value = attribute.getValue();
                        if (value instanceof Geometry) {
                            // FIXME: Try to set SRID in a different place
                            ((Geometry) value).setSRID(epsg);
                            value = JTS.transform((Geometry) value, transformer);
                            ((Geometry) value).setSRID(targetEpsg);

                        }
                        values.add(value);
                    }
                } catch (MismatchedDimensionException | TransformException e) {
                    throw new AppRuntimeException("Unable to transform geometries");
                }

                // Populate entity fields
                Object instanceObject = fillObjectWithData(p.getFormat(), values.toArray(new Object[0]), clazzObject);
                // Save entity
                saveEntity(clazzRepositoryName, saveMethod, instanceObject);
            }

        }
    }

    /**
     * Checks:
     * <ul>
     * <li>No more than one file with the same extension inside the zip
     * file</li>
     * <li>dbf and shp files are provided inside the zip file</li>
     * </ul>
     */
    private void checkFilesProvided(List<ZipEntry> entries) throws ImportException {

        Set<String> extensions = new HashSet<String>();

        for (ZipEntry e : entries) {
            String ext = FilenameUtils.getExtension(e.getName());
            if (extensions.contains(ext)) {
                throw new ImportGisFileRepeatedException(ext);
            }
            extensions.add(ext);
        }

        if (!extensions.contains("dbf") || !extensions.contains("shp")) {
            throw new ImportGisMissingFilesException();
        }
    }

}
/*% } %*/
