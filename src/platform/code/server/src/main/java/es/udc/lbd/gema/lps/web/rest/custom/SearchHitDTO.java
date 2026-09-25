/*% if (feature.MV_T_F_BasicSearch) { %*/
package es.udc.lbd.gema.lps.web.rest.custom;

/** A search result for the map: what to call it, and where it is. */
public class SearchHitDTO {

  private Object id;
  private String displayString;
  /** xmin, ymin, xmax, ymax in the map's coordinates (degrees), or null when it has no geometry. */
  private double[] bbox;

  public SearchHitDTO(Object id, String displayString, double[] bbox) {
    this.id = id;
    this.displayString = displayString;
    this.bbox = bbox;
  }

  public Object getId() {
    return id;
  }

  public String getDisplayString() {
    return displayString;
  }

  public double[] getBbox() {
    return bbox;
  }
}
/*% } %*/
