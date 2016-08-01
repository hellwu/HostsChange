# HostsChange
Use shell change hosts in the code.<br/>
使用shell改变hosts,兼容busybox和非busybox<br/>
1.检查是否root<br/>
2获得/system的挂载路径<br/>
3.利用/system的挂载路径，重新挂载为可读写<br/>
4.echo textValue >> /system/etc/hosts<br/>
