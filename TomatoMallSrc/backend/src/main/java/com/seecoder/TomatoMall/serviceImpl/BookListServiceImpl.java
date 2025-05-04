package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.*;
import com.seecoder.TomatoMall.repository.*;
import com.seecoder.TomatoMall.service.BookListService;
import com.seecoder.TomatoMall.vo.BookListVO;
import com.seecoder.TomatoMall.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookListServiceImpl implements BookListService {

    @Autowired
    private BookListRepository listRepo;

    @Autowired
    private BookListItemRepository itemRepo;

    @Autowired
    private BookListFavoriteRepository favRepo;

    @Autowired
    private ProductRepository productRepo;      // 已有

    @Autowired
    private AccountRepository accountRepo;      // 已有
    @Autowired
    private BookListRepository bookListRepository;

    @Override
    @Transactional
    public BookListVO create(
            Integer creatorId,
            String title, String description, List<Integer> productIds
            ) {
        Account creator = accountRepo.getOne(creatorId);

        BookList list = new BookList();
        list.setTitle(title);
        list.setDescription(description);
        list.setCreator(creator);
        list = listRepo.save(list);

        int order = 0;
        for (Integer pid : productIds) {
            Product p = productRepo.getOne(pid);
            BookListItem item = new BookListItem();
            item.setBookList(list);
            item.setProduct(p);
            item.setSortOrder(order++);
            list.getItems().add(item);
            itemRepo.save(item);
        }
        return toVO(list);
    }

    @Override
    @Transactional
    public void delete(Integer creatorId, Integer bookListId) {
        BookList list = listRepo.findById(bookListId)
                .orElseThrow(() -> new TomatoMallException("书单不存在"));
        if (!list.getCreator().getId().equals(creatorId)) {
            throw new TomatoMallException("只能删除自己的书单");
        }
        listRepo.delete(list);        // 级联删除 items
        favRepo.deleteByBookListId(bookListId); // 如使用 ON DELETE CASCADE 可省
    }

    @Override
    public Page<BookListVO> listMyBookLists(Integer creatorId, int p, int sz) {
        return listRepo.findByCreatorId(
                        creatorId, PageRequest.of(p, sz, Sort.by("id").descending()))
                .map(this::toVO);
    }

    @Override
    public Page<BookListVO> listCollected(Integer accountId, int p, int sz) {
        Page<BookListFavorite> favPage =
                favRepo.findByAccountId(accountId, PageRequest.of(p, sz));
        List<BookList> lists = new ArrayList<>();
        List<BookListFavorite> item = favPage.getContent();
        item.stream().forEach(fav ->
                lists.add(
                        bookListRepository.findById(fav.getBookListId())
                                .orElseThrow(() -> new TomatoMallException("未找到该书单"))
                )
        );
        Map<Integer, BookListVO> map = lists.stream()
                .map(this::toVO)
                .collect(Collectors.toMap(BookListVO::getId, v -> v));
        return new PageImpl<>(
                favPage.getContent().stream()
                        .map(f -> map.get(f.getBookListId()))
                                .collect(Collectors.toList()),
                favPage.getPageable(),
                favPage.getTotalElements());
    }

    @Override
    @Transactional
    public void collect(Integer accountId, Integer bookListId) {
        if (favRepo.existsByAccountIdAndBookListId(accountId, bookListId))
            throw new TomatoMallException("已收藏");
        BookListFavorite favorite = new BookListFavorite();
        favorite.setAccountId(accountId);
        favorite.setBookListId(bookListId);
        favRepo.save(favorite);
        listRepo.incrementFavoriteCount(bookListId); // 写一个 update +1 的 @Modifying SQL
    }

    @Override
    @Transactional
    public void cancelCollect(Integer accountId, Integer bookListId) {
        favRepo.deleteByAccountIdAndBookListId(accountId, bookListId);
        listRepo.decrementFavoriteCount(bookListId);
    }

    /* === A. 新增 / 删除条目 ================================= */

    @Override
    @Transactional
    public void addItem(Integer creatorId, Integer listId, Integer productId) {
        BookList list = listRepo.findById(listId)
                .orElseThrow(() -> new TomatoMallException("书单不存在"));
        if (!list.getCreator().getId().equals(creatorId))
            throw new TomatoMallException("只能修改自己的书单");

        // 判重
        boolean exists = list.getItems().stream()
                .anyMatch(i -> i.getProduct().getId().equals(productId));
        if (exists) throw new TomatoMallException("该书已在书单中");

        Product p = productRepo.getOne(productId);
        BookListItem item = new BookListItem();
        item.setBookList(list);
        item.setProduct(p);
        item.setSortOrder(list.getItems().size());
        list.getItems().add(item);              // 维护双向
        // 因 cascade=ALL，无需显式保存 item
    }

    @Override
    @Transactional
    public void removeItem(Integer creatorId, Integer listId, Integer productId) {
        BookList list = listRepo.findById(listId)
                .orElseThrow(() -> new TomatoMallException("书单不存在"));
        if (!list.getCreator().getId().equals(creatorId))
            throw new TomatoMallException("只能修改自己的书单");

        boolean removed = list.getItems()
                .removeIf(i -> i.getProduct().getId().equals(productId));
        if (!removed) throw new TomatoMallException("书单不含该书");

        // orphanRemoval=true 时，item 会随父实体更新自动删除
    }

    /* === B. 查询 ============================================ */

    @Override
    public Page<BookListVO> listAll(int p, int sz) {
        return listRepo.findAll(
                        PageRequest.of(p, sz, Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toVO);
    }

    @Override
    public Page<BookListVO> listByUser(Integer userId, int p, int sz) {
        return listRepo.findByCreatorId(
                        userId, PageRequest.of(p, sz, Sort.by("id").descending()))
                .map(this::toVO);
    }




    /* ---------- helper ---------- */
    private BookListVO toVO(BookList list) {
        BookListVO vo = new BookListVO();
        vo.setId(list.getId());
        vo.setTitle(list.getTitle());
        vo.setDescription(list.getDescription());
        vo.setCreatorId(list.getCreator().getId());
        vo.setCreatorName(list.getCreator().getUsername());
        vo.setCreatorAvatar(list.getCreator().getAvatar());
        vo.setFavouriteCount(list.getFavoriteCount());
        vo.setCreationDate(list.getCreateTime());
        List<ProductVO> products = new ArrayList<>();
        list.getItems().stream()
                .sorted(Comparator.comparing(BookListItem::getSortOrder))
                        .forEach(listItem -> products.add(listItem.getProduct().toVO()));
        vo.setProducts(products);
        return vo;
    }
}
