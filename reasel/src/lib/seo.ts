import { siteConfig } from '@/config/site';
import type { Product } from './types';

/** Dane strukturalne schema.org/Product (JSON-LD) dla strony produktu. */
export function productJsonLd(product: Product) {
  const url = `${siteConfig.url}/produkt/${product.slug}`;
  const inStock = product.sizes.some((s) => !product.soldOutSizes?.includes(s));
  return {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: product.name,
    description: product.description,
    sku: product.sku,
    image: product.images.map((i) => i.src),
    brand: { '@type': 'Brand', name: 'Reasel' },
    category: product.category,
    color: product.colors.map((c) => c.name).join(', '),
    material: product.composition,
    aggregateRating: {
      '@type': 'AggregateRating',
      ratingValue: product.rating,
      reviewCount: product.reviewsCount,
    },
    offers: {
      '@type': 'Offer',
      url,
      priceCurrency: 'PLN',
      price: (product.price / 100).toFixed(2),
      availability: inStock ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock',
      itemCondition: 'https://schema.org/NewCondition',
      seller: { '@type': 'Organization', name: siteConfig.name },
      shippingDetails: {
        '@type': 'OfferShippingDetails',
        shippingDestination: { '@type': 'DefinedRegion', addressCountry: 'PL' },
        shippingRate: { '@type': 'MonetaryAmount', value: '13.99', currency: 'PLN' },
      },
      hasMerchantReturnPolicy: {
        '@type': 'MerchantReturnPolicy',
        applicableCountry: 'PL',
        returnPolicyCategory: 'https://schema.org/MerchantReturnFiniteReturnWindow',
        merchantReturnDays: 30,
      },
    },
  };
}

export function breadcrumbJsonLd(items: { name: string; path: string }[]) {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, i) => ({
      '@type': 'ListItem',
      position: i + 1,
      name: item.name,
      item: `${siteConfig.url}${item.path}`,
    })),
  };
}

export function organizationJsonLd() {
  return {
    '@context': 'https://schema.org',
    '@graph': [
      {
        '@type': 'Organization',
        name: siteConfig.name,
        url: siteConfig.url,
        email: siteConfig.email,
        telephone: siteConfig.phone,
        sameAs: Object.values(siteConfig.social),
      },
      {
        '@type': 'WebSite',
        name: siteConfig.name,
        url: siteConfig.url,
        inLanguage: 'pl-PL',
      },
    ],
  };
}
