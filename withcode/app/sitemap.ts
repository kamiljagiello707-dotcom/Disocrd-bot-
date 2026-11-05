import type { MetadataRoute } from "next";
import { features } from "@/content/features";
import { docSlugs } from "@/content/docs/manifest";
import { siteUrl } from "@/lib/utils";

const staticPaths = [
  "/",
  "/features",
  "/commands",
  "/premium",
  "/docs",
  "/status",
  "/changelog",
  "/demo",
  "/partners",
  "/contact",
  "/legal/terms",
  "/legal/privacy",
  "/legal/cookies",
];

export default function sitemap(): MetadataRoute.Sitemap {
  const paths = [
    ...staticPaths,
    ...features.map((f) => `/features/${f.slug}`),
    ...docSlugs.map((s) => `/docs/${s}`),
  ];
  const now = new Date();
  return paths.map((p) => {
    const pl = siteUrl(p === "/" ? "/" : p);
    const en = siteUrl(p === "/" ? "/en" : `/en${p}`);
    return {
      url: pl,
      lastModified: now,
      changeFrequency: p === "/status" ? "hourly" : p === "/" ? "weekly" : "monthly",
      priority: p === "/" ? 1 : p.startsWith("/features") || p === "/premium" ? 0.8 : 0.6,
      alternates: { languages: { pl, en } },
    };
  });
}
