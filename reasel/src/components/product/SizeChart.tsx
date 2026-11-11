import type { Category } from '@/lib/types';
import { sizeCharts } from '@/lib/size-chart';

export function SizeChart({ category }: { category: Category }) {
  const chart = sizeCharts[category];
  return (
    <div>
      <div className="overflow-x-auto rounded-2xl border border-white/10">
        <table className="w-full text-left text-sm">
          <caption className="sr-only">Tabela rozmiarów w centymetrach</caption>
          <thead className="bg-white/5">
            <tr>
              {chart.headers.map((h) => (
                <th key={h} scope="col" className="whitespace-nowrap px-4 py-3 font-semibold uppercase tracking-[0.1em] text-ink-100">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {chart.rows.map((row) => (
              <tr key={row[0]} className="border-t border-white/10">
                {row.map((cell, i) =>
                  i === 0 ? (
                    <th key={i} scope="row" className="px-4 py-3 font-semibold text-acid">
                      {cell}
                    </th>
                  ) : (
                    <td key={i} className="px-4 py-3 tabular-nums text-ink-200">
                      {cell}
                    </td>
                  ),
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="mt-4 text-sm text-ink-300">Wymiary w centymetrach, mierzone na płasko. {chart.note}</p>
      <h3 className="mt-8 font-semibold">Jak się zmierzyć?</h3>
      <ul className="mt-3 list-disc space-y-1.5 pl-5 text-sm text-ink-300">
        <li>Klatka piersiowa – w najszerszym miejscu, pod pachami.</li>
        <li>Pas – w najwęższym miejscu tułowia.</li>
        <li>Najlepiej porównaj wymiary z ulubionym ubraniem, które już masz.</li>
      </ul>
    </div>
  );
}
