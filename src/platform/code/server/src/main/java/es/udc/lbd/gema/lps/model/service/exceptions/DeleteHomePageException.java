/*% if (data.statics && data.statics.find(s => s.id == 'Home')) { %*/
package es.udc.lbd.gema.lps.model.service.exceptions;

public class DeleteHomePageException extends OperationNotAllowedException {
  public DeleteHomePageException() {
    super("static_editor.errors.removing_home_page");
  }
}
/*% } %*/
