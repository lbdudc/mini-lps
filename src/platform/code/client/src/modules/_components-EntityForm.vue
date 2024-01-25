/*%@ return data.forms
      .filter(function(en) {
         return !en.abstract;
      })
      .map(function(form) {
        return {
          fileName: normalize(form.entity) + '/' + 'components' + '/' + normalize(form.id) +'Form.vue',
          context: form
          };
      });
%*/
/*%
if (context.editable || context.creatable) { %*/
/*%
    var entity = getEntity(data, context.entity);
    var entityIdProperty = getEntityProperty(entity, 'id');
    var hasGeographicProperty = false;
    var hasNumberField = false;
    var hasOneToOne = false;
    var hasMultiple = false;
    var noMultipleEntities = [];
    var hasDateProperty = false;
    var hasDateTimeProperty = false;
    var hasEntityProperty = false;
    var hasRegexConstraint = false;
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
              <v-icon>mdi-arrow-left</v-icon>
              <span class="d-none d-sm-block"> {{ $t("cancel") }} </span>
            </v-btn>
            <v-btn class="success ml-2" @click="save(entity)">
              <v-icon>save</v-icon>
              <span class="d-none d-sm-block"> {{ $t("save") }} </span>
            </v-btn>
          </v-col>
        </v-row>
      </v-card-title>
      <v-card-text>
        <v-form ref="form">
          <v-row dense>
            /*%
            var inputProperties = [];
            var columnProperties = [];
            var rowProperties = [];
            var hasColumnProperties = false;
            const properties = context.properties.map(e => { return { fromEntity: e.owner || context.entity, ...e }; });
            properties.forEach((prop) => {
              var propOwner = getEntity(data, prop.fromEntity);
              prop.entityProp = getProperty(propOwner, prop.property);
              if (prop.editing) {
                prop.entityProperty = getEntity(data, prop.entityProp.class);
                if (prop.entityProperty) {
                  prop.entityPropertyProp = getProperty(prop.entityProperty, prop.entityProp.bidirectional);
                }
              }
              if (prop.editing) {
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
            <v-col cols="12" md="6">
            /*% } %*/
            /*%

            inputProperties.forEach(function(prop) {
              var enumProperty = getEnum(data, prop.entityProp.class);
              var autoinc = prop.entityProp.class.indexOf('autoinc') != -1;
              var entityPropertyPK = getEntityProperty(prop.entityProperty, 'id');
              %*/

            /*% if (autoinc) { %*/
            <v-col cols="12"/*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <v-text-field
                dense
                v-model="entity./*%= normalize(prop.entityProp.name) %*/"
                type="number"
                :disabled="true"
                :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
              ></v-text-field>
            </v-col>
            /*% } else if ( prop.entityProp.class == 'Long' || prop.entityProp.class == 'Integer' || prop.entityProp.class == 'BigDecimal' || prop.entityProp.class == 'Float' || prop.entityProp.class == 'Double' ) {  %*/
              /*% hasNumberField = true; %*/
            <v-col cols="12" /*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <number-field
                v-model="entity./*%= normalize(prop.entityProp.name) %*/"
                :rules="[
                /*% if (prop.entityProp.required) { %*/
                  v => !!v || $t('error.required'),
                /*% } %*/
                /*% if (prop.entityProp.min) { %*/
                  v => !v || parseFloat(v) >= /*%= prop.entityProp.min %*/ || $t('error.value.min', { value: /*%= prop.entityProp.min %*/}),
                /*% } %*/
                /*% if (prop.entityProp.max) { %*/
                  v => !v || parseFloat(v) <= /*%= prop.entityProp.max %*/ || $t('error.value.max', { value: /*%= prop.entityProp.max %*/}),
                /*% } %*/
                ]"
                type="/*%= normalize(prop.entityProp.class) %*/"
                :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
              ></number-field>
            </v-col>
            /*% } else if (prop.entityProp.class == 'Boolean') { %*/
            <v-col cols="12" /*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <v-switch
                dense
                v-model="entity./*%= normalize(prop.entityProp.name) %*/"
                class="ma-2 tp-0"
                :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
              ></v-switch>
            </v-col>
            /*% } else if ( prop.entityProp.class == 'Date') { %*/
            /*% hasDateProperty = true; %*/
            <v-col cols="12" /*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <dateAndHourPicker
                :datePickerProp="{data: entity./*%= normalize(prop.entityProp.name) %*/, label: $t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')}"
                @update-time="updateDateTime('/*%= normalize(prop.entityProp.name) %*/', false, ...arguments)"
              ></dateAndHourPicker>
            </v-col>
            /*% } else if ( prop.entityProp.class == 'DateTime') { %*/
            /*% hasDateTimeProperty = true; %*/
            <v-col cols="12" /*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <dateAndHourPicker
                :datePickerProp="{data: entity./*%= normalize(prop.entityProp.name) %*/, label: $t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')}"
                :timePickerProp="{
                  data: entity./*%= normalize(prop.entityProp.name) %*/
                    ? entity./*%= normalize(prop.entityProp.name) %*/.map(el =>
                        ('0' + el).slice(-2)
                      )
                    : entity./*%= normalize(prop.entityProp.name) %*/,
                  label: $t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')
                }"
                @update-time="updateDateTime('/*%= normalize(prop.entityProp.name) %*/', true, ...arguments)"
              >
              </dateAndHourPicker>
            </v-col>
            /*% } else if ( prop.entityProp.class == 'Text' ) { %*/
            <v-col cols="12" /*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <v-text-field
                dense
                v-model="entity./*%= normalize(prop.entityProp.name) %*/"
                type="text"
                :rules="[
                /*% if (prop.entityProp.required) { %*/
                  v => !!v || $t('error.required'),
                /*% } %*/
                /*% if (prop.entityProp.min) { %*/
                  v => !v || v.length >= /*%= prop.entityProp.min %*/ || $t('error.length.min', { value: /*%= prop.entityProp.min %*/}),
                /*% } %*/
                /*% if (prop.entityProp.max) { %*/
                  v => !v || v.length <= /*%= prop.entityProp.max %*/ || $t('error.length.max', { value: /*%= prop.entityProp.max %*/}),
                /*% } %*/
                /*% if (prop.entityProp.patternType) { %*/
                  /*% hasRegexConstraint = true; %*/
                  /*% if (prop.entityProp.patternType === 'ipPattern') { %*/
                  v => !v || this.regex.IP_REGEX.test(v) || $t('error.regex./*%= prop.entityProp.patternType %*/'),
                  /*% } %*/
                  /*% if (prop.entityProp.patternType === 'urlPattern') { %*/
                  v => !v || this.regex.URL_REGEX.test(v) || $t('error.regex./*%= prop.entityProp.patternType %*/'),
                  /*% } %*/
                  /*% if (prop.entityProp.patternType === 'emailPattern') { %*/
                  v => !v || this.regex.EMAIL_REGEX.test(v) || $t('error.regex./*%= prop.entityProp.patternType %*/'),
                  /*% } %*/
                  /*% if (prop.entityProp.patternType === 'customPattern') { %*/
                  v => !v || /'^$|/*%= prop.entityProp.pattern.replace(/\\/g,'\\\\') %*//.test(v) || this.$t('error.regex./*%= prop.entityProp.patternType %*/')
                  /*% } %*/
                /*% } %*/
                ]"
                :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
              ></v-text-field>
            </v-col>
            /*% } else if ( enumProperty ) { %*/
            <v-col cols="12" /*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <v-select
                dense
                :items="/*%= enumProperty.name.toLowerCase() %*/Property"
                :item-text="item => $t(item.text)"
                item-value="value"
                :menu-props="{ offsetY: true }"
                :rules="[
                /*% if (prop.entityProp.required) { %*/
                  v => !!v || $t('error.required'),
                /*% } %*/
                ]"
                v-model="entity./*%= normalize(prop.entityProp.name) %*/"
                :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
              >
              </v-select>
            </v-col>
            /*% } else if ( prop.entityProperty ) {
              if ( prop.entityProp.owner ) {
                hasEntityProperty = true; %*/
            <v-col cols="12" /*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <autocomplete
                dense
                  /*% if (!prop.entityPropertyProp.multiple){
                    hasOneToOne = true;  %*/
                :items="/*%= pluralize(normalize(prop.entityProp.name)) %*/Disp"
                  /*% } else {
                    hasMultiple = true; %*/
                :items="/*%= normalize(prop.entityProp.name) %*/"
                :loading="/*%= normalize(prop.entityProp.name) %*/Loading"
                :search-input.sync="/*%= normalize(prop.entityProp.name) %*/Search"
                  /*% } %*/
                v-model="entity./*%= normalize(prop.entityProp.name) %*/"
                :rules="[
                /*% if (prop.entityProp.required) { %*/
                  v => !!v || $t('error.required'),
                /*% } %*/
                ]"
                :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
                  /*% if (prop.entityProp.multiple) { %*/ multiple /*% } %*/
                item-text="/*%= getDisplayStringName(prop.entityProperty) %*/"
                return-object
              >
              </autocomplete>
            </v-col>
            /*% }
             } else { %*/
            <v-col cols="12" /*% if (!hasColumnProperties) { %*/ md="6"/*% } %*/ >
              <v-text-field
                dense
                v-model="entity./*%= normalize(prop.entityProp.name) %*/"
                type="text"
                :rules="[
                /*% if (prop.entityProp.required) { %*/
                  v => !!v || $t('error.required'),
                /*% } %*/
                /*% if (prop.entityProp.min) { %*/
                  v => !v || v.length >= /*%= prop.entityProp.min %*/ || $t('error.length.min', { value: /*%= prop.entityProp.min %*/}),
                /*% } %*/
                /*% if (prop.entityProp.max) { %*/
                  v => !v || v.length <= /*%= prop.entityProp.max %*/ || $t('error.length.max', { value: /*%= prop.entityProp.max %*/}),
                /*% } %*/
                /*% if (prop.entityProp.patternType) { %*/
                  /*% hasRegexConstraint = true; %*/
                  /*% if (prop.entityProp.patternType === 'ipPattern') { %*/
                  v => !v || this.regex.IP_REGEX.test(v) || $t('error.regex./*%= prop.entityProp.patternType %*/'),
                  /*% } %*/
                  /*% if (prop.entityProp.patternType === 'urlPattern') { %*/
                  v => !v || this.regex.URL_REGEX.test(v) || $t('error.regex./*%= prop.entityProp.patternType %*/'),
                  /*% } %*/
                  /*% if (prop.entityProp.patternType === 'emailPattern') { %*/
                  v => !v || this.regex.EMAIL_REGEX.test(v) || $t('error.regex./*%= prop.entityProp.patternType %*/'),
                  /*% } %*/
                  /*% if (prop.entityProp.patternType === 'customPattern') { %*/
                  v => !v || /'^$|/*%= prop.entityProp.pattern.replace(/\\/g,'\\\\') %*//.test(v) || this.$t('error.regex./*%= prop.entityProp.patternType %*/')
                  /*% } %*/
                /*% } %*/
                ]"
                :label="$t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/')"
              ></v-text-field>
            </v-col>
            /*% } %*/
            /*% }); %*/
            /*% if (hasColumnProperties) { %*/
            </v-col>
            /*% } %*/
            /*%
            columnProperties.forEach(function(prop) {
              if (isGeographic(prop.entityProp)) {
                hasGeographicProperty = true; %*/
            <v-col cols="12" md="6">
              <v-col cols="12" class="font-weight-bold">
                {{ $t('t_/*%= normalize(prop.fromEntity) %*/.prop./*%= normalize(prop.entityProp.name) %*/') }}:
              </v-col>
              <v-col cols="12">
                <map-field
                  id="/*%= normalize(prop.entityProp.name) %*/"
                  v-bind:entity="entity"
                  geomType="/*%= prop.entityProp.class %*/"
                  :editable="true"
                  height="400px"
                  propName="/*%= normalize(prop.entityProp.name) %*/"
                  entityName="/*%= normalize(prop.fromEntity) %*/" >
                </map-field>
              </v-col>
            </v-col>
            /*% } %*/
            /*% }); %*/
          </v-row>
        </v-form>
      </v-card-text>
    </v-card>
    <loading-page v-if="isLoading"></loading-page>
  </v-container>
</template>

<script>
import LoadingPage from "@/components/loading-page/LoadingPage.vue";
/*% if (hasGeographicProperty) { %*/
import MapField from "@/components/map-field/MapField.vue";
/*% } %*/
import checkInvalidID from "@/common/checkInvalidID";
/*% if (hasNumberField) { %*/
import NumberField from "@/components/number-field/NumberField.vue";
/*% } %*/
/*% if (hasDateProperty || hasDateTimeProperty) { %*/
import DateAndHourPicker from "@/components/calendar/DateAndHourPicker.vue";
/*% } %*/
/*% if (hasRegexConstraint) { %*/
import regex from "@/common/regex-validation";
/*% } %*/
/*% if (hasEntityProperty) { %*/
import Autocomplete from "@/components/debouncing-inputs/Autocomplete"
/*% } %*/

/*%
  const processedEnums = new Set();
  properties
    .filter((prop) => prop.editing)
    .forEach((prop) => {
        var enumProperty = getEnum(data, prop.entityProp.class);
        if (enumProperty && !processedEnums.has(enumProperty.name)) {
          processedEnums.add(enumProperty.name); %*/
import /*%= enumProperty.name.toLowerCase() %*/Property from "@/enumerates//*%= normalize(enumProperty.name.toLowerCase()) %*/";
/*% }
  });
%*/
import RepositoryFactory from "@/repositories/RepositoryFactory"
const /*%= normalize(context.entity, true) %*/EntityRepository = RepositoryFactory.get("/*%= normalize(context.entity, true) %*/EntityRepository");
/*%
    const processedEntities = new Set([context.entity]);
    properties.filter((prop) => prop.editing)
      .forEach((prop) => {
        if ( prop.entityProperty && !processedEntities.has(prop.entityProperty.name) ) {
          if ( prop.entityProp.owner ) {
            processedEntities.add(prop.entityProperty.name); %*/
const /*%= normalize(prop.entityProperty.name, true) %*/EntityRepository = RepositoryFactory.get("/*%= normalize(prop.entityProperty.name, true) %*/EntityRepository");
      /*%
          }
        }
      }); %*/

export default {
  name: "/*%= normalize(context.id) %*/Form",
  components: {
    LoadingPage,
    /*% if (hasGeographicProperty) { %*/
    MapField,
    /*% } %*/
    /*% if (hasDateProperty || hasDateTimeProperty) { %*/
    DateAndHourPicker,
    /*% } %*/
    /*% if (hasNumberField) { %*/
    NumberField,
    /*% } %*/
    /*% if (hasEntityProperty) { %*/
    Autocomplete
    /*% } %*/
  },
  data() {
    return {
      loading: false,
      entity: {/*%
        inputProperties.filter((prop) => prop.entityProp.class == 'Boolean')
        .forEach((prop) => { %*/
        /*%= normalize(prop.entityProp.name) %*/: false,
       /*% }); %*/},
      /*% const processedEnumsData = new Set();
          properties
            .filter((prop) => prop.editing)
            .forEach((prop) => {
              var enumProperty = getEnum(data, prop.entityProp.class);
              if (enumProperty && !processedEnumsData.has(enumProperty.name)) {
                  processedEnumsData.add(enumProperty.name); %*/
      /*%= enumProperty.name.toLowerCase() %*/Property: /*%= enumProperty.name.toLowerCase() %*/Property,
      /*%     }
              if (prop.entityProperty) {
                if (prop.entityProp.owner && !prop.entityPropertyProp.multiple ) { %*/
      /*%= pluralize(normalize(prop.entityProp.name)) %*/Disp : [],
      /*%       } else if (prop.entityProp.owner && prop.entityPropertyProp.multiple ) { %*/
      /*%= normalize(prop.entityProp.name) %*/: [],
      /*%= normalize(prop.entityProp.name) %*/Search: null,
      /*%= normalize(prop.entityProp.name) %*/Loading: false,
      /*%       }
              }
            });
      %*/
      /*% if (hasRegexConstraint) { %*/
      regex
      /*% } %*/
    };
  },
  beforeRouteUpdate(to, from, next) {
    // si se accede a la misma ruta con diferentes parámetros, se cargará el nuevo objeto
    if (to.params.id) this._fetchData(to.params.id);
    next();
  },
  created() {
    /*%
    properties
      .filter((prop) => prop.editing)
      .forEach((prop) => {
        if ( prop.entityProperty ) {
          if ( prop.entityProp.owner ) {
            if (!prop.entityPropertyProp.multiple) {
              noMultipleEntities.push(prop.entityProp); %*/
    /*%= normalize(prop.entityProperty.name, true) %*/EntityRepository.getAllWithout("/*%= normalize(context.entity) %*/")
      .then(data => data.forEach(el => this./*%= pluralize(normalize(prop.entityProp.name)) %*/Disp.push(el)));
          /*% } else { %*/
    this.fetch/*%= normalize(prop.entityProp.name, true) %*/Items();
        /*%   }
            }
          }
        }); %*/
    if (this.$route.params.id)
      this._fetchData(this.$route.params.id)
      /*% if(noMultipleEntities.length >= 1) { %*/
      .then(() => {
        /*% noMultipleEntities.forEach(function(theProperty) { %*/
        if (this.entity./*%= normalize(theProperty.name) %*/) {
          this./*%= pluralize(normalize(theProperty.name)) %*/Disp.push(this.entity./*%= normalize(theProperty.name) %*/);
        }
        /*% }); %*/
      });
      /*% } %*/
  },
  computed: {
    isLoading() {
      return this.loading;
    }
  },
  watch: {
    /*% properties
          .filter((prop) => prop.editing)
          .forEach((prop) => {
            if (prop.entityProperty) {
              if (prop.entityProp.owner) {
                if (prop.entityPropertyProp.multiple) {
    %*/
    /*%= normalize(prop.entityProp.name) %*/Search() {
      this.fetch/*%= normalize(prop.entityProp.name, true) %*/Items();
    },
    /*%         }
              }
            }
          });
    %*/
  },
  methods: {
    _fetchData(id) {
      this.loading = true;
      return /*%= normalize(context.entity, true) %*/EntityRepository.get(id)
        .then(response => {
          this.entity = response;
        })
        .catch(err => checkInvalidID(err))
        .finally(() => (this.loading = false));
    },
    back() {
      this.$router.push({
        name: /*% if (theList != null) { %*/"/*%= theList.id %*/"/*% } else { %*/"/*%= context.id %*/Detail"/*% } %*/,
        params: { backAction: true }
      });
    },
    /*% properties
          .filter((prop) => prop.editing)
          .forEach((prop) => {
            if (prop.entityProperty) {
              if (prop.entityProp.owner) {
                if (prop.entityPropertyProp.multiple) {
    %*/
    fetch/*%= normalize(prop.entityProp.name, true) %*/Items() {
      let options = {
        params: {
          page: 0,
          pageSize: 10
        }
      };
      if (this./*%= normalize(prop.entityProp.name) %*/Search) {
        options.params.filters = "/*%= getDisplayStringName(prop.entityProperty) %*/:" + this./*%= normalize(prop.entityProp.name) %*/Search;
      }
      this./*%= normalize(prop.entityProp.name) %*/Loading = true;
      /*%= normalize(prop.entityProperty.name, true) %*/EntityRepository.getAll(options)
        .then(data => {
          this./*%= normalize(prop.entityProp.name) %*/ = (this.entity./*%= normalize(prop.entityProp.name) %*/
          ? /*% if (prop.entityProp.multiple) { %*/ this.entity./*%= normalize(prop.entityProp.name) %*/.concat(data.content)
          /*% } else { %*/ [this.entity./*%= normalize(prop.entityProp.name) %*/, ...data.content] /*% } %*/
          : data.content);
        })
        .finally(() => this./*%= normalize(prop.entityProp.name) %*/Loading = false);
    },
    /*%         }
              }
            }
          });
    %*/
    /*% if (hasDateProperty || hasDateTimeProperty) { %*/
    updateDateTime(name, hasTime, data) {
      this.entity[name] = data.date
        ? hasTime
          ? data.time
            ? data.date.concat(data.time)
            : data.date.concat([0, 0])
          : data.date
        : null;
    },
    /*% } %*/
    async save() {
      if (!this.$refs.form.validate()) {
        this.$notify({
          type: "error",
          text: this.$t('t_/*%= normalize(context.entity) %*/.error.form-errors')
        });
      } else if (this.$route.params.id) {
        this.loading = true;
        /*%= normalize(context.entity, true) %*/EntityRepository.save(this.entity)
          .then(() =>
            this.$router.push({
              name: "/*%= context.id %*/Detail",
              params: { id: this.entity.id, backAction: this.$route.params.backPrevious }
            })
          )
          .finally(() => (this.loading = false));
      } else {
        this.loading = true;

        /*%= normalize(context.entity, true) %*/EntityRepository.save(this.entity)
          .then(() => this.$router.push({
            name: /*% if (theList != null) { %*/"/*%= theList.id %*/"/*% } else { %*/"/*%= context.id %*/Detail"/*% } %*/
          }))
          .finally(() => (this.loading = false));
      }
    }
  }
};
</script>
/*% } %*/
