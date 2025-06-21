package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.service.BookListService;
import com.seecoder.TomatoMall.vo.BookListVO;
import com.seecoder.TomatoMall.vo.PageResponseVO;
import com.seecoder.TomatoMall.vo.Response;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booklist")
public class BookListController {
    @Autowired
    private BookListService bookListService;

    @Autowired
    private AccountService accountService;

    @Data
    public static class BookListCreateDTO {
        private String title;
        private String description;
        private String picture;
        private List<Integer> productIds;         // Product.id 列表
    }

    @Data
    public static class BookListCollectDTO {
        private Integer bookListId;
    }

    @Data
    public static class BookListItemModifyDTO {
        private Integer bookListId;   // 目标书单
        private Integer productId;    // 要添加/删除的图书
    }

    /** 创建书单 */
    @PostMapping
    public Response<BookListVO> create(@RequestBody BookListCreateDTO dto) {
        Integer uid = accountService.getInformation().getId();
        return Response.buildSuccess(bookListService.create(
                uid, dto.getTitle(), dto.getDescription(), dto.getPicture(), dto.getProductIds()
                ));
    }

    /** 删除书单（只能删自己创建的） */
    @DeleteMapping("/{id}")
    public Response<String> delete(@PathVariable Integer id) {
        Integer uid = accountService.getInformation().getId();
        bookListService.delete(uid, id);
        return Response.buildSuccess("删除成功");
    }

    /** 我创建的书单 */
    @GetMapping("/mine")
    public Response<PageResponseVO<BookListVO>> myLists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer uid = accountService.getInformation().getId();
        return Response.buildSuccess(
                new PageResponseVO<>(bookListService.listMyBookLists(uid, page, size)));
    }

    /** 收藏书单 */
    @PostMapping("/collect")
    public Response<String> collect(@RequestBody BookListCollectDTO dto) {
        Integer uid = accountService.getInformation().getId();
        bookListService.collect(uid, dto.getBookListId());
        return Response.buildSuccess("已收藏");
    }

    /** 取消收藏 */
    @DeleteMapping("/collect")
    public Response<String> cancelCollect(@RequestBody BookListCollectDTO dto) {
        Integer uid = accountService.getInformation().getId();
        bookListService.cancelCollect(uid, dto.getBookListId());
        return Response.buildSuccess("已取消");
    }

    /** 我收藏的书单 */
    @GetMapping("/favourites")
    public Response<PageResponseVO<BookListVO>> favourites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer uid = accountService.getInformation().getId();
        return Response.buildSuccess(
                new PageResponseVO<>(bookListService.listCollected(uid, page, size)));
    }

    /* ---------- A. 修改书单条目 ---------- */

    /** 添加一本书 */
    @PostMapping("/{listId}/item/{productId}")
    public Response<String> addItem(@PathVariable Integer listId,
                                    @PathVariable Integer productId) {
        Integer uid = accountService.getInformation().getId();
        bookListService.addItem(uid, listId, productId);
        return Response.buildSuccess("已添加");
    }

    /** 移除一本书 */
    @DeleteMapping("/{listId}/item/{productId}")
    public Response<String> removeItem(@PathVariable Integer listId,
                                       @PathVariable Integer productId) {
        Integer uid = accountService.getInformation().getId();
        bookListService.removeItem(uid, listId, productId);
        return Response.buildSuccess("已移除");
    }

    /* ---------- B. 查询 ---------- */

    /** 全站书单（分页） */
    @GetMapping("/all")
    public Response<PageResponseVO<BookListVO>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Response.buildSuccess(
                new PageResponseVO<>(bookListService.listAll(page, size)));
    }

    @GetMapping("/top")
    public Response<List<BookListVO>> top(
            @RequestParam(name = "n", defaultValue = "4") int n) {
        List<BookListVO> tops = bookListService.listTopBookLists(n);
        return Response.buildSuccess(tops);
    }


    /** 某用户的书单 */
    @GetMapping("/user/{uid}")
    public Response<PageResponseVO<BookListVO>> listByUser(
            @PathVariable Integer uid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Response.buildSuccess(
                new PageResponseVO<>(bookListService.listByUser(uid, page, size)));
    }

}
