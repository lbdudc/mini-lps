import Vue from "vue";
import Vuetify from "vuetify/lib";
import i18n from "./i18n";

Vue.use(Vuetify);

export default new Vuetify({
  /*% if (getExtraConfigFromSpec(data, "primary_color", "")) { %*/
  theme: {
    themes: {
      light: { primary: /*%= JSON.stringify(getExtraConfigFromSpec(data, "primary_color", "")) %*/ },
    },
  },
  /*% } %*/
  lang: {
    t: (key, ...params) => i18n.t(key, params)
  },
  icons: {
    iconfont: "mdi"
  }
});
