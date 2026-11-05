import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

export default defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    rules: {
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/consistent-type-imports": ["warn", { fixStyle: "inline-type-imports" }],
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
      "react/no-unknown-property": "off",
    },
  },
  {
    // R3F: zasoby GPU tworzone w useMemo i mutowane w useFrame (uniformy, materiały) oraz
    // losowe atrybuty cząsteczek to udokumentowany, wydajny wzorzec three.js — reguły
    // React Compiler o czystości/niemutowalności nie mają tu zastosowania.
    files: ["components/three/**/*.{ts,tsx}"],
    rules: {
      "react-hooks/purity": "off",
      "react-hooks/immutability": "off",
    },
  },
  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "lib/db/generated/**",
    "playwright-report/**",
    "test-results/**",
  ]),
]);
