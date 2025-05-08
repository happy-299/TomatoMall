package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.po.Note;
import com.seecoder.TomatoMall.repository.AccountRepository;
import com.seecoder.TomatoMall.repository.NoteRepository;
import com.seecoder.TomatoMall.service.NoteService;
import com.seecoder.TomatoMall.util.SecurityUtil;
import com.seecoder.TomatoMall.vo.NoteVO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Getter
@Setter
public class NoteServiceImpl implements NoteService
{

    @Autowired
    SecurityUtil securityUtil;
    @Autowired
    NoteRepository noteRepository;
    @Autowired
    UtilServiceImpl utilServiceImpl;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    AccountServiceImpl accountServiceImpl;
    @Autowired
    private OrderServiceImpl orderServiceImpl;

    @Override
    @Transactional
    public NoteVO create(NoteVO noteVO)
    {
        if (noteVO.getTitle() == null || noteVO.getContent() == null
                || noteVO.getPrice() == null)
        {
            throw TomatoMallException.noteCreateError();
        }
        if (noteVO.getPrice() < 0)
        {
            throw TomatoMallException.tomatoPriceError();
        }

        Note note = new Note();
        Account creator = securityUtil.getCurrentAccount();
        note.setCreator(creator);
        note.setTitle(noteVO.getTitle());
        note.setContent(noteVO.getContent());
        note.setPrice(noteVO.getPrice());
        note.setLikeCnt(0);
        note.setViewCnt(0);
        note.setCreateTime(LocalDateTime.now());

        List<Account> payers = new ArrayList<>();
        payers.add(creator);//作者默认付费，可以访问自己的内容
        note.setPayers(payers);

        note.setImg(noteVO.getImg());
        note.setLikers(new ArrayList<>());
        return noteRepository.save(note).toVO();
    }

    @Override
    @Transactional
    public String delete(Integer id)
    {
        noteRepository.findById(id)
                .orElseThrow(TomatoMallException::noteNotFound);
        noteRepository.deleteById(id);
        return "删除成功";
    }

    @Override
    @Transactional
    public String update(NoteVO noteVO)
    {
        if (noteVO.getId() == null)
        {
            throw TomatoMallException.noteNotFound();
        }
        Note note = noteRepository.findById(noteVO.getId())
                .orElseThrow(TomatoMallException::noteNotFound);
        //检验，不应该包含除了id title,content,price img以外的其他字段
        if (noteVO.getCreateTime() != null || noteVO.getLikeCnt() != null
//                || noteVO.getPayersId() != null
                || noteVO.getCreatorId() != null)
        {
            throw TomatoMallException.noteUpdateError();
        }
        if (noteVO.getPrice() < 0)
        {
            throw TomatoMallException.tomatoPriceError();
        }
        utilServiceImpl.updateFromVO(note, noteVO);//更新剩余非空字段
        return "更新成功";
    }

    @Override
    public NoteVO getNote(Integer id)
    {
        return noteRepository.findById(id)
                .orElseThrow(TomatoMallException::noteNotFound)
                .toVO();
    }

    @Override
    public List<NoteVO> getNoteOfUser(Integer uid)
    {
        Account acc = accountRepository.findById(uid)
                .orElseThrow(TomatoMallException::userNotFound);
        return noteRepository.findAllByCreator(acc)
                .stream().map(Note::toVO).collect(Collectors.toList());
    }

    @Override
    public List<NoteVO> getAll()
    {
        return noteRepository.findAll()
                .stream().map(Note::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String addViewCnt(Integer id)
    {
        Note note = noteRepository.findById(id)
                .orElseThrow(TomatoMallException::noteNotFound);
        note.setViewCnt(note.getViewCnt() + 1);
        noteRepository.save(note);
        return "浏览量成功+1";
    }

    @Override
    @Transactional
    public Boolean checkLiked(Integer id)
    {
        Account acc = securityUtil.getCurrentAccount();
        Note note = noteRepository.findById(id)
                .orElseThrow(TomatoMallException::noteNotFound);
        List<Account> likers = note.getLikers();
        for (Account a : likers)
        {
            if (a.getId().equals(acc.getId()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public String addLikeCnt(Integer id)
    {
        if (checkLiked(id))
        {
            throw TomatoMallException.noteLiked();
        }

        Note note = noteRepository.findById(id)
                .orElseThrow(TomatoMallException::noteNotFound);
        note.setLikeCnt(note.getLikeCnt() + 1);//点赞数+1

        Account acc = securityUtil.getCurrentAccount();
        note.getLikers().add(acc);//点赞列表增加

//        noteRepository.save(note);
        return "点赞成功+1";
    }

    @Override
    public Boolean checkPaid(Integer id)
    {
        Account acc = securityUtil.getCurrentAccount();
        Note note = noteRepository.findById(id)
                .orElseThrow(TomatoMallException::noteNotFound);
        List<Account> payers = note.getPayers();
        for (Account a : payers)
        {
            if (a.getId().equals(acc.getId()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public Boolean payForNote(Integer id)
    {
        if (checkPaid(id))
        {
            throw TomatoMallException.notePaid();
        }

        Note note = noteRepository.findById(id)
                .orElseThrow(TomatoMallException::noteNotFound);
        orderServiceImpl.payTomato(note.getPrice());

        //纳入已购买名单
        Account acc = securityUtil.getCurrentAccount();
        note.getPayers().add(acc);

        return true;
    }
}
