package bean

/**
 *author: 十年
 *signal: 爱生活爱陈奕迅
 *current time: 2018/7/14  18:19
 */
//任务类型：1：任务广播 2：同步请求
class BroadBean(val type:Int,val node:Int,val transaction: Transaction?,val list:ArrayList<Block>?)