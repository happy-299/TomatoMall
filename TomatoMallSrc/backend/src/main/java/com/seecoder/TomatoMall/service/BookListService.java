package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.vo.BookListVO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BookListService {

    BookListVO create(Integer creatorId, String title, String description, String picture, List<Integer> productIds);

    void delete(Integer creatorId, Integer bookListId);        // 只能删自己的

    Page<BookListVO> listMyBookLists(Integer creatorId, int p, int sz);

    Page<BookListVO> listCollected(Integer accountId, int p, int sz);

    void collect(Integer accountId, Integer bookListId);

    void cancelCollect(Integer accountId, Integer bookListId);

    /** 向书单添加一本书 */
    void addItem(Integer creatorId, Integer listId, Integer productId);

    /** 从书单移除一本书 */
    void removeItem(Integer creatorId, Integer listId, Integer productId);

    /** 分页查询全部书单 */
    Page<BookListVO> listAll(int p, int sz);

    /** 分页查询某用户创建的书单 */
    Page<BookListVO> listByUser(Integer userId, int p, int sz);

    List<BookListVO> listTopBookLists(int top);

}