import re
import time
import requests
import pymysql
from bs4 import BeautifulSoup

# —— 数据库连接 —— #
conn = pymysql.connect(
    host='localhost',
    user='root',
    password='helloworld',
    db='TomatoMall',
    charset='utf8mb4'
)
cursor = conn.cursor()

headers = {
    'User-Agent': (
        'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 '
        '(KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36'
    )
}

def scrape_winxuan_to_db(url):
    """
    抓取文轩网 榜单分类 category 下 type=page_type 的第一页，
    并插入 product 表。
    """
    resp = requests.get(url, headers=headers, timeout=10)
    resp.raise_for_status()
    resp.encoding = resp.apparent_encoding
    soup = BeautifulSoup(resp.text, 'html.parser')

    # ←—— 这里改成 div.rank-item.border ——←
    items = soup.select('div.rank-item.border')
    for li in items:
        print("hello")  # 确认能跑到这里

        # —— 封面 —— #
        img = li.select_one('div.product-img a img')
        cover = img['src'].strip() if img and img.has_attr('src') else ''
        if cover.startswith('//'):
            cover = 'https:' + cover
        cover = cover[:500]

        # —— 书名 —— #
        a = li.select_one('div.product-desc p.book-title a')
        title = a.get_text(strip=True) if a else ''
        title = title[:50]

        # —— 作者 / 出版社 作为 detail —— #
        auth = li.select_one('div.product-desc p.author')
        detail = auth.get_text(" / ", strip=True)[:500] if auth else ''

        # —— 描述 —— #
        description = ''

        # —— 价格 —— #
        price_node = li.select_one('div.product-desc div.price span.saleprice')
        price = '0.00'
        if price_node:
            txt = price_node.get_text(strip=True)
            m = re.search(r'[\d]+\.[\d]+|[\d]+', txt)
            if m:
                price = m.group(0)

        # —— 评分 占位 —— #
        rate = 0.0

        # —— 写入数据库 —— #
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
        print(f'插入：{title} | {detail} | {price} | {cover}')

if __name__ == '__main__':
    url = 'https://www.winxuan.com/front/ranklist/category/'
    url_main = url + '1?type=default'
    url_true = url + '1?type=true'
    url_literature = url + '20000'
    try:
        scrape_winxuan_to_db(url_main)
        scrape_winxuan_to_db(url_true)
        scrape_winxuan_to_db(url_literature)
    finally:
        cursor.close()
        conn.close()
