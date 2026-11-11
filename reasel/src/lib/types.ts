/**
 * Typy domenowe sklepu.
 * Struktura jest celowo zbliżona do modeli Shopify / Stripe (ceny w groszach,
 * warianty kolor × rozmiar, SKU), aby podpięcie prawdziwego backendu było proste.
 */

export type Category = 'bluzy' | 't-shirty' | 'spodnie' | 'kurtki' | 'akcesoria';

export type Size = 'XS' | 'S' | 'M' | 'L' | 'XL' | 'XXL' | 'ONE SIZE';

/** Rodzaj modelu 3D używanego w podglądzie (proceduralny lub podmieniony na .glb). */
export type GarmentType = 'hoodie' | 'jacket' | 'tshirt' | 'pants' | 'cap' | 'bag';

/** Rodzina kolorów – używana przez filtr kolorów w sklepie. */
export type ColorFamily =
  | 'czarny'
  | 'biały'
  | 'szary'
  | 'beżowy'
  | 'zielony'
  | 'niebieski'
  | 'czerwony'
  | 'pomarańczowy'
  | 'różowy'
  | 'żółty'
  | 'brązowy'
  | 'limonkowy';

export interface ColorVariant {
  /** Nazwa wyświetlana, np. „Grafit” */
  name: string;
  /** Kolor HEX – używany w swatchach i do barwienia modelu 3D */
  hex: string;
  family: ColorFamily;
}

export interface ProductImage {
  src: string;
  alt: string;
}

export interface Product {
  id: string;
  slug: string;
  sku: string;
  name: string;
  category: Category;
  /** Cena w groszach (jak w Stripe / Shopify Storefront API) */
  price: number;
  /** Cena przed obniżką w groszach */
  compareAtPrice?: number;
  shortDescription: string;
  description: string;
  details: string[];
  composition: string;
  care: string[];
  colors: ColorVariant[];
  sizes: Size[];
  /** Rozmiary chwilowo niedostępne */
  soldOutSizes?: Size[];
  images: ProductImage[];
  model: GarmentType;
  tags: Array<'nowosc' | 'bestseller' | 'limitowana'>;
  rating: number;
  reviewsCount: number;
  /** Data dodania (ISO) – do sortowania „Najnowsze” */
  createdAt: string;
}

export interface CartItem {
  /** Unikalny klucz: produkt + kolor + rozmiar */
  key: string;
  productId: string;
  slug: string;
  name: string;
  price: number;
  image: string;
  color: Pick<ColorVariant, 'name' | 'hex'>;
  size: Size;
  quantity: number;
}
