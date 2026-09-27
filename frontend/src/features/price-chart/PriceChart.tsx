import { useEffect, useRef } from "react"
import {
  CandlestickSeries,
  HistogramSeries,
  createChart,
  type CandlestickData,
  type HistogramData,
  type IChartApi,
  type ISeriesApi,
  type Time,
} from "lightweight-charts"
import {
  FALL,
  RISE,
  VOLUME_PANE_RATIO,
  candleOptions,
  chartOptions,
  volumeOptions,
  won,
} from "./chartOptions"
import { useDailyPrices } from "./useDailyPrices"

const FULL_HISTORY_FROM = "2000-01-01"
const INITIAL_VISIBLE_BARS = 120

type Props = {
  stockCode: string
  from?: string
  to?: string
  height?: number
}

export function PriceChart({
  stockCode,
  from = FULL_HISTORY_FROM,
  to,
  height = 420,
}: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const candleRef = useRef<ISeriesApi<"Candlestick"> | null>(null)
  const volumeRef = useRef<ISeriesApi<"Histogram"> | null>(null)

  const { prices, error, isLoading } = useDailyPrices(stockCode, from, to)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const chart = createChart(container, chartOptions)
    candleRef.current = chart.addSeries(CandlestickSeries, candleOptions)
    volumeRef.current = chart.addSeries(HistogramSeries, volumeOptions, 1)
    chart.panes()[1]?.setHeight(Math.round(height * VOLUME_PANE_RATIO))
    chartRef.current = chart

    return () => {
      chart.remove()
      chartRef.current = null
      candleRef.current = null
      volumeRef.current = null
    }
  }, [height])

  useEffect(() => {
    const candles = candleRef.current
    const volume = volumeRef.current
    if (!candles || !volume || !prices) return

    candles.setData(
      prices.map<CandlestickData<Time>>((price) => ({
        time: price.tradeDate,
        open: price.openPrice,
        high: price.highPrice,
        low: price.lowPrice,
        close: price.closePrice,
      })),
    )

    volume.setData(
      prices.map<HistogramData<Time>>((price) => ({
        time: price.tradeDate,
        value: price.volume,
        color: price.closePrice >= price.openPrice ? `${RISE}66` : `${FALL}66`,
      })),
    )

    chartRef.current?.timeScale().setVisibleLogicalRange({
      from: Math.max(0, prices.length - INITIAL_VISIBLE_BARS),
      to: prices.length,
    })
  }, [prices])

  const latest = prices?.at(-1)

  return (
    <section className="flex flex-col gap-2">
      <header className="flex items-baseline justify-between">
        <h2 className="text-base font-semibold">{stockCode}</h2>
        {latest && (
          <p className="text-sm text-neutral-500">
            {latest.tradeDate}
            <span className="ml-2 font-medium text-neutral-900">
              {won.format(latest.closePrice)}원
            </span>
          </p>
        )}
      </header>

      <div className="relative" style={{ height }}>
        <div ref={containerRef} className="h-full w-full" />
        {!prices?.length && (
          <ChartOverlay error={error} isLoading={isLoading} />
        )}
      </div>
    </section>
  )
}

function ChartOverlay({
  error,
  isLoading,
}: {
  error: string | null
  isLoading: boolean
}) {
  const message = error ?? (isLoading ? "불러오는 중" : "해당 기간에 거래일이 없습니다")

  return (
    <div className="absolute inset-0 grid place-items-center bg-white/80 text-sm">
      <p className={error ? "text-red-600" : "text-neutral-400"}>{message}</p>
    </div>
  )
}
