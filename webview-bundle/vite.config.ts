import { defineConfig } from 'vite';
import path from 'node:path';

export default defineConfig({
  resolve: {
    alias: {
      '@core': path.resolve(__dirname, 'src/core'),
    },
  },
  build: {
    outDir: 'dist',
    // Single classic script for the headless engine page (spec 3.3);
    // public/engine.html is copied next to it.
    lib: {
      entry: path.resolve(__dirname, 'src/engine.ts'),
      formats: ['iife'],
      name: 'AvEngine',
      fileName: () => 'engine.js',
    },
    copyPublicDir: true,
  },
  test: {
    globals: true,
    environment: 'node',
  },
});
