package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.NoteService;
import com.seecoder.TomatoMall.vo.NoteVO;
import com.seecoder.TomatoMall.vo.Response;
import org.aspectj.weaver.ast.Not;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController
{
    @Autowired
    NoteService noteService;

    //创建笔记，返回NoteVO或者失败信息
    //需要有title content price img
    @PostMapping()
    public Response<NoteVO> create(@RequestBody NoteVO noteVO)
    {
        return Response.buildSuccess(noteService.create(noteVO));
    }

    //删除笔记
    @DeleteMapping("/{id}")
    public Response<String> delete(@PathVariable Integer id)
    {
        return Response.buildSuccess(noteService.delete(id));
    }

    //更新笔记
    //需要提供id，应该只更改title content price img的内容,其他字段需要为null
    @PutMapping()
    public Response<String> update(@RequestBody NoteVO noteVO)
    {
        return Response.buildSuccess(noteService.update(noteVO));
    }

    //根据 Note ID 查询笔记
    @GetMapping("/{id}")
    public Response<NoteVO> getNote(@PathVariable Integer id)
    {
        return Response.buildSuccess(noteService.getNote(id));
    }

    //查询某一用户创建的所有笔记
    @GetMapping("/all/{uid}")
    public Response<List<NoteVO>> getNoteOfUser(@PathVariable Integer uid)
    {
        return Response.buildSuccess(noteService.getNoteOfUser(uid));
    }

    //查询所有笔记
    @GetMapping("/all")
    public Response<List<NoteVO>> getAll()
    {
        return Response.buildSuccess(noteService.getAll());
    }

    //浏览量+1
    @PostMapping("/view/{id}")
    public Response<String> addViewCnt(@PathVariable Integer id)
    {
        return Response.buildSuccess(noteService.addViewCnt(id));
    }

    //点赞+1
    @PostMapping("/like/{id}")
    public Response<String> addLikeCnt(@PathVariable Integer id)
    {
        return Response.buildSuccess(noteService.addLikeCnt(id));
    }

    //检查当前用户是否已经购买->true为已经购买
    @GetMapping("/pay/{id}")
    public Response<Boolean> checkPaid(@PathVariable Integer id)
    {
        return Response.buildSuccess(noteService.checkPaid(id));
    }

    //付费购买->true为购买成功
    @PostMapping("/pay/{id}")
    public Response<Boolean> payForNote(@PathVariable Integer id)
    {
        return Response.buildSuccess(noteService.payForNote(id));
    }
}
