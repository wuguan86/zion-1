import assert from 'node:assert/strict'
import { formatDateTime } from '../src/utils/datetime.ts'

assert.equal(formatDateTime('2026-05-16T16:06:33.336047'), '2026-05-16 16:06:33')
assert.equal(formatDateTime('2026-05-15 00:11:59.586853'), '2026-05-15 00:11:59')
assert.equal(formatDateTime('2026-01-29T22:42:08'), '2026-01-29 22:42:08')
assert.equal(formatDateTime(null), '-')
assert.equal(formatDateTime(''), '-')

console.log('时间格式化验证通过')
