import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  cacheComponents: true,
  partialPrefetching: true,
  devIndicators: process.env.DAILYMART_DEVTOOLS === "1" ? { position: "bottom-left" } : false,
  turbopack: {
    rules: {
      "*.css": {
        condition: { not: { path: "**/*.module.css" } },
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
      "**/next/dist/compiled/next-devtools/index.js": {
        condition: { all: ["development", "browser"] },
        loaders: ["./scripts/localize-next-devtools.cjs"],
      },
    },
  },
  experimental: {
    proxyClientMaxBodySize: "15mb",
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://127.0.0.1:8085/api/:path*",
      },
    ];
  },
};

export default nextConfig;
