export type DailyPrice = {
  tradeDate: string
  openPrice: number
  highPrice: number
  lowPrice: number
  closePrice: number
  volume: number
}

type ApiResponse<T> = { data: T }

export async function fetchDailyPrices(
  stockCode: string,
  range: { from?: string; to?: string },
  signal?: AbortSignal,
): Promise<DailyPrice[]> {
  const query = new URLSearchParams()
  if (range.from) query.set("from", range.from)
  if (range.to) query.set("to", range.to)
  const suffix = query.size > 0 ? `?${query}` : ""
  
  const response = await fetch(`/api/stocks/${stockCode}/prices${suffix}`, {
    signal,
  })
  if (!response.ok) {
    throw new Error(`시세 조회에 실패했습니다 (${response.status})`)
  }

  const body: ApiResponse<DailyPrice[]> = await response.json()
  return body.data
}
