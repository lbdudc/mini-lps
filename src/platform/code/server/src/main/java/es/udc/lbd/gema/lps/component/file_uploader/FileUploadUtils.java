/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by sfernandez on 07/12/2016.
 */
public class FileUploadUtils {

    /**
     * Removes <string>file</strong> or <strong>folder<strong> quietly (Recursively). <br>
     * Use with precaution!
     *
     * @param file
     */
    public static void removeFileOrFolder(File file) {
        FileUtils.deleteQuietly(file);
    }

    /**
     * Normalices a string.
     *
     * @param fileName Input string
     * @return Normalized string
     */
    public static String normaliceFileName(String fileName) {
        String name = fileName;

        name = name.toLowerCase();
        name = name.replaceAll("á", "a");
        name = name.replaceAll("é", "e");
        name = name.replaceAll("í", "i");
        name = name.replaceAll("ó", "o");
        name = name.replaceAll("ú", "u");
        name = name.replaceAll("à", "a");
        name = name.replaceAll("è", "e");
        name = name.replaceAll("ì", "i");
        name = name.replaceAll("ó", "o");
        name = name.replaceAll("ú", "u");
        name = name.replaceAll("â", "a");
        name = name.replaceAll("ê", "e");
        name = name.replaceAll("î", "i");
        name = name.replaceAll("ô", "o");
        name = name.replaceAll("û", "u");
        name = name.replaceAll("ä", "a");
        name = name.replaceAll("ë", "e");
        name = name.replaceAll("ï", "i");
        name = name.replaceAll("ö", "o");
        name = name.replaceAll("ü", "u");
        name = name.replaceAll("º", "");
        name = name.replaceAll("ª", "");
        name = name.replaceAll("ñ", "n");

        name = org.springframework.util.StringUtils.trimAllWhitespace(name);
        name = name.replaceAll("[^a-zA-Z0-9.-]", "_");

        return name;
    }
}
/*% } %*/
