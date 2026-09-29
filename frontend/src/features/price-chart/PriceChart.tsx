import { useEffect, useRef, useState } from "react"
import {
  CandlestickSeries,
  HistogramSeries,
  createChart,
  type CandlestickData,
  type HistogramData,
  type IChartApi,
  type ISeriesApi,
  type MouseEventParams,
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
import { PriceHistogram } from "./PriceHistogram"
import { RangeHighlight } from "./rangeHighlight"
import { RangeStats } from "./RangeStats"
import { useDailyPrices } from "./useDailyPrices"
import { usePriceDistribution } from "./usePriceDistribution"
import { useRangeSelection } from "./useRangeSelection"

const FULL_HISTORY_FROM = "2000-01-01"
const INITIAL_VISIBLE_BARS = 120
const DISTRIBUTION_BARS = 60

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
  const candleRef = useRef<ISeriesApi<"Candlestick"> | null>(null)
  const volumeRef = useRef<ISeriesApi<"Histogram"> | null>(null)
  const highlightRef = useRef<RangeHighlight | null>(null)
  const [chart, setChart] = useState<IChartApi | null>(null)

  const { prices, error, isLoading } = useDailyPrices(stockCode, from, to)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const chart = createChart(container, chartOptions)
    candleRef.current = chart.addSeries(CandlestickSeries, candleOptions)
    volumeRef.current = chart.addSeries(HistogramSeries, volumeOptions, 1)
    chart.panes()[1]?.setHeight(Math.round(height * VOLUME_PANE_RATIO))

    const highlight = new RangeHighlight()
    chart.panes()[0]?.attachPrimitive(highlight)
    highlightRef.current = highlight
    setChart(chart)

    return () => {
      chart.remove()
      candleRef.current = null
      volumeRef.current = null
      highlightRef.current = null
      setChart(null)
    }
  }, [height])

  useEffect(() => {
    const candles = candleRef.current
    const volume = volumeRef.current
    if (!chart || !candles || !volume || !prices) return

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

    chart.timeScale().setVisibleLogicalRange({
      from: Math.max(0, prices.length - INITIAL_VISIBLE_BARS),
      to: prices.length,
    })
  }, [chart, prices])

  const selection = useRangeSelection()
  const { pick, track, cancel, clear } = selection

  useEffect(() => clear(), [stockCode, clear])

  useEffect(() => {
    if (!selection.isPicking) return

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") cancel()
    }
    window.addEventListener("keydown", onKeyDown)

    return () => window.removeEventListener("keydown", onKeyDown)
  }, [selection.isPicking, cancel])

  useEffect(() => {
    if (!chart || !prices?.length) return

    const clamp = (logical: number) =>
      Math.min(prices.length - 1, Math.max(0, Math.round(logical)))
    const onClick = (param: MouseEventParams<Time>) => {
      if (param.logical !== undefined) pick(clamp(param.logical))
    }
    const onMove = (param: MouseEventParams<Time>) => {
      track(param.logical === undefined ? null : clamp(param.logical))
    }

    chart.subscribeClick(onClick)
    chart.subscribeCrosshairMove(onMove)

    return () => {
      chart.unsubscribeClick(onClick)
      chart.unsubscribeCrosshairMove(onMove)
    }
  }, [chart, prices, pick, track])

  const latest = prices?.at(-1)
  const defaultRange = prices?.length
    ? { from: Math.max(0, prices.length - DISTRIBUTION_BARS), to: prices.length - 1 }
    : null
  const activeRange = selection.committed ?? defaultRange

  const shown = selection.preview ?? activeRange
  const shownFrom = shown?.from ?? null
  const shownTo = shown?.to ?? null

  useEffect(() => {
    highlightRef.current?.setRange(
      shownFrom === null || shownTo === null
        ? null
        : { from: shownFrom, to: shownTo },
    )
  }, [chart, shownFrom, shownTo])

  const windowFrom =
    prices && activeRange
      ? prices[Math.min(activeRange.from, activeRange.to)]?.tradeDate
      : undefined
  const windowTo =
    prices && activeRange
      ? prices[Math.max(activeRange.from, activeRange.to)]?.tradeDate
      : undefined

  const distributionState = usePriceDistribution(stockCode, windowFrom, windowTo)

  return (
    <section className="flex flex-col gap-4">
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

      {windowFrom && windowTo && (
        <section className="flex flex-col gap-2">
          <div className="flex items-baseline gap-3">
            <h3 className="text-sm font-semibold text-neutral-700">주가 분포</h3>
            <p className="text-xs text-neutral-400">
              {selection.isPicking
                ? "끝 지점을 클릭하세요 · Esc 취소"
                : "차트를 두 번 클릭해 구간을 고릅니다"}
            </p>
            {selection.committed && (
              <button
                type="button"
                onClick={clear}
                className="ml-auto text-xs text-neutral-500 underline underline-offset-2"
              >
                최근 {DISTRIBUTION_BARS}일로
              </button>
            )}
          </div>
          <div className="grid gap-6 md:grid-cols-[1fr_180px]">
            {distributionState.distribution ? (
              <>
                <PriceHistogram distribution={distributionState.distribution} />
                <RangeStats
                  distribution={distributionState.distribution}
                  from={windowFrom}
                  to={windowTo}
                />
              </>
            ) : (
              <DistributionNotice
                error={distributionState.error}
                isLoading={distributionState.isLoading}
              />
            )}
          </div>
        </section>
      )}
    </section>
  )
}

function DistributionNotice({
  error,
  isLoading,
}: {
  error: string | null
  isLoading: boolean
}) {
  const message = error ?? (isLoading ? "불러오는 중" : "구간에 거래일이 없습니다")

  return (
    <p className={`text-sm ${error ? "text-red-600" : "text-neutral-400"}`}>
      {message}
    </p>
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
