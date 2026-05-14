import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('main app plugin registration', () => {
  it('registers Naive UI globally so login form controls render with styles', () => {
    const mainSource = readFileSync(resolve(__dirname, '../main.ts'), 'utf-8')

    expect(mainSource).toMatch(/import\s+naive(?:\s*,\s*\{[^}]*\})?\s+from\s+['"]naive-ui['"]/)
    expect(mainSource).toContain('app.use(naive)')
  })
})
