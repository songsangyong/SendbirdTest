package com.example.sendbirdtest

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirdtest.Extension.convertLoginToTime
import com.example.sendbirdtest.databinding.ItemMyMessageBinding
import com.example.sendbirdtest.databinding.ItemPartnerMessageBinding


class MessageAdapter(val context: Context) : ListAdapter<MessageModel, RecyclerView.ViewHolder>(diffUtil) {

    //  상대방
    inner class PartnerMessageViewHolder(private val binding:ItemPartnerMessageBinding): RecyclerView.ViewHolder(binding.root){
        lateinit var messageModel:MessageModel
        init {

        }

        fun bind(message: MessageModel){
            this.messageModel = message
            binding.dateTextView.text = message.createdAt?.convertLoginToTime()
            binding.messageTextView.text = message.message
        }
    }

    //  로그인 사용자
    inner class MyMessageViewHolder(private val binding: ItemMyMessageBinding):RecyclerView.ViewHolder(binding.root){
        lateinit var messageModel: MessageModel

        init {

        }

        fun bind(message: MessageModel){
            this.messageModel = message
            binding.dateTextView.text = message.createdAt?.convertLoginToTime()
            binding.messageTextView.text = message.message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val partnerMessageBinding = ItemPartnerMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val myMessageBinding = ItemMyMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return when(viewType){
            Constants.MY_MESSAGE -> MyMessageViewHolder(myMessageBinding)
            else -> PartnerMessageViewHolder(partnerMessageBinding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(currentList[position].sender){
            context.getString(R.string.currentUser) -> {
                (holder as MyMessageViewHolder).bind(currentList[position])
            }
            else -> {
                (holder as PartnerMessageViewHolder).bind(currentList[position])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = currentList[position]
        return if(message.sender == context.getString(R.string.currentUser)){
            Constants.MY_MESSAGE
        }else{
            Constants.PARTNER_MESSAGE
        }
    }

    companion object{
        val diffUtil = object : DiffUtil.ItemCallback<MessageModel>(){
            override fun areItemsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
                return oldItem.messageId == newItem.messageId
            }

            override fun areContentsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}