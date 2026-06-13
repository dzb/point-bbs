import urllib.request, json

BASE = 'http://localhost:8082'

def post(path, data, token=None):
    headers = {'Content-Type': 'application/json; charset=utf-8'}
    if token: headers['Authorization'] = f'Bearer {token}'
    body = json.dumps(data, ensure_ascii=False).encode('utf-8')
    req = urllib.request.Request(BASE+path, data=body, headers=headers, method='POST')
    return json.loads(urllib.request.urlopen(req).read())

r = post('/api/auth/signup', {'nickname':'墨问','email':'mw@mw.com','password':'123'})
token = r['data']['token']
print('Signup OK, userId:', r['data']['userId'])

for i in range(1, 6):
    r = post('/api/topics', {'title': f'测试帖子 #{i}', 'content': '## 你好世界\n\n极简设计，留白美学\n\n> 让形式退后，让内容走在前列', 'type': 0}, token)
    print(f'Topic {i} id={r["data"]["id"]}')

r = post('/api/articles', {'title': '极简设计与留白美学', 'content': '## 留白的力量\n\n做设计久了就会明白**留白才是最好看的**。\n\n> 设计日课', 'contentType': 'markdown'}, token)
print(f'Article id={r["data"]["id"]}')

r = post('/api/articles/1/comments', {'content': '好帖，深有同感'}, token)
print('Article comment OK')
r = post('/api/topics/1/comments', {'content': '留白确实重要'}, token)
print('Topic comment OK')

post('/api/topics/1/like', {}, token)
post('/api/articles/1/like', {}, token)
print('Likes OK')

req = urllib.request.Request(BASE+'/api/topics')
items = json.loads(urllib.request.urlopen(req).read())['data']['items']
print(f'\n=== Verify: {len(items)} topics ===')
print(f'First: {items[0]["title"]}')
