import {
  TickMarkType,
  isBusinessDay,
  type CandlestickSeriesPartialOptions,
  type ChartOptions,
  type DeepPartial,
  type HistogramSeriesPartialOptions,
  type Time,
} from "lightweight-charts"

export const RISE = "#d24f45"
export const FALL = "#1e6fd9"

export const GRID = "#eceff3"
export const TEXT = "#5b6472"

export const BAR = "#9db0c7"
export const BAR_ACTIVE = "#5d7797"
export const MEAN = "#2f3a47"
export const SELECTION = "rgba(45, 111, 217, 0.10)"

export const won = new Intl.NumberFormat("ko-KR")

function ymd(time: Time) {
  if (isBusinessDay(time)) {
    return { year: time.year, month: time.month, day: time.day }
  }
  const date = new Date(typeof time === "number" ? time * 1000 : String(time))
  return {
    year: date.getUTCFullYear(),
    month: date.getUTCMonth() + 1,
    day: date.getUTCDate(),
  }
}

function tickMark(time: Time, type: TickMarkType) {
  const { year, month, day } = ymd(time)
  if (type === TickMarkType.Year) return `${year}년`
  if (type === TickMarkType.Month) return `${month}월`
  if (type === TickMarkType.DayOfMonth) return `${day}일`
  return `${month}.${day}`
}

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
  timeScale: {
    borderColor: GRID,
    rightOffset: 4,
    tickMarkFormatter: (time: Time, type: TickMarkType) => tickMark(time, type),
  },
  localization: {
    locale: "ko-KR",
    dateFormat: "yyyy-MM-dd",
    priceFormatter: (price: number) => won.format(price),
  },
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
