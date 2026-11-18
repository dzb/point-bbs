import { defineConfig } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:8082'
const externalServer = Boolean(process.env.E2E_BASE_URL)

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL,
    viewport: { width: 900, height: 1000 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  // Start the real single-machine app unless the caller points E2E_BASE_URL
  // at an already running instance. Requires `./point.sh build` first.
  webServer: externalServer
    ? undefined
    : {
        command: './point.sh run --profile dev',
        cwd: '..',
        url: `${baseURL}/healthz`,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
})
