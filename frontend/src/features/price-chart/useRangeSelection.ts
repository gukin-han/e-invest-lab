import { useCallback, useRef, useState } from "react"
import type { LogicalRange } from "./rangeHighlight"

export type RangeSelection = {
  committed: LogicalRange | null
  preview: LogicalRange | null
  isPicking: boolean
  pick: (index: number) => void
  track: (index: number | null) => void
  cancel: () => void
  clear: () => void
}

export function useRangeSelection(): RangeSelection {
  const [committed, setCommitted] = useState<LogicalRange | null>(null)
  const [anchor, setAnchor] = useState<number | null>(null)
  const [hover, setHover] = useState<number | null>(null)
  const anchorRef = useRef<number | null>(null)

  const pick = useCallback((index: number) => {
    if (anchorRef.current === null) {
      anchorRef.current = index
      setAnchor(index)
      setHover(index)
      return
    }
    setCommitted({ from: anchorRef.current, to: index })
    anchorRef.current = null
    setAnchor(null)
    setHover(null)
  }, [])

  const track = useCallback((index: number | null) => {
    if (anchorRef.current === null || index === null) return
    setHover(index)
  }, [])

  const cancel = useCallback(() => {
    anchorRef.current = null
    setAnchor(null)
    setHover(null)
  }, [])

  const clear = useCallback(() => {
    cancel()
    setCommitted(null)
  }, [cancel])

  return {
    committed,
    preview: anchor === null || hover === null ? null : { from: anchor, to: hover },
    isPicking: anchor !== null,
    pick,
    track,
    cancel,
    clear,
  }
}
