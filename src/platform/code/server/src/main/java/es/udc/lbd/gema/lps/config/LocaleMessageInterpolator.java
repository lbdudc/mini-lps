/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.config;

import java.util.Locale;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * This class forces jakarta.validation to use localized messages. <br>
 * It uses spring's LocaleContextHolder class to access the requested application Locale
 */
public class LocaleMessageInterpolator extends ResourceBundleMessageInterpolator {

  @Override
  public String interpolate(final String messageTemplate, final Context context) {
    return interpolate(messageTemplate, context, LocaleContextHolder.getLocale());
  }

  @Override
  public String interpolate(
      final String messageTemplate, final Context context, final Locale locale) {
    return super.interpolate(messageTemplate, context, locale);
  }
}
/*% } %*/
