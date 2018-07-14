package server

import bean.BroadBean
import bean.Notebook
import bean.Transaction
import com.alibaba.fastjson.JSON
import io.ktor.application.call
import io.ktor.content.resources
import io.ktor.content.static
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import rsautil.RSAUtil
import rsautil.SignatureUtils
import utils.SHA256Utils
import webSocket.MyClient
import webSocket.MyServer

/**
 *author: 十年
 *signal: 爱生活爱陈奕迅
 *current time: 2018/7/4  13:14
 */

val notebook= Notebook()
//websocket服务端
val server= MyServer(70)
//客户端地址集合
val clientList = listOf(MyClient("ws://localhost:80"))

fun main(args: Array<String>) {
    //开启ws服务器
    Thread(server).start()
    Thread{
        Thread.sleep(12000)
        //客户端连接服务器
        clientList.forEach {
            it.connect()
        }
    }.start()

    notebook.loadFile()
    embeddedServer(Netty, 71) {
        routing {
            static("/static"){
                resources("static")
            }
            post("/addGenesis"){
                val genesis = call.receiveParameters().get("genesis")
                val result = notebook.addGenesis(genesis)
                if (result){
                    call.respondText { "添加首页成功" }
                }else{
                    call.respondText { "添加首页失败" }
                }
            }
            post("/addNote"){
                val note = call.receiveParameters().get("note")
                val result = notebook.addNote(note)
                if (result){
                    call.respondText { "添加内容成功" }
                }else{
                    call.respondText { "添加内容失败" }
                }
            }
            get("/listAll"){
                val result=notebook.findAll()
                println(result)
                call.respondText { result }
            }
            post("/modify"){
                val params = call.receiveParameters()
                val index = params.get("index")!!.toInt()
                val content = params.get("content")!!
                notebook.modify(index,content)
                call.respondText { "篡改数据成功" }
            }
            get("/check"){
                var sb=StringBuffer(0)
                val list=notebook.list
                list.forEachIndexed { index, block ->
                    val nonce=block.nonce
                    val currentHash = block.hash
                    val content = block.content
                    val savePrehash=block.prehash
                    val truePrehash=if (index == 0) "0000000000000000000000000000000000000000000000000000000000000000" else list.get(index - 1).hash
                    val checkhash = SHA256Utils.getSHA256StrJava("${nonce}${savePrehash}${content}")

                    if (currentHash.equals(checkhash)&&savePrehash.equals(truePrehash)){
                        sb.append("第${index}条数据正确\r\n")
                    }else{
                        sb.append("第${index}条数据错误\r\n")
                    }
                }
                call.respondText { sb.toString() }
            }
            post("/addTransaction"){
               val msg = call.receive<String>()
                println(msg)
                //将接收到的json字符串转换成transaction对象
                val transaction = JSON.parseObject(msg, Transaction::class.java)
                //1.把任务广播给其他节点
                val broadBean = BroadBean(1,1,transaction,null)
                //将数据转换为json数据格式
                val broadMsg = JSON.toJSONString(broadBean)
                server.broadcast(broadMsg)
                //2.解析接受到的字符串，自己进行校验
                val content=transaction.content
                val signed=transaction.signed
                val pubS = transaction.publicKey
                //将接收到的公钥字符串转换成公钥对象
                val publicKey = RSAUtil.createPublicKey(pubS)
                //信息验证
                val result = SignatureUtils.verifySignature(content, publicKey, signed)
                if (result){
                    println("数据正确")
                    notebook.addNote(msg)
                    //广播给其他节点进行更新
                    val broadBean = BroadBean(2,1,null,notebook.list)
                    //json字符串
                    val msg = JSON.toJSONString(broadBean)
                    server.broadcast(msg)
                }else{
                    println("数据错误")
                }
                call.respondText { "请求响应" }
            }
        }
    }.start(wait = true)
}