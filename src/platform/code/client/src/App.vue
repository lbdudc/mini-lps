<template>
  <v-app>
    <notifications :max="3" :width="300" position="top center" />
    <v-main>
      <router-view class="content">
        <!-- -->
      </router-view>
    </v-main>
  </v-app>
</template>

<script>

export default {
  name: "App"
};
</script>
