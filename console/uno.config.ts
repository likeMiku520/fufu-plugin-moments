import { presetWind3 } from "unocss";
import { transformerCompileClass } from "unocss";

export default {
  presets: [presetWind3()],
  transformers: [transformerCompileClass()],
  theme: {
    colors: {
      primary: "#39C5BB",
    },
  },
  blocklist: ["transform", "inline"],
};