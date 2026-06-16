import MarkdownIt from 'markdown-it'

/**
 * markdown-it plugin: wraps pure-image paragraphs in grid containers.
 *
 * Handles the case where 2+ consecutive image lines form a paragraph
 * whose only children are <img> tokens separated by hardbreaks/softbreaks.
 * This is the token-level (orthodox) path — no regex on HTML output.
 */
function imageGridPlugin(md: MarkdownIt) {
  // state is markdown-it's StateCore — internal type, not exported from public API
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  md.core.ruler.push('image_grid', (state: any) => {
    const tokens = state.tokens
    // state.Token is the markdown-it Token constructor — not directly typed in public API
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const T = (state as any).Token as new (type: string, tag: string, nesting: number) => any

    for (let i = 0; i < tokens.length; i++) {
      if (tokens[i].type !== 'paragraph_open') continue
      const inline = tokens[i + 1]
      const closeIdx = i + 2
      if (closeIdx >= tokens.length || tokens[closeIdx].type !== 'paragraph_close') continue
      if (inline.type !== 'inline' || !inline.children) continue

      const children = inline.children
      const imageCount = children.filter((c: any) => c.type === 'image').length
      if (imageCount < 2) continue
      const allImgOrBreak = children.every((c: any) =>
        c.type === 'image' || c.type === 'hardbreak' || c.type === 'softbreak'
      )
      if (!allImgOrBreak) continue

      const cols = imageCount === 2 ? 'cols-2' : imageCount === 3 ? 'cols-3' : 'cols-4'
      const open = new T('html_block', '', 0)
      open.content = `<div class="img-grid ${cols}">`
      tokens[i] = open
      const close = new T('html_block', '', 0)
      close.content = '</div>'
      tokens[closeIdx] = close
    }
  })
}

/** Shared markdown-it instance. */
export const md = new MarkdownIt({ breaks: true, linkify: true })
  .use(imageGridPlugin)

/**
 * Render markdown to HTML with image grid support.
 *
 * Two-path strategy:
 * 1. Content-level segmentation: consecutive ![](url) lines in the raw markdown
 *    are extracted and rendered as grid HTML directly. This handles the common
 *    case where breaks:true merges text + images into one paragraph (the plugin
 *    alone can't separate them).
 * 2. Token-level plugin: if segmentation doesn't apply (e.g. images separated
 *    by blank lines), the markdown-it plugin handles pure-image paragraphs.
 *
 * Both paths use html:false — no XSS vector.
 */
export function renderMarkdown(content: string): string {
  // Step 0: convert @username mentions to markdown links before processing
  content = content.replace(/@([\w一-鿿]{1,32})/g, '[@$1](/users/$1)')

  const parts: string[] = []
  let lastIndex = 0

  // Step 1: find groups of 2+ consecutive image lines in the raw markdown.
  // These form image blocks that should be gridded even when adjacent text
  // would otherwise merge with them due to breaks:true.
  const imgGroupRe = /(?:^|\n)(\s*!\[[^\]]*\]\([^)]+\)[\t ]*(?:\n|$)(?:\s*!\[[^\]]*\]\([^)]+\)[\t ]*(?:\n|$)){1,})/gm
  let match: RegExpExecArray | null

  while ((match = imgGroupRe.exec(content)) !== null) {
    // Render text before this image group via markdown-it (plugin handles pure-image paragraphs there)
    if (match.index > lastIndex) {
      parts.push(md.render(content.substring(lastIndex, match.index)))
    }
    // Build grid HTML for the image group directly
    const imgs = match[1].match(/!\[[^\]]*\]\([^)]+\)/g) || []
    const count = imgs.length
    const cols = count === 2 ? 'cols-2' : count === 3 ? 'cols-3' : 'cols-4'
    const tags = imgs.map(img => {
      const src = img.match(/\(([^)]+)\)/)?.[1] || ''
      const alt = img.match(/!\[([^\]]*)\]/)?.[1] || ''
      return `<img src="${src}" alt="${alt}" />`
    }).join('')
    parts.push(`<div class="img-grid ${cols}">${tags}</div>`)
    lastIndex = match.index + match[0].length
  }

  // Step 2: render remaining text via markdown-it (plugin handles any pure-image paragraphs)
  if (lastIndex < content.length) {
    parts.push(md.render(content.substring(lastIndex)))
  }

  return parts.join('')
}
