import { notFound } from "next/navigation";

/** Łapie nieznane ścieżki w obrębie locale, żeby wyrenderować lokalizowaną stronę 404. */
export default function CatchAll() {
  notFound();
}
