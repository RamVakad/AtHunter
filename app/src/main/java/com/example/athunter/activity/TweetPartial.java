package com.example.athunter.activity;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.athunter.R;
import com.example.athunter.model.Comment;
import com.example.athunter.util.GeneralTools;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class TweetPartial extends RecyclerView.ViewHolder {
    TextView tweetTextView;
    ImageView tweetImage;
    TextView tweetTime;
    TextView numLikes;
    TextView likeButton;
    TextView numComments;
    ImageView fullScreenImgView;

    TweetPartial(View v) {
        super(v);
        tweetTextView = itemView.findViewById(R.id.tweetText);
        tweetImage = itemView.findViewById(R.id.tweetImage);
        tweetTime = itemView.findViewById(R.id.tweetTime);
        likeButton = itemView.findViewById(R.id.likeButton);
        numLikes = itemView.findViewById(R.id.numLikes);
        numComments = itemView.findViewById(R.id.numComments);

        tweetImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Clicked.");
                fullScreenImgView.setImageDrawable(tweetImage.getDrawable());
                fullScreenImgView.setVisibility(ImageView.VISIBLE);
            }
        });
    }

    public void setCommentsListener(final String key, final DatabaseReference tweetsRealtimeRef) {
        DatabaseReference comments = tweetsRealtimeRef.child(key).child("comments");
        comments.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long num = dataSnapshot.getChildrenCount();
                numComments.setText(num == null ? "0" : Integer.parseInt(num + "") + " replies");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Comments listener cancelled for reason: " + databaseError.getMessage());
            }
        });

    }

    public void setLikesListener(final String key, final DatabaseReference tweetsRealtimeRef) {
        DatabaseReference likes = tweetsRealtimeRef.child(key).child("likes");
        likes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long likes = dataSnapshot.getValue(Long.class);
                numLikes.setText(likes == null ? "0" : Integer.parseInt(likes + "") + "");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Likes listener cancelled for reason: " + databaseError.getMessage());
            }
        });

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tweetsRealtimeRef.child(key).child("likes").setValue(Integer.parseInt(numLikes.getText().toString()) + 1);
            }
        });

    }


}