import { siteUrl } from "@/lib/utils";

export function JsonLd({ data }: { data: Record<string, unknown> }) {
  return (
    <script
      type="application/ld+json"
      // treść generowana przez nas, escapujemy „<” przeciw wstrzyknięciu </script>
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data).replace(/</g, "\\u003c") }}
    />
  );
}

export function softwareApplication(locale: string, offers?: { name: string; price: number }[]) {
  return {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: "WithBot",
    applicationCategory: "CommunicationApplication",
    operatingSystem: "Discord",
    url: siteUrl(),
    inLanguage: locale,
    publisher: { "@type": "Organization", name: "WithCode.pl", url: siteUrl() },
    offers: (offers ?? [{ name: "Free", price: 0 }]).map((o) => ({
      "@type": "Offer",
      name: o.name,
      price: o.price.toFixed(2),
      priceCurrency: "PLN",
    })),
  };
}

export function faqPage(items: { q: string; a: string }[]) {
  return {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: items.map((i) => ({
      "@type": "Question",
      name: i.q,
      acceptedAnswer: { "@type": "Answer", text: i.a },
    })),
  };
}
