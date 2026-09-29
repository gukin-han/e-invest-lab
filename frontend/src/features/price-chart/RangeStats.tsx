import { won } from "./chartOptions"
import type { PriceDistribution } from "./api"

type Props = {
  distribution: PriceDistribution
  from: string
  to: string
}

export function RangeStats({ distribution, from, to }: Props) {
  return (
    <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm md:grid-cols-1">
      <Stat label="기간" value={`${from} ~ ${to}`} />
      <Stat label="거래일" value={`${distribution.tradingDays}일`} />
      <Stat
        label="평균"
        value={`${won.format(Math.round(distribution.mean))}원`}
        emphasis
      />
      <Stat
        label="최저 · 최고"
        value={`${won.format(distribution.minPrice)} · ${won.format(distribution.maxPrice)}원`}
      />
    </dl>
  )
}

function Stat({
  label,
  value,
  emphasis = false,
}: {
  label: string
  value: string
  emphasis?: boolean
}) {
  return (
    <div>
      <dt className="text-xs text-neutral-500">{label}</dt>
      <dd
        className={
          emphasis
            ? "text-base font-semibold text-neutral-900"
            : "text-neutral-800"
        }
      >
        {value}
      </dd>
    </div>
  )
}
