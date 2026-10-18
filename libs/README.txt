# 把原 mod（Singularity Iteration）的 jar 放到本目录，命名为 mio_icif.jar
#
# 不需要重新构建原 mod —— 直接用你整合包 mods/ 目录里已有的 jar 即可，例如：
#   cp "[SI] SingularityIteration-0.1.7.35.1-beta.jar" libs/mio_icif.jar
#
# 该 jar 只用于编译期读取类型（build.gradle 里是 compileOnly），
# 不会被打进本模组的产物，也不进版本库（已被 .gitignore 忽略）。
