package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.vo.NoteVO;
import org.aspectj.weaver.ast.Not;

import java.util.List;

public interface NoteService
{
    NoteVO create(NoteVO noteVO);

    String delete(Integer id);

    String update(NoteVO noteVO);

    NoteVO getNote(Integer id);

    List<NoteVO> getNoteOfUser(Integer uid);

    List<NoteVO> getAll();

    String addViewCnt(Integer id);

    Boolean checkLiked(Integer id);

    String addLikeCnt(Integer id);

    String subLikeCnt(Integer id);

    Boolean checkPaid(Integer id);

    Boolean payForNote(Integer id);
}
