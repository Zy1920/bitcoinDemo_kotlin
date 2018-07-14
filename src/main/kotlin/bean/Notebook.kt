package bean
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.TypeReference
import utils.SHA256Utils
import java.io.File

/**
 *author: 十年
 *signal: 爱生活爱陈奕迅
 *current time: 2018/7/4  10:36
 */
class Notebook {
    //保存账本的集合
    var list=ArrayList<Block>()
    //保存文件的地址
    val path="note.txt"

    //添加封面
    fun addGenesis(genesis:String?):Boolean{
        try {
            //1.没有信息才能添加
            require(list.size==0&&genesis!=null,{"封面已经存在了"})
            val prehash="0000000000000000000000000000000000000000000000000000000000000000"
            val nonce=mine(prehash,genesis!!)
            val hash=SHA256Utils.getSHA256StrJava("${nonce}${prehash}${genesis}")
            //2.添加到集合中
            var block=Block(list.size,genesis,hash,nonce,prehash)
            list.add(block)
            //3.保存数据到本地
            save2Disk()
            return true

        } catch (e: Exception) {
            return false
        }
    }

    //添加内容
    fun addNote(note:String?):Boolean{
        try {
            //1.没有信息才能添加
            require(list.size>0&&note!=null,{"封面已经存在了"})
            val prehash=list.get(list.size-1).hash
            val nonce=mine(prehash,note!!)
            val hash=SHA256Utils.getSHA256StrJava("${nonce}${prehash}${note}")
            //2.添加到集合中
            var block=Block(list.size,note!!,hash,nonce,prehash )
            list.add(block)
            //3.保存数据到本地
            save2Disk()

            return true

        } catch (e: Exception) {
            return false
        }
    }

    //显示数据
    fun findAll():String{
        val result=JSON.toJSONString(list)
        return result
    }

    //保存数据到本地
    fun save2Disk(){
        val list:String=findAll()
        val file=File(path)
        file.writeText(list)
    }

    //加载文件
    fun loadFile(){
        //读取文件
        val file=File(path)
        //看文件是否存在
        if (!file.exists()){
            return
        }
        var str=file.readText()
        //集合应该传递type
        val parseList = JSON.parseObject(str, object : TypeReference<ArrayList<Block>>() {})
        //清空原有的集合
        list.clear()
        //将最新的文件加载出来
        list.addAll(parseList)
    }

    //篡改数据
    fun modify(index: Int, content: String) {
        trueModify(index,content)
        if (index+1<list.size){
            (index+1..list.size-1).forEach {
                trueModify(it,list[it].content)
            }
        }
        save2Disk()
    }

    fun trueModify(index: Int,content: String){
        val block = list.get(index)
        if (index>0){
            block.prehash=list.get(index-1).hash
        }
        val nonce=mine(block.prehash,content)
        val hash=SHA256Utils.getSHA256StrJava("${nonce}${block.prehash}${content}")
        block.content=content
        block.hash=hash
        block.nonce=nonce


    }

    //工作量证明
    fun mine(prehash: String,content: String):Int{
        (0..Int.MAX_VALUE).forEach {
            val hash=SHA256Utils.getSHA256StrJava("${it}${prehash}${content}")
            if (hash.startsWith("000")){
                return it
            }
        }
        throw error("挖矿失败")
    }

    //更新节点
    fun update(list:ArrayList<Block>){
        if (list.size>this.list.size){
            this.list.clear()
            this.list.addAll(list)
        }
    }

}

class Block(var id: Int, var content: String, var hash: String,var nonce:Int,var prehash:String)