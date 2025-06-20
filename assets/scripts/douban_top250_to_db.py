import requests
from bs4 import BeautifulSoup
import pymysql
import time
import re

# —— 数据库连接 —— #
conn = pymysql.connect(
    host='localhost', user='root', password='helloworld',
    db='TomatoMall', charset='utf8mb4'
)
cursor = conn.cursor()

headers = {
    'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 '
                  '(KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36'
}

def parse_page(start):
    url = f'https://book.douban.com/top250?start={start}'
    resp = requests.get(url, headers=headers, timeout=10)
    resp.raise_for_status()
    resp.encoding = 'utf-8'
    soup = BeautifulSoup(resp.text, 'html.parser')

    for table in soup.select('div.indent table'):
        print("hello2")
        # 封面
        img = table.select_one('a.nbg img')
        cover = img['src'].strip()

        # 书名
        title = table.select_one('div.pl2 a')['title'].strip()

        # 简介（可能为空）
        q = table.select_one('span.inq')
        description = q.get_text(strip=True) if q else ''

        # 详情行：作者/出版社/年份/价格
        detail = table.select_one('p.pl').get_text(strip=True)

        # 提取价格：只保留数字和小数点
        parts = [p.strip() for p in detail.split('/') if p.strip()]
        price_text = parts[-1] if parts else ''
        m = re.search(r'[\d]+\.[\d]+|[\d]+', price_text)
        price = m.group(0) if m else '0.00'

        # 评分
        rate = table.select_one('span.rating_nums').get_text(strip=True)

        # 插入数据库
        sql = """
        INSERT INTO product (cover, description, detail, price, rate, title)
        VALUES (%s, %s, %s, %s, %s, %s)
        """
        cursor.execute(sql, (cover, description, detail, price, rate, title))
        conn.commit()
        print(f'插入：{title}')



if __name__ == '__main__':
    try:
        for start in range(0, 250, 25):
            print("hello")
            parse_page(start)
            time.sleep(1)   # 礼貌爬取，防封
    finally:
        cursor.close()
        conn.close()
