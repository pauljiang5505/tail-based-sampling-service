## 整体流程交互
本demo主要是用于演示数据交互过程，性能很一般，仅供各选手参考
![enter image description here](https://tianchi-public.oss-cn-hangzhou.aliyuncs.com/public/files/forum/158937741003137571589377409737.png)

请参考之前竞赛过的source code
https://github.com/INotWant/tail-based-sampling

解题思路
https://dingmingcheng.github.io/2020/10/05/%E9%A6%96%E5%B1%8A%E4%BA%91%E5%8E%9F%E7%94%9F%E7%BC%96%E7%A8%8B%E6%8C%91%E6%88%98%E8%B5%9B%E5%88%9D%E8%B5%9B%E8%AE%B0%E5%BD%95/

阿里云私有images registry 操作指南

1. 登录阿里云Docker Registry

$ sudo docker login --username=pauljiang4402 registry.cn-shanghai.aliyuncs.com
用于登录的用户名为阿里云账号全名，密码为开通服务时设置的密码。

您可以在访问凭证页面修改凭证密码。

2. 从Registry中拉取镜像

$ sudo docker pull registry.cn-shanghai.aliyuncs.com/hase_wpb_xa_competition/hase_ali_2020:[镜像版本号]
3. 将镜像推送到Registry

$ sudo docker login --username=pauljiang4402 registry.cn-shanghai.aliyuncs.com
$ sudo docker tag [ImageId] registry.cn-shanghai.aliyuncs.com/hase_wpb_xa_competition/hase_ali_2020:[镜像版本号]
$ sudo docker push registry.cn-shanghai.aliyuncs.com/hase_wpb_xa_competition/hase_ali_2020:[镜像版本号]
请根据实际镜像信息替换示例中的[ImageId]和[镜像版本号]参数。

4. 选择合适的镜像仓库地址

从ECS推送镜像时，可以选择使用镜像仓库内网地址。推送速度将得到提升并且将不会损耗您的公网流量。

如果您使用的机器位于VPC网络，请使用 registry-vpc.cn-shanghai.aliyuncs.com 作为Registry的域名登录。
5. 示例

使用"docker tag"命令重命名镜像，并将它通过专有网络地址推送至Registry。

$ sudo docker images
REPOSITORY                                                         TAG                 IMAGE ID            CREATED             VIRTUAL SIZE
registry.aliyuncs.com/acs/agent                                    0.7-dfb6816         37bb9c63c8b2        7 days ago          37.89 MB
$ sudo docker tag 37bb9c63c8b2 registry-vpc.cn-shanghai.aliyuncs.com/acs/agent:0.7-dfb6816
使用 "docker push" 命令将该镜像推送至远程。

$ sudo docker push registry-vpc.cn-shanghai.aliyuncs.com/acs/agent:0.7-dfb6816