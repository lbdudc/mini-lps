/*%@ return data.forms
      .filter(function(en) {
         return !en.abstract;
      })
      .map(function(form) {
        return {
          fileName: normalize(form.entity) + '/' + 'components' + '/' + normalize(form.id) +'Detail.vue',
          context: form
        };
      });
%*/
  /*%
    var entity = getEntity(data, context.entity);
    var hasGeographicProperty = false;
    var theList = data.lists.find((list) => list.form == context.id );
  %*/
<template>
  <v-container>
    <v-card v-if="!isLoading">
      <v-card-title>
        <v-row align="center" justify="space-between" no-gutters>
          <v-col class="d-none d-md-block">
            <span class="headline no-split-words">
              {{ $t($route.meta.label) }}
            </span>
          </v-col>

          <v-col class="text-right">
            <v-btn @click="back()">
              <v-icon>arrow_back</v-icon>
              <span class="d-none d-sm-block"> {{ $t("back") }} </span>
            </v-btn>

          </v-col>
        </v-row>
      </v-card-title>
      <v-card-text>
        <v-row dense>
          /*%
          var inputProperties = [];
          var columnProperties = [];
          var rowProperties = [];
          var hasColumnProperties = false;
          const properties = context.properties.map(e => { return { fromEntity: e.owner || context.entity, ...e }; });
          properties.forEach(function(prop) {
            if (prop.viewing) {
              var propOwner = getEntity(data, prop.fromEntity);
              let p = getProperty(propOwner, prop.property);
              if (isInput(data, p)) inputProperties.push(prop);
              else if (isColumn(p)) {
                hasColumnProperties = true;
                columnProperties.push(prop);
              } else rowProperties.push(prop);
            }
          });
          %*/
          /*% if (hasColumnProperties) { %*/
          <v-col cols="12" md="6" class="d-flex" style="flex-direction:column">
          /*% } %*/
          /*%
          inputProperties.forEach(function(prop) {
            var propOwner = getEntity(data, prop.fromEntity);
            var theProperty = getProperty(propOwner, prop.property);
            var enumProperty = getEnum(data, theProperty.class);
            var entityProperty = getEntity(data, theProperty.class);
            var entityPropertyPK = getEntityProperty(entityProperty, 'id');
            %*/
          /*% if (hasColumnProperties) { %*/
          <v-row align="center" dense>
          /*% } %*/
          /*%
            if (entityProperty){
              if (theProperty.owner){
                if (theProperty.multiple){  %*/

          <v-col cols="3" md="2" class="text-left font-weight-bold">
            {{ $t("t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(theProperty.name) %*/") }}:
          </v-col>
          <v-col cols="9" md="10">
            <div class="float-left mr-3" v-for="/*%= normalize(prop.property) %*/ in entity./*%= normalize(prop.property) %*/" :key="/*%= normalize(prop.property) %*/./*%= normalize(entityPropertyPK.name) %*/">
                  /*%
                  var aForm = data.forms.find(function(form) { return form.entity == entityProperty.name; });
                  if (aForm) { %*/
              <router-link
                :to="{
                  name: '/*%= aForm.id %*/Detail',
                  params: { id: /*%= normalize(prop.property) %*/./*%= normalize(entityPropertyPK.name) %*/ }
                }"
              >
                {{/*%= normalize(prop.property) %*/./*%= getDisplayStringName(entityProperty) %*/}}
              </router-link>
                  /*% } else { %*/
              {{/*%= normalize(prop.property) %*/./*%= getDisplayStringName(entityProperty) %*/}}
                  /*% } %*/
            </div>
          </v-col>
                /*% } else { %*/

          <v-col cols="3" md="2" class="text-left font-weight-bold">
            {{ $t("t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(theProperty.name) %*/") }}:
          </v-col>
          <v-col cols="9" md="10">
                  /*%
                  var aForm = data.forms.find(function(form) { return form.entity == entityProperty.name; });
                  if (aForm) { %*/
            <router-link
              :to="{
                name: '/*%= aForm.id %*/Detail',
                params: { id: entity./*%= normalize(prop.property) %*/./*%= normalize(entityPropertyPK.name) %*/ }
              }"
              v-if="entity./*%= normalize(prop.property) %*/"
            >
              {{entity./*%= normalize(prop.property) %*/./*%= getDisplayStringName(entityProperty) %*/}}
            </router-link>
                  /*% } else { %*/
            {{entity./*%= normalize(prop.property) %*/./*%= getDisplayStringName(entityProperty) %*/}}
                  /*% } %*/
          </v-col>
                /*% } %*/
              /*% } %*/
            /*% } else if (enumProperty) {  %*/
          <v-col cols="3" md="2" class="text-left font-weight-bold">
            {{ $t("t_/*%= normalize(context.entity) %*/.prop./*%= normalize(theProperty.name) %*/") }}:
          </v-col>
          <v-col cols="9" md="10">
            <span v-if="entity./*%= normalize(theProperty.name) %*/">
              {{ $t(`/*%= normalize(theProperty.name.toLowerCase()) %*/.${entity./*%= normalize(theProperty.name) %*/}`) }}
            </span>
          </v-col>
            /*% } else if (theProperty.class == 'Date') {  %*/
          <v-col cols="3" md="2" class="text-left font-weight-bold">
            {{ $t("t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(theProperty.name) %*/") }}:
          </v-col>
          <v-col cols="9" md="10">
            {{ entity./*%= normalize(theProperty.name) %*/ | dateFormat }}
          </v-col>
            /*% } else if (theProperty.class == 'DateTime') {  %*/
          <v-col cols="3" md="2" class="text-left font-weight-bold">
            {{ $t("t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(theProperty.name) %*/") }}:
          </v-col>
          <v-col cols="9" md="10">
            {{ entity./*%= normalize(theProperty.name) %*/ | dateTimeFormat }}
          </v-col>
            /*% } else { %*/

          <v-col cols="3" md="2" class="text-left font-weight-bold">
            {{ $t("t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(theProperty.name) %*/") }}:
          </v-col>
          <v-col cols="9" md="10">
            {{ entity./*%= normalize(theProperty.name) %*/ }}
          </v-col>
            /*% } %*/
          /*% if (hasColumnProperties) { %*/
          </v-row>
          /*% } %*/
          /*% }); %*/
          /*% if (hasColumnProperties) { %*/
          </v-col>
          /*% } %*/
          /*%
          columnProperties.forEach(function(prop) {
            var propOwner = getEntity(data, prop.fromEntity);
            var theProperty = getProperty(propOwner, prop.property);
            %*/
          <v-col cols="12" md="6" class="d-flex" style="flex-direction:column">
            <v-row align="center" dense>
            /*% if (isGeographic(theProperty)) {  %*/
              /*% hasGeographicProperty = true; %*/
            <v-col cols="3" md="2" class="text-left font-weight-bold">
              {{ $t("t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(theProperty.name) %*/") }}:
            </v-col>
            <v-col cols="9" md="10">
              <map-field
                id="/*%= normalize(theProperty.name) %*/"
                v-bind:entity="entity"
                :editable="false"
                height="400px"
                propName="/*%= normalize(theProperty.name) %*/"
                entityName="/*%= normalize(context.entity) %*/"
              ></map-field>
            </v-col>
              /*% } %*/
            </v-row>
          </v-col>
          /*% }); %*/
          /*%
          rowProperties.forEach(function(prop) {
            var propOwner = getEntity(data, prop.fromEntity);
            var theProperty = getProperty(propOwner, prop.property);
            %*/
          <v-col cols="12" class="d-flex" style="flex-direction:column">
            <v-row align="center" dense>
            </v-row>
          </v-col>
          /*% }); %*/
        </v-row>
      </v-card-text>
    </v-card>
    <loading-page v-if="isLoading"></loading-page>
  </v-container>
</template>

<script>
import checkInvalidID from "@/common/checkInvalidID";
/*% if (hasGeographicProperty) { %*/
import MapField from "@/components/map-field/MapField.vue";
/*% } %*/
import LoadingPage from "@/components/loading-page/LoadingPage.vue";

import RepositoryFactory from "@/repositories/RepositoryFactory";
const /*%= normalize(context.entity, true) %*/EntityRepository = RepositoryFactory.get("/*%= normalize(context.entity, true) %*/EntityRepository");

export default {
  name: "/*%= normalize(context.id) %*/Detail",
  components: {LoadingPage, /*% if (hasGeographicProperty) { %*/MapField,/*% } %*/},
  data() {
    return {
      loading: false,
      entity: null,
    }
  },
  computed: {
    isLoading() {
      return this.loading;
    }
  },
  beforeRouteUpdate(to, from, next) {
    this._fetchData(to.params.id);
    next();
  },
  created() {
    this._fetchData(this.$route.params.id);
  },
  methods: {
    _fetchData(id) {
      this.loading = true;

     return /*%= normalize(context.entity, true) %*/EntityRepository.get(id)
        .then(res => this.entity = res)
        .catch((err) => checkInvalidID(err))
        .finally(() => (this.loading = false));
    },
    back() {
      /*% if (theList != null) { %*/
      this.$router.push({ name: "/*%= theList.id %*/", params: { backAction: true } });
      /*% } else { %*/
      this.$router.push({ name: "Home", params: { backAction: true } });
      /*% } %*/
    },
  }
};
</script>
