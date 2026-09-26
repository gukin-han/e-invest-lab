import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

function App() {
  return (
    <div className="min-h-screen bg-background px-6 py-10">
      <Card className="mx-auto max-w-xl">
        <CardHeader>
          <CardTitle>e-invest-lab</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          대시보드 재작성 중입니다. 기존 화면은{" "}
          <a className="underline" href="/index.html">
            /index.html
          </a>{" "}
          에 있습니다.
        </CardContent>
      </Card>
    </div>
  )
}

export default App
