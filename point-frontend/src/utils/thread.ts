import type { Comment } from '@/types'

/** X-style conversation threading result for one comment page. */
export interface ThreadLayout {
  /**
   * Comments in X order: each root's subtree is contiguous, chronological
   * within it. Children follow their parent directly, which is what makes
   * one continuous line per conversation possible.
   */
  items: Comment[]
  /** Rows a conversation line passes through. */
  railRows: Set<number>
  /** Rows where a line starts (drawn from the avatar centre down). */
  railHeads: Set<number>
  /**
   * Rows that open a conversation, i.e. where a horizontal separator is
   * drawn. X separates conversations, not individual replies — members of
   * one group run together with only the vertical line between them.
   * The first row of the whole list is excluded (nothing above it).
   */
  groupStarts: Set<number>
}

/**
 * Reorder comments depth-first and mark the rows each conversation line
 * runs through.
 *
 * X's two rules, both visible in the thread view:
 * 1. A reply sits directly below its parent, and one line runs from the
 *    root's avatar down through the subtree. Because subtrees are
 *    contiguous, separate conversations occupy disjoint row ranges and
 *    never need a second lane — that is why sujian's segment and moke's
 *    segment read as two separate lines instead of one merged line.
 * 2. The last reply of a conversation carries no line (nitter's
 *    `thread-last` rule), so the rail stops at that avatar rather than
 *    trailing into empty space below it.
 * 3. Replies inside one conversation have no horizontal separator — the
 *    separator belongs to the group boundary, so a group reads as one
 *    block. `groupStarts` marks those boundaries.
 *
 * Orphans (parent on another page, or deleted) stay roots and get no line;
 * their 回复 @x label carries the context.
 */
export function layoutThread(list: Comment[]): ThreadLayout {
  const byId = new Map(list.map((c) => [c.id, c]))
  const children = new Map<number, Comment[]>()
  const roots: Comment[] = []

  for (const c of list) {
    if (c.quoteId > 0 && c.quoteId !== c.id && byId.has(c.quoteId)) {
      const kids = children.get(c.quoteId)
      if (kids) kids.push(c)
      else children.set(c.quoteId, [c])
    } else {
      roots.push(c)
    }
  }

  const items: Comment[] = []
  const railRows = new Set<number>()
  const railHeads = new Set<number>()
  const emitted = new Set<number>()

  // Depth-first; a node already emitted is skipped, so malformed cyclic
  // data degrades to the first-seen order instead of recursing forever.
  // Returns the last id of the subtree rooted here.
  const walk = (c: Comment, inConversation: boolean): { last: number; hasChild: boolean } => {
    emitted.add(c.id)
    items.push(c)
    if (inConversation) railRows.add(c.id)
    let last = c.id
    let hasChild = false
    for (const kid of children.get(c.id) ?? []) {
      if (emitted.has(kid.id)) continue
      hasChild = true
      last = walk(kid, true).last
    }
    return { last, hasChild }
  }

  for (const c of list) {
    if (!emitted.has(c.id)) roots.push(c)
  }
  const groupStarts = new Set<number>()
  for (const root of roots) {
    if (emitted.has(root.id)) continue
    const { last, hasChild } = walk(root, false)
    groupStarts.add(root.id)
    if (hasChild) {
      railHeads.add(root.id)
      railRows.add(root.id)
      // X's rule: the final reply of a conversation carries no line — the
      // rail stops at that last avatar instead of trailing below it.
      railRows.delete(last)
    }
  }
  // Nothing precedes the first row, so it carries no separator.
  const first = items[0]
  if (first) groupStarts.delete(first.id)

  return { items, railRows, railHeads, groupStarts }
}

/**
 * Line geometry per host.
 *
 * X runs the line in the empty gutter to the LEFT of the avatar, not
 * through it — the avatar hangs off the line like a note on a string. The
 * row reserves `gutter` of left padding for this, and the line is drawn
 * at its centre.
 */
export interface RailGeom {
  /** Left padding every row reserves for the gutter (px). */
  gutter: number
  /** Line width (px). */
  width: number
  /** y where a head row's line starts, level with the avatar centre. */
  headTop: number
}

/** Row class for the conversation line, '' when the row has none. */
export function railClass(layout: ThreadLayout, id: number): string {
  if (!layout.railRows.has(id)) return ''
  return layout.railHeads.has(id) ? 'ct-rail ct-rail-head' : 'ct-rail'
}

/** True when the row opens a conversation and so carries a separator. */
export function hasSeparator(layout: ThreadLayout, id: number): boolean {
  return layout.groupStarts.has(id)
}

/** CSS vars: gutter for every row, line position for rows that have one. */
export function railStyle(layout: ThreadLayout, id: number, g: RailGeom): Record<string, string> {
  const s: Record<string, string> = { '--gutter': `${g.gutter}px` }
  if (!layout.railRows.has(id)) return s
  const x = Math.round((g.gutter - g.width) / 2)
  s['--rx'] = `${x}px`
  s['--rt'] = layout.railHeads.has(id) ? `${g.headTop}px` : '0px'
  return s
}
