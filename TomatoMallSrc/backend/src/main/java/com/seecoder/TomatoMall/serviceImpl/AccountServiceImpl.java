package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.repository.AccountRepository;
import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.util.SecurityUtil;
import com.seecoder.TomatoMall.util.TokenUtil;
import com.seecoder.TomatoMall.vo.AccountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TokenUtil tokenUtil;

    @Autowired
    SecurityUtil securityUtil;


    @Override
    public Boolean register(AccountVO accountVO) {
        Account account = accountRepository.findByUsername(accountVO.getUsername());
        if (account != null) {
            throw TomatoMallException.usernameAlreadyExists();
        }
        Account newAccount = accountVO.toPO();
        newAccount.setCreateTime(new Date());
        accountRepository.save(newAccount);
        return true;
    }

    @Override
    public String login(String phone, String password) {
        Account account = accountRepository.findByUsernameAndPassword(phone, password);
        if (account == null) {
            throw TomatoMallException.loginFail();
        }
        return tokenUtil.getToken(account);
    }

    @Override
    public AccountVO getInformation() {
        Account account=securityUtil.getCurrentAccount();
        return account.toVO();
    }

    @Override
    public Boolean updateInformation(AccountVO accountVO) {
        Account account=securityUtil.getCurrentAccount();
        if (accountVO.getPassword()!=null){
            account.setPassword(accountVO.getPassword());
        }
        if (accountVO.getName()!=null){
            account.setName(accountVO.getName());
        }
        if (accountVO.getAddress()!=null){
            account.setAddress(accountVO.getAddress());
        }
        accountRepository.save(account);
        return true;
    }

}
