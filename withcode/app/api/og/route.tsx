import { ImageResponse } from "next/og";

/** Dynamiczny obrazek Open Graph w stylu strony: czerń, znak „W”, duży tytuł. */
export function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const title = (searchParams.get("title") ?? "WithBot").slice(0, 90);
  const subtitle = (searchParams.get("subtitle") ?? "Moderacja Discorda, która nie śpi.").slice(
    0,
    140,
  );
  return new ImageResponse(
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        padding: 72,
        background: "radial-gradient(circle at 78% 40%, #262626 0%, #050505 45%, #000 70%)",
        color: "#fff",
        fontFamily: "sans-serif",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 18 }}>
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: 16,
            border: "2px solid rgba(255,255,255,0.3)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 34,
            fontWeight: 800,
          }}
        >
          W
        </div>
        <div style={{ fontSize: 30, fontWeight: 600 }}>
          WithCode<span style={{ color: "#6b6b6b" }}>.pl</span>
        </div>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
        <div
          style={{
            fontSize: 84,
            fontWeight: 800,
            letterSpacing: -3,
            lineHeight: 0.95,
            maxWidth: 980,
          }}
        >
          {title}
        </div>
        <div style={{ fontSize: 30, color: "#a1a1a1", maxWidth: 900 }}>{subtitle}</div>
      </div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          fontSize: 22,
          color: "#6b6b6b",
          letterSpacing: 4,
        }}
      >
        <span>WITHBOT</span>
        <span>DISCORD MODERATION</span>
      </div>
    </div>,
    { width: 1200, height: 630 },
  );
}
