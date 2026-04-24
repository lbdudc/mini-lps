// considered inputs conditions for auto generated form
const PROCESS_INPUTS = [
  {
    type: "number",
    component: "v-text-field",
    condition: (input) => {
      return input.schema.type === "number";
    },
  },
  {
    type: "string",
    component: "v-text-field",
    condition: (input) => {
      return (
        (input.schema.type === "string" && !input.schema["enum"]) ||
        input.schema.type === "object"
      );
    },
  },
  {
    type: "integer",
    component: "v-text-field",
    condition: (input) => {
      return input.schema.type === "integer";
    },
  },
  {
    type: "boolean",
    component: "v-checkbox",
    condition: (input) => {
      return input.schema.type === "boolean";
    },
  },
  {
    type: "enum",
    component: "v-select",
    condition: (input) => {
      return input.schema.type === "string" && input.schema?.enum != null;
    },
  },
  {
    type: "application/json",
    component: "v-text-area",
    condition: (input) => {
      return input.schema.type === "application/json";
    },
  },
];

export { PROCESS_INPUTS };
