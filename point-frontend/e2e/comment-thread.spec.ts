import { expect, test } from '@playwright/test'

interface Category {
  id: number
}

interface CommentRow {
  id: number
}

test('comment threads render contiguous conversation rails', async ({ page, request }) => {
  const login = await request.post('/api/auth/signin', {
    data: { loginName: 'moke', password: '123456' },
  })
  if (!login.ok()) {
    test.skip(true, 'dev demo user unavailable; run against the dev profile')
    return
  }
  const token = (await login.json()).data.token as string
  const auth = { Authorization: `Bearer ${token}` }

  const categoriesResponse = await request.get('/api/categories')
  expect(categoriesResponse.ok()).toBeTruthy()
  const categories = (await categoriesResponse.json()).data as Category[]
  if (!categories.length) {
    test.skip(true, 'no category available for the fixture')
    return
  }

  const stamp = Date.now()
  const topicResponse = await request.post('/api/topics', {
    headers: auth,
    data: {
      type: 0,
      categoryId: categories[0].id,
      title: `E2E thread ${stamp}`,
      content: 'E2E thread fixture',
      contentType: 'markdown',
    },
  })
  expect(topicResponse.status()).toBe(201)
  const topicId = (await topicResponse.json()).data.id as number

  const createdComments: number[] = []
  try {
    const createComment = async (content: string, quoteId = 0) => {
      const response = await request.post(`/api/topics/${topicId}/comments`, {
        headers: auth,
        data: { content, contentType: 'text', quoteId },
      })
      expect(response.status()).toBe(201)
      const comment = (await response.json()).data as CommentRow
      createdComments.push(comment.id)
      return comment.id
    }

    const root = await createComment('root')
    const child = await createComment('child', root)
    const grandchild = await createComment('grandchild', child)
    const siblingRoot = await createComment('sibling root')

    await page.goto(`/#/topics/${topicId}`)
    const rows = page.locator('[data-comment-id]')
    await expect(rows).toHaveCount(4)

    const metrics = await rows.evaluateAll((elements) =>
      elements.map((el) => {
        const row = el.getBoundingClientRect()
        const avatar = el.querySelector('.v-avatar, img, a')?.getBoundingClientRect()
        const before = getComputedStyle(el, '::before')
        const hasRail = before.content !== 'none' && before.content !== 'normal'
        return {
          id: Number((el as HTMLElement).dataset.commentId),
          hasRail,
          isHead: el.classList.contains('ct-rail-head'),
          hasSeparator: el.classList.contains('ct-sep'),
          lineX: hasRail ? Number.parseFloat(before.left) : null,
          rowX: row.x,
          avatarX: avatar ? avatar.x : null,
          paddingLeft: Number.parseFloat(getComputedStyle(el).paddingLeft),
        }
      }),
    )

    expect(metrics.map((row) => row.id)).toEqual([
      root,
      child,
      grandchild,
      siblingRoot,
    ])
    expect(metrics.every((row) => row.paddingLeft > 0)).toBe(true)

    const rails = metrics.filter((row) => row.hasRail)
    expect(rails.map((row) => row.id)).toEqual([root, child])
    expect(new Set(rails.map((row) => row.lineX)).size).toBe(1)
    expect(metrics.find((row) => row.id === root)?.isHead).toBe(true)
    expect(metrics.find((row) => row.id === siblingRoot)?.hasSeparator).toBe(true)

    for (const row of rails) {
      expect(row.lineX).not.toBeNull()
      expect(row.lineX!).toBeGreaterThanOrEqual(0)
      if (row.avatarX !== null) {
        // The rail lives in the gutter left of the avatar, never through it.
        expect(row.rowX + row.lineX!).toBeLessThanOrEqual(row.avatarX + 1)
      }
    }
  } finally {
    for (const commentId of createdComments.reverse()) {
      await request.post(`/api/topics/${topicId}/comments/${commentId}/delete`, {
        headers: auth,
      })
    }
    await request.post(`/api/topics/delete/${topicId}`, { headers: auth })
  }
})
