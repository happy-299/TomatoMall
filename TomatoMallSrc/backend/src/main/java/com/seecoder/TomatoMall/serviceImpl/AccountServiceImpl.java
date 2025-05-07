package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.po.UserFollow;
import com.seecoder.TomatoMall.repository.AccountRepository;
import com.seecoder.TomatoMall.repository.UserFollowRepository;
import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.util.SecurityUtil;
import com.seecoder.TomatoMall.util.TokenUtil;
import com.seecoder.TomatoMall.vo.AccountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService
{

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TokenUtil tokenUtil;

    @Autowired
    SecurityUtil securityUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserFollowRepository followRepository;


    @Override
    public Boolean register(AccountVO accountVO)
    {
        Account account = accountRepository.findByUsername(accountVO.getUsername());
        if (account != null)
        {
            throw TomatoMallException.usernameAlreadyExists();
        }
        // encode the password
        String rawPassword = accountVO.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        accountVO.setPassword(encodedPassword);

        Account newAccount = accountVO.toPO();
        newAccount.setCreateTime(new Date());
        newAccount.setTomato(100);//新用户送100个番茄
        accountRepository.save(newAccount);
        return true;
    }

    @Override
    public String login(String username, String rawPassword)
    {
        Account account = accountRepository.findByUsername(username);
        if (account != null
                && passwordEncoder.matches(rawPassword, account.getPassword()))
        {
            return tokenUtil.getToken(account);
        }
        throw TomatoMallException.loginFail();
    }

    @Override
    public AccountVO getInformation()
    {
        Account account = securityUtil.getCurrentAccount();
        return account.toVO();
    }

    @Override
    public Boolean updateInformation(AccountVO accountVO)
    {
        Account account = securityUtil.getCurrentAccount();
        if (accountVO.getPassword() != null)
        {
            String rawPassword = accountVO.getPassword();
            String encodedPassword = passwordEncoder.encode(rawPassword);
            account.setPassword(encodedPassword);
        }
        if (accountVO.getName() != null)
        {
            account.setName(accountVO.getName());
        }
        if (accountVO.getLocation() != null)
        {
            account.setLocation(accountVO.getLocation());
        }
        if (accountVO.getAvatar() != null)
        {
            account.setAvatar(accountVO.getAvatar());
        }
        if (accountVO.getEmail() != null)
        {
            account.setEmail(accountVO.getEmail());
        }
        if (accountVO.getTelephone() != null)
        {
            account.setTelephone(accountVO.getTelephone());
        }
        if (accountVO.getRole() != null)
        {
            account.setRole(accountVO.getRole());
        }
        //todo:may wrong--是否更新后来添加的字段？
        accountRepository.save(account);
        return true;
    }

    /*
     * ===============================
     *      follow related
     * ===============================
     */
    // 关注用户
    @Override
    @Transactional
    public void followUser(Integer followerId, Integer followedId)
    {
        if (followerId.equals(followedId))
        {
            throw TomatoMallException.selfFollowError();
        }

        if (!followRepository.existsByFollowerIdAndFollowedId(followerId, followedId))
        {
            UserFollow uf = new UserFollow();
            uf.setFollowerId(followerId);
            uf.setFollowedId(followedId);
            uf.setCreateTime(LocalDateTime.now());
            followRepository.save(uf);

            // 更新计数
            accountRepository.incrementFollowingCount(followerId);
            accountRepository.incrementFollowerCount(followedId);
        }
    }

    // 取消关注
    @Override
    @Transactional
    public void unfollowUser(Integer followerId, Integer followedId)
    {
        if (followRepository.existsByFollowerIdAndFollowedId(followerId, followedId))
        {
            followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);

            // 更新计数
            accountRepository.decrementFollowingCount(followerId);
            accountRepository.decrementFollowerCount(followedId);
        }
    }

    // 获取关注列表
    @Override
    public List<AccountVO> getFollowingList(Integer userId)
    {
        List<Integer> ids = followRepository.findFollowingIds(userId);
        List<Account> users = accountRepository.findAllById(ids);
        return users.stream().map(u -> u.toVO()).collect(Collectors.toList());
    }


    // 获取粉丝列表
    @Override
    public List<AccountVO> getFollowerList(Integer userId)
    {
        List<Integer> ids = followRepository.findFollowerIds(userId);
        List<Account> users = accountRepository.findAllById(ids);
        return users.stream().map(u -> u.toVO()).collect(Collectors.toList());
    }
}
