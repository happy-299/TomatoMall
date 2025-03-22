package com.seecoder.TomatoMall.service;
import com.seecoder.TomatoMall.vo.AccountVO;

public interface AccountService {
    Boolean register(AccountVO accountVO);

    String login(String username,String password);

    AccountVO getInformation();

    Boolean updateInformation(AccountVO accountVO);
}
