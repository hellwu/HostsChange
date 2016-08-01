# HostsChange
Use shell change hosts in the code.\n
使用shell改变hosts,兼容busybox和非busybox
1.检查是否root
2获得/system的挂载路径
3.利用/system的挂载路径，重新挂载为可读写
4.echo textValue >> /system/etc/hosts
