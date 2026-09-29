import { useState } from "react"
import { BAR, BAR_ACTIVE, GRID, MEAN, TEXT, won } from "./chartOptions"
import type { PriceDistribution } from "./api"

const VIEW_WIDTH = 800
const VIEW_HEIGHT = 200
const PAD_TOP = 28
const PAD_BOTTOM = 22
const PAD_SIDE = 8
const BAR_GAP = 1.5
const X_TICKS = 5
const LABEL_ROOM = 70

type Props = {
  distribution: PriceDistribution
}

export function PriceHistogram({ distribution }: Props) {
  const [hovered, setHovered] = useState<number | null>(null)

  const { bins, mean } = distribution
  const plotWidth = VIEW_WIDTH - PAD_SIDE * 2
  const plotHeight = VIEW_HEIGHT - PAD_TOP - PAD_BOTTOM
  const baseline = PAD_TOP + plotHeight

  const maxCount = Math.max(...bins.map((bin) => bin.count))
  const barWidth = plotWidth / bins.length
  const low = bins[0].from
  const high = bins[bins.length - 1].to

  const meanX = PAD_SIDE + ((mean - low) / (high - low)) * plotWidth
  const meanAnchor =
    meanX < LABEL_ROOM ? "start" : meanX > VIEW_WIDTH - LABEL_ROOM ? "end" : "middle"

  const tickStep = Math.max(1, Math.ceil(bins.length / X_TICKS))
  const active = hovered === null ? null : bins[hovered]

  return (
    <div className="flex flex-col gap-1">
      <p className="text-xs text-neutral-500">
        {active
          ? `${won.format(active.from)} ~ ${won.format(active.to)}원 · ${active.count}일`
          : `구간 폭 ${won.format(bins[0].to - low)}원 · 최다 ${maxCount}일`}
      </p>

      <svg viewBox={`0 0 ${VIEW_WIDTH} ${VIEW_HEIGHT}`} className="h-auto w-full">
        <line x1={PAD_SIDE} y1={PAD_TOP} x2={VIEW_WIDTH - PAD_SIDE} y2={PAD_TOP} stroke={GRID} />
        <line x1={PAD_SIDE} y1={baseline} x2={VIEW_WIDTH - PAD_SIDE} y2={baseline} stroke={GRID} />

        {bins.map((bin, index) => {
          const height = (bin.count / maxCount) * plotHeight
          return (
            <rect
              key={bin.from}
              x={PAD_SIDE + index * barWidth}
              y={baseline - height}
              width={Math.max(barWidth - BAR_GAP, 1)}
              height={height}
              fill={index === hovered ? BAR_ACTIVE : BAR}
              onMouseEnter={() => setHovered(index)}
              onMouseLeave={() => setHovered(null)}
            />
          )
        })}

        <line
          x1={meanX}
          y1={PAD_TOP - 10}
          x2={meanX}
          y2={baseline}
          stroke={MEAN}
          strokeWidth={1.5}
          strokeDasharray="4 3"
        />
        <text x={meanX} y={14} fill={MEAN} fontSize={11} textAnchor={meanAnchor}>
          평균 {won.format(Math.round(mean))}원
        </text>

        {bins.map((bin, index) =>
          index % tickStep === 0 && index * barWidth < plotWidth - LABEL_ROOM ? (
            <text
              key={bin.from}
              x={PAD_SIDE + index * barWidth}
              y={baseline + 15}
              fill={TEXT}
              fontSize={11}
            >
              {won.format(bin.from)}
            </text>
          ) : null,
        )}
        <text
          x={VIEW_WIDTH - PAD_SIDE}
          y={baseline + 15}
          fill={TEXT}
          fontSize={11}
          textAnchor="end"
        >
          {won.format(high)}
        </text>
      </svg>
    </div>
  )
}
