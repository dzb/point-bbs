/**
 * Group consecutive image markdown lines into grid containers.
 * E.g. three consecutive ![](url) lines → <div class="img-grid cols-3">...
 */
export function groupConsecutiveImages(content: string): string {
  return content.replace(/(?:^|\n)(!\[[^\]]*\]\([^)]+\)\s*){2,}/gm, (match: string) => {
    const imgs = match.match(/!\[[^\]]*\]\([^)]+\)/g) || []
    const count = imgs.length
    const cols = count === 2 ? 'cols-2' : count === 3 ? 'cols-3' : 'cols-4'
    const tags = imgs.map((img: string) => {
      const src = img.match(/\(([^)]+)\)/)?.[1] || ''
      return `<img src="${src}" />`
    }).join('')
    return `\n<div class="img-grid ${cols}">${tags}</div>\n`
  })
}
