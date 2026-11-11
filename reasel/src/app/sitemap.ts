import type { MetadataRoute } from 'next';
import { siteConfig } from '@/config/site';
import { categories, getProducts } from '@/lib/catalog';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const products = await getProducts();
  const now = new Date();
  const staticPages = ['', '/sklep', '/o-nas', '/kontakt', '/regulamin', '/polityka-prywatnosci', '/zwroty'].map((path) => ({
    url: `${siteConfig.url}${path}`,
    lastModified: now,
    changeFrequency: path === '' || path === '/sklep' ? ('daily' as const) : ('monthly' as const),
    priority: path === '' ? 1 : path === '/sklep' ? 0.9 : 0.4,
  }));
  const categoryPages = categories.map((c) => ({
    url: `${siteConfig.url}/sklep?kategoria=${c.slug}`,
    lastModified: now,
    changeFrequency: 'weekly' as const,
    priority: 0.7,
  }));
  const productPages = products.map((p) => ({
    url: `${siteConfig.url}/produkt/${p.slug}`,
    lastModified: new Date(p.createdAt),
    changeFrequency: 'weekly' as const,
    priority: 0.8,
  }));
  return [...staticPages, ...categoryPages, ...productPages];
}
