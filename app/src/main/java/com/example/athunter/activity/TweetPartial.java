package com.example.athunter.activity;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.athunter.R;

public class TweetPartial extends RecyclerView.ViewHolder {
    TextView tweetTextView;
    ImageView tweetImage;
    TextView tweetTime;

    TweetPartial(View v) {
        super(v);
        tweetTextView = itemView.findViewById(R.id.tweetText);
        tweetImage = itemView.findViewById(R.id.tweetImage);
        tweetTime = itemView.findViewById(R.id.tweetTime);
    }
}