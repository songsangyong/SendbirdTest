package com.example.sendbirdtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirdtest.databinding.ActivityMainBinding
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannelParams
import com.sendbird.android.MessagePayloadFilter
import com.sendbird.android.PreviousMessageListQuery
import com.sendbird.android.ReplyTypeFilter
import com.sendbird.android.SendBird
import com.sendbird.android.SendBirdException
import com.sendbird.android.User

class MainActivity : AppCompatActivity(), OnClickListener {
    val mBinding by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }
    val TAG = javaClass.simpleName
    lateinit var currentUrl: String
    private var messageList: MutableList<MessageModel> = mutableListOf()
    private lateinit var adapter: MessageAdapter
    lateinit var chatMessageEt: EditText
    lateinit var mRecyclerView: RecyclerView

    private fun initAdpater() {
        adapter = MessageAdapter(this)
        mBinding.recyclerView.adapter = adapter
    }

    private fun init() {
        mBinding.button.setOnClickListener(this)
        mBinding.btnInvite.setOnClickListener(this)
        mBinding.btnExit.setOnClickListener(this)
        chatMessageEt = mBinding.chatMessageEt
        mRecyclerView = mBinding.recyclerView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        init()

        initAdpater()

        sendBirdInitAndConnect()
    }

    /**
     * 샌드버드 초기화및 커넥트
     */
    private fun sendBirdInitAndConnect(){
        val app = application as App
        app.initCallback { isSuccess ->
            if (isSuccess) {
                // 아래 USER_ID는 Sendbird 애플리케이션에 고유해야 합니다.
                //  채팅을 하려는 상대방아이디
                SendBird.connect(
                    getString(R.string.currentUser),
                    object : SendBird.ConnectHandler {
                        override fun onConnected(
                            user: User?,
                            sendBirdException: SendBirdException?
                        ) {
                            Log.d(TAG, "user:$user")
                            if (user != null) {
                                if (sendBirdException != null) {
                                    // Proceed in offline mode with the data stored in the local database.
                                    // Later, connection will be made automatically
                                    // and can be notified through the ConnectionHandler.onReconnectSucceeded().

                                    //로컬 데이터베이스에 저장된 데이터를 가지고 오프라인 모드로 진행합니다.
                                    //나중에 자동으로 연결됩니다
                                    //ConnectionHandler.onReconnectSucceeded()를 통해 알림을 받을 수 있습니다.
                                } else {
                                    // Proceed in online mode.
                                    //온라인 모드로 진행하세요.
                                    GroupChannel.createMyGroupChannelListQuery()
                                        .next { list, _ ->
                                            Log.d(TAG, "list:$list")
                                            if (list.isNullOrEmpty()) {
                                                //  채팅방생성
                                                createChannel()
                                            } else {
                                                //  이미 채팅방존재
                                                prevMsg(list)
                                            }
                                        }
                                }
                            } else {
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
    private fun prevMsg(list: MutableList<GroupChannel>) {
        currentUrl = list.first().url
        getGroupUrl(currentUrl) { groupChannel ->
            val listQuery = groupChannel.createPreviousMessageListQuery()
            //  보여지는 글갯수
            //  TODO 확인
            listQuery.limit = 50
            //  TODO 확인
            listQuery.setReverse(false)
            //  TODO 확인
            listQuery.replyTypeFilter = ReplyTypeFilter.ALL
            listQuery.messagePayloadFilter = MessagePayloadFilter.Builder()
                .setIncludeParentMessageInfo(true)
                .setIncludeThreadInfo(true)
                .build()

            listQuery.load(object : PreviousMessageListQuery.MessageListQueryResult {
                override fun onResult(
                    baseMessage: MutableList<BaseMessage>?,
                    e: SendBirdException?
                ) {
                    if (e == null) {
                        if (baseMessage != null) {
                            for (message in baseMessage) {
                                messageList.add(
                                    MessageModel(
                                        message.message,
                                        message.sender.userId,
                                        message.messageId,
                                        message.createdAt
                                    )
                                )
                            }
                        }

                        adapter.submitList(messageList)
                        adapter.notifyDataSetChanged()

                        //  하단으로 이동
                        mBinding.recyclerView.scrollToPosition(messageList.size - 1)
                    }
                }
            })
        }
    }

    /**
     * 방생성
     */
    private fun createChannel() {
        val userList = listOf<String>(getString(R.string.currentUser))
        val params = GroupChannelParams()
            .setPublic(false)
            .setEphemeral(false)
            .setDistinct(false)
            .setSuper(false)
            .addUserIds(userList)
            .setName(getString(R.string.currentUser))
        GroupChannel.createChannel(params) { groupChannel, _ ->
            currentUrl = groupChannel.url
        }
    }

    private fun getGroupUrl(url: String, afterLogin: (GroupChannel) -> Unit) {
        GroupChannel.getChannel(url) { groupChannel, e ->
            afterLogin(groupChannel)
        }
    }

    /**
     * 상대방이 입력한 메시지 받기
     */
    private fun msgRecevier() {
        SendBird.addChannelHandler(GROUP_HANDLER_ID, object : SendBird.ChannelHandler() {
            override fun onMessageReceived(p0: BaseChannel?, message: BaseMessage?) {
                val userId = message?.sender?.userId
                messageList.add(
                    MessageModel(
                        message = message?.message,
                        sender = message?.sender!!.userId,
                        messageId = message?.messageId,
                        createdAt = message?.createdAt
                    )
                )
                adapter.submitList(messageList)
                //  TODO 확인필요
                adapter.notifyItemInserted(messageList.size - 1)
                //  하단으로 이동
                mBinding.recyclerView.scrollToPosition(messageList.size - 1)
            }
        })
    }

    override fun onResume() {
        super.onResume()

        msgRecevier()
    }

    companion object {
        const val GROUP_HANDLER_ID = "GROUP_HANDLER_ID"
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            //  메시지입력
            R.id.button -> {
                //  1.방찾기
                GroupChannel.getChannel(currentUrl) { groupChannel, e ->
                    //  2.입장(메시지 보내기 위해서는 필수)
                    groupChannel.join {}
                    //  3.메시지 보내기
                    groupChannel.sendUserMessage(chatMessageEt?.text.toString()) { message, e ->
                        messageList.add(
                            MessageModel(
                                message = message?.message,
                                sender = message?.sender?.userId,
                                messageId = message?.messageId,
                                createdAt = message?.createdAt
                            )
                        )
                        adapter.submitList(messageList)
                        adapter.notifyItemInserted(messageList.size - 1)
                        //  하단으로 이동
                        mRecyclerView?.scrollToPosition(messageList.size - 1)
                        //  입력창초기화
                        chatMessageEt?.setText("")
                    }
                }
            }
            //  나가기
            R.id.btn_exit -> {
                getGroupUrl(currentUrl) { groupChannel ->
                    groupChannel.leave {
                        Toast.makeText(this@MainActivity, "탈퇴", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            //  초대
            R.id.btn_invite -> {
                val userIds = listOf(getString(R.string.opponentUser))
                getGroupUrl(currentUrl) { groupChannel ->
                    groupChannel.inviteWithUserIds(userIds) {}
                }
            }
        }
    }
}