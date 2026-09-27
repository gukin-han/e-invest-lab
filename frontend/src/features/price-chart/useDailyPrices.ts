import { useEffect, useState } from "react"
import { fetchDailyPrices, type DailyPrice } from "./api"

type Settled =
  | { key: string; prices: DailyPrice[] }
  | { key: string; error: string }

export type DailyPricesState = {
  prices: DailyPrice[] | null
  error: string | null
  isLoading: boolean
}

function requestKey(stockCode: string, from?: string, to?: string) {
  return `${stockCode}|${from ?? ""}|${to ?? ""}`
}

export function useDailyPrices(
  stockCode: string,
  from?: string,
  to?: string,
): DailyPricesState {
  const [settled, setSettled] = useState<Settled | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    const key = requestKey(stockCode, from, to)

    fetchDailyPrices(stockCode, { from, to }, controller.signal)
      .then((prices) => setSettled({ key, prices }))
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
    prices: current && "prices" in current ? current.prices : null,
    error: current && "error" in current ? current.error : null,
    isLoading: current === null,
  }
}
