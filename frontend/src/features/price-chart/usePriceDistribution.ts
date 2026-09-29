import { useEffect, useState } from "react"
import { fetchPriceDistribution, type PriceDistribution } from "./api"

type Settled =
  | { key: string; distribution: PriceDistribution | null }
  | { key: string; error: string }

export type PriceDistributionState = {
  distribution: PriceDistribution | null
  error: string | null
  isLoading: boolean
}

function requestKey(stockCode: string, from?: string, to?: string) {
  return `${stockCode}|${from ?? ""}|${to ?? ""}`
}

export function usePriceDistribution(
  stockCode: string,
  from?: string,
  to?: string,
): PriceDistributionState {
  const [settled, setSettled] = useState<Settled | null>(null)

  useEffect(() => {
    if (!from || !to) return

    const controller = new AbortController()
    const key = requestKey(stockCode, from, to)

    fetchPriceDistribution(stockCode, { from, to }, controller.signal)
      .then((distribution) => setSettled({ key, distribution }))
      .catch((cause: unknown) => {
        if (controller.signal.aborted) return
        setSettled({
          key,
          error: cause instanceof Error ? cause.message : "알 수 없는 오류",
        })
      })

    return () => controller.abort()
  }, [stockCode, from, to])

  const current =
    settled?.key === requestKey(stockCode, from, to) ? settled : null

  return {
    distribution: current && "distribution" in current ? current.distribution : null,
    error: current && "error" in current ? current.error : null,
    isLoading: current === null && Boolean(from && to),
  }
}
