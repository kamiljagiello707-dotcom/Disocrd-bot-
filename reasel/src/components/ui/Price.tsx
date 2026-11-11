import { formatPrice, cn } from '@/lib/format';

export function Price({ price, compareAt, className }: { price: number; compareAt?: number; className?: string }) {
  const onSale = compareAt && compareAt > price;
  return (
    <span className={cn('inline-flex items-baseline gap-2 tabular-nums', className)}>
      <span className={onSale ? 'text-acid' : undefined}>
        {onSale && <span className="sr-only">Cena promocyjna: </span>}
        {formatPrice(price)}
      </span>
      {onSale && (
        <s className="text-[0.85em] text-ink-400">
          <span className="sr-only">Cena regularna: </span>
          {formatPrice(compareAt)}
        </s>
      )}
    </span>
  );
}
