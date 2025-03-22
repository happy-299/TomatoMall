
# Commit and Branch Guideline

#### 该规范摘自HHLife项目，致谢happytech-web
原规范链接 => https://github.com/happytech-web/HHLife/blob/master/docs/meeting/meetingLog0.md

1.  [Git](#orge920318)
    1.  [commit 规范](#org1011bb1)
    2.  [branch](#orga7da68b)
    3.  [workflow](#orgb218367)
        1.  [提交自己的分支](#orgf9d710e)
        2.  [合并](#orge8b426a)



<a id="orge920318"></a>

# Git


<a id="org1011bb1"></a>

## commit 规范

请在以下几个 `type` 之间选择一个

    [type]: [description]
    type表示本次提交的类型：
    - feature
    - fix
    - docs
    - style


<a id="orga7da68b"></a>

## branch

请在以下几个 `type` 之间选择一个

-   master:始终是稳定状态，经过充分测试并且保持可部署状态
-   feature: 特性或开发分支
-   iss: 解决突发问题

&#x2026;


<a id="orgb218367"></a>

## workflow


<a id="orgf9d710e"></a>

### 提交自己的分支

git push -u origin `your-branch`
不要交到主分支了


<a id="orge8b426a"></a>

### 合并

每次开发任务只在一个branch下工作, 并自行在自己分支下commit，完成任务并且经过测试之后

-   首先在你自己的分支： `git checkout [yourbranch]`
-   拉取远程仓库的最新状态： `git fetch origin`
-   将最新的main分支合并到你的分支： `git merge orgin/master`
-   解决所有冲突，然后commit掉
-   确保你的代码可以正确运行，不影响主分支的稳定性后
-   切换到主分支： `git checkout master`
-   将你的分支合并到master:  `git merge --no-ff [branch] -m "commit message"`
-   如果还冲突，自行解决冲突
-   最终推送到远程仓库

