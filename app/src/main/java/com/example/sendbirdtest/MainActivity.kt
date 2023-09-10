package com.example.sendbirdtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import com.example.sendbirdtest.databinding.ActivityMainBinding
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannel.GroupChannelJoinHandler
import com.sendbird.android.GroupChannelParams
import com.sendbird.android.SendBird
import com.sendbird.android.SendBirdException
import com.sendbird.android.User
import java.sql.SQLOutput

class MainActivity : AppCompatActivity() {
    val mBinding by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }
    val TAG = javaClass.simpleName
    lateinit var currentUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        mBinding.button.setOnClickListener {
            //  1.방찾기
            GroupChannel.getChannel(currentUrl){ groupChannel, e ->
                //  2.입장(메시지 보내기 위해서는 필수)
                groupChannel.join{}
                //  3.메시지 보내기
                groupChannel.sendUserMessage(mBinding.chatMessageEt.text.toString()){ msg, e ->
                    mBinding.chatTv.append("${msg.message}\n")
                }
            }
        }

        //  초대
        mBinding.btnInvite.setOnClickListener {
            val userIds = listOf(getString(R.string.opponentUser))
            getGroupUrl(currentUrl){groupChannel ->
                groupChannel.inviteWithUserIds(userIds){}
            }
        }

        //  나가기
        mBinding.btnExit.setOnClickListener { 
            getGroupUrl(currentUrl){ groupChannel ->  
                groupChannel.leave{
                    Toast.makeText(this, "탈퇴", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val app = application as App
        app.initCallback { isSuccess ->
            if(isSuccess){
                // 아래 USER_ID는 Sendbird 애플리케이션에 고유해야 합니다.
                //  채팅을 하려는 상대방아이디
                SendBird.connect(getString(R.string.currentUser), object:SendBird.ConnectHandler{
                    override fun onConnected(user: User?, sendBirdException: SendBirdException?) {
                        Log.d(TAG, "user:$user")
                        if(user != null){
                            if(sendBirdException != null){
                                // Proceed in offline mode with the data stored in the local database.
                                // Later, connection will be made automatically
                                // and can be notified through the ConnectionHandler.onReconnectSucceeded().

                                //로컬 데이터베이스에 저장된 데이터를 가지고 오프라인 모드로 진행합니다.
                                //나중에 자동으로 연결됩니다
                                //ConnectionHandler.onReconnectSucceeded()를 통해 알림을 받을 수 있습니다.
                            }else{
                                // Proceed in online mode.
                                //온라인 모드로 진행하세요.
                                GroupChannel.createMyGroupChannelListQuery().next{ list, _ ->
                                    Log.d(TAG, "list:$list")
                                    if(list.isNullOrEmpty()){
                                        //  채팅방생성
                                        createChannel()
                                    }else{
                                        //  이미 채팅방존재
                                        currentUrl = list.first().url
                                        getGroupUrl(currentUrl){ groupChannel ->
                                            prevMsg()
                                        }
                                    }
                                }
                            }
                        }else{
                            // Handle error.
                        }
                    }
                })
            }
        }
    }

    /**
     * 이전메시지 얻기
     */
    private fun prevMsg(){
        //  주고받은 메시지정보
        GroupChannel.createMyGroupChannelListQuery().next{ list, e ->
            Log.d(TAG, "list:$list")
            list.forEach {
                mBinding.chatTv.append("${it.lastMessage.message}\n")
            }
        }
    }

    /**
     * 방생성
     */
    private fun createChannel(){
        val userList = listOf<String>(getString(R.string.currentUser))
        val params = GroupChannelParams()
            .setPublic(false)
            .setEphemeral(false)
            .setDistinct(false)
            .setSuper(false)
            .addUserIds(userList)
            .setName(getString(R.string.currentUser))
        GroupChannel.createChannel(params){ groupChannel, _ ->
            currentUrl = groupChannel.url
        }
    }

    private fun getGroupUrl(url:String, afterLogin:(GroupChannel) -> Unit){
        GroupChannel.getChannel(url){ groupChannel, e ->
            afterLogin(groupChannel)
        }
    }

    override fun onResume() {
        super.onResume()

        //  상대방이 입력한 메시지 받기
        SendBird.addChannelHandler(GROUP_HANDLER_ID, object:SendBird.ChannelHandler(){
            override fun onMessageReceived(p0: BaseChannel?, message: BaseMessage?) {
                mBinding.chatTv.append("${message!!.message}\n")
            }
        })
    }

    companion object {
        const val GROUP_HANDLER_ID = "GROUP_HANDLER_ID"
    }
}