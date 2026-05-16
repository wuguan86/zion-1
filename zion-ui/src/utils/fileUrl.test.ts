import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizeFileUrl } from './fileUrl.ts'

test('normalizeFileUrl converts stored local image path to public file api path', () => {
  assert.equal(
    normalizeFileUrl('/images/2026/05/16/avatar.png'),
    '/api/files/images/2026/05/16/avatar.png'
  )
})

test('normalizeFileUrl keeps already public and external urls unchanged', () => {
  assert.equal(normalizeFileUrl('/api/files/images/avatar.png'), '/api/files/images/avatar.png')
  assert.equal(normalizeFileUrl('https://cdn.example.com/avatar.png'), 'https://cdn.example.com/avatar.png')
  assert.equal(normalizeFileUrl('data:image/png;base64,abc'), 'data:image/png;base64,abc')
  assert.equal(normalizeFileUrl('blob:http://localhost/avatar'), 'blob:http://localhost/avatar')
})
