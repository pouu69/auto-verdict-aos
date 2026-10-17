// Minimal Node typings for tests (no @types/node in this package). Extend only as needed.
declare module 'node:fs' {
  export function readFileSync(path: string, encoding: 'utf8'): string;
  export function writeFileSync(path: string, data: string): void;
  export function existsSync(path: string): boolean;
}
declare module 'node:url' {
  export function fileURLToPath(url: string | URL): string;
}
declare const process: { env: Record<string, string | undefined> };
