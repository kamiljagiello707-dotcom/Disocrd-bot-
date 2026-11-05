import Link from "next/link";

export default function GlobalNotFound() {
  return (
    <html lang="pl">
      <body
        style={{
          background: "#000",
          color: "#fff",
          fontFamily: "system-ui",
          display: "grid",
          placeItems: "center",
          minHeight: "100vh",
          margin: 0,
        }}
      >
        <main style={{ textAlign: "center" }}>
          <h1 style={{ fontSize: "4rem", margin: 0 }}>404</h1>
          <p style={{ color: "#a1a1a1" }}>Nie znaleziono strony · Page not found</p>
          <Link href="/" style={{ color: "#fff" }}>
            WithCode.pl
          </Link>
        </main>
      </body>
    </html>
  );
}
