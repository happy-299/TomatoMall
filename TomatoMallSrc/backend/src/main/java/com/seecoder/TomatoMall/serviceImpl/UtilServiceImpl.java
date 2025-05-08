package com.seecoder.TomatoMall.serviceImpl;

import com.alipay.api.domain.SeatInfo;
import com.seecoder.TomatoMall.controller.UtilController;
import com.seecoder.TomatoMall.po.*;
import com.seecoder.TomatoMall.repository.*;
import com.seecoder.TomatoMall.service.UtilService;
import com.seecoder.TomatoMall.util.SecurityUtil;
import com.seecoder.TomatoMall.vo.SearchHistoryVO;
import com.seecoder.TomatoMall.vo.SearchResultVO;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.seecoder.TomatoMall.util.OssUtil;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UtilServiceImpl implements UtilService
{
    @Autowired
    OssUtil oss;

    private final static Boolean IS_TEST = true;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BookListRepository bookListRepository;
    @Autowired
    private SecurityUtil securityUtil;
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    @Autowired
    private AccountServiceImpl accountServiceImpl;
    @Autowired
    private NoteRepository noteRepository;

    @Override
    public String upload(MultipartFile rawFileContent)
    {
        return oss.upload(rawFileContent);
    }

    @Override
    public String getCurrentTime()
    {
        return oss.getCurrentTime();
    }

    @Override
    public void updateFromVO(Object target, Object vo)
    {
        BeanWrapper srcWrap = new BeanWrapperImpl(vo);
        BeanWrapper targetWrap = new BeanWrapperImpl(target);
        for (PropertyDescriptor pd : srcWrap.getPropertyDescriptors())
        {
            String propertyName = pd.getName();
            Object value = srcWrap.getPropertyValue(propertyName);
            if (value != null && !pd.getName().equals("class")
                    && !propertyName.equals("specifications")
                    && !propertyName.equals("id"))//仅更新非空字段
            {
                targetWrap.setPropertyValue(propertyName, value);
            }
        }
    }

    @Override
    @Transactional
    public SearchResultVO searchAll(String keyword)
    {
        //save keyword to SearchHistory
        saveSearchHistory(keyword);

        List<Account> accounts = accountRepository.searchAccounts(keyword);
        List<Product> products = productRepository.searchProducts(keyword);
        List<BookList> bookLists = bookListRepository.searchBookLists(keyword);
        List<Note> notes = noteRepository.searchNotes(keyword);

        return new SearchResultVO(accounts, products, bookLists, notes);
    }

    @Transactional
    public void saveSearchHistory(String keyword)
    {
        Account account = securityUtil.getCurrentAccount();
        List<SearchHistory> his = searchHistoryRepository.findAllByAccount_Id(account.getId());
        //重复的关键词不再保存
        for (SearchHistory h : his)
        {
            if (h.getKeyword().equals(keyword))
            {
                return;
            }
        }
        //保存数量上限
        final int SAVE_LIMIT = 10;
        if (his.size() > SAVE_LIMIT)
        {
            //删除最早的
            int id = his.get(0).getId();
            searchHistoryRepository.deleteById(id);
        }
        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setAccount(account);
        searchHistory.setKeyword(keyword);
        searchHistoryRepository.save(searchHistory);
    }

    @Override
    public List<SearchHistoryVO> getSearchHistory()
    {
        Account account = securityUtil.getCurrentAccount();
        return searchHistoryRepository.findAllByAccount_Id(account.getId())
                .stream().map(SearchHistory::toVO).collect(Collectors.toList());
    }


}
