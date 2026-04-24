<template>
  <v-container class="spinnerContainer">
    <v-progress-circular
      color="primary"
      indeterminate
      model-value="20"
      :size="size"
      :width="width"
    ></v-progress-circular>
    <div v-if="message" class="mt-10 text-center">{{ $t(message) }}</div>
  </v-container>
</template>

<script>
export default {
  name: "LoadingSpinner",
  props: {
    size: {
      type: Number,
      default: 77,
    },
    width: {
      type: Number,
      default: 6,
    },
    message: {
      type: String,
      default: null,
    },
  },
};
</script>

<style scoped>
.spinnerContainer {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
</style>
