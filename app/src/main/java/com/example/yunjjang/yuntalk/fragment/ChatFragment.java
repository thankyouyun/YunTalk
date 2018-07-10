package com.example.yunjjang.yuntalk.fragment;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.yunjjang.yuntalk.R;
import com.example.yunjjang.yuntalk.chat.MessageActivity;
import com.example.yunjjang.yuntalk.model.ChatModel;
import com.example.yunjjang.yuntalk.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class ChatFragment extends Fragment {


    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm");
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_chat,container,false);
        RecyclerView recyclerView = view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(new ChatRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));


         return view;


    }


    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        private List<ChatModel> chatModels= new ArrayList<>();

        private String uid;
        private ArrayList<String> destinationUsers = new ArrayList<>();
        public ChatRecyclerViewAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    chatModels.clear();
                    for(DataSnapshot item : dataSnapshot.getChildren()){

                        chatModels.add(item.getValue(ChatModel.class));
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat,parent,false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
                final CustomViewHolder customViewHolder = (CustomViewHolder)holder;
                String destinationUid =null;
                //챗방에 있는 유저 체크
                for(String user: chatModels.get(position).users.keySet()){
                    if(!user.equals(uid)){
                        destinationUid = user;
                        destinationUsers.add(destinationUid);

                    }
                }

                FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        UserModel userModel = dataSnapshot.getValue(UserModel.class);
                        Glide.with(customViewHolder.itemView.getContext())
                                .load(userModel.profileImageUrl)
                                .apply(new RequestOptions().circleCrop())
                                .into(customViewHolder.imageView);

                        customViewHolder.textview_title.setText(userModel.userName);


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //메세지를 내림 차순으로 정렬 후 마지막 메세지의 키값을 가져옴
                Map<String,ChatModel.Comment> commentMap = new TreeMap<>(Collections.<String>reverseOrder());
                commentMap.putAll(chatModels.get(position).comments);
                String lastMessageKey = (String)commentMap.keySet().toArray()[0];
                customViewHolder.textview_last_message.setText(chatModels.get(position).comments.get(lastMessageKey).message);

                customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){
                        Intent intent = new Intent(getView().getContext(), MessageActivity.class);
                        intent.putExtra("destinationUid",destinationUsers.get(position));
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
                            ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(getView().getContext(), R.anim.fromright, R.anim.toleft);

                            startActivity(intent, activityOptions.toBundle());
                        }
                    }
                });

                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asaa/Seoul"));
                long unixTime = (long) chatModels.get(position).comments.get(lastMessageKey).timestamp;
                Date date = new Date(unixTime);
                customViewHolder.textview_timestamp.setText(simpleDateFormat.format(date));


        }



        @Override
        public int getItemCount() {
            return chatModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView textview_title;
            public TextView textview_last_message;
            public TextView textview_timestamp;

            public CustomViewHolder(View view) {
                super(view);

                imageView= view.findViewById(R.id.chatitem_imageview);
                textview_title = view.findViewById(R.id.chatitem_textview_title);
                textview_last_message = view.findViewById(R.id.chatitem_textview_lastMessage);
                textview_timestamp = view.findViewById(R.id.chatitem_textview_timestamp);
            }
        }
    }



}
