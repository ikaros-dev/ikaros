# 开源参与指南

## 代码贡献步骤

## Fork 此仓库

点击 [Ikaros 仓库](https://github.com/ikaros-dev/ikaros)主页右上角的 `Fork` 按钮即可。

## 1.Clone 仓库到本地

```bash
git clone https://github.com/{YOUR_USERNAME}/ikaros --recursive
```

## 2.添加主仓库

添加主仓库方便未来同步主仓库最新的 commits 以及创建新的分支。

```bash
git remote add upstream https://github.com/ikaros-dev/ikaros.git
git fetch upstream
```

## 3.git module 初始化

需要初始化是由于主题使用的是git module关联的另一个仓库

```git
git submodule init
git submodule update
```

## 4.创建新的开发分支

我们需要从主仓库的主分支创建一个新的开发分支。

```bash
git checkout upstream/master
git checkout -b {YOU_BRANCH_NAME}
```

## 5.开发

在新创建的分支做一些开发

## 6.提交代码

```bash
git add .
git commit -s -m "Fix a bug or issue"
```

## 7.推送到你的github的fork库

在提交 Pull Request 之前，尽量保证当前分支和上游仓库的代码尽可能同步，这时需要我们手动操作。确保当前所处于新建的分支

示例：

```bash
git fetch upstream master
git merge upstream master
git push origin {YOU_BRANCH_NAME}
```

**注意**：merge 上游仓库可能会存在冲突，需要你手动解决并提交一个commit，再进行push

## 8.Pull Request

进入此阶段说明已经完成了代码的编写，测试和自测，并且准备好接受 Code Review。

### 9.创建 Pull Request

回到自己的仓库页面，选择 `New pull request` 按钮，创建 `Pull request` 到原仓库的 `master` 分支。
然后等待我们 Review 即可，如有 `Change Request`，再本地修改之后再次提交(commit)并推送(push)即可。

提交 Pull Request 的注意事项：

- 提交 Pull Request 请充分自测。
- 每个 Pull Request 尽量只解决一个 Issue，特殊情况除外。
- 应尽可能多的添加单元测试，其他测试（集成测试和 E2E 测试）可看情况添加。

### 10.更新 commits

Code Review 阶段可能需要 Pull Request 作者重新修改代码，请直接在当前分支 commit 并 push 即可，无需关闭并重新提交 Pull Request。示例：

```bash
git add .
git commit -s -m "Refactor some code according code review"
git push origin bug/xxx
```

同时，若已经进入 Code Review 阶段，请不要强制推送 commits 到当前分支。否则 Reviewers 需要从头开始 Code Review。

### 此文参考

- [开源项目Halo贡献指南](https://github.com/halo-dev/halo/blob/master/CONTRIBUTING.md)
