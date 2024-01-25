import Vue from "vue";
import VueI18n from "vue-i18n";

Vue.use(VueI18n);

/*%
  data.basicData.languages.forEach(function(lang) {
%*/
const locale/*%= normalize(lang, true) %*/ = require.context("../locale//*%= normalize(lang) %*/", true, /\.json$/);
/*%
  });
%*/

const messages = {
/*%
  data.basicData.languages.forEach(function(lang) {
%*/
  /*%= lang.toLocaleUpperCase() %*/: {},
/*%
  });
%*/
};

/*%
  data.basicData.languages.forEach(function(lang) {
%*/
locale/*%= normalize(lang, true) %*/.keys().forEach(filename => {
  Object.keys(locale/*%= normalize(lang, true) %*/(filename)).forEach(key => {
    messages./*%= lang.toLocaleUpperCase() %*/[key] = locale/*%= normalize(lang, true) %*/(filename)[key];
  });
});
/*%
  });
%*/

const i18n = new VueI18n({
  locale: localStorage.getItem("lang") || "/*%= data.basicData.languages[0].toLocaleUpperCase() %*/", // set locale
  fallbackLocale: "/*%= data.basicData.languages[0].toLocaleUpperCase() %*/", // set fallback locale
  messages // set locale messages
});

export default i18n;
