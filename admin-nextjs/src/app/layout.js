import './globals.css'

export const metadata = {
  title: 'Madadwala Admin Dashboard',
  description: 'Professional Management Portal',
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link href="https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,300..800;1,300..800&display=swap" rel="stylesheet" />
      </head>
      <body style={{ fontFamily: "'Open Sans', sans-serif" }} className="antialiased font-normal">
        {children}
      </body>
    </html>
  )
}
