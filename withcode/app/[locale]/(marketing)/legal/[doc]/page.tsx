import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { setRequestLocale } from "next-intl/server";
import { routing } from "@/i18n/routing";
import { pageMetadata } from "@/lib/seo/metadata";

const DOCS = ["terms", "privacy", "cookies"] as const;
type Doc = (typeof DOCS)[number];
const isDoc = (d: string): d is Doc => (DOCS as readonly string[]).includes(d);

export const dynamicParams = false;

export function generateStaticParams() {
  return routing.locales.flatMap((locale) => DOCS.map((doc) => ({ locale, doc })));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string; doc: string }>;
}): Promise<Metadata> {
  const { locale, doc } = await params;
  if (!isDoc(doc)) return {};
  return pageMetadata({ locale, path: `/legal/${doc}`, titleKey: doc });
}

export default async function LegalPage({
  params,
}: {
  params: Promise<{ locale: string; doc: string }>;
}) {
  const { locale, doc } = await params;
  if (!isDoc(doc)) notFound();
  setRequestLocale(locale);
  const { default: Content } = (await import(
    `@/content/legal/${locale === "en" ? "en" : "pl"}/${doc}.mdx`
  )) as { default: React.ComponentType };
  return (
    <section className="container-x pt-36 pb-28 md:pt-48">
      <article className="prose-wc">
        <Content />
      </article>
    </section>
  );
}
