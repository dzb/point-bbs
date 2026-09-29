import type { Comment } from '@/types'

/** Vertical rails for X-style conversation lines on a flat comment list. */
export interface RailRows {
  /** Rows crossed by at least one segment: id → lanes + which lane starts here. */
  rows: Map<number, { lanes: number[]; startLane: number | null }>
}

/**
 * Assign one lane per top-level conversation (ultimate quote ancestor),
 * so overlapping pairs render as distinct parallel rails git-graph style
 * instead of merging into one misleading line. A single chain (A→B→C)
 * shares its root's lane and stays one continuous line.
 *
 * Lanes cap at 2 (only ::before/::after available); a third overlapping
 * conversation reuses lane 1. Pairs whose parent is absent (other page,
 * deleted) mark nothing — the 回复 @x label carries the context alone.
 */
export function markRails(list: Comment[]): RailRows {
  const idx = new Map<number, number>()
  list.forEach((c, i) => idx.set(c.id, i))
  const byId = new Map(list.map((c) => [c.id, c]))

  const rootOf = (id: number): number => {
    const seen = new Set<number>()
    let cur = id
    for (;;) {
      if (seen.has(cur)) return cur
      seen.add(cur)
      const c = byId.get(cur)
      if (!c || c.quoteId <= 0) return cur
      if (!byId.has(c.quoteId)) return cur
      cur = c.quoteId
    }
  };

  const laneOfRoot = new Map<number, number>()
  let nextLane = 0
  const laneFor = (rootId: number): number => {
    let lane = laneOfRoot.get(rootId)
    if (lane === undefined) {
      lane = Math.min(nextLane++, 1)
      laneOfRoot.set(rootId, lane)
    }
    return lane
  };

  const rows = new Map<number, { lanes: number[]; startLane: number | null }>()
  // Lanes follow root first-appearance order so the earliest conversation
  // owns the outer (avatar-center) lane.
  for (const c of list) laneFor(rootOf(c.id))
  const ensure = (id: number) => {
    let r = rows.get(id)
    if (!r) {
      r = { lanes: [], startLane: null }
      rows.set(id, r)
    }
    return r
  };
  for (const c of list) {
    if (c.quoteId <= 0) continue
    const p = idx.get(c.quoteId)
    const ci = idx.get(c.id)
    if (p === undefined || ci === undefined || p >= ci) continue
    const lane = laneFor(rootOf(c.id))
    const start = ensure(c.quoteId)
    if (start.startLane === null) start.startLane = lane
    if (!start.lanes.includes(lane) && start.lanes.length < 2) start.lanes.push(lane)
    // The lane runs through every row from the parent down to AND INCLUDING
    // the child (full height, diving behind the child avatar) — no fixed-size
    // stub, so no gap can open between segment end and avatar.
    for (let i = p + 1; i <= ci; i++) {
      const mid = ensure(list[i].id)
      if (!mid.lanes.includes(lane) && mid.lanes.length < 2) mid.lanes.push(lane)
    }
  }
  // Sorting puts the start lane first so it renders as ::before; a row with
  // two lanes must never draw the incoming (full-height) lane as a stub.
  for (const r of rows.values()) {
    if (r.startLane !== null) {
      r.lanes = [r.startLane, ...r.lanes.filter((l) => l !== r.startLane)]
    } else {
      r.lanes.sort((a, b) => a - b)
    }
  }
  return { rows }
}

/** Lane geometry per host (avatar size differs). */
export interface RailGeom {
  /** x of lane 0 (avatar center). */
  base: number
  /** x step per extra lane. */
  step: number
  /** y where a starting rail leaves the avatar (avatar center). */
  startTop: number
}

/** CSS classes for a row: '' | 'ct-rails' | 'ct-rails ct-rails-2'. */
export function railClass(rails: RailRows, id: number): string {
  const r = rails.rows.get(id)
  if (!r || r.lanes.length === 0) return ''
  return r.lanes.length > 1 ? 'ct-rails ct-rails-2' : 'ct-rails'
}

/** CSS vars (--rx1/--rt1/--rx2/--rt2) positioning the row's rails. */
export function railStyle(rails: RailRows, id: number, g: RailGeom): Record<string, string> {
  const r = rails.rows.get(id)
  if (!r || r.lanes.length === 0) return {}
  const x = (l: number) => `${g.base + g.step * l}px`
  const top = (l: number) => (r.startLane === l ? `${g.startTop}px` : '0px')
  const s: Record<string, string> = { '--rx1': x(r.lanes[0]), '--rt1': top(r.lanes[0]) }
  if (r.lanes.length > 1) {
    s['--rx2'] = x(r.lanes[1])
    s['--rt2'] = top(r.lanes[1])
  }
  return s
}
