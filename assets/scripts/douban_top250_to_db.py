import re
import time
import requests
import pymysql
from bs4 import BeautifulSoup

# —— 数据库连接 —— #
conn = pymysql.connect(
    host='localhost',
    user='root',
    password='123456',
    db='TomatoMall',
    charset='utf8mb4'
)
cursor = conn.cursor()

headers = {
    'User-Agent': (
        'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 '
        '(KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36'
    )
}

def parse_page(page):
    url = (
        'http://bang.dangdang.com/books/newhotsales/'
        '01.00.00.00.00.00-recent7-0-0-1-{}'
    ).format(page)
    resp = requests.get(url, headers=headers, timeout=10)
    resp.raise_for_status()
    resp.encoding = resp.apparent_encoding
    soup = BeautifulSoup(resp.text, 'lxml')

    items = soup.select('ul.bang_list.clearfix.bang_list_mode > li')
    for li in items:
        # —— 封面 —— #
        cover = ''
        # 优先尝试这几个路径
        for sel in ('a.pic img', 'div.pic img', 'img'):
            img = li.select_one(sel)
            if img and img.has_attr('data-original') and img['data-original'].strip():
                cover = img['data-original'].strip()
                break
            if img and img.get('src', '').strip():
                cover = img['src'].strip()
                break

        # 如果还是没取到，打印 li 片段供调试
        if not cover:
            print(">>> 无法提取封面，请检查下面的 HTML:")
            print(li.prettify())

        # 自动补全“//”开头的 URL
        if cover.startswith('//'):
            cover = 'https:' + cover

        # —— 书名 —— #
        a = li.select_one('div.name > a')
        title = ''
        if a:
            title = a.get('title', a.get_text(strip=True)).strip()
        title = title[:50]

        # —— 详情 —— #
        pub = li.select('div.publisher_info')
        detail = ''
        if pub:
            detail = ' '.join(p.get_text(strip=True) for p in pub)[:500]

        # —— 描述 —— #
        description = ''

        # —— 价格 —— #
        pn = li.select_one('span.price_n')
        price = '0.00'
        if pn:
            txt = pn.get_text(strip=True)
            m = re.search(r'[\d]+\.[\d]+|[\d]+', txt)
            if m:
                price = m.group(0)

        # —— 评分 占位 —— #
        rate = 0.0

        # —— 插入 —— #
        sql = """
        INSERT INTO product (cover, description, detail, price, rate, title)
        VALUES (%s, %s, %s, %s, %s, %s)
        """
        cursor.execute(sql, (
            cover,
            description,
            detail,
            price,
            rate,
            title
        ))
        conn.commit()
        print(f'插入：{title}  |  封面：{cover}')

if __name__ == '__main__':
    try:
        for p in range(1, 6):
            print(f'=== 第 {p} 页 ===')
            parse_page(p)
            time.sleep(1)
    finally:
        cursor.close()
        conn.close()
