import type messages from "./messages/pl.json";

declare module "next-intl" {
  interface AppConfig {
    Messages: typeof messages;
  }
}
