package webSocket

import bean.BroadBean
import com.alibaba.fastjson.JSON
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import rsautil.RSAUtil
import rsautil.SignatureUtils
import server.notebook
import server.server
import java.lang.Exception
import java.net.URI

/**
 *author: 十年
 *signal: 爱生活爱陈奕迅
 *current time: 2018/7/14  17:16
 */
class MyClient(path: String): WebSocketClient(URI(path)) {
    override fun onOpen(handshakedata: ServerHandshake?) {
        println("节点1客户端连接打开,连接服务器的端口:${uri.port}")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println("节点1客户端连接关闭")
    }

    override fun onMessage(message: String?) {
        val broadBean = JSON.parseObject(message, BroadBean::class.java)
        if(broadBean.type==1){
            //如果是任务广播，验证数据
            val transaction = broadBean.transaction!!
            //明文
            val content = transaction.content
            //公钥字符串
            val publicStr = transaction.publicKey
            //将公钥字符串还原为公钥对象
            val publicKey = RSAUtil.createPublicKey(publicStr)
            //签名之后的密文
            val signed = transaction.signed
            val result = SignatureUtils.verifySignature(content, publicKey, signed)
            if (result) {
                println("数据正确")
                //保存到本地节点里面
                notebook.addNote(content)
                //广播给其他节点进行更新
                val broadBean = BroadBean(2,1,null, notebook.list)
                //json字符串
                val msg = JSON.toJSONString(broadBean)
                server.broadcast(msg)
            } else {
                println("数据错误")
            }
        }else if (broadBean.type==2){
            val list = broadBean.list!!
            notebook.update(list)
        }
    }

    override fun onError(ex: Exception?) {
        println("节点1客户端连接失败")
    }
}