/*%@ return data.dataModel.enums.map(function(en) {
    return {
        fileName: normalize(en.name, true) + '.java',
        context: en
    };
}) %*/
package es.udc.lbd.gema.lps.model.domain;

public enum /*%= normalize(context.name, true) %*/ {
    /*%= context.values.join(', ') %*/
}
