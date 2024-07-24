/*%@ return data.lists
      .filter(function(en) {
         return !en.abstract;
      })
      .map(function(list) {
        return {
          fileName: normalize(list.entity) + '/' + 'components' + '/' + normalize(list.id) +'List.vue',
          context: list
          };
      });
%*/
 /*%
  var entity = getEntity(data, context.entity);
  var hasAutocomplete = false;
  var hasBooleanFilter = false;
  var hasDebouncedTextField = false;
  var hasEntityProperty = false;
  var hasNumberField = false;
  var hasDateFilter = false;
  var hasDateTimeFilter = false;
  var entityIdProperty = getEntityProperty(entity, 'id');
  var pkName = normalize(entityIdProperty.name);
  var hasGeographicProperties = checkEntityContainsGeographicProperties(entity);
  var theForm = data.forms.find((form) => form.id == context.form);
  const properties = context.properties.map(e => { return { fromEntity: e.owner || context.entity, ...e }; });
  properties.forEach((prop) => {
    /* entityProp -> definition of the property from the entity */
    var propOwner = getEntity(data, prop.fromEntity);
    prop.entityProp = getProperty(propOwner, prop.property);
    /* entityProperty -> Entity to which the property belongs */
    prop.entityProperty = getEntity(data, prop.entityProp.class);
    prop.enumProperty = getEnum(data, prop.entityProp.class);
  });

 %*/
<template>
  <v-container v-if="items">
    <v-card>
      <v-card-title>
        <v-row align="center" justify="space-between" no-gutters>
          <v-col cols="auto" class="d-none d-md-flex">
            <span class="headline no-split-words">
              {{ $t($route.meta.label) }}
            </span>
            <v-btn class="ml-3" icon>
              <v-icon @click="routeBack">mdi-arrow-left</v-icon>
            </v-btn>
          </v-col>
          /*% if (context.filtering) { %*/
          <v-col
            cols="12"
            sm="7"
            md="4"
            lg="6"
            xl="8"
            order="2"
            order-sm="1"
            class="text-center text-sm-right mt-4 mt-sm-0"
          >
            <v-btn
              color="primary"
              outlined
              @click="showFilters = !showFilters"
            >
              <v-icon left dark>mdi-magnify</v-icon>
              <span> {{ showFilters ? $t("hideFilters") : $t("showFilters") }} </span>
              <v-icon right dark v-if="showFilters">mdi-chevron-up</v-icon>
              <v-icon right dark v-else>mdi-chevron-down</v-icon>
            </v-btn>
          </v-col>
          /*% } %*/
          /*% if (context.searching) {
            hasDebouncedTextField = true; %*/
          <v-col
            cols="12"
            md="4"
            lg="7"
            xl="8"
            order="2"
            order-md="1"
            class="d-md-inline-block text-right mt-4 mt-md-0"
          >
            <debounced-text-field
              v-model="search"
              append-icon="search"
              class="d-md-inline-block"
              dense
              hide-details
              :label="$t('search')"
              @input="redirectOnFilterChange"
            ></debounced-text-field>
          </v-col>
          /*% } %*/
          <v-col order="1" order-md="2" class="text-right">
            /*% if (context.form && theForm.creatable) { %*/
            <v-btn
              color="success ml-2"
              :to="{ name: '/*%= theForm.id %*/Create' }"
            >
              <v-icon>add</v-icon>
              <span class="d-none d-sm-block"> {{ $t("new") }} </span>
            </v-btn>
            /*% } %*/
          </v-col>
        </v-row>
      </v-card-title>

      <v-card-text>
        /*% if (context.filtering) { %*/
        <v-row align="center" v-show="showFilters" justify="space-between">
          /*%
          properties.forEach((prop) => {
            var autoinc = prop.entityProp.class.indexOf('autoinc') != -1;
            var entityPropertyPK = getEntityProperty(prop.entityProperty, 'id');
            %*/
            /*% if (prop.entityProp.class == 'Boolean') { %*/
              /*% hasBooleanFilter = true; %*/
          <v-col cols="12" md="6" xl="3">
            <v-row>
              <v-col cols="6">
                <v-switch
                  dense
                  :disabled="!/*%= normalize(prop.entityProp.name) %*/FilterAllowed"
                  @change="redirectOnFilterChange"
                  v-model="/*%= normalize(prop.entityProp.name) %*/Filter"
                  class="ma-2 tp-0"
                  :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
                ></v-switch>
              </v-col>
              <v-col cols="6">
                <v-tooltip top>
                  <template v-slot:activator="{ on }">
                    <v-checkbox
                      dense
                      v-on="on"
                      @change="handleBooleanFilter('/*%= normalize(prop.entityProp.name) %*/')"
                      v-model="/*%= normalize(prop.entityProp.name) %*/FilterAllowed"
                      :label="$t('activateFilter.label')"
                    ></v-checkbox>
                  </template>
                  <span>{{ $t("activateFilter.tooltip", { param: $t("t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/") }) }}</span>
                </v-tooltip>
              </v-col>
            </v-row>
          </v-col>
            /*% } else if ( prop.entityProp.class == 'Long' || prop.entityProp.class == 'Integer' || prop.entityProp.class == 'BigDecimal' || prop.entityProp.class == 'Float' || prop.entityProp.class == 'Double' ) {  %*/
              /*% hasNumberField = true; %*/
          <v-col cols="6" md="2" xl="1">
            <number-field
              :debouncing="300"
              v-model="/*%= normalize(prop.entityProp.name) %*/Filter"
              type="/*%= normalize(prop.entityProp.class) %*/"
              :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
            ></number-field>
          </v-col>
            /*% } else if ( prop.entityProp.class == 'Text' || prop.entityProp.class == 'String' ) { %*/
              /*% hasDebouncedTextField = true; %*/
          <v-col cols="6" md="2" xl="1">
            <debounced-text-field
              dense
              v-model="/*%= normalize(prop.entityProp.name) %*/Filter"
              :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
            ></debounced-text-field>
          </v-col>
            /*% } else if ( prop.entityProp.class == 'Date') { %*/
              /*% hasDateFilter = true; %*/
          <v-col cols="12" md="4" xl="2">
            <dateAndHourPicker
              :datePickerProp="{data: /*%= normalize(prop.entityProp.name) %*/Filter, label: $t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')}"
              @update-time="updateDateTime('/*%= normalize(prop.entityProp.name) %*/Filter', false, ...arguments)"
            ></dateAndHourPicker>
          </v-col>
            /*% } else if ( prop.entityProp.class == 'DateTime') { %*/
              /*% hasDateTimeFilter = true; %*/
          <v-col cols="12" md="6" xl="4">
            <dateAndHourPicker
              :datePickerProp="{data: /*%= normalize(prop.entityProp.name) %*/Filter, label: $t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')}"
              :timePickerProp="{data: /*%= normalize(prop.entityProp.name) %*/Filter, label: $t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')}"
              @update-time="updateDateTime('/*%= normalize(prop.entityProp.name) %*/Filter', true, ...arguments)"
            >
            </dateAndHourPicker>
          </v-col>
            /*% } else if ( prop.enumProperty ) { %*/
          <v-col cols="12" md="2" xl="1">
            <v-select
              dense
              @change="redirectOnFilterChange"
              clearable
              :items="/*%= prop.enumProperty.name.toLowerCase() %*/Property"
              :item-text="item => $t(item.text)"
              item-value="value"
              :menu-props="{ offsetY: true }"
              v-model="/*%= normalize(prop.entityProp.name) %*/Filter"
              :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
            >
            </v-select>
          </v-col>
            /*% } else if ( prop.entityProperty ) {
              if ( prop.entityProp.owner ) {
                hasAutocomplete = true;
                hasEntityProperty = true; %*/
          <v-col cols="12" md="2" xl="1">
            <autocomplete
              dense
              no-filter
              solo
              :debouncing="300"
              @change="redirectOnFilterChange"
              :items="/*%= normalize(prop.entityProp.name) %*/s.items"
              :loading="/*%= normalize(prop.entityProp.name) %*/s.loading"
              :search-input.sync="/*%= normalize(prop.entityProp.name) %*/Search"
              v-model="/*%= normalize(prop.entityProp.name) %*/Filter"
              :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
                /*% if (prop.entityProp.multiple) { %*/ multiple /*% } %*/
              item-text="/*%= getDisplayStringName(prop.entityProperty) %*/"
              item-value="/*%= prop.entityProperty.properties.find(prop => prop.pk).name %*/"
            >
            </autocomplete>
          </v-col>
              /*% } %*/
            /*% } %*/
          /*% }); %*/
        </v-row>
        /*% } %*/
        <v-data-table
          :headers="headers"
          :items="items"
          /*% if (context.searching) { %*/
          :search="search"
          /*% } %*/
          :options="entitiesPage"
          :server-items-length="totalItems"
          :loading="loading"
          :footer-props="tableFooterProps"
          /*% if (context.form) { %*/
          @click:row="entityDetail"
          /*% } %*/
          @update:options="redirectOnTableChange"
        >
        /*%
        properties.forEach((prop) => {
          var entityPropertyPK = getEntityProperty(prop.entityProperty, 'id');
          if (prop.enumProperty !== undefined) { %*/
            <template v-slot:[`item./*%= normalize(prop.property) %*/`]="{ item }">
              <span v-if="item./*%= normalize(prop.property) %*/">
                {{ $t(`/*%= normalize(prop.enumProperty.name.toLowerCase()) %*/.${item./*%= normalize(prop.property) %*/}`) }}
              </span>
            </template>
          /*% }
          if (prop.entityProperty) {
            if (prop.entityProp.multiple){ %*/
          <template v-slot:[`item./*%= normalize(prop.property) %*/`]="{item}">
              /*%
              var theList = data.lists.find(function(list) { return list.entity == prop.entityProperty.name; });
              if (theList) { %*/
            <router-link
              :to="{ name: '/*%= theList.id %*/'}"
            >
              {{displayManyRelationship(item./*%= normalize(prop.property) %*/ , '/*%= getDisplayStringName(prop.entityProperty) %*/' )}}
            </router-link>
              /*% } else { %*/
            <span>
              {{displayManyRelationship(item./*%= normalize(prop.property) %*/ , '/*%= getDisplayStringName(prop.entityProperty) %*/' )}}
            </span>
              /*% } %*/
          </template>
           /*% } else { %*/

          <template v-slot:[`item./*%= normalize(prop.property) %*/`]="{item}">
              /*%
              var aForm = data.forms.find(function(form) { return form.entity == prop.entityProperty.name; });
              if (aForm) { %*/
            <router-link
              v-if="item./*%= normalize(prop.property) %*/"
              :to="{
                name: '/*%= aForm.id %*/Detail',
                params: { id: item./*%= normalize(prop.property) %*/./*%= normalize(entityPropertyPK.name) %*/ }
              }"
            >
             {{item./*%= normalize(prop.property) %*/./*%= getDisplayStringName(prop.entityProperty) %*/}}
            </router-link>
              /*% } else { %*/
            <span
              v-if="item./*%= normalize(prop.property) %*/"
            >
             {{item./*%= normalize(prop.property) %*/./*%= getDisplayStringName(prop.entityProperty) %*/}}
            </span>
              /*% } %*/
          </template>
          /*% }
            } else { %*/
              /*% if (prop.entityProp.class === 'Date') { %*/
          <template v-slot:[`item./*%= normalize(prop.property) %*/`]="{item}">
            {{item./*%= normalize(prop.property) %*/ | dateFormat}}
          </template>
            /*% } else if (prop.entityProp.class === 'DateTime') { %*/
          <template v-slot:[`item./*%= normalize(prop.property) %*/`]="{item}">
            {{item./*%= normalize(prop.property) %*/ | dateTimeFormat}}
          </template>
            /*% }
            }
          }); %*/

          /*% if (context.form && context.formLink) { %*/
          <template v-slot:[`item.action`]="{ item }">
            <v-icon
              color="primary"
              @click.stop="entityDetail(item)">
              description
            </v-icon>
          </template>
          /*% } %*/
        </v-data-table>
      </v-card-text>
    </v-card>

  </v-container>
</template>

<script>
/*% if (hasAutocomplete) { %*/
import Autocomplete from "@/components/debouncing-inputs/Autocomplete.vue";
/*% } %*/
/*% if (hasDebouncedTextField) { %*/
import DebouncedTextField from "@/components/debouncing-inputs/DebouncedTextField.vue";
/*% } %*/
/*% if (hasNumberField) { %*/
import NumberField from "@/components/number-field/NumberField.vue";
/*% } %*/
/*% if (hasDateFilter || hasDateTimeFilter) { %*/
import DateAndHourPicker from "@/components/calendar/DateAndHourPicker.vue";
/*% } %*/

import defaultPaginationSettings from "@/common/default-pagination-settings";
/*% if (context.sorting) { %*/
import {
  generateSort,
  parseStringToSortBy,
  parseStringToSortDesc,
} from "@/common/pagination-utils";
/*% } %*/
import tableFooterProps from "@/common/table-footer-props";

/*%
  const processedEnums = new Set();
  properties.forEach(function(prop) {
    if (prop.enumProperty && !processedEnums.has(prop.enumProperty.name)) {
      processedEnums.add(prop.enumProperty.name); %*/
import /*%= prop.enumProperty.name.toLowerCase() %*/Property from "@/enumerates//*%= normalize(prop.enumProperty.name.toLowerCase()) %*/";
/*% }
  });
%*/
import displayManyRelationship from "@/common/DisplayManyRelationships";
import RepositoryFactory from "@/repositories/RepositoryFactory"
const /*%= normalize(context.entity, true) %*/EntityRepository = RepositoryFactory.get("/*%= normalize(context.entity, true) %*/EntityRepository");
/*% if (context.filtering) {
    const processedEntities = new Set([entity.name]);
    properties.forEach((prop) => {
      if (prop.entityProperty && prop.entityProp.owner && !processedEntities.has(prop.entityProperty.name)) {
        processedEntities.add(prop.entityProperty.name); %*/
const /*%= normalize(prop.entityProperty.name, true) %*/EntityRepository = RepositoryFactory.get("/*%= normalize(prop.entityProperty.name, true) %*/EntityRepository");
      /*% } %*/
    /*% }); %*/
/*% } %*/

export default {
  name: "/*%= normalize(context.id) %*/List",
  components: {
    /*% if (hasDateFilter || hasDateTimeFilter) { %*/
    DateAndHourPicker,
    /*% } %*/
    /*% if (hasAutocomplete) { %*/
    Autocomplete,
    /*% } %*/
    /*% if (hasDebouncedTextField) { %*/
    DebouncedTextField,
    /*% } %*/
    /*% if (hasNumberField) { %*/
    NumberField
    /*% } %*/
  },
  data() {
    return {
      items: [],
    /*% if (context.filtering) { %*/
      showFilters: false,
      /*% const processedEnumsData = new Set();
          properties
            .forEach(function(prop) {
              if (prop.enumProperty && !processedEnumsData.has(prop.enumProperty.name)) {
                processedEnumsData.add(prop.enumProperty.name); %*/
      /*%= prop.enumProperty.name.toLowerCase() %*/Property: /*%= prop.enumProperty.name.toLowerCase() %*/Property,
      /*%     }
              if ( prop.entityProperty) {
                if ( prop.entityProp.owner ) { %*/
      /*%= normalize(prop.entityProp.name) %*/Search : null,
      /*%       }
              }
            });
      %*/
    /*% } else if (context.searching) { %*/
      search: null,
    /*% } %*/
    /*% if (context.sorting) { %*/
    /*% } %*/
      /*% if (context.filtering) { %*/
        /*% properties.forEach((prop) => { %*/
      /*%= normalize(prop.property) %*/Filter: null,
          /*% if (prop.entityProp.class == 'Boolean') { %*/
      /*%= normalize(prop.property) %*/FilterAllowed: false,
          /*% } %*/
        /*% }); %*/
      /*% } %*/
      entitiesPage: {
        page:
          parseInt(this.$route.query.page) || defaultPaginationSettings.page,
        itemsPerPage:
          parseInt(this.$route.query.pageSize) ||
          defaultPaginationSettings.itemsPerPage,
        /*% if (context.sorting) { %*/
        sortBy: parseStringToSortBy(this.$route.query.sort),
        sortDesc: parseStringToSortDesc(this.$route.query.sort),
        /*% } %*/
      },
      /*% if (context.filtering) { %*/
        /*% properties
            .forEach(function(prop) {
              if (prop.entityProperty) {
                var entityPropertyProp = getProperty(prop.entityProperty, prop.entityProp.bidirectional);
                if (prop.entityProp.owner) {
        %*/
      /*%= normalize(prop.entityProp.name) %*/s: {
        items: [],
        loading: false
      },
        /*%       }
                }
              });
        %*/
      /*% } %*/
      totalItems: 0,
      loading: false,
      tableFooterProps,
    };
  },
  computed: {
    headers() {
      return [
        /*% properties.forEach((prop) => { %*/
        {
          text: this.$t("t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.property) %*/"),
          /*% if (!context.sorting) { %*/
          sortable: false,
          /*% } %*/
          value: "/*%= normalize(prop.property) %*/"
        },
        /*% }); %*/
        { text: "", sortable: false, value: "action" }
      ];
    },
    /*% if (context.filtering) { %*/
    filters() {
      let filters = "";
      /*% properties.forEach((prop) => {
        if ( prop.entityProp.class == 'Date' || prop.entityProp.class == 'DateTime' ) { %*/
      filters = filters + (
        ( this./*%= normalize(prop.property) %*/Filter
          /*% if ( prop.entityProp.class == 'DateTime' ) { %*/&& this./*%= normalize(prop.property) %*/Filter.length === 5/*% } %*/ )
          ? "/*%= normalize(prop.property) %*/:" + this./*%= normalize(prop.property) %*/Filter.slice(0,3)
            .map(e => (e < 10) ? ("0" + e.toString()) : e.toString()).join("-").toString()
          : ""
        );
         /*% if ( prop.entityProp.class == 'DateTime' ) { %*/
      filters = filters + (
        (this./*%= normalize(prop.property) %*/Filter && this./*%= normalize(prop.property) %*/Filter.length === 5)
          ? "T" + this./*%= normalize(prop.property) %*/Filter.slice(3)
            .map(e => ("0" + e.toString()).slice(-2)).join(":").toString() + ":00"
          : ""
        );
         /*% } %*/
      filters = filters + (( this./*%= normalize(prop.property) %*/Filter
          /*% if ( prop.entityProp.class == 'DateTime' ) { %*/&& this./*%= normalize(prop.property) %*/Filter.length === 5/*% } %*/ )
          ? "," : "");
        /*% } else if (prop.entityProperty) { %*/
          /*% if (prop.entityProp.owner) { %*/
      filters = filters  + (this./*%= normalize(prop.property) %*/Filter ?
          "/*%= normalize(prop.property) %*/./*%= prop.entityProperty.properties.find(prop => prop.pk).name %*/:" + this./*%= normalize(prop.property) %*/Filter.toString() + "," : "");
          /*% } %*/
        /*% } else { %*/
      filters = filters + (this./*%= normalize(prop.property) %*/Filter != null && this./*%= normalize(prop.property) %*/Filter !== "" ?
          "/*%= normalize(prop.property) %*/:" + this./*%= normalize(prop.property) %*/Filter.toString() + "," : "");
        /*% } %*/
      /*% }); %*/
      return filters !== "" ? filters : null;
    }
    /*% } %*/
  },
  watch: {
    /*% if (context.filtering) { %*/
      /*%
        let processedEntities = [];
        properties.forEach((prop) => {
          if (prop.entityProperty && prop.entityProp.owner && processedEntities.indexOf(prop.entityProperty.name) == -1) {
            processedEntities.push(prop.entityProperty.name); %*/
    /*%= normalize(prop.entityProp.name) %*/Search: {
      handler() {
        this.get/*%= normalize(prop.entityProp.name, true) %*/Items();
      }
    },
      /*% } %*/
      /*% }); %*/
    filters() {
      this.redirectOnFilterChange();
    },
    /*% } %*/
  },
  created() {
  /*% if (context.filtering) { %*/
    /*% properties.forEach((prop) => { %*/
      /*% if (!prop.entityProp.pk) { %*/
    if (this.$route.query./*%= normalize(prop.property) %*/Filter) {
      this.showFilters = true;
        /*% if (prop.entityProp.class == 'Boolean') { %*/
      this./*%= normalize(prop.property) %*/FilterAllowed = true;
      this./*%= normalize(prop.property) %*/Filter = (this.$route.query./*%= normalize(prop.property) %*/Filter === "true");
        /*% } else if ( prop.entityProp.class == 'Long' || prop.entityProp.class == 'Integer' || prop.entityProp.class == 'BigDecimal' || prop.entityProp.class == 'Float' || prop.entityProp.class == 'Double' ) {  %*/
      let value = parseFloat(this.$route.query./*%= normalize(prop.property) %*/Filter);
      this./*%= normalize(prop.property) %*/Filter = isNaN(value) ? null : value;
        /*% } else if ( prop.enumProperty || prop.entityProp.class == 'Text' || prop.entityProp.class == 'String' ) { %*/
      this./*%= normalize(prop.property) %*/Filter = this.$route.query./*%= normalize(prop.property) %*/Filter;
        /*% } else if ( prop.entityProp.class == 'Date' || prop.entityProp.class == 'DateTime' ) { %*/
      this./*%= normalize(prop.property) %*/Filter = this.$route.query./*%= normalize(prop.property) %*/Filter.split("-").map(e => parseInt(e));
          /*% if ( prop.entityProp.class == 'DateTime' ) { %*/
      if (this.$route.query./*%= normalize(prop.property) %*/TimeFilter)
        this.$route.query./*%= normalize(prop.property) %*/TimeFilter.split(":")
          .map(e => ("0" + e).slice(-2)).forEach(e => this./*%= normalize(prop.property) %*/Filter.push(e))
          /*% } %*/
        /*% } else if ( prop.entityProperty ) {
          if ( prop.entityProp.owner ) {
            if ( prop.entityProp.multiple ) { %*/
      if (this.$route.query./*%= normalize(prop.property) %*/Filter.split(",").length > 0) {
        this./*%= normalize(prop.property) %*/Filter = [];
        this.$route.query./*%= normalize(prop.property) %*/Filter.split(",").forEach(id => {
          this./*%= normalize(prop.property) %*/Filter.push(parseFloat(id));
        });
      }
            /*% } else { %*/
      let value = parseFloat(this.$route.query./*%= normalize(prop.property) %*/Filter);
      this./*%= normalize(prop.property) %*/Filter = isNaN(value) ? null : value;
            /*% } %*/
          /*% } %*/
        /*% } %*/
    }
      /*% } %*/
    /*% }); %*/
    /*%
      let processedEntities = [];
      properties.forEach((prop) => {
        if (prop.entityProperty && prop.entityProp.owner && processedEntities.indexOf(prop.entityProperty.name) == -1) {
          processedEntities.push(prop.entityProperty.name); %*/
    this.get/*%= normalize(prop.entityProp.name, true) %*/Items();
    /*% } %*/
    /*% }); %*/
  /*% } else if (context.searching) { %*/
    this.search = this.$route.query.search ? this.$route.query.search : null;
  /*% } %*/
    if(JSON.stringify(this.$route.query)!="{}")
         this.getItems();
  },
  methods: {
    getItems() {
      this.loading = true;
      const options = {
        params: {
          page: this.entitiesPage.page - 1,
          /*% if (context.searching) { %*/
          search: this.search,
          /*% } %*/
          /*% if (context.filtering) { %*/
          filters: this.filters,
          /*% } %*/
          /*% if (context.sorting) { %*/
          sort: this.$route.query.sort,
          /*% } %*/
          size: this.entitiesPage.itemsPerPage
        }
      };
      /*%= normalize(context.entity, true) %*/EntityRepository.getAll(options)
        .then(response => {
          this.items = response.content;
          this.totalItems = response.totalElements;
        })
        .finally(() => (this.loading = false));
    },
    /*% if (context.filtering) { %*/
      /*%
        let processedEntities = [];
        properties.forEach((prop) => {
          if (prop.entityProperty && prop.entityProp.owner && processedEntities.indexOf(prop.entityProperty.name) == -1) {
            processedEntities.push(prop.entityProperty.name); %*/
    get/*%= normalize(prop.entityProp.name, true) %*/Items() {
      this./*%= normalize(prop.entityProp.name) %*/s.loading = true;
      const options = {
        params: {
          search: this./*%= normalize(prop.entityProp.name) %*/Search
        }
      };
      /*%= normalize(prop.entityProperty.name, true) %*/EntityRepository.getAll(options)
        .then(response => this./*%= normalize(prop.entityProp.name) %*/s.items = response.content)
        .finally(() => (this./*%= normalize(prop.entityProp.name) %*/s.loading = false));
    },
          /*% } %*/
        /*% }); %*/
    /*% } %*/
    redirect(query) {
      if (JSON.stringify(this.$route.query) !== JSON.stringify(query)) {
        this.$router
          .replace({
            name: "/*%= context.id %*/",
            query: query
          })
          .then(() => this.getItems());
      }
    },
    /*% if (context.form) { %*/
    entityDetail(entity) {
      const selection = window.getSelection().toString();
      if (selection.length === 0) {
        this.$router.push({
          name: "/*%= theForm.id %*/Detail",
          params: { id: entity.id, backPrevious: true }
        });
      }
    },
    /*% } %*/
    redirectOnTableChange(pagination = this.entitiesPage) {
      this.entitiesPage = pagination;
      let query = JSON.parse(JSON.stringify(this.$route.query));
      query.page = this.entitiesPage.page.toString();
      query.pageSize = this.entitiesPage.itemsPerPage.toString();
      /*% if (context.sorting) { %*/
        /*% if (hasEntityProperty) { %*/
      const mapping = {
          /*% properties.forEach((prop) => {
            if (prop.entityProperty && prop.entityProp.owner) { %*/
        /*%= normalize(prop.entityProp.name) %*/: "/*%= normalize(prop.entityProp.name) %*/./*%= getDisplayStringName(prop.entityProperty) %*/",
            /*% } %*/
          /*% }); %*/
      };
        /*% } %*/
      query.sort = generateSort(this.entitiesPage, /*% if (hasEntityProperty) { %*/mapping/*% } %*/);
      /*% } %*/
      /*% if (context.filtering) { %*/
      this.changeQueryFilters(query);
      /*% } %*/
      /*% if (context.searching) { %*/
      query.search = this.search || undefined;
      /*% } %*/
      this.redirect(query);
    },
    /*% if (context.filtering) { %*/
      /*% if (hasBooleanFilter) { %*/
    handleBooleanFilter(name) {
      if (this[name + "FilterAllowed"]) {
        this[name + "Filter"] = true;
      } else {
        this[name + "Filter"] = null;
      }
      this.redirectOnFilterChange();
    },
      /*% } %*/
      /*% if (hasDateFilter || hasDateTimeFilter) { %*/
    updateDateTime(name, hasTime, data) {
      this[name] = hasTime && data.date ? data.date.concat(data.time) : data.date;
    },
      /*% } %*/
    changeQueryFilters(query) {
      /*% properties.forEach((prop) => {
          if (!prop.entityProp.pk) { %*/
          /*% if ( prop.entityProperty ) {
            if ( prop.entityProp.owner ) {
              if ( prop.entityProp.multiple ) { %*/
      query./*%= normalize(prop.property) %*/Filter = this./*%= normalize(prop.property) %*/Filter && this./*%= normalize(prop.property) %*/Filter.length >= 1
        ? this./*%= normalize(prop.property) %*/Filter
        : undefined;
              /*% } else { %*/
      query./*%= normalize(prop.property) %*/Filter = this./*%= normalize(prop.property) %*/Filter ? this./*%= normalize(prop.property) %*/Filter : undefined;
              /*% } %*/
            /*% } %*/
          /*% } else if ( prop.entityProp.class == 'Date' || prop.entityProp.class == 'DateTime' ) { %*/
      query./*%= normalize(prop.property) %*/Filter = ( this./*%= normalize(prop.property) %*/Filter /*% if ( prop.entityProp.class == 'DateTime' ) { %*/&& this./*%= normalize(prop.property) %*/Filter.length === 5/*% } %*/ )
        ? this./*%= normalize(prop.property) %*/Filter.slice(0,3).map(e => (e < 10) ? ("0" + e.toString()) : e.toString()).join("-")
        : undefined;
            /*% if ( prop.entityProp.class == 'DateTime' ) { %*/
      query./*%= normalize(prop.property) %*/TimeFilter = ( this./*%= normalize(prop.property) %*/Filter && this./*%= normalize(prop.property) %*/Filter.length === 5 )
        ? this./*%= normalize(prop.property) %*/Filter.slice(3).map(e => ("0" + e.toString()).slice(-2)).join(":")
        : undefined;
            /*% } %*/
          /*% } else { %*/
      query./*%= normalize(prop.property) %*/Filter = this./*%= normalize(prop.property) %*/Filter != null ? this./*%= normalize(prop.property) %*/Filter : undefined;
          /*% } %*/
        /*% } %*/
      /*% }); %*/
    },
    /*% } %*/
    /*% if (context.filtering || context.searching) { %*/
    redirectOnFilterChange() {
      if (this.entitiesPage.page !== 1) {
        this.entitiesPage.page = 1;
      } else {
        this.redirectOnTableChange();
      }
    },
    /*% } %*/
    displayManyRelationship,
    routeBack(){
      this.$router.push({ path: '/' });
    }
  }
};
</script>
