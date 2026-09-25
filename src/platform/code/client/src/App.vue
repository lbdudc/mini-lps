<template>
  <v-app>
    <v-app-bar app color="primary" dark dense>
      <v-app-bar-nav-icon @click.stop="drawer = !drawer"></v-app-bar-nav-icon>
      <img v-if="logo" :src="logo" alt="" class="app-logo" />
      <v-toolbar-title>{{ appName }}</v-toolbar-title>
    </v-app-bar>

    <v-navigation-drawer v-model="drawer" app temporary>
      <v-list nav dense>
        <v-list-item-group color="primary">
          <v-list-item
            v-for="item in navItems"
            :key="item.name"
            :to="{ name: item.name }"
            @click="drawer = false"
          >
            <v-list-item-title>{{ $t(item.label) }}</v-list-item-title>
          </v-list-item>
        </v-list-item-group>
      </v-list>
    </v-navigation-drawer>

    <notifications :max="3" :width="300" position="top center" />
    <v-main>
      <router-view class="content">
        <!-- -->
      </router-view>
    </v-main>
  </v-app>
</template>

<script>
// Every route already carries a meta.label i18n key (router.js, and each
// entity module's _router.js) and the locale files' `menu` block already
// has strings waiting for them -- nothing here reads any new data, it just
// surfaces navigation that already existed with no way to reach it (see
// mini-lps App.vue's "app shell" enhancement).
//
// Only top-level, always-meaningful destinations are listed: MapViewer,
// ChartViewer, the shapefile importer, and each entity's list view. Entity
// Form/Create/Detail routes are reached contextually (from a list or the
// map), not from global nav, so they're deliberately excluded here.
const NAV_ROUTE_NAME_PATTERN = /^(MapViewer|chartViewer|about|Shapefile)$| List$/;

import properties from "@/properties";

export default {
  name: "App",
  data() {
    return {
      drawer: false,
      appName: properties.APP_NAME,
      logo: properties.LOGO,
    };
  },
  mounted() {
    /* The tooltips and controls that use --appColor follow the branding colour */
    if (properties.PRIMARY_COLOR) {
      document.documentElement.style.setProperty(
        "--appColor",
        properties.PRIMARY_COLOR
      );
    }
  },
  computed: {
    navItems() {
      return this.$router.options.routes
        .filter(
          (route) =>
            route.meta &&
            route.meta.label &&
            NAV_ROUTE_NAME_PATTERN.test(route.name || "")
        )
        .map((route) => ({ name: route.name, label: route.meta.label }));
    },
  },
};
</script>

<style scoped>
.app-logo {
  height: 28px;
  max-width: 140px;
  object-fit: contain;
  margin-right: 12px;
}
</style>
