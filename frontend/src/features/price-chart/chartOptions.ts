import {
  type CandlestickSeriesPartialOptions,
  type ChartOptions,
  type DeepPartial,
  type HistogramSeriesPartialOptions,
} from "lightweight-charts"

export const RISE = "#d24f45"
export const FALL = "#1e6fd9"

const GRID = "#eceff3"
const TEXT = "#5b6472"

export const won = new Intl.NumberFormat("ko-KR")

export const chartOptions: DeepPartial<ChartOptions> = {
  autoSize: true,
  layout: {
    background: { color: "transparent" },
    textColor: TEXT,
    panes: { separatorColor: GRID, separatorHoverColor: GRID },
  },
  grid: {
    vertLines: { color: GRID },
    horzLines: { color: GRID },
  },
  rightPriceScale: { borderColor: GRID },
  timeScale: { borderColor: GRID, rightOffset: 4 },
  localization: { priceFormatter: (price: number) => won.format(price) },
}

export const candleOptions: CandlestickSeriesPartialOptions = {
  upColor: RISE,
  downColor: FALL,
  borderUpColor: RISE,
  borderDownColor: FALL,
  wickUpColor: RISE,
  wickDownColor: FALL,
  priceFormat: { type: "price", precision: 0, minMove: 1 },
}

export const volumeOptions: HistogramSeriesPartialOptions = {
  priceFormat: { type: "volume" },
  priceLineVisible: false,
}

export const VOLUME_PANE_RATIO = 0.22
