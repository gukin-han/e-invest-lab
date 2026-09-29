import type {
  IChartApi,
  IPanePrimitive,
  IPanePrimitivePaneView,
  IPrimitivePaneRenderer,
  Logical,
  PaneAttachedParameter,
  Time,
} from "lightweight-charts"
import { SELECTION } from "./chartOptions"

type RenderTarget = Parameters<IPrimitivePaneRenderer["draw"]>[0]

export type LogicalRange = {
  from: number
  to: number
}

export class RangeHighlight implements IPanePrimitive<Time> {
  private chart: IChartApi | null = null
  private requestUpdate: (() => void) | null = null
  private range: LogicalRange | null = null

  private readonly views: readonly IPanePrimitivePaneView[] = [
    {
      zOrder: () => "bottom",
      renderer: () => this.buildRenderer(),
    },
  ]

  attached({ chart, requestUpdate }: PaneAttachedParameter<Time>) {
    this.chart = chart
    this.requestUpdate = requestUpdate
  }

  detached() {
    this.chart = null
    this.requestUpdate = null
  }

  paneViews() {
    return this.views
  }

  setRange(range: LogicalRange | null) {
    this.range = range
    this.requestUpdate?.()
  }

  private buildRenderer(): IPrimitivePaneRenderer | null {
    const chart = this.chart
    const range = this.range
    if (!chart || !range) return null

    const timeScale = chart.timeScale()
    const margin = timeScale.options().barSpacing / 2
    const left = timeScale.logicalToCoordinate(
      Math.min(range.from, range.to) as Logical,
    )
    const right = timeScale.logicalToCoordinate(
      Math.max(range.from, range.to) as Logical,
    )
    if (left === null || right === null) return null

    return {
      draw: (target: RenderTarget) =>
        target.useMediaCoordinateSpace(({ context, mediaSize }) => {
          context.fillStyle = SELECTION
          context.fillRect(left - margin, 0, right - left + margin * 2, mediaSize.height)
        }),
    }
  }
}
