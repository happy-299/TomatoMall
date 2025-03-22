# Commit and Branch Guideline

## branch related

本次branch规范如下,其他分支可以商讨决定:

- master: 每次lab/组会任务完全结束后再合并到master
- dev: 前后端合作分支，此分支可以看作是master分支的开发版本，该分支上是较为稳定的分支
- dev-backend: 后端协作分支
- dev-frontend: 前端协作分支


> workflow: 前后端分别在自己分支上解决任务后在dev分支上维护稳定版本,其他分支从dev分支上拉取最新内容进行开发

## 项目结构

参考.gitignore, 如果要更改.gitignore请单独一个commit说明

请写好gitignore不要upload奇怪的文件

## commit related

- feat/feture: {your commit here}
- fix: {your commit here}
- style: {your commit here}
- util: {your commit here}
- docs: {your commit here}

