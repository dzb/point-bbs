import type { Comment } from '@/types'

/** Rail marks for X-style conversation lines on a flat comment list. */
export interface ThreadMarks {
  /** Parents with a visible reply below: rail starts under their avatar. */
  starts: Set<number>
  /** Rows bridged between a visible parent and its reply: rail runs full height. */
  mids: Set<number>
}

/**
 * Mark the rows a parent→reply rail passes through.
 *
 * The child keeps its own stub above it (existing `op-connector`); the
 * parent's rail-start plus the full-height rails of every row strictly
 * between them join into one line from the parent avatar to the child.
 * Pairs whose parent is absent (other page, deleted) mark nothing, so no
 * misleading line is drawn — the 回复 @x label carries the context alone.
 */
export function markThreads(list: Comment[]): ThreadMarks {
  const idx = new Map<number, number>()
  list.forEach((c, i) => idx.set(c.id, i))
  const starts = new Set<number>()
  const mids = new Set<number>()
  for (const c of list) {
    if (c.quoteId <= 0) continue
    const p = idx.get(c.quoteId)
    const ci = idx.get(c.id)
    if (p === undefined || ci === undefined || p >= ci) continue
    starts.add(c.quoteId)
    for (let i = p + 1; i < ci; i++) mids.add(list[i].id)
  }
  return { starts, mids }
}
