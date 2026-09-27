import { StrictMode } from "react"
import { createRoot } from "react-dom/client"
import "./index.css"
import { PriceChart } from "./features/price-chart/PriceChart"

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <main className="mx-auto max-w-4xl p-6">
      <PriceChart stockCode="005930" />
    </main>
  </StrictMode>,
)
