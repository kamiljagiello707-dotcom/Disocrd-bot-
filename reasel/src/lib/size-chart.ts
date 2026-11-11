import type { Category } from './types';

/** Tabele rozmiarów w centymetrach (wymiary produktu na płasko). */
export const sizeCharts: Record<Category, { headers: string[]; rows: string[][]; note: string }> = {
  bluzy: {
    headers: ['Rozmiar', 'Szerokość klatki', 'Długość', 'Długość rękawa'],
    rows: [
      ['XS', '58', '66', '58'],
      ['S', '61', '68', '60'],
      ['M', '64', '70', '62'],
      ['L', '67', '72', '64'],
      ['XL', '70', '74', '66'],
      ['XXL', '73', '76', '68'],
    ],
    note: 'Bluzy oversize mają celowo obniżoną linię ramion. Jeśli wolisz dopasowany krój, wybierz rozmiar mniejszy.',
  },
  't-shirty': {
    headers: ['Rozmiar', 'Szerokość klatki', 'Długość', 'Długość rękawa'],
    rows: [
      ['XS', '50', '68', '20'],
      ['S', '53', '70', '21'],
      ['M', '56', '72', '22'],
      ['L', '59', '74', '23'],
      ['XL', '62', '76', '24'],
      ['XXL', '65', '78', '25'],
    ],
    note: 'Koszulki są wstępnie skurczone – po praniu zachowają wymiary (tolerancja ±1 cm).',
  },
  spodnie: {
    headers: ['Rozmiar', 'Obwód pasa', 'Obwód bioder', 'Długość nogawki wewn.'],
    rows: [
      ['XS', '70', '92', '78'],
      ['S', '76', '98', '80'],
      ['M', '82', '104', '81'],
      ['L', '88', '110', '82'],
      ['XL', '94', '116', '83'],
      ['XXL', '100', '122', '84'],
    ],
    note: 'Jeansy RAW Selvedge mogą rozciągnąć się w pasie o ok. 1–2 cm w trakcie noszenia.',
  },
  kurtki: {
    headers: ['Rozmiar', 'Szerokość klatki', 'Długość', 'Długość rękawa'],
    rows: [
      ['XS', '54', '64', '61'],
      ['S', '57', '66', '63'],
      ['M', '60', '68', '65'],
      ['L', '63', '70', '67'],
      ['XL', '66', '72', '69'],
      ['XXL', '69', '74', '71'],
    ],
    note: 'Kurtki zostały zaprojektowane tak, aby zmieścić pod nimi bluzę.',
  },
  akcesoria: {
    headers: ['Rozmiar', 'Obwód głowy / wymiary'],
    rows: [['ONE SIZE', 'Czapki: 54–61 cm (regulacja) · Plecak: 45 × 30 × 14 cm · Portfel: 11 × 9 cm']],
    note: 'Akcesoria dostępne są w jednym, uniwersalnym rozmiarze.',
  },
};
