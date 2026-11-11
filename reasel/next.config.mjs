/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Three.js i drei korzystają z ESM – transpilacja zapewnia zgodność z bundlerem Next.js
  transpilePackages: ['three'],
  images: {
    formats: ['image/avif', 'image/webp'],
    remotePatterns: [{ protocol: 'https', hostname: 'images.unsplash.com' }],
  },
  experimental: {
    optimizePackageImports: ['@react-three/drei', 'framer-motion', 'gsap'],
  },
  async headers() {
    return [
      {
        // Długie cache dla modeli 3D, fontów i dekodera Draco
        source: '/:dir(models|draco|fonts)/:path*',
        headers: [{ key: 'Cache-Control', value: 'public, max-age=31536000, immutable' }],
      },
    ];
  },
};

export default nextConfig;
